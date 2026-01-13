package de.kolping.cockpit.mapping.core

import kotlinx.serialization.Serializable

@Serializable
data class RecordChain(
    val id: ChainId,
    val name: String,
    val rootNodeId: NodeId? = null,
    val nodeIds: List<NodeId> = emptyList(),
)

@Serializable
data class ChainPoint(
    val id: NodeId,
    val name: String,
    val url: String,
    val urlPattern: String? = null,
    val tags: List<String> = emptyList(),
    val signature: String = "",
    val isHub: Boolean = false,
)

@Serializable
data class ChainEdge(
    val id: EdgeId,
    val fromNodeId: NodeId,
    val toNodeId: NodeId,
    val createdBy: CreatedBy = CreatedBy.AUTO,
    val reason: EdgeReason = EdgeReason.DIRECT_NAV,
    val label: String? = null,
)

@Serializable
enum class CreatedBy { MANUAL, AUTO }

@Serializable
enum class EdgeReason { MANUAL_PARENT, HUB_MATCH, NAV_CLICK, CONTEXT_SWITCH, DIRECT_NAV }

@Serializable
data class RecordingSession(
    val id: SessionId,
    val chainId: ChainId? = null,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val targetUrl: String? = null,
    val filters: CaptureFilters = CaptureFilters(),
    val events: List<Event> = emptyList(),
    val calls: List<HttpCall> = emptyList(),
)

@Serializable
data class CaptureFilters(
    val hostAllowlist: List<String> = emptyList(),
    val contentTypeAllowlist: List<String> = listOf("application/json", "text/html"),
    val maxBodyBytes: Int = 256_000,
    val redact: Boolean = true,
)

@Serializable
sealed class Event {
    abstract val tsEpochMs: Long

    @Serializable
    @kotlinx.serialization.SerialName("navigation")
    data class Navigation(
        override val tsEpochMs: Long,
        val url: String,
        val phase: Phase,
    ) : Event()

    @Serializable
    @kotlinx.serialization.SerialName("network_request")
    data class NetworkRequest(
        override val tsEpochMs: Long,
        val callId: CallId,
        val method: String,
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val bodySnippet: String? = null,
    ) : Event()

    @Serializable
    @kotlinx.serialization.SerialName("network_response")
    data class NetworkResponse(
        override val tsEpochMs: Long,
        val callId: CallId,
        val status: Int,
        val headers: Map<String, String> = emptyMap(),
        val bodySnippet: String? = null,
        val contentType: String? = null,
    ) : Event()

    @Serializable
    @kotlinx.serialization.SerialName("click")
    data class Click(
        override val tsEpochMs: Long,
        val cssPath: String,
        val textSnippet: String? = null,
    ) : Event()

    @Serializable
    @kotlinx.serialization.SerialName("marker")
    data class Marker(
        override val tsEpochMs: Long,
        val name: String,
    ) : Event()

    @Serializable
    enum class Phase { STARTED, FINISHED }
}

@Serializable
data class HttpCall(
    val callId: CallId,
    val url: String,
    val method: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val status: Int? = null,
    val responseHeaders: Map<String, String> = emptyMap(),
    val responseBody: String? = null,
    val contentType: String? = null,
    val graphqlOperationName: String? = null,
    val moodleAjaxMethod: String? = null,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
)
