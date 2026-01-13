package de.kolping.cockpit.mapping.core

import kotlinx.serialization.Serializable

@Serializable
data class MapGraph(
    private val nodeMap: MutableMap<NodeId, ChainPoint> = mutableMapOf(),
    private val edgeMap: MutableMap<EdgeId, ChainEdge> = mutableMapOf(),
    val metadata: GraphMetadata = GraphMetadata()
) {
    val nodes: List<ChainPoint> get() = nodeMap.values.toList()
    val edges: List<ChainEdge> get() = edgeMap.values.toList()

    fun upsertNode(node: ChainPoint): ChainPoint {
        nodeMap[node.id] = node
        return node
    }

    fun upsertEdge(edge: ChainEdge): ChainEdge {
        edgeMap[edge.id] = edge
        return edge
    }

    fun findNode(id: NodeId): ChainPoint? = nodeMap[id]

    /**
     * Finds all hub ancestors by traversing edges backward from the given node.
     * Uses a visited set to handle potential cycles in the graph. While cycles should
     * ideally be prevented at edge creation time, this defensive approach ensures the
     * traversal terminates even if the graph contains unexpected cycles.
     */
    fun findHubAncestors(nodeId: NodeId): List<ChainPoint> {
        val result = mutableListOf<ChainPoint>()
        var currentId: NodeId? = nodeId
        val visited = mutableSetOf<NodeId>()
        while (currentId != null && currentId !in visited) {
            visited.add(currentId)
            val node = findNode(currentId)
            if (node?.isHub == true) result.add(node)
            val parentEdge = edges.find { it.toNodeId == currentId }
            currentId = parentEdge?.fromNodeId
        }
        return result
    }
}

@Serializable
data class GraphMetadata(
    val generatedAt: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
    val appVersion: String = "0.1.0",
    val filtersUsed: CaptureFilters = CaptureFilters()
)
