package de.kolping.cockpit.android.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility object for file-related operations.
 * Centralizes common file formatting logic to avoid duplication across ViewModels.
 * 
 * Review Finding Fix: Extracted from ModuleDetailViewModel and OfflineLibraryViewModel
 * to eliminate code duplication.
 */
object FileUtils {
    
    /**
     * File type categories for grouping and icon selection
     */
    enum class FileTypeCategory {
        PDF,
        DOCUMENT,
        SPREADSHEET,
        PRESENTATION,
        IMAGE,
        VIDEO,
        AUDIO,
        ARCHIVE,
        OTHER
    }
    
    /**
     * Thread-safe cached DateFormat instances.
     * Review Finding Fix: Using ThreadLocal instead of creating new instances per call.
     */
    private val dateTimeFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)
    }
    
    private val dateFormat: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
        SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    }
    
    /**
     * Format file size in human-readable format
     */
    fun formatFileSize(sizeBytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        
        return when {
            sizeBytes >= gb -> String.format(Locale.US, "%.2f GB", sizeBytes / gb)
            sizeBytes >= mb -> String.format(Locale.US, "%.2f MB", sizeBytes / mb)
            sizeBytes >= kb -> String.format(Locale.US, "%.2f KB", sizeBytes / kb)
            else -> "$sizeBytes B"
        }
    }
    
    /**
     * Format timestamp in human-readable date format (with time)
     */
    fun formatDownloadDate(timestamp: Long): String {
        return dateTimeFormat.get()?.format(Date(timestamp)) ?: ""
    }
    
    /**
     * Format timestamp as relative time or absolute date
     */
    fun formatRelativeDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000
        
        return when {
            diff < 60_000 -> "gerade eben"
            minutes < 60 -> "vor $minutes Min."
            hours < 24 -> "vor $hours Std."
            days < 7 -> "vor $days Tag${if (days > 1) "en" else ""}"
            else -> dateFormat.get()?.format(Date(timestamp)) ?: ""
        }
    }
    
    /**
     * Get file type category based on file extension.
     * Comprehensive list of supported file types.
     */
    fun getFileTypeCategory(fileType: String): FileTypeCategory {
        return when (fileType.lowercase()) {
            "pdf" -> FileTypeCategory.PDF
            "doc", "docx", "odt", "txt", "rtf" -> FileTypeCategory.DOCUMENT
            "xls", "xlsx", "ods", "csv" -> FileTypeCategory.SPREADSHEET
            "ppt", "pptx", "odp" -> FileTypeCategory.PRESENTATION
            "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp" -> FileTypeCategory.IMAGE
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" -> FileTypeCategory.VIDEO
            "mp3", "wav", "ogg", "m4a", "flac", "aac" -> FileTypeCategory.AUDIO
            "zip", "rar", "7z", "tar", "gz", "bz2" -> FileTypeCategory.ARCHIVE
            else -> FileTypeCategory.OTHER
        }
    }
}

// Top-level convenience functions that delegate to FileUtils object
// for backward compatibility

/**
 * @see FileUtils.FileTypeCategory
 */
typealias FileTypeCategory = FileUtils.FileTypeCategory

/**
 * @see FileUtils.getFileTypeCategory
 */
fun getFileTypeCategory(fileType: String): FileTypeCategory = FileUtils.getFileTypeCategory(fileType)

/**
 * @see FileUtils.formatFileSize
 */
fun formatFileSize(sizeBytes: Long): String = FileUtils.formatFileSize(sizeBytes)

/**
 * @see FileUtils.formatDownloadDate
 */
fun formatDownloadDate(timestamp: Long): String = FileUtils.formatDownloadDate(timestamp)
