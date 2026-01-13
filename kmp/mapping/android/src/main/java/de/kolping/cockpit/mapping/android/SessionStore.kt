package de.kolping.cockpit.mapping.android

import android.content.Context
import de.kolping.cockpit.mapping.core.ExportBundle
import java.io.File

class SessionStore(private val context: Context) {
    
    fun writeBundle(bundle: ExportBundle.BundleContent, exportName: String): String {
        val baseDir = File(context.filesDir, "mapping_exports/$exportName")
        baseDir.mkdirs()

        File(baseDir, "map.json").writeText(bundle.mapJson)

        val chainsDir = File(baseDir, "chains")
        chainsDir.mkdirs()
        bundle.chainsJson.forEach { (id, json) ->
            File(chainsDir, "$id.json").writeText(json)
        }

        val sessionsDir = File(baseDir, "sessions")
        sessionsDir.mkdirs()
        bundle.sessionsJson.forEach { (id, json) ->
            File(sessionsDir, "$id.json").writeText(json)
        }

        val schemasDir = File(baseDir, "schemas")
        schemasDir.mkdirs()
        bundle.schemasJson.forEach { (id, json) ->
            File(schemasDir, "$id.json").writeText(json)
        }

        return baseDir.absolutePath
    }

    fun listExports(): List<String> {
        val baseDir = File(context.filesDir, "mapping_exports")
        return baseDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
    }

    fun getExportPath(exportName: String): String {
        return File(context.filesDir, "mapping_exports/$exportName").absolutePath
    }
}
