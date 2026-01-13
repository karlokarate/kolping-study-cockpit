package de.kolping.cockpit.mapping.core

import kotlinx.serialization.Serializable

@Serializable
data class MapGraph(
    val nodes: MutableList<ChainPoint> = mutableListOf(),
    val edges: MutableList<ChainEdge> = mutableListOf(),
    val metadata: GraphMetadata = GraphMetadata()
) {
    fun upsertNode(node: ChainPoint): ChainPoint {
        val existing = nodes.find { it.id == node.id }
        if (existing != null) {
            nodes.remove(existing)
        }
        nodes.add(node)
        return node
    }

    fun upsertEdge(edge: ChainEdge): ChainEdge {
        val existing = edges.find { it.id == edge.id }
        if (existing != null) {
            edges.remove(existing)
        }
        edges.add(edge)
        return edge
    }

    fun findNode(id: NodeId): ChainPoint? = nodes.find { it.id == id }

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
