package de.kolping.cockpit.mapping.core

import kotlinx.serialization.json.*

object Redaction {
    private val sensitiveHeaderKeys = setOf("authorization", "cookie", "set-cookie")
    private val sensitiveJsonKeys = setOf(
        "access_token", "refresh_token", "id_token", "code", "state", "nonce", "session_state"
    )
    private val sensitiveQueryParams = setOf("code", "state", "nonce", "session_state")

    fun redactHeaders(headers: Map<String, String>): Map<String, String> =
        headers.mapValues { (k, v) -> if (k.lowercase() in sensitiveHeaderKeys) "<redacted>" else v }

    /**
     * Redacts sensitive keys from JSON using proper JSON parsing.
     * This ensures escaped quotes and nested structures are handled correctly.
     */
    fun redactJsonKeys(jsonString: String): String {
        return try {
            val element = Json.parseToJsonElement(jsonString)
            val redacted = redactJsonElement(element)
            Json.encodeToString(JsonElement.serializer(), redacted)
        } catch (e: Exception) {
            // Fallback: return original if not valid JSON
            jsonString
        }
    }

    private fun redactJsonElement(element: JsonElement): JsonElement {
        return when (element) {
            is JsonObject -> {
                val redactedMap = element.mapValues { (key, value) ->
                    if (key in sensitiveJsonKeys) {
                        JsonPrimitive("<redacted>")
                    } else {
                        redactJsonElement(value)
                    }
                }
                JsonObject(redactedMap)
            }
            is JsonArray -> {
                JsonArray(element.map { redactJsonElement(it) })
            }
            else -> element
        }
    }

    fun redactUrlQuery(url: String): String {
        val qIdx = url.indexOf('?')
        if (qIdx < 0) return url
        val base = url.substring(0, qIdx)
        val query = url.substring(qIdx + 1)
        val redactedParams = query.split('&').joinToString("&") { param ->
            val eqIdx = param.indexOf('=')
            if (eqIdx < 0) param
            else {
                val key = param.substring(0, eqIdx)
                if (key in sensitiveQueryParams) "$key=<redacted>" else param
            }
        }
        return "$base?$redactedParams"
    }

    fun redactSession(session: RecordingSession): RecordingSession {
        if (!session.filters.redact) return session
        return session.copy(
            events = session.events.map { event ->
                when (event) {
                    is Event.NetworkRequest -> event.copy(
                        headers = redactHeaders(event.headers),
                        bodySnippet = event.bodySnippet?.let { redactJsonKeys(it) },
                        url = redactUrlQuery(event.url)
                    )
                    is Event.NetworkResponse -> event.copy(
                        headers = redactHeaders(event.headers),
                        bodySnippet = event.bodySnippet?.let { redactJsonKeys(it) }
                    )
                    is Event.Navigation -> event.copy(url = redactUrlQuery(event.url))
                    else -> event
                }
            },
            calls = session.calls.map { call ->
                call.copy(
                    requestHeaders = redactHeaders(call.requestHeaders),
                    responseHeaders = redactHeaders(call.responseHeaders),
                    requestBody = call.requestBody?.let { redactJsonKeys(it) },
                    responseBody = call.responseBody?.let { redactJsonKeys(it) },
                    url = redactUrlQuery(call.url)
                )
            }
        )
    }
}
