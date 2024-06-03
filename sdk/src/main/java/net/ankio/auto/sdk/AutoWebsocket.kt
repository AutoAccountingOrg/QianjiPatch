package net.ankio.auto.sdk

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.util.UUID
import kotlin.coroutines.resume

abstract class AutoWebsocket(open val context: Context) {
    private val callbacks: HashMap<String, (json: JSONObject) -> Unit> = HashMap()

    fun getToken(): String {
        val sp = context.getSharedPreferences("AutoAccountingConfig", Context.MODE_PRIVATE)
        return sp.getString("token", "") ?: ""
    }

    fun setToken(token: String) {
        with(context.getSharedPreferences("AutoAccountingConfig", Context.MODE_PRIVATE).edit()) {
            putString("token", token)
            apply()
        }
    }

    fun onReceivedMessage(message: String) {
        val messageJson = jsonDecode(message)
        when (messageJson.getString("type")) {
            "auth" -> {
                val token = getToken()
                if (token.isNotEmpty()) {
                    val msg =
                        JSONObject().apply {
                            put("type", "auth")
                            put("data", token.trim())
                            put("id", "")
                        }
                    sendMsgToServer(jsonEncode(msg))
                } else {
                    close(1000, "Token not found")
                }
            }

            "auth/success" -> {
                onConnectedSuccess()
            }

            else -> {
                val id = messageJson.getString("id")
                callbacks[id]?.invoke(messageJson)
                callbacks.remove(id)
            }
        }
    }

    abstract fun close(
        code: Int,
        reason: String,
    )

    abstract fun onConnectedSuccess()

    abstract fun isConnected(): Boolean

    abstract fun jsonEncode(message: JSONObject): String

    abstract fun jsonDecode(message: String): JSONObject

    abstract fun sendMsgToServer(msg: String)

    suspend fun sendMsg(
        type: String,
        data: Any? = null,
    ): Any? =
        suspendCancellableCoroutine { continuation ->
            if (!isConnected()) {
                //  Logger.d("WebSocket未连接")
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            val json = JSONObject()
            val id = UUID.randomUUID().toString()
            json.put("id", id)
            json.put("type", type)
            json.put("data", data)
            callbacks[id] = { it: JSONObject ->
                continuation.resume(it.get("data"))
            }

            sendMsgToServer(jsonEncode(json))
            // 等待返回
        }

    abstract fun connect(url: String)
}
