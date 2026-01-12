package de.kolping.cockpit.android.util

/**
 * File type categories for UI grouping and icons
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
 * Get file type category for UI grouping
 */
fun getFileTypeCategory(fileType: String): FileTypeCategory {
    return when (fileType.lowercase()) {
        "pdf" -> FileTypeCategory.PDF
        "doc", "docx" -> FileTypeCategory.DOCUMENT
        "xls", "xlsx" -> FileTypeCategory.SPREADSHEET
        "ppt", "pptx" -> FileTypeCategory.PRESENTATION
        "jpg", "jpeg", "png", "gif" -> FileTypeCategory.IMAGE
        "mp4", "avi", "mov" -> FileTypeCategory.VIDEO
        "mp3", "wav" -> FileTypeCategory.AUDIO
        "zip", "rar", "7z" -> FileTypeCategory.ARCHIVE
        else -> FileTypeCategory.OTHER
    }
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
 * Format download date to human-readable string
 */
fun formatDownloadDate(timestamp: Long): String {
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
        else -> {
            val sdf = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.GERMAN)
            sdf.format(java.util.Date(timestamp))
        }
    }
}
