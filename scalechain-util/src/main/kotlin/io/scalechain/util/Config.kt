package io.scalechain.util

import java.io.File

import com.typesafe.config.ConfigFactory
import io.scalechain.util.GlobalEnvironemnt.ScaleChainHome

data class PeerAddress(val address : String, val port : Int)

open class Config(val config : com.typesafe.config.Config) {
    fun hasPath(path : String) = config.hasPath(path)
    fun getInt(path: String) = config.getInt(path)
    fun getString(path: String) = config.getString(path)
    fun getConfiglistOf(path : String) = config.getConfigList(path)
    var privateCache : Boolean? = null

    fun isPrivate() : Boolean {
        fun getIsPrivate() : Boolean {
            val configValue =
                if ( config.hasPath("scalechain.private") ) {
                    true
                } else {
                    false
                }
            privateCache = configValue
            return configValue
        }
        return privateCache ?: getIsPrivate()
    }

    fun peerAddresses() : List<PeerAddress> {
        return getConfiglistOf("scalechain.p2p.peers").map { peer ->
            PeerAddress(peer.getString("address"), peer.getInt("port"))
        }
    }

    // BUGBUG : Need to change to config/scalechain.conf before the integration tests.
    companion object {
        val MAX_BLOCK_SIZE = 1024 * 1024; // The maxium block size is hard coded as 1M.
        val theConfig : Config = Config(ConfigFactory.parseFile( File(ScaleChainHome + "config/scalechain.conf")))
        fun get() : Config {
            return theConfig
        }
    }
}

