package net.ankio.qianji.auto

import android.content.Context
import com.google.gson.Gson
import net.ankio.auto.sdk.AutoWebsocket
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

class Websocket(override val context: Context) : AutoWebsocket(context) {
    private var ws: WebSocket? = null
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null

    override fun close(
        code: Int,
        reason: String,
    ) {
        if (webSocket == null) return
        webSocket!!.close(code, reason)
        ws = null
    }

    override fun onConnectedSuccess() {
        ws = webSocket
    }

    override fun isConnected(): Boolean {
        return ws != null
    }

    override fun jsonEncode(message: JSONObject): String {
        return Gson().toJson(message)
    }

    override fun jsonDecode(message: String): JSONObject {
        return Gson().fromJson(message, JSONObject::class.java)
    }

    override fun sendMsgToServer(msg: String) {
        if (ws == null)return
        ws!!.send(msg)
    }

    override fun connect(url: String) {
        val request =
            Request.Builder()
                .url(url)
                .build()
        val listener: WebSocketListener =
            object : WebSocketListener() {
                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response,
                ) {
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    text: String,
                ) {
                    onReceivedMessage(text)
                }

                override fun onMessage(
                    webSocket: WebSocket,
                    bytes: ByteString,
                ) {
                }

                override fun onClosing(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String,
                ) {
                    ws = null
                    webSocket.close(1000, null)
                }

                override fun onClosed(
                    webSocket: WebSocket,
                    code: Int,
                    reason: String,
                ) {
                    ws = null
                }

                override fun onFailure(
                    webSocket: WebSocket,
                    t: Throwable,
                    response: Response?,
                ) {
                    if (!t.message?.contains("unexpected end of stream")!!) {
                        return
                    }
                }
            }

        webSocket = client!!.newWebSocket(request, listener)
    }
}
