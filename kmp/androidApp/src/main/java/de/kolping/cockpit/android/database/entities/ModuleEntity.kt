package de.kolping.cockpit.android.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing module information offline.
 * Based on MyStudentModule from GraphQL API and issue #6 requirements.
 */
@Entity(tableName = "modules")
data class ModuleEntity(
    @PrimaryKey
    val modulId: String,
    val semester: Int,
    val modulbezeichnung: String,
    val eCTS: Double,
    val grade: String? = null,
    val note: String? = null,
    val points: Double? = null,
    val pruefungsform: String? = null,
    val examStatus: String? = null,
    val color: String? = null,
    val lastSync: Long
)
