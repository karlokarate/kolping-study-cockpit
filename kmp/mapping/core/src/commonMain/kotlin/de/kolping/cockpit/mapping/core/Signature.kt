package de.kolping.cockpit.mapping.core

import kotlinx.serialization.json.*

object Signature {
    private val volatileParams = setOf("code", "state", "nonce", "session_state", "t", "_")

    fun normalizeUrl(url: String): String {
        val idx = url.indexOf('#')
        val noFragment = if (idx >= 0) url.substring(0, idx) else url
        val qIdx = noFragment.indexOf('?')
        if (qIdx < 0) return noFragment
        val base = noFragment.substring(0, qIdx)
        val query = noFragment.substring(qIdx + 1)
        val params = query.split('&')
            .filter { it.isNotBlank() }
            .mapNotNull { p ->
                val eqIdx = p.indexOf('=')
                if (eqIdx < 0) null else p.substring(0, eqIdx) to p.substring(eqIdx + 1)
            }
            .filterNot { it.first in volatileParams }
            .sortedBy { it.first }
            .joinToString("&") { "${it.first}=${it.second}" }
        return if (params.isEmpty()) base else "$base?$params"
    }

    fun computeNodeSignature(url: String, graphqlOps: List<String>, ajaxMethods: List<String>): String {
        val normalized = normalizeUrl(url)
        val opsHash = graphqlOps.sorted().joinToString(",")
        val ajaxHash = ajaxMethods.sorted().joinToString(",")
        return "url:$normalized|gql:$opsHash|ajax:$ajaxHash"
    }

    fun computeCallSignature(url: String, method: String, contentType: String?, opName: String?, ajaxMethod: String?): String {
        val normalized = normalizeUrl(url)
        return "call:$method:$normalized|ct:${contentType.orEmpty()}|op:${opName.orEmpty()}|ajax:${ajaxMethod.orEmpty()}"
    }

    fun extractGraphqlOperationName(body: String?): String? {
        if (body == null) return null
        return try {
            val json = Json.parseToJsonElement(body)
            json.jsonObject["operationName"]?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            null // Fallback for malformed JSON
        }
    }

    fun extractMoodleAjaxMethod(body: String?): String? {
        if (body == null) return null
        return try {
            val json = Json.parseToJsonElement(body)
            json.jsonObject["methodname"]?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            null // Fallback for malformed JSON
        }
    }
}
