package de.kolping.cockpit.mapping.android

import android.util.Log
import android.webkit.JavascriptInterface
import de.kolping.cockpit.mapping.core.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class JsBridge(
    private val onNetworkRequest: (Event.NetworkRequest) -> Unit,
    private val onNetworkResponse: (Event.NetworkResponse) -> Unit,
    private val onClick: (Event.Click) -> Unit
) {
    private val json = Json { ignoreUnknownKeys = true }

    @JavascriptInterface
    fun postMessage(message: String) {
        try {
            val obj = json.parseToJsonElement(message).jsonObject
            val type = obj["type"]?.jsonPrimitive?.content ?: return
            val ts = System.currentTimeMillis()

            when (type) {
                "NET_REQ" -> {
                    val event = Event.NetworkRequest(
                        tsEpochMs = ts,
                        callId = obj["callId"]?.jsonPrimitive?.content ?: Ids.callId(),
                        method = obj["method"]?.jsonPrimitive?.content ?: "GET",
                        url = obj["url"]?.jsonPrimitive?.content ?: "",
                        bodySnippet = obj["body"]?.jsonPrimitive?.content?.take(1000)
                    )
                    onNetworkRequest(event)
                }
                "NET_RES" -> {
                    val event = Event.NetworkResponse(
                        tsEpochMs = ts,
                        callId = obj["callId"]?.jsonPrimitive?.content ?: "",
                        status = obj["status"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        bodySnippet = obj["body"]?.jsonPrimitive?.content?.take(10000),
                        contentType = obj["contentType"]?.jsonPrimitive?.content
                    )
                    onNetworkResponse(event)
                }
                "CLICK" -> {
                    val event = Event.Click(
                        tsEpochMs = ts,
                        cssPath = obj["cssPath"]?.jsonPrimitive?.content ?: "",
                        textSnippet = obj["text"]?.jsonPrimitive?.content?.take(100)
                    )
                    onClick(event)
                }
            }
        } catch (e: Exception) {
            Log.w("JsBridge", "Failed to parse JS message: ${e.message}", e)
        }
    }
}
