package de.kolping.cockpit.mapping.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ExportBundle {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    data class BundleContent(
        val mapJson: String,
        val chainsJson: Map<String, String>,
        val sessionsJson: Map<String, String>,
        val schemasJson: Map<String, String>
    )

    fun createBundle(
        graph: MapGraph,
        chains: List<RecordChain>,
        sessions: List<RecordingSession>
    ): BundleContent {
        val redactedSessions = sessions.map { Redaction.redactSession(it) }

        val mapJson = json.encodeToString(graph)
        val chainsJson = chains.associate { it.id to json.encodeToString(it) }
        val sessionsJson = redactedSessions.associate { it.id to json.encodeToString(it) }

        val schemasJson = mutableMapOf<String, String>()
        for (session in redactedSessions) {
            for (call in session.calls) {
                call.responseBody?.let { body ->
                    if (call.contentType?.contains("json") == true) {
                        val schemaResult = SchemaDeriver.deriveSchema(body)
                        schemaResult.getOrNull()?.let { schema ->
                            if (schema.isNotEmpty()) {
                                schemasJson[call.callId] = json.encodeToString(schema)
                            }
                        }
                    }
                }
            }
        }

        return BundleContent(mapJson, chainsJson, sessionsJson, schemasJson)
    }

    fun sessionJson(session: RecordingSession): String = json.encodeToString(Redaction.redactSession(session))
    fun chainJson(chain: RecordChain): String = json.encodeToString(chain)
    fun graphJson(graph: MapGraph): String = json.encodeToString(graph)
}
