package de.kolping.cockpit.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kolping.cockpit.android.database.entities.FileEntity
import de.kolping.cockpit.android.repository.OfflineRepository
import de.kolping.cockpit.android.storage.FileStorageManager
import de.kolping.cockpit.android.util.FileUtils
import kotlinx.coroutines.flow.*

/**
 * ViewModel for OfflineLibraryScreen
 * Manages browsing of all downloaded files with search functionality
 * Based on Issue #6 PR 5 requirements
 */
class OfflineLibraryViewModel(
    private val offlineRepository: OfflineRepository,
    private val fileStorageManager: FileStorageManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "OfflineLibraryViewModel"
    }
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()
    
    /**
     * UI state using combine to properly manage Flow lifecycle and apply search/sort
     */
    val uiState: StateFlow<OfflineLibraryUiState> = combine(
        offlineRepository.getAllFiles(),
        _searchQuery,
        _sortOrder
    ) { files, query, order ->
        try {
            val filteredFiles = applySearchAndSort(files, query, order)
            val storageInfo = getStorageInfo(files)
            
            OfflineLibraryUiState.Success(
                allFiles = filteredFiles,
                storageInfo = storageInfo
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process files", e)
            OfflineLibraryUiState.Error("Fehler beim Laden der Offline-Bibliothek: ${e.message}")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OfflineLibraryUiState.Loading
    )
    
    /**
     * Update search query
     */
    fun search(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Change sort order
     */
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }
    
    /**
     * Clear search query
     */
    fun clearSearch() {
        search("")
    }
    
    /**
     * Apply search filter and sorting to files list
     */
    private fun applySearchAndSort(files: List<FileEntity>, query: String, order: SortOrder): List<FileEntity> {
        var result = files
        
        // Apply search filter
        val trimmedQuery = query.trim()
        if (trimmedQuery.isNotEmpty()) {
            result = result.filter { file ->
                file.fileName.contains(trimmedQuery, ignoreCase = true) ||
                file.fileType.contains(trimmedQuery, ignoreCase = true)
            }
        }
        
        // Apply sorting
        result = when (order) {
            SortOrder.NAME_ASC -> result.sortedBy { it.fileName.lowercase() }
            SortOrder.NAME_DESC -> result.sortedByDescending { it.fileName.lowercase() }
            SortOrder.DATE_ASC -> result.sortedBy { it.downloadedAt }
            SortOrder.DATE_DESC -> result.sortedByDescending { it.downloadedAt }
            SortOrder.SIZE_ASC -> result.sortedBy { it.sizeBytes }
            SortOrder.SIZE_DESC -> result.sortedByDescending { it.sizeBytes }
            SortOrder.TYPE_ASC -> result.sortedBy { it.fileType.lowercase() }
            SortOrder.TYPE_DESC -> result.sortedByDescending { it.fileType.lowercase() }
        }
        
        return result
    }
    
    /**
     * Get storage information
     */
    private fun getStorageInfo(files: List<FileEntity>): StorageInfo {
        val totalSize = fileStorageManager.getTotalSize()
        val fileCount = files.size
        val totalFileSize = files.sumOf { it.sizeBytes }
        
        // Group by file type
        val fileTypeBreakdown = files.groupBy { it.fileType }
            .mapValues { (_, files) -> files.sumOf { it.sizeBytes } }
            .toList()
            .sortedByDescending { it.second }
        
        return StorageInfo(
            totalSizeBytes = totalSize,
            totalFileSize = totalFileSize,
            fileCount = fileCount,
            fileTypeBreakdown = fileTypeBreakdown
        )
    }
    
    /**
     * Refresh offline library
     */
    fun refresh() {
        // Trigger a refresh by re-collecting the flow
        // The flow will automatically re-emit when the database changes
        // This is a no-op since we're using reactive flows
    }
    
    /**
     * Format file size in human-readable format.
     * Review Finding Fix: Delegates to shared FileUtils to avoid code duplication.
     */
    fun formatFileSize(sizeBytes: Long): String = FileUtils.formatFileSize(sizeBytes)
    
    /**
     * Format download date in human-readable format.
     * Review Finding Fix: Delegates to shared FileUtils with thread-safe DateFormat.
     */
    fun formatDownloadDate(timestamp: Long): String = FileUtils.formatDownloadDate(timestamp)
    
    /**
     * Get file type category based on file extension.
     * Review Finding Fix: Delegates to shared FileUtils instead of cross-referencing ModuleDetailViewModel.
     */
    fun getFileTypeCategory(fileType: String): FileUtils.FileTypeCategory = FileUtils.getFileTypeCategory(fileType)
    
    /**
     * UI State for OfflineLibraryScreen
     */
    sealed class OfflineLibraryUiState {
        object Loading : OfflineLibraryUiState()
        data class Success(
            val allFiles: List<FileEntity>,
            val storageInfo: StorageInfo
        ) : OfflineLibraryUiState()
        data class Error(val message: String) : OfflineLibraryUiState()
    }
    
    /**
     * Storage information
     */
    data class StorageInfo(
        val totalSizeBytes: Long,
        val totalFileSize: Long,
        val fileCount: Int,
        val fileTypeBreakdown: List<Pair<String, Long>>
    )
    
    /**
     * Sort order options
     */
    enum class SortOrder(val displayName: String) {
        NAME_ASC("Name (A-Z)"),
        NAME_DESC("Name (Z-A)"),
        DATE_ASC("Datum (älteste zuerst)"),
        DATE_DESC("Datum (neueste zuerst)"),
        SIZE_ASC("Größe (kleinste zuerst)"),
        SIZE_DESC("Größe (größte zuerst)"),
        TYPE_ASC("Typ (A-Z)"),
        TYPE_DESC("Typ (Z-A)")
    }
}
