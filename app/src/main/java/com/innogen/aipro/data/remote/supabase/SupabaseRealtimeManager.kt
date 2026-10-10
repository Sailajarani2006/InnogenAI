package com.innogen.aipro.data.remote.supabase

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseRealtimeManager @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var isConnected = false

    var onProjectUpserted: ((SupabaseProjectDto) -> Unit)? = null
    var onProjectDeleted: ((String) -> Unit)? = null

    fun connect() {
        if (isConnected && webSocket != null) return

        val request = Request.Builder()
            .url(SupabaseConfig.WEBSOCKET_URL)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                isConnected = true
                Log.d("SupabaseRealtime", "Connected to Supabase Realtime WebSocket")

                // Join channel with postgres_changes listener
                val joinMsg = """
                {
                    "topic": "realtime:public:projects",
                    "event": "phx_join",
                    "payload": {
                        "config": {
                            "broadcast": { "self": true },
                            "presence": { "key": "" },
                            "postgres_changes": [
                                { "event": "*", "schema": "public", "table": "projects" }
                            ]
                        }
                    },
                    "ref": "join_projects"
                }
                """.trimIndent()
                ws.send(joinMsg)

                startHeartbeat(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                Log.w("SupabaseRealtime", "WebSocket connection issue: ${t.message}")
                heartbeatJob?.cancel()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                isConnected = false
                heartbeatJob?.cancel()
            }
        })
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JsonParser.parseString(text).asJsonObject
            val event = json.get("event")?.asString ?: return

            if (event == "postgres_changes") {
                val payload = json.getAsJsonObject("payload") ?: return
                val data = payload.getAsJsonObject("data") ?: return
                val changeType = data.get("type")?.asString ?: return

                when (changeType) {
                    "INSERT", "UPDATE" -> {
                        val record = data.getAsJsonObject("record")
                        if (record != null) {
                            val dto = gson.fromJson(record, SupabaseProjectDto::class.java)
                            onProjectUpserted?.invoke(dto)
                        }
                    }
                    "DELETE" -> {
                        val oldRecord = data.getAsJsonObject("old_record")
                        val id = oldRecord?.get("id")?.asString
                        if (!id.isNullOrBlank()) {
                            onProjectDeleted?.invoke(id)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseRealtime", "Failed to parse message: ${e.message}")
        }
    }

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isConnected) {
                delay(30_000L)
                val hb = """{"topic":"phoenix","event":"heartbeat","payload":{},"ref":"hb"}"""
                ws.send(hb)
            }
        }
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Normal Closure")
        webSocket = null
        isConnected = false
    }
}
