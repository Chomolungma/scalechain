package io.scalechain.wallet

import java.io.File

import io.scalechain.blockchain.chain.ChainEventListener
import io.scalechain.blockchain.proto._
import io.scalechain.blockchain.transaction.SigHash.SigHash
import io.scalechain.blockchain.transaction.TransactionSigner.SignedTransaction
import io.scalechain.blockchain.transaction.{TransactionSigner, PrivateKey, UnspentTransactionOutput}

// [Wallet layer] A wallet keeps a list of private keys, and signs transactions using a private key, etc.
object Wallet extends ChainEventListener {
  // BUGBUG : Change the folder name without the "target" path.
  val walletFolder = new File("./target/wallet")

  val store = new WalletStore(walletFolder)

  /** signs a transaction.
    *
    * Used by : signrawtransaction RPC.
    *
    * @param transaction The transaction to sign.
    * @param dependencies  Unspent transaction output details. The previous outputs being spent by this transaction.
    * @param privateKeys An array holding private keys.
    * @param sigHash The type of signature hash to use for all of the signatures performed.
    * @return
    */
  def signTransaction(transaction   : Transaction,
                      dependencies  : List[UnspentTransactionOutput],
                      privateKeys   : Option[List[PrivateKey]],
                      sigHash       : SigHash
                     ) : SignedTransaction = {
    if (privateKeys.isEmpty) {
      // Wallet Store : Iterate private keys for all accounts
      val privateKeysFromWallet = store.getPrivateKeys.toList

      TransactionSigner.sign(transaction, dependencies, privateKeysFromWallet, sigHash )
    } else {
      TransactionSigner.sign(transaction, dependencies, privateKeys.get, sigHash )
    }
  }

  case class CoinAmount(value : scala.math.BigDecimal)

  /** Returns the total amount received by the specified address
    *
    * Used by : getreceivedbyaddress RPC.
    *
    * @param address
    * @param confirmations
    */
  def getReceivedByAddress(address : CoinAddress, confirmations : Long) : CoinAmount = {
    // Step 1 : Wallet Store : Iterate UTXOs for an output ownership.
    val total = store.getTransactionOutputs(Some(address)).foldLeft(scala.math.BigDecimal(0L)) {
      // Step 2 : Sum up the amount of UTXOs.
      (sum : scala.math.BigDecimal, utxo : UnspentCoinDescriptor) =>
        sum + utxo.amount
    }
    CoinAmount(total)
  }



  /** Returns the most recent transactions that affect the wallet.
    *
    * Used by : listtransactions RPC.
    *
    * @param account The name of an account to get transactinos from
    * @param count The number of the most recent transactions to list
    * @param skip The number of the most recent transactions which should not be returned.
    * @param includeWatchOnly If set to true, include watch-only addresses in details and calculations as if they were regular addresses belonging to the wallet.
    * @return List of transactions affecting the wallet.
    */
  def listTransactions(
                        account         : String,
                        count           : Int,
                        skip            : Long,
                        includeWatchOnly: Boolean
                      ) : List[TransactionDescriptor] = {
    // TODO : Implement
    assert(false)
    null
    // Wallet Store : Iterate transactions by providing an account and the skip count.
    /*
    store.getTransactions(account, skip, includeWatchOnly).map { transaction : Transaction =>

    }*/
    //   Stop the iteration after getting 'count' transactions.
  }


  /** Returns an array of unspent transaction outputs belonging to this wallet.
    *
    * Used by : listunspent RPC.
    *
    * @param minimumConfirmations The minimum number of confirmations the transaction containing an output must have in order to be returned.
    * @param maximumConfirmations The maximum number of confirmations the transaction containing an output may have in order to be returned.
    * @param addressesOption If present, only outputs which pay an address in this array will be returned.
    * @return a list of objects each describing an unspent output
    */
  def listUnspent(
                   minimumConfirmations: Long,
                   maximumConfirmations: Long,
                   addressesOption     : Option[List[String]]
                 ) : List[UnspentCoinDescriptor] = {
    // TODO : Implement

    if (addressesOption.isEmpty) {
      // Step A.1 : Wallet Store : Iterate UTXOs for all output ownerships. Filter UTXOs based on confirmations.
    } else {
      // Step B.1 : Wallet Store : Iterate UTXOs for a given addresses. Filter UTXOs based on confirmations.
    }

    assert(false)
    null
  }

  /** Import a watch-only address into the wallet.
    *
    * @param outputOwnership The ownership object that describes an ownership of a coin.
    * @param account The account name
    * @param rescanBlockchain Whether to rescan whole blockchain to re-index unspent transaction outputs(coins)
    */
  def importOutputOwnership(
    account : String,
    outputOwnership : OutputOwnership,
    rescanBlockchain : Boolean
  ): Unit = {

    // Step 1 : Wallet Store : Create an account if it does not exist.

    // Step 2 : Wallet Store : Add an output ownership to an account.
    
    // Step 3 : Rescan blockchain
    if (rescanBlockchain) {
      // Step 4 : Wallet Store : Put transactions into the output ownership.
      // Step 5 : Wallet Store : Put UTXOs into the output ownership.
    }

    // TODO : Implement
    assert(false)
  }

  /** Find an account by coin address.
    *
    * Used by : getaccount RPC.
    *
    * @param address The coin address, which is attached to the account.
    * @return The found account.
    */
  def getAccount(address : CoinAddress) : String = {
    // TODO : Implement

    // Wallet Store : Get an account by a coin address.

    assert(false)
    null
  }

  /** Returns a new coin address for receiving payments.
    *
    * Used by : newaddress RPC.
    *
    * @return the new address for receiving payments.
    */
  def newAddress(account: String) : CoinAddress = {
    // TODO : Implement

    // Step 1 : Generate a random number for a private key.

    // Step 2 : Create a public key.

    // Step 3 : Hash the public key.

    // Step 4 : Create an address.

    // Step 5 : Wallet Store : Put an address into an account.

    // Step 6 : Wallet Store : Put the private key into an the address.

    assert(false)
    null
  }


  /** Returns the current address for receiving payments to this account.
    *
    * Used by : getaccountaddress RPC.
    *
    * @return The coin address for receiving payments.
    */
  def getReceivingAddress(account:String) : CoinAddress = {
    // TODO : Implement

    // Wallet Store : Get the receiving address of an account.

    assert(false)
    null
  }


  /** Called whenever a new transaction comes into a block or the mempool.
    *
    * @param transaction The newly found transaction.
    * @see ChainEventListener
    */
  def onNewTransaction(transaction : Transaction): Unit = {
    // TODO : Implement

    // Wallet Store : Iterate for each output ownerships
    { // loop
      // if the output ownership receives or spends UTXOs related to the transaction.
      /*if (...)*/ {
        // call registerNewTransaction
      }
    }

    assert(false)
  }

  protected[wallet] def registerNewTransaction(ownership: OutputOwnership, transaction : Transaction): Unit = {
    // TODO : Implement

    // Step 1 : Wallet Store : Put a transaction into the output ownership.


    transaction.outputs.map { transactionOutput =>
      if (ownership.owns(transactionOutput)) {
        // Step 2 : Wallet Store : Put a UTXO into the output ownership.
      }
    }

    transaction.inputs.map { transactionInput =>
      // Step 3 : Block Store : Get the transaction output the input is spending.
      // val spentOutput : TransactionOutput = ...

      // Step 4 : Wallet Store : Mark a UTXO spent searching by OutPoint.
      // spentOutput
    }

    assert(false)
  }


  protected[wallet] def unregisterNewTransaction(ownership: OutputOwnership, transaction : Transaction): Unit = {
    // TODO : Implement

    // Step 1 : Wallet Store : Remove the transaction from the output ownership by transaction hash

    transaction.outputs.map { transactionOutput =>
      if (ownership.owns(transactionOutput)) {
        // Step 2 : Wallet Store : Remove a UTXO from the output ownership.
      }
    }

    assert(false)
  }


  /** Called whenever a new transaction is removed from the mempool.
    * This also means the transaction does not exist in any block, as the mempool has transactions
    * that are not in any block in the best block chain.
    *
    * @param transaction The transaction removed from the mempool.
    * @see ChainEventListener
    */
  def onRemoveTransaction(transaction : Transaction): Unit = {
    // TODO : Implement

    // Wallet Store : iterate for each output ownerships
    {
      // loop
      // if the output ownership receives or spends UTXOs related to the transaction.

      /*if (...)*/ {
        // Wallet Store : Remove the transaction from the output ownership by transaction hash
      }
    }
    assert(false)
  }
}