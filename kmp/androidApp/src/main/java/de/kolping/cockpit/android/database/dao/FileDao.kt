package de.kolping.cockpit.android.database.dao

import androidx.room.*
import de.kolping.cockpit.android.database.entities.FileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for File entities.
 * Provides methods for CRUD operations on downloaded files.
 */
@Dao
interface FileDao {
    @Query("SELECT * FROM files ORDER BY downloadedAt DESC")
    fun getAllFiles(): Flow<List<FileEntity>>
    
    @Query("SELECT * FROM files WHERE moduleId = :moduleId ORDER BY fileName ASC")
    fun getFilesByModule(moduleId: String): Flow<List<FileEntity>>
    
    @Query("SELECT * FROM files WHERE courseId = :courseId ORDER BY fileName ASC")
    fun getFilesByCourse(courseId: Int): Flow<List<FileEntity>>
    
    @Query("SELECT * FROM files WHERE fileId = :fileId")
    suspend fun getFileById(fileId: String): FileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileEntity>)
    
    @Update
    suspend fun updateFile(file: FileEntity)
    
    @Delete
    suspend fun deleteFile(file: FileEntity)
    
    @Query("DELETE FROM files WHERE moduleId = :moduleId")
    suspend fun deleteFilesByModule(moduleId: String)
    
    @Query("DELETE FROM files")
    suspend fun deleteAllFiles()
}
