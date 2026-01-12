package de.kolping.cockpit.android.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing calendar events offline.
 * Based on MoodleCalendarEvent from AJAX API and issue #6 requirements.
 */
@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val description: String? = null,
    val eventtype: String? = null,
    val timestart: Long,
    val timeduration: Long? = null,
    val courseId: Int? = null,
    val courseName: String? = null,
    val viewurl: String? = null,
    val lastSync: Long
)
