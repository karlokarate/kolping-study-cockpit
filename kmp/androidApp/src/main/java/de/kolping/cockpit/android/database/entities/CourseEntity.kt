package de.kolping.cockpit.android.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing Moodle course information offline.
 * Based on MoodleCourse from AJAX API and issue #6 requirements.
 */
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey
    val id: Int,
    val fullname: String,
    val shortname: String? = null,
    val courseimage: String? = null,
    val progress: Double? = null,
    val viewurl: String? = null,
    val startdate: Long? = null,
    val enddate: Long? = null,
    val lastSync: Long
)
