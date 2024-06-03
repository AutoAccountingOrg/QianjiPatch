package net.ankio.auto.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoAccounting {
    private val url = "ws://127.0.0.1:52045"
    private var autoWebsocket: AutoWebsocket? = null

    fun init(autoWebsocket: AutoWebsocket) {
        autoWebsocket.connect(url)
    }

    suspend fun getWaitSyncBills(): Any? =
        withContext(Dispatchers.IO) {
            autoWebsocket?.sendMsg("bill/sync/list", null)
        }

    suspend fun setBillSynced(billId: String) =
        withContext(Dispatchers.IO) {
            autoWebsocket?.sendMsg("bill/sync/update", hashMapOf("id" to billId))
        }

    suspend fun setAsset(
        assetId: Int,
        assetName: String,
        type: Int,
        sort: Int,
        icon: String,
        extra: String,
    ) = withContext(Dispatchers.IO) {
        autoWebsocket?.sendMsg(
            "asset/put",
            hashMapOf(
                "id" to assetId,
                "name" to assetName,
                "type" to type,
                "sort" to sort,
                "icon" to icon,
                "extra" to extra,
            ),
        )
    }

    suspend fun removeAsset(assetId: Int) =
        withContext(Dispatchers.IO) {
            autoWebsocket?.sendMsg("asset/remove", hashMapOf("id" to assetId))
        }

    fun setToken(token: String) {
        autoWebsocket?.setToken(token)
    }

    fun isConnected(): Boolean {
        return autoWebsocket?.isConnected() ?: false
    }

    suspend fun setConfig(configText: String?) =
        withContext(Dispatchers.IO) {
            autoWebsocket?.sendMsg("setting/set", hashMapOf("app" to "server", "key" to "config", "value" to configText))
        }

    suspend fun getHash(key: String): String =
        withContext(Dispatchers.IO) {
            autoWebsocket?.sendMsg("setting/get", hashMapOf("app" to "server", "key" to key)) as String
        }

    suspend fun setBooks(
        sync: String,
        md5: String,
    ) = withContext(Dispatchers.IO) {
        autoWebsocket?.sendMsg("book/sync", hashMapOf("data" to sync, "md5" to md5))
    }

    suspend fun setAssets(
        sync: String,
        md5: String,
    ) = withContext(Dispatchers.IO) {
        autoWebsocket?.sendMsg("assets/sync", hashMapOf("data" to sync, "md5" to md5))
    }

    suspend fun setBills(sync: String) =
        withContext(Dispatchers.IO) {
            autoWebsocket?.sendMsg("app/bill/add", hashMapOf("bills" to sync))
        }

    suspend fun getBills(): Any? =
        withContext(Dispatchers.IO) {
            autoWebsocket?.sendMsg("bill/sync/list", null)
        }
}
