package de.kolping.cockpit.android.sync

/**
 * Result of a synchronization operation
 * Contains information about what was synced and any errors
 */
sealed class SyncResult {
    /**
     * Sync completed successfully
     */
    data class Success(
        val modulesCount: Int = 0,
        val coursesCount: Int = 0,
        val filesDownloaded: Int = 0,
        val eventsCount: Int = 0,
        val bytesDownloaded: Long = 0L,
        val durationMs: Long = 0L,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncResult()
    
    /**
     * Sync failed with an error
     */
    data class Failure(
        val error: Exception,
        val phase: SyncPhase,
        val message: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncResult()
    
    /**
     * Sync was cancelled by user
     */
    data class Cancelled(
        val phase: SyncPhase,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncResult()
}
