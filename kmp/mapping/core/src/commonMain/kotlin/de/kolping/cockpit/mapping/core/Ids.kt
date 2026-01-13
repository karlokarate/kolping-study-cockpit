package de.kolping.cockpit.mapping.core

import kotlin.random.Random

typealias ChainId = String
typealias NodeId = String
typealias EdgeId = String
typealias SessionId = String
typealias CallId = String

/**
 * ID generation utility for creating unique identifiers within the application.
 * 
 * Note: Uses kotlin.random.Random without explicit seeding, combined with timestamps.
 * While collisions are unlikely due to timestamp inclusion, IDs are not globally unique
 * across different app instances. This is acceptable for local session tracking but should
 * not be relied upon for distributed systems.
 */
object Ids {
    private val chars = "abcdefghijklmnopqrstuvwxyz0123456789"

    fun newId(prefix: String = ""): String {
        val ts = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val rand = (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return if (prefix.isNotEmpty()) "${prefix}_${ts}_$rand" else "${ts}_$rand"
    }

    fun chainId(): ChainId = newId("chain")
    fun nodeId(): NodeId = newId("node")
    fun edgeId(): EdgeId = newId("edge")
    fun sessionId(): SessionId = newId("sess")
    fun callId(): CallId = newId("call")
}
