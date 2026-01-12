package de.kolping.cockpit.android.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kolping.cockpit.android.database.entities.FileEntity
import de.kolping.cockpit.android.repository.OfflineRepository
import de.kolping.cockpit.android.storage.FileStorageManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    
    private val _uiState = MutableStateFlow<OfflineLibraryUiState>(OfflineLibraryUiState.Loading)
    val uiState: StateFlow<OfflineLibraryUiState> = _uiState.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()
    
    init {
        loadFiles()
    }
    
    /**
     * Load all files from offline database
     */
    private fun loadFiles() {
        viewModelScope.launch {
            try {
                _uiState.value = OfflineLibraryUiState.Loading
                
                offlineRepository.getAllFiles().collect { files ->
                    val filteredFiles = applySearchAndSort(files)
                    val storageInfo = getStorageInfo(files)
                    
                    _uiState.value = OfflineLibraryUiState.Success(
                        allFiles = filteredFiles,
                        storageInfo = storageInfo
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load files", e)
                _uiState.value = OfflineLibraryUiState.Error("Fehler beim Laden: ${e.message}")
            }
        }
    }
    
    /**
     * Update search query
     */
    fun search(query: String) {
        _searchQuery.value = query
        
        val currentState = _uiState.value
        if (currentState is OfflineLibraryUiState.Success) {
            viewModelScope.launch {
                offlineRepository.getAllFiles().collect { files ->
                    val filteredFiles = applySearchAndSort(files)
                    _uiState.value = currentState.copy(allFiles = filteredFiles)
                }
            }
        }
    }
    
    /**
     * Change sort order
     */
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        
        val currentState = _uiState.value
        if (currentState is OfflineLibraryUiState.Success) {
            viewModelScope.launch {
                offlineRepository.getAllFiles().collect { files ->
                    val filteredFiles = applySearchAndSort(files)
                    _uiState.value = currentState.copy(allFiles = filteredFiles)
                }
            }
        }
    }
    
    /**
     * Clear search query
     */
    fun clearSearch() {
        search("")
    }
    
    /**
     * Refresh file list
     */
    fun refresh() {
        loadFiles()
    }
    
    /**
     * Apply search filter and sorting to files list
     */
    private fun applySearchAndSort(files: List<FileEntity>): List<FileEntity> {
        var result = files
        
        // Apply search filter
        val query = _searchQuery.value.trim()
        if (query.isNotEmpty()) {
            result = result.filter { file ->
                file.fileName.contains(query, ignoreCase = true) ||
                file.fileType.contains(query, ignoreCase = true)
            }
        }
        
        // Apply sorting
        result = when (_sortOrder.value) {
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
     * Format file size to human-readable string
     */
    fun formatFileSize(sizeBytes: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024
        
        return when {
            sizeBytes >= gb -> String.format("%.2f GB", sizeBytes.toDouble() / gb)
            sizeBytes >= mb -> String.format("%.2f MB", sizeBytes.toDouble() / mb)
            sizeBytes >= kb -> String.format("%.2f KB", sizeBytes.toDouble() / kb)
            else -> "$sizeBytes Bytes"
        }
    }
    
    /**
     * Format download date
     */
    fun formatDownloadDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000
        
        return when {
            minutes < 60 -> "vor $minutes Min."
            hours < 24 -> "vor $hours Std."
            days < 7 -> "vor $days Tag${if (days > 1) "en" else ""}"
            else -> {
                val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.GERMAN)
                sdf.format(java.util.Date(timestamp))
            }
        }
    }
    
    /**
     * Get file type category for UI grouping
     */
    fun getFileTypeCategory(fileType: String): ModuleDetailViewModel.FileTypeCategory {
        return when (fileType.lowercase()) {
            "pdf" -> ModuleDetailViewModel.FileTypeCategory.PDF
            "doc", "docx" -> ModuleDetailViewModel.FileTypeCategory.DOCUMENT
            "xls", "xlsx" -> ModuleDetailViewModel.FileTypeCategory.SPREADSHEET
            "ppt", "pptx" -> ModuleDetailViewModel.FileTypeCategory.PRESENTATION
            "jpg", "jpeg", "png", "gif" -> ModuleDetailViewModel.FileTypeCategory.IMAGE
            "mp4", "avi", "mov" -> ModuleDetailViewModel.FileTypeCategory.VIDEO
            "mp3", "wav" -> ModuleDetailViewModel.FileTypeCategory.AUDIO
            "zip", "rar", "7z" -> ModuleDetailViewModel.FileTypeCategory.ARCHIVE
            else -> ModuleDetailViewModel.FileTypeCategory.OTHER
        }
    }
    
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
