package de.kolping.cockpit.models

import kotlinx.serialization.Serializable

/**
 * Moodle course information
 * Ported from Python moodle_client.py
 */
@Serializable
data class MoodleCourse(
    val id: String,
    val name: String,
    val shortname: String? = null,
    val category: String? = null,
    val url: String? = null,
    val progress: Double? = null,
    val visible: Boolean = true
)

/**
 * Moodle calendar event (deadlines, exams, etc.)
 * Ported from Python moodle_client.py
 */
@Serializable
data class MoodleEvent(
    val id: String,
    val title: String,
    val description: String? = null,
    val courseId: String? = null,
    val courseName: String? = null,
    val eventType: String? = null,  // assignment, quiz, etc.
    val startTime: String? = null,
    val endTime: String? = null,
    val url: String? = null
)

/**
 * Moodle assignment information
 * Ported from Python moodle_client.py
 */
@Serializable
data class MoodleAssignment(
    val id: String,
    val name: String,
    val courseId: String? = null,
    val courseName: String? = null,
    val dueDate: String? = null,
    val cutoffDate: String? = null,
    val description: String? = null,
    val submissionStatus: String? = null,
    val gradingStatus: String? = null,
    val grade: String? = null,
    val url: String? = null
)

/**
 * Moodle grade item
 * Ported from Python moodle_client.py
 */
@Serializable
data class MoodleGrade(
    val id: String? = null,
    val itemName: String = "",
    val grade: String? = null,
    val rangeMin: String? = null,
    val rangeMax: String? = null,
    val percentage: String? = null,
    val feedback: String? = null,
    val courseId: String? = null,
    val courseName: String? = null
)

/**
 * Complete Moodle dashboard data
 * Ported from Python moodle_client.py
 */
@Serializable
data class MoodleDashboard(
    val userName: String? = null,
    val courses: List<MoodleCourse> = emptyList(),
    val events: List<MoodleEvent> = emptyList(),
    val assignments: List<MoodleAssignment> = emptyList(),
    val notifications: List<String> = emptyList(),
    val rawHtml: String? = null
)
