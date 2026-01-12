package de.kolping.cockpit.android.storage

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.IOException
import java.io.SecurityException

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
     * Validates moduleId to prevent path traversal
     */
    fun getModuleDirectory(moduleId: String): File {
        validateId(moduleId, "moduleId")
        return File(getModulesDirectory(), moduleId).also { it.mkdirs() }
    }
    
    /**
     * Get directory for module files
     * Validates moduleId to prevent path traversal
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
     * Validates courseId to prevent path traversal
     */
    fun getCourseDirectory(courseId: Int): File {
        val courseIdStr = courseId.toString()
        validateId(courseIdStr, "courseId")
        return File(getCoursesDirectory(), courseIdStr).also { it.mkdirs() }
    }
    
    /**
     * Validate ID to prevent path traversal attacks
     * Uses whitelist approach: only allows alphanumeric characters, hyphens, and underscores
     * This automatically prevents path traversal, control characters, and null bytes
     * @param id The identifier to validate
     * @param idName The name of the identifier for error messages
     * @throws IllegalArgumentException if the ID contains invalid characters
     */
    private fun validateId(id: String, idName: String) {
        // Whitelist approach: only allow alphanumeric, hyphens, and underscores
        // This inherently prevents: path traversal (.., /, \), null bytes, and control characters
        if (!id.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            throw IllegalArgumentException("Invalid $idName: only alphanumeric characters, hyphens, and underscores are allowed")
        }
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
     * Delegates to saveFile for consistent path validation
     * @return File object pointing to the saved file
     */
    fun saveModuleFile(moduleId: String, fileName: String, inputStream: InputStream): File {
        return saveFile(getModuleFilesDirectory(moduleId), fileName, inputStream)
    }
    
    /**
     * Save file to a specific directory
     * Performs canonical path validation to prevent path traversal via fileName.
     * Note: This method performs I/O operations for canonical path resolution.
     * Consider caching results if called frequently for the same directory.
     * @return File object pointing to the saved file
     * @throws IllegalArgumentException if fileName contains path traversal sequences
     * @throws IOException if canonical path resolution fails
     */
    fun saveFile(directory: File, fileName: String, inputStream: InputStream): File {
        try {
            // Ensure the target directory exists
            directory.mkdirs()

            // Resolve canonical paths (expensive I/O operations)
            val canonicalDir = directory.canonicalFile
            val targetFile = File(canonicalDir, fileName)
            val canonicalTarget = targetFile.canonicalFile

            // Verify that the canonical target path is still within the canonical directory
            // Use explicit ignoreCase = false for consistent behavior across platforms
            val canonicalDirPath = canonicalDir.path + File.separator
            if (!canonicalTarget.path.startsWith(canonicalDirPath, ignoreCase = false)) {
                throw IllegalArgumentException("Invalid file name: path traversal is not allowed")
            }

            // Write to the validated canonical target
            canonicalTarget.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            return canonicalTarget
        } catch (e: IOException) {
            android.util.Log.e("FileStorageManager", "IOException saving file: ${e.message}")
            throw e
        } catch (e: SecurityException) {
            android.util.Log.e("FileStorageManager", "SecurityException saving file: ${e.message}")
            throw e
        }
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
     * Check if file exists within sync directory
     * @param filePath Relative path within sync directory
     * @return true if file exists and is within sync directory, false otherwise
     */
    fun fileExists(filePath: String): Boolean {
        return try {
            val file = File(syncDir, filePath)
            val canonicalSyncPath = syncDir.canonicalPath + File.separator
            file.exists() && file.canonicalPath.startsWith(canonicalSyncPath, ignoreCase = false)
        } catch (e: IOException) {
            // Log and return false if canonical path resolution fails
            android.util.Log.w("FileStorageManager", "IOException checking file existence: ${e.message}")
            false
        } catch (e: SecurityException) {
            // Log and return false if security manager denies access
            android.util.Log.w("FileStorageManager", "SecurityException checking file existence: ${e.message}")
            false
        }
    }
    
    /**
     * Get file by relative path within sync directory
     * @param filePath Relative path within sync directory
     * @return File object if it exists and is within sync directory, null otherwise
     */
    fun getFile(filePath: String): File? {
        return try {
            val file = File(syncDir, filePath)
            val canonicalSyncPath = syncDir.canonicalPath + File.separator
            if (file.exists() && file.canonicalPath.startsWith(canonicalSyncPath, ignoreCase = false)) {
                file
            } else {
                null
            }
        } catch (e: IOException) {
            // Log and return null if canonical path resolution fails
            android.util.Log.w("FileStorageManager", "IOException getting file: ${e.message}")
            null
        } catch (e: SecurityException) {
            // Log and return null if security manager denies access
            android.util.Log.w("FileStorageManager", "SecurityException getting file: ${e.message}")
            null
        }
    }
}
