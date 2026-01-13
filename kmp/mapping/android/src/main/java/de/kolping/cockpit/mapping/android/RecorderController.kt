package de.kolping.cockpit.mapping.android

import de.kolping.cockpit.mapping.core.*

class RecorderController {
    private var currentChain: RecordChain? = null
    private var currentSession: RecordingSession? = null
    private var mapGraph = MapGraph()
    private val chains = mutableListOf<RecordChain>()
    private val sessions = mutableListOf<RecordingSession>()
    private val events = mutableListOf<Event>()
    private val calls = mutableListOf<HttpCall>()
    private val engine = AutoMappingEngine()

    val isRecording: Boolean get() = currentSession != null
    val currentChainName: String? get() = currentChain?.name
    val eventCount: Int get() = events.size
    val currentUrl: String get() = events.filterIsInstance<Event.Navigation>().lastOrNull()?.url ?: ""

    fun createChain(name: String): RecordChain {
        val chain = RecordChain(id = Ids.chainId(), name = name)
        chains.add(chain)
        currentChain = chain
        return chain
    }

    fun startSession(targetUrl: String? = null, filters: CaptureFilters = CaptureFilters()): RecordingSession {
        val session = RecordingSession(
            id = Ids.sessionId(),
            chainId = currentChain?.id,
            startedAtEpochMs = System.currentTimeMillis(),
            targetUrl = targetUrl,
            filters = filters
        )
        currentSession = session
        events.clear()
        calls.clear()
        return session
    }

    fun stopSession(): RecordingSession? {
        val session = currentSession?.copy(
            endedAtEpochMs = System.currentTimeMillis(),
            events = events.toList(),
            calls = calls.toList()
        ) ?: return null
        
        sessions.add(session)
        currentChain?.let { chain ->
            mapGraph = engine.updateGraph(mapGraph, session, chain)
        }
        currentSession = null
        return session
    }

    fun addNavigationEvent(url: String, phase: Event.Phase) {
        if (currentSession == null) return
        events.add(Event.Navigation(
            tsEpochMs = System.currentTimeMillis(),
            url = url,
            phase = phase
        ))
    }

    fun addNetworkRequest(event: Event.NetworkRequest) {
        if (currentSession == null) return
        events.add(event)
        calls.add(HttpCall(
            callId = event.callId,
            url = event.url,
            method = event.method,
            requestHeaders = event.headers,
            requestBody = event.bodySnippet,
            startedAtEpochMs = event.tsEpochMs
        ))
    }

    fun addNetworkResponse(event: Event.NetworkResponse) {
        if (currentSession == null) return
        events.add(event)
        val idx = calls.indexOfFirst { it.callId == event.callId }
        if (idx >= 0) {
            calls[idx] = calls[idx].copy(
                status = event.status,
                responseBody = event.bodySnippet,
                contentType = event.contentType,
                completedAtEpochMs = event.tsEpochMs
            )
        }
    }

    fun addClickEvent(event: Event.Click) {
        if (currentSession == null) return
        events.add(event)
    }

    fun saveTargetUrl(url: String) {
        currentSession = currentSession?.copy(targetUrl = url)
    }

    fun addChainPoint(name: String, url: String, parentNodeId: NodeId? = null): ChainPoint {
        val sig = Signature.computeNodeSignature(url, emptyList(), emptyList())
        val isHub = engine.detectHub(url, sig)
        val node = ChainPoint(
            id = Ids.nodeId(),
            name = name,
            url = url,
            signature = sig,
            isHub = isHub
        )
        mapGraph.upsertNode(node)
        
        if (parentNodeId != null) {
            val edge = ChainEdge(
                id = Ids.edgeId(),
                fromNodeId = parentNodeId,
                toNodeId = node.id,
                createdBy = CreatedBy.MANUAL,
                reason = EdgeReason.MANUAL_PARENT
            )
            mapGraph.upsertEdge(edge)
        }
        
        currentChain = currentChain?.let { chain ->
            chain.copy(nodeIds = chain.nodeIds + node.id)
        }
        return node
    }

    fun getChainPoints(): List<ChainPoint> = mapGraph.nodes.toList()

    fun export(): ExportBundle.BundleContent {
        return ExportBundle.createBundle(mapGraph, chains, sessions)
    }
}
