package de.kolping.cockpit.android.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing downloaded file metadata.
 * Tracks local file storage for PDFs, documents, etc.
 */
@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey
    val fileId: String,
    val moduleId: String,
    val courseId: Int? = null,
    val fileName: String,
    val filePath: String,  // Local file path
    val fileUrl: String,   // Original URL
    val fileType: String,  // pdf, doc, etc.
    val sizeBytes: Long,
    val downloadedAt: Long
)
