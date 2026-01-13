package de.kolping.cockpit.mapping.core

class AutoMappingEngine {
    private val hubPatterns = listOf("/my/", "/myStudent", "/dashboard", "/course/view.php")
    private val learnedHubSignatures = mutableSetOf<String>()

    fun detectHub(url: String, signature: String): Boolean {
        if (hubPatterns.any { url.contains(it, ignoreCase = true) }) return true
        return signature in learnedHubSignatures
    }

    fun markAsHub(signature: String) {
        learnedHubSignatures.add(signature)
    }

    data class ParentInference(
        val parentNodeId: NodeId?,
        val reason: EdgeReason
    )

    fun inferParentNode(
        lastNodeId: NodeId?,
        knownHubs: List<ChainPoint>,
        newNodeIsHub: Boolean,
        lastClickWasNav: Boolean,
        contextSwitch: Boolean,
        manualParentId: NodeId? = null
    ): ParentInference {
        if (manualParentId != null) {
            return ParentInference(manualParentId, EdgeReason.MANUAL_PARENT)
        }
        if (newNodeIsHub) {
            return ParentInference(null, EdgeReason.HUB_MATCH)
        }
        val closestHub = knownHubs.firstOrNull()
        if (lastClickWasNav && closestHub != null) {
            return ParentInference(closestHub.id, EdgeReason.NAV_CLICK)
        }
        if (contextSwitch && closestHub != null) {
            return ParentInference(closestHub.id, EdgeReason.CONTEXT_SWITCH)
        }
        return ParentInference(lastNodeId, EdgeReason.DIRECT_NAV)
    }

    fun updateGraph(
        graph: MapGraph,
        session: RecordingSession,
        chain: RecordChain
    ): MapGraph {
        var lastNodeId: NodeId? = chain.rootNodeId
        val graphqlOps = mutableListOf<String>()
        val ajaxMethods = mutableListOf<String>()

        for (event in session.events) {
            when (event) {
                is Event.Navigation -> {
                    if (event.phase == Event.Phase.FINISHED) {
                        val sig = Signature.computeNodeSignature(event.url, graphqlOps, ajaxMethods)
                        val isHub = detectHub(event.url, sig)
                        val existingNode = graph.nodes.find { it.signature == sig }

                        val nodeId = existingNode?.id ?: Ids.nodeId()
                        if (existingNode == null) {
                            val node = ChainPoint(
                                id = nodeId,
                                name = extractPageName(event.url),
                                url = event.url,
                                signature = sig,
                                isHub = isHub
                            )
                            graph.upsertNode(node)
                        }

                        if (lastNodeId != null && lastNodeId != nodeId) {
                            val hubs = graph.findHubAncestors(lastNodeId)
                            val inference = inferParentNode(lastNodeId, hubs, isHub, false, false)
                            if (inference.parentNodeId != null) {
                                val edge = ChainEdge(
                                    id = Ids.edgeId(),
                                    fromNodeId = inference.parentNodeId,
                                    toNodeId = nodeId,
                                    reason = inference.reason
                                )
                                graph.upsertEdge(edge)
                            }
                        }
                        lastNodeId = nodeId
                        graphqlOps.clear()
                        ajaxMethods.clear()
                    }
                }
                is Event.NetworkResponse -> {
                    event.contentType?.let {
                        if (it.contains("json")) {
                            Signature.extractGraphqlOperationName(event.bodySnippet)?.let { op -> graphqlOps.add(op) }
                            Signature.extractMoodleAjaxMethod(event.bodySnippet)?.let { m -> ajaxMethods.add(m) }
                        }
                    }
                }
                else -> {}
            }
        }
        return graph
    }

    private fun extractPageName(url: String): String {
        val path = url.substringAfter("://").substringBefore("?").substringAfter("/")
        return path.split("/").lastOrNull()?.ifBlank { "root" } ?: "root"
    }
}
