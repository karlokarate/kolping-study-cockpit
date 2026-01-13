package de.kolping.cockpit.mapping.core

import kotlinx.serialization.Serializable

/**
 * Represents a sequence of web page interactions forming a navigation chain.
 * @property id Unique identifier for the chain
 * @property name Human-readable name describing the chain
 * @property rootNodeId Optional ID of the starting node/page in the chain
 * @property nodeIds List of all node IDs that make up this chain
 */
@Serializable
data class RecordChain(
    val id: ChainId,
    val name: String,
    val rootNodeId: NodeId? = null,
    val nodeIds: List<NodeId> = emptyList(),
)

/**
 * Represents a single point/page in a navigation chain.
 * @property id Unique identifier for this point
 * @property name Human-readable name for this page/point
 * @property url The actual URL of this page
 * @property urlPattern Optional pattern for matching similar URLs
 * @property tags Categorization tags for this point
 * @property signature Computed signature for deduplication based on URL and API calls
 * @property isHub Whether this point acts as a hub (common navigation point)
 */
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

/**
 * Represents a directional connection between two points in a navigation chain.
 * @property id Unique identifier for this edge
 * @property fromNodeId Source node ID
 * @property toNodeId Destination node ID
 * @property createdBy Whether this edge was created manually or automatically
 * @property reason The reasoning/mechanism that created this edge
 * @property label Optional descriptive label for the edge
 */
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

/**
 * Represents a recording session capturing user interactions and network activity.
 * @property id Unique identifier for this session
 * @property chainId Optional ID linking this session to a specific chain
 * @property startedAtEpochMs Session start timestamp in milliseconds
 * @property endedAtEpochMs Optional session end timestamp
 * @property targetUrl Optional target URL being recorded
 * @property filters Capture filters applied during this session
 * @property events List of captured events (navigation, clicks, network)
 * @property calls List of captured HTTP calls with full request/response data
 */
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

/**
 * Configuration for what data to capture during recording.
 * @property hostAllowlist List of host patterns to capture (empty = all)
 * @property contentTypeAllowlist Content types to capture response bodies for
 * @property maxBodyBytes Maximum bytes to capture from request/response bodies
 * @property redact Whether to redact sensitive data like tokens
 */
@Serializable
data class CaptureFilters(
    val hostAllowlist: List<String> = emptyList(),
    val contentTypeAllowlist: List<String> = listOf("application/json", "text/html"),
    val maxBodyBytes: Int = 256_000,
    val redact: Boolean = true,
)

/**
 * Base class for all capturable events during a recording session.
 */
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

/**
 * Represents a complete HTTP call with request and response data.
 */
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
