package de.kolping.cockpit.android.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing downloaded file metadata.
 * Tracks local file storage for PDFs, documents, etc.
 * 
 * Note: Files can belong to either a module or a course (or both).
 * At least one of moduleId or courseId should be set.
 * 
 * The moduleId field references ModuleEntity.modulId (note: API uses German 'modulId').
 * We use 'moduleId' here as it's a foreign key reference and follows standard naming.
 */
@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey
    val fileId: String,
    val moduleId: String? = null,  // References ModuleEntity.modulId
    val courseId: Int? = null,
    val fileName: String,
    val filePath: String,  // Local file path
    val fileUrl: String,   // Original URL
    val fileType: String,  // pdf, doc, etc.
    val sizeBytes: Long,
    val downloadedAt: Long
)
