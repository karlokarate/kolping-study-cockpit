package de.kolping.cockpit.mapping.android

import android.content.Context
import de.kolping.cockpit.mapping.core.ExportBundle
import java.io.File
import java.io.IOException

class SessionStore(private val context: Context) {
    
    /**
     * Writes a bundle to disk with proper error handling.
     * @return Result containing the export path on success, or IOException on failure
     */
    fun writeBundle(bundle: ExportBundle.BundleContent, exportName: String): Result<String> {
        return try {
            val baseDir = File(context.filesDir, "mapping_exports/$exportName")
            if (!baseDir.mkdirs() && !baseDir.exists()) {
                return Result.failure(IOException("Failed to create directory: ${baseDir.absolutePath}"))
            }

            File(baseDir, "map.json").writeText(bundle.mapJson)

            val chainsDir = File(baseDir, "chains")
            if (!chainsDir.mkdirs() && !chainsDir.exists()) {
                return Result.failure(IOException("Failed to create chains directory"))
            }
            bundle.chainsJson.forEach { (id, json) ->
                File(chainsDir, "$id.json").writeText(json)
            }

            val sessionsDir = File(baseDir, "sessions")
            if (!sessionsDir.mkdirs() && !sessionsDir.exists()) {
                return Result.failure(IOException("Failed to create sessions directory"))
            }
            bundle.sessionsJson.forEach { (id, json) ->
                File(sessionsDir, "$id.json").writeText(json)
            }

            val schemasDir = File(baseDir, "schemas")
            if (!schemasDir.mkdirs() && !schemasDir.exists()) {
                return Result.failure(IOException("Failed to create schemas directory"))
            }
            bundle.schemasJson.forEach { (id, json) ->
                File(schemasDir, "$id.json").writeText(json)
            }

            Result.success(baseDir.absolutePath)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(IOException("Permission denied", e))
        }
    }

    fun listExports(): List<String> {
        val baseDir = File(context.filesDir, "mapping_exports")
        return baseDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
    }

    fun getExportPath(exportName: String): String {
        return File(context.filesDir, "mapping_exports/$exportName").absolutePath
    }
}
