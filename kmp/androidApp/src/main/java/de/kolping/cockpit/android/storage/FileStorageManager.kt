package de.kolping.cockpit.android.storage

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Manages local file storage for downloaded content.
 * Creates and maintains the folder structure for modules, courses, and calendar data.
 */
class FileStorageManager(private val context: Context) {
    
    private val syncDir: File by lazy {
        File(context.filesDir, "sync").also { it.mkdirs() }
    }
    
    /**
     * Get the root sync directory
     */
    fun getSyncDirectory(): File = syncDir
    
    /**
     * Get directory for student data
     */
    fun getStudentDirectory(): File {
        return File(syncDir, "student").also { it.mkdirs() }
    }
    
    /**
     * Get directory for modules data
     */
    fun getModulesDirectory(): File {
        return File(syncDir, "modules").also { it.mkdirs() }
    }
    
    /**
     * Get directory for a specific module
     */
    fun getModuleDirectory(moduleId: String): File {
        return File(getModulesDirectory(), moduleId).also { it.mkdirs() }
    }
    
    /**
     * Get directory for module files
     */
    fun getModuleFilesDirectory(moduleId: String): File {
        return File(getModuleDirectory(moduleId), "files").also { it.mkdirs() }
    }
    
    /**
     * Get directory for courses data
     */
    fun getCoursesDirectory(): File {
        return File(syncDir, "courses").also { it.mkdirs() }
    }
    
    /**
     * Get directory for a specific course
     */
    fun getCourseDirectory(courseId: Int): File {
        return File(getCoursesDirectory(), courseId.toString()).also { it.mkdirs() }
    }
    
    /**
     * Get directory for calendar data
     */
    fun getCalendarDirectory(): File {
        return File(syncDir, "calendar").also { it.mkdirs() }
    }
    
    /**
     * Get directory for assignments data
     */
    fun getAssignmentsDirectory(): File {
        return File(syncDir, "assignments").also { it.mkdirs() }
    }
    
    /**
     * Save file to module files directory
     * @return File object pointing to the saved file
     */
    fun saveModuleFile(moduleId: String, fileName: String, inputStream: InputStream): File {
        val targetFile = File(getModuleFilesDirectory(moduleId), fileName)
        targetFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return targetFile
    }
    
    /**
     * Save file to a specific directory
     * @return File object pointing to the saved file
     */
    fun saveFile(directory: File, fileName: String, inputStream: InputStream): File {
        directory.mkdirs()
        val targetFile = File(directory, fileName)
        targetFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return targetFile
    }
    
    /**
     * Delete file if it exists
     * @return true if file was deleted, false otherwise
     */
    fun deleteFile(file: File): Boolean {
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
    
    /**
     * Delete all files in module directory
     */
    fun deleteModuleFiles(moduleId: String) {
        getModuleDirectory(moduleId).deleteRecursively()
    }
    
    /**
     * Delete all files in course directory
     */
    fun deleteCourseFiles(courseId: Int) {
        getCourseDirectory(courseId).deleteRecursively()
    }
    
    /**
     * Clear all sync data
     */
    fun clearAllData() {
        syncDir.deleteRecursively()
        syncDir.mkdirs()
    }
    
    /**
     * Get total size of sync directory in bytes
     */
    fun getTotalSize(): Long {
        return calculateDirectorySize(syncDir)
    }
    
    /**
     * Get size of a directory recursively
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.exists()) {
            directory.walk().forEach { file ->
                if (file.isFile) {
                    size += file.length()
                }
            }
        }
        return size
    }
    
    /**
     * Check if file exists
     */
    fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }
    
    /**
     * Get file by path
     */
    fun getFile(filePath: String): File? {
        val file = File(filePath)
        return if (file.exists()) file else null
    }
}
