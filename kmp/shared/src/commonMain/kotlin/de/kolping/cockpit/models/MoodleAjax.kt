package de.kolping.cockpit.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Moodle AJAX API models
 * Based on Feldlisten.md and example.md
 * Used for /lib/ajax/service.php endpoints
 */

/**
 * One element in the JSON array posted to /lib/ajax/service.php
 */
@Serializable
data class MoodleAjaxCall(
    val index: Int,
    val methodname: String,
    val args: Map<String, JsonElement> = emptyMap()
)

/**
 * Exception object returned in AJAX responses
 */
@Serializable
data class MoodleAjaxException(
    val errorcode: String? = null,
    val message: String? = null,
    val link: String? = null,
    val moreinfourl: String? = null
)

/**
 * One element in the JSON array returned by /lib/ajax/service.php
 */
@Serializable
data class MoodleAjaxResult(
    val error: Boolean? = null,
    val data: JsonElement? = null,
    val exception: MoodleAjaxException? = null
)

/**
 * Response data for core_course_get_enrolled_courses_by_timeline_classification
 */
@Serializable
data class MoodleEnrolledCoursesData(
    val courses: List<MoodleAjaxCourse> = emptyList(),
    val nextoffset: Int? = null
)

/**
 * Course information from AJAX API
 * Extends the basic MoodleCourse with AJAX-specific fields
 */
@Serializable
data class MoodleAjaxCourse(
    val id: Int,
    val idnumber: String? = null,
    val shortname: String? = null,
    val fullname: String,
    val fullnamedisplay: String? = null,
    val coursecategory: String? = null,
    val courseimage: String? = null,
    val summary: String? = null,
    val summaryformat: Int? = null,
    val startdate: Long? = null,
    val enddate: Long? = null,
    val visible: Boolean? = null,
    val hidden: Boolean? = null,
    val viewurl: String? = null,
    val showshortname: Boolean? = null,
    val hasprogress: Boolean? = null,
    val progress: Double? = null,
    val isfavourite: Boolean? = null,
    val showactivitydates: Boolean? = null,
    val showcompletionconditions: Boolean? = null
)

/**
 * Date object used in calendar responses
 */
@Serializable
data class MoodleCalendarDate(
    val year: Int? = null,
    val month: Int? = null,
    val mon: Int? = null,
    val mday: Int? = null,
    val wday: Int? = null,
    val weekday: String? = null,
    val yday: Int? = null,
    val hours: Int? = null,
    val minutes: Int? = null,
    val seconds: Int? = null,
    val timestamp: Long? = null
)

/**
 * Course information within calendar events
 */
@Serializable
data class MoodleCalendarEventCourse(
    val id: Int? = null,
    val idnumber: String? = null,
    val shortname: String? = null,
    val fullname: String? = null,
    val fullnamedisplay: String? = null,
    val coursecategory: String? = null,
    val courseimage: String? = null,
    val summary: String? = null,
    val summaryformat: Int? = null,
    val startdate: Long? = null,
    val enddate: Long? = null,
    val visible: Boolean? = null,
    val hidden: Boolean? = null,
    val viewurl: String? = null,
    val showshortname: Boolean? = null,
    val hasprogress: Boolean? = null,
    val progress: Double? = null,
    val isfavourite: Boolean? = null,
    val showactivitydates: Boolean? = null,
    val showcompletionconditions: Boolean? = null
)

/**
 * Calendar event from AJAX API
 */
@Serializable
data class MoodleCalendarEvent(
    val id: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val descriptionformat: Int? = null,
    val eventtype: String? = null,
    val normalisedeventtype: String? = null,
    val normalisedeventtypetext: String? = null,
    val formattedtime: String? = null,
    val url: String? = null,
    val viewurl: String? = null,
    val icon: String? = null,
    val component: String? = null,
    val modulename: String? = null,
    val categoryid: Int? = null,
    val groupid: Int? = null,
    val groupname: String? = null,
    val userid: Int? = null,
    val repeatid: Int? = null,
    val eventcount: Int? = null,
    val timestart: Long? = null,
    val timeduration: Long? = null,
    val timesort: Long? = null,
    val timemodified: Long? = null,
    val timeusermidnight: Long? = null,
    val visible: Boolean? = null,
    val draggable: Boolean? = null,
    val candelete: Boolean? = null,
    val canedit: Boolean? = null,
    val deleteurl: String? = null,
    val editurl: String? = null,
    val popupname: String? = null,
    val subscription: JsonElement? = null,
    val course: MoodleCalendarEventCourse? = null
)

/**
 * Calendar day with events
 */
@Serializable
data class MoodleCalendarDay(
    val year: Int? = null,
    val mday: Int? = null,
    val wday: Int? = null,
    val yday: Int? = null,
    val hours: Int? = null,
    val minutes: Int? = null,
    val seconds: Int? = null,
    val timestamp: Long? = null,
    val daytitle: String? = null,
    val popovertitle: String? = null,
    val istoday: Boolean? = null,
    val isweekend: Boolean? = null,
    val hasevents: Boolean? = null,
    val haslastdayofevent: Boolean? = null,
    val neweventtimestamp: Long? = null,
    val viewdaylink: String? = null,
    val viewdaylinktitle: String? = null,
    val calendareventtypes: JsonElement? = null,
    val nextperiod: JsonElement? = null,
    val previousperiod: JsonElement? = null,
    val navigation: JsonElement? = null,
    val events: List<MoodleCalendarEvent> = emptyList()
)

/**
 * Calendar week containing days
 */
@Serializable
data class MoodleCalendarWeek(
    val days: List<MoodleCalendarDay> = emptyList()
)

/**
 * Response data for core_calendar_get_calendar_monthly_view
 */
@Serializable
data class MoodleCalendarMonthlyViewData(
    val categoryid: Int? = null,
    val courseid: Int? = null,
    val date: MoodleCalendarDate? = null,
    val daynames: List<String> = emptyList(),
    val defaulteventcontext: JsonElement? = null,
    val includenavigation: Boolean? = null,
    val initialeventsloaded: Boolean? = null,
    val larrow: String? = null,
    val rarrow: String? = null,
    val nextperiod: MoodleCalendarDate? = null,
    val previousperiod: MoodleCalendarDate? = null,
    val nextperiodlink: String? = null,
    val nextperiodname: String? = null,
    val previousperiodlink: String? = null,
    val previousperiodname: String? = null,
    val periodname: String? = null,
    val url: String? = null,
    val view: String? = null,
    val weeks: List<MoodleCalendarWeek> = emptyList()
)
