package de.kolping.cockpit.android.sync

/**
 * Progress information for ongoing sync operation
 * Used to update UI with current sync status
 */
data class SyncProgress(
    /** Current phase of synchronization */
    val phase: SyncPhase,
    
    /** Name of the item currently being processed */
    val currentItem: String = "",
    
    /** Overall progress as a fraction (0.0 to 1.0) */
    val progress: Float = 0f,
    
    /** Number of files downloaded so far */
    val filesDownloaded: Int = 0,
    
    /** Total number of files to download */
    val totalFiles: Int = 0,
    
    /** Number of bytes downloaded so far */
    val bytesDownloaded: Long = 0L,
    
    /** Total bytes to download (0 if unknown) */
    val totalBytes: Long = 0L,
    
    /** Optional message for additional context */
    val message: String? = null
) {
    /**
     * Calculate file download progress percentage
     */
    val fileProgress: Float
        get() = if (totalFiles > 0) filesDownloaded.toFloat() / totalFiles else 0f
    
    /**
     * Calculate byte download progress percentage
     */
    val byteProgress: Float
        get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
}
