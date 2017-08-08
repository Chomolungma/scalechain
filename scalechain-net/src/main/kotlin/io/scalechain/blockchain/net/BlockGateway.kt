package io.scalechain.blockchain.net

import java.util.concurrent.TimeUnit

import bftsmart.tom.ServiceProxy
import io.scalechain.blockchain.chain.Blockchain
import io.scalechain.blockchain.chain.processor.BlockProcessor
import io.scalechain.blockchain.proto.codec.BlockHeaderCodec
import io.scalechain.blockchain.proto.Hash
import io.scalechain.blockchain.proto.Block
import io.scalechain.blockchain.proto.BlockHeader
import io.scalechain.blockchain.script.hash
import io.scalechain.util.*
import org.slf4j.LoggerFactory

class BlockBroadcaster( nodeIndex : Int, configHome : String ) {
  val bftProxy : ServiceProxy = ServiceProxy(nodeIndex, configHome)

  /**
    * Try to make a header a consensual header that agrees other nodes as well.
    *
    * @param header
    */
  fun broadcastHeader(header : BlockHeader) {
    val rawBlockHeader : ByteArray = BlockHeaderCodec.encode( header )
    bftProxy.invokeOrdered(rawBlockHeader)
  }

  companion object {
    var theBlockBroadcaster : BlockBroadcaster? = null
    var theBlockConsensusServer : BlockConsensusServer? = null

    fun create(nodeIndex : Int) : BlockBroadcaster {
      if (theBlockBroadcaster == null) {
        val configHome = GlobalEnvironemnt.ScaleChainHome + "config"
        theBlockBroadcaster = BlockBroadcaster(nodeIndex, configHome)
        assert( theBlockConsensusServer == null )
        theBlockConsensusServer = BlockConsensusServer(nodeIndex, configHome)
      }
      return theBlockBroadcaster!!
    }

    fun get() : BlockBroadcaster {
      assert(theBlockBroadcaster != null)
      assert(theBlockConsensusServer != null)
      return theBlockBroadcaster!!
    }
  }
}

object PeerIndexCalculator {

  tailrec fun getPeerIndexInternal(p2pPort : Int, index : Int, peers : List<PeerAddress>) : Int? {
    if (peers.isEmpty()) { // base case
      return null
    } else {
      val localAddresses = NetUtil.getLocalAddresses()
      val peerAddress = peers.first()
      if ( localAddresses.contains(peerAddress.address) && peerAddress.port == p2pPort) { // Found a match.
        return index
      } else {
        return getPeerIndexInternal(p2pPort, index + 1, peers.drop(1))
      }
    }
  }

  /**
    * Return the peer index of the peers array in the scalechain.conf file.
    * For eaxmple, in case we have the following list of peers,
    * and the node ip address is 127.0.0.1 with p2p port 7645,
    * the peer index is 2
    *
    *   p2p {
    *     port = 7643
    *     peers = [
    *       { address:"127.0.0.1", port:"7643" }, # index 0
    *       { address:"127.0.0.1", port:"7644" }, # index 1
    *       { address:"127.0.0.1", port:"7645" }, # index 2
    *       { address:"127.0.0.1", port:"7646" }, # index 3
    *       { address:"127.0.0.1", port:"7647" }  # index 4
    *     ]
    *   }
    *
    * @return The peer index from <0, peer count-1)
    */
  fun getPeerIndex(p2pPort : Int) : Int? {
    val peerAddresses : List<PeerAddress> = Config.get().peerAddresses()
    return getPeerIndexInternal(p2pPort, 0, peerAddresses)
  }
}

/**
  * Created by kangmo on 7/18/16.
  */
open class BlockGateway {
  private val logger = LoggerFactory.getLogger(BlockGateway::class.java)

  val ConsensualBlockHeaderCache = TimeBasedCache<BlockHeader>(5, TimeUnit.MINUTES)

  val ReceivedBlockCache = TimeBasedCache<Block>(5, TimeUnit.MINUTES)

  fun putConsensualHeader(header : BlockHeader) {
    synchronized(this) {
      val chain = Blockchain.get()

      // BUGBUG : A fork can happen if the block comes too late. (The consensual header in ConsensualBlockHeaderCache expires)
      val consensualHeader = ConsensualBlockHeaderCache.get(header.hashPrevBlock)
      if (consensualHeader != null) {
        // We already have a consensual header. We only accept the first consensual block header.
        // The all consensual block headers with the same previous block hash of the first one are discarded.
      } else {
        // New consensual header.
        ConsensualBlockHeaderCache.put(header.hashPrevBlock, header)

        // Check if we have a received block that matches the header.
        val blockHash = header.hash()
        val receivedBlock = ReceivedBlockCache.get(blockHash)
        if (receivedBlock != null) {
          val currentBestBlockHash = chain.getBestBlockHash(chain.db)
          if ( currentBestBlockHash!! == receivedBlock.header.hashPrevBlock) {
            //            logger.trace("Received block found while putting consensual header.")
            val acceptedAsNewBestBlock = BlockProcessor.get().acceptBlock(blockHash, receivedBlock)
            assert(acceptedAsNewBestBlock == true)
            // Recursively process orphan blocks depending on the newly added block
            BlockProcessor.get().acceptChildren(blockHash)

            // We reached at consensus. No need to download blocks using IBD.
            if (Node.get().isInitialBlockDownload()) {
              logger.info("Reached at consensus by consensual header, Stopping IBD. Block Hash : ${blockHash}")
              Node.get().stopInitialBlockDownload()
            }
          } else {
            // TODO : BUGBUG : Need to check if no sibling exists on the blockchain.
            // put the block as an orphan block
            logger.trace("Current best block. Hash : ${currentBestBlockHash}")
            logger.trace("Added orphan block. Hash : ${blockHash}")
            BlockProcessor.get().putOrphan(receivedBlock)
          }
        } else {
//            logger.trace("Received block was NOT found while putting consensual header.")
        }
      }
    }
  }

  fun putReceivedBlock(blockHash : Hash, block : Block) : Unit {
    synchronized(this) {
      // Put into the received block cache.
      if (ReceivedBlockCache.get(blockHash) == null) {
        ReceivedBlockCache.put(blockHash, block)
      }

      val chain = Blockchain.get()

      val consensualBlockHeader = ConsensualBlockHeaderCache.get(block.header.hashPrevBlock)
      if ( consensualBlockHeader != null ) {
//            logger.trace("Found a consensual header on the previous block.")
        if (consensualBlockHeader == block.header) {
          val currentBestBlockHash = chain.getBestBlockHash(chain.db)
          if ( currentBestBlockHash == block.header.hashPrevBlock) {
            // logger.trace(s"The received block is on top of the best block. Hash : ${blockHash}")
            // logger.trace("The received block matches the consensual header.")
            // Already reached consensus.
            val acceptedAsNewBestBlock = BlockProcessor.get().acceptBlock(blockHash, block)
            assert(acceptedAsNewBestBlock == true)
            // TODO : Would be great if we can remove the ReceivedBlockCache
            // Recursively process orphan blocks depending on the newly added block
            BlockProcessor.get().acceptChildren(blockHash)

            // We reached at consensus. No need to download blocks using IBD.
            if (Node.get().isInitialBlockDownload()) {
              logger.info("Reached at consensus by received block, Stopping IBD. Block Hash : ${blockHash}")
              Node.get().stopInitialBlockDownload()
            }
          } else {
            // TODO : BUGBUG : Need to check if no sibling exists on the blockchain.
            // put the block as an orphan block
            logger.trace("Added orphan block. Hash : ${blockHash}")
            BlockProcessor.get().putOrphan(block)
          }
        } else {
//              logger.trace(s"The received block does not match the consensual header. consensual : ${consensualBlockHeader.get}, received : ${block.header}")
        }
      } else {
//            logger.trace("No consensual header was found for the received block.")
      }
    }
  }

  /**
    * Put a block directly into the local block chain.
    *
    * @param blockHash
    * @param block
    */
  fun putBlock(blockHash : Hash, block : Block) : Boolean {
    synchronized(this) {
      assert( Node.get().isInitialBlockDownload() )

      val chain = Blockchain.get()

      val currentBestBlockHash = chain.getBestBlockHash(chain.db)
      if ( currentBestBlockHash == block.header.hashPrevBlock) {
        val acceptedAsNewBestBlock = BlockProcessor.get().acceptBlock(blockHash, block)
        assert(acceptedAsNewBestBlock == true)
        // TODO : Would be great if we can remove the ReceivedBlockCache
        // Recursively process orphan blocks depending on the newly added block.
        val acceptedChildHashes = BlockProcessor.get().acceptChildren(blockHash)

        // An orphan block is one that received the consensual header, but did not have the parent of it.
        // If we accepted any orphan block, it means we reached at consensus. No need to do IBD any more.
        if (acceptedChildHashes.isNotEmpty()) {
          logger.info("Reached at consensus accepting children, which received consensual headers, Stopping IBD. Parent Block Hash : ${blockHash}")
          Node.get().stopInitialBlockDownload()
        }

        return true
      } else {
        // Ignore the block. We should not put the block as an orphan block, as orphan blocks are
        // ones that received the consensual header.

        // Actually this case should not happen, as during the IBD, we should receive blocks in order.
        return false
      }
    }
  }
  companion object : BlockGateway() {}
}
