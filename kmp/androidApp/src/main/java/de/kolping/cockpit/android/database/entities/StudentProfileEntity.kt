package de.kolping.cockpit.android.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing student profile information offline.
 * Based on MyStudentProfile from GraphQL API and issue #6 requirements.
 */
@Entity(tableName = "student_profile")
data class StudentProfileEntity(
    @PrimaryKey
    val studentId: String,
    val vorname: String,
    val nachname: String,
    val emailKh: String? = null,
    val currentSemester: Int? = null,
    val overallGrade: String? = null,
    val totalEcts: Double? = null,
    val lastSync: Long
)
