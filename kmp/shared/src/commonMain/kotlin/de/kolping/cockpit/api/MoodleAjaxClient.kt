package de.kolping.cockpit.api

import de.kolping.cockpit.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

/**
 * Moodle AJAX API client
 * Handles calls to /lib/ajax/service.php endpoints
 * 
 * Based on Feldlisten.md section 3 (Moodle JSON API)
 * Requires both session cookie and sesskey for authentication
 */
class MoodleAjaxClient(
    private val sessionCookie: String,
    private val sesskey: String,
    private val baseUrl: String = "https://portal.kolping-hochschule.de"
) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        
        install(HttpCookies) {
            storage = object : CookiesStorage {
                override suspend fun get(requestUrl: Url): List<Cookie> {
                    return if (requestUrl.host.contains("kolping-hochschule.de")) {
                        listOf(
                            Cookie(
                                name = "MoodleSession",
                                value = sessionCookie,
                                domain = "portal.kolping-hochschule.de",
                                path = "/"
                            )
                        )
                    } else {
                        emptyList()
                    }
                }
                
                override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
                    // Intentionally empty - session cookie is pre-configured
                }
                
                override fun close() {
                    // Intentionally empty - no resources to clean up
                }
            }
        }
        
        install(Logging) {
            level = LogLevel.INFO
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
        
        defaultRequest {
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            header("Accept", "application/json")
            header("Accept-Language", "de-DE,de;q=0.9,en;q=0.8")
        }
    }
    
    /**
     * Execute AJAX method calls
     * @param calls List of AJAX calls to execute
     * @return List of results corresponding to each call
     */
    private suspend fun executeAjaxCalls(calls: List<MoodleAjaxCall>): Result<List<MoodleAjaxResult>> {
        return try {
            val url = "$baseUrl/lib/ajax/service.php?sesskey=$sesskey&info=${calls.firstOrNull()?.methodname ?: ""}"
            
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(calls)
            }
            
            if (response.status != HttpStatusCode.OK) {
                return Result.failure(Exception("AJAX call failed: ${response.status}"))
            }
            
            val results = response.body<List<MoodleAjaxResult>>()
            
            // Check for errors
            results.forEach { result ->
                if (result.error == true || result.exception != null) {
                    val errorMsg = result.exception?.message ?: "Unknown error"
                    return Result.failure(Exception("AJAX error: $errorMsg"))
                }
            }
            
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get enrolled courses by timeline classification
     * Method: core_course_get_enrolled_courses_by_timeline_classification
     * 
     * @param classification Filter: "all", "inprogress", "future", "past"
     * @param sort Sort field: "fullname", "ul.timeaccess desc", "lastaccess"
     * @param limit Maximum number of courses to return (0 = all)
     * @param offset Offset for pagination
     * @return List of enrolled courses with progress information
     */
    suspend fun getEnrolledCourses(
        classification: String = "all",
        sort: String = "fullname",
        limit: Int = 0,
        offset: Int = 0
    ): Result<MoodleEnrolledCoursesData> {
        return try {
            val args = buildJsonObject {
                put("offset", JsonPrimitive(offset))
                put("limit", JsonPrimitive(limit))
                put("classification", JsonPrimitive(classification))
                put("sort", JsonPrimitive(sort))
            }
            
            val call = MoodleAjaxCall(
                index = 0,
                methodname = "core_course_get_enrolled_courses_by_timeline_classification",
                args = args.toMap()
            )
            
            val results = executeAjaxCalls(listOf(call)).getOrThrow()
            val dataElement = results.firstOrNull()?.data
                ?: return Result.failure(Exception("No data returned"))
            
            val data = json.decodeFromJsonElement<MoodleEnrolledCoursesData>(dataElement)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get calendar monthly view
     * Method: core_calendar_get_calendar_monthly_view
     * 
     * @param year Year to view
     * @param month Month to view (1-12)
     * @param day Day within month (default: 1)
     * @param courseId Course ID to filter events (1 = all courses)
     * @param categoryId Category ID to filter events (optional)
     * @param includeNavigation Include navigation links
     * @param mini Return minimal data for mini calendar view
     * @return Calendar data with events organized by weeks and days
     */
    suspend fun getCalendarMonthlyView(
        year: Int,
        month: Int,
        day: Int = 1,
        courseId: Int = 1,
        categoryId: Int? = null,
        includeNavigation: Boolean = true,
        mini: Boolean = false
    ): Result<MoodleCalendarMonthlyViewData> {
        return try {
            val args = buildJsonObject {
                put("year", JsonPrimitive(year))
                put("month", JsonPrimitive(month))
                put("day", JsonPrimitive(day))
                put("courseid", JsonPrimitive(courseId))
                categoryId?.let { put("categoryid", JsonPrimitive(it)) }
                put("includenavigation", JsonPrimitive(includeNavigation))
                put("mini", JsonPrimitive(mini))
            }
            
            val call = MoodleAjaxCall(
                index = 0,
                methodname = "core_calendar_get_calendar_monthly_view",
                args = args.toMap()
            )
            
            val results = executeAjaxCalls(listOf(call)).getOrThrow()
            val dataElement = results.firstOrNull()?.data
                ?: return Result.failure(Exception("No data returned"))
            
            val data = json.decodeFromJsonElement<MoodleCalendarMonthlyViewData>(dataElement)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get calendar events for a date range
     * Convenience method that fetches multiple months and aggregates events
     * 
     * @param startYear Start year
     * @param startMonth Start month (1-12)
     * @param monthCount Number of months to fetch
     * @param courseId Course ID to filter events (1 = all courses)
     * @return List of all calendar events in the specified range
     */
    suspend fun getCalendarEvents(
        startYear: Int,
        startMonth: Int,
        monthCount: Int = 6,
        courseId: Int = 1
    ): Result<List<MoodleCalendarEvent>> {
        return try {
            val allEvents = mutableListOf<MoodleCalendarEvent>()
            var currentYear = startYear
            var currentMonth = startMonth
            
            repeat(monthCount) {
                val calendarData = getCalendarMonthlyView(
                    year = currentYear,
                    month = currentMonth,
                    courseId = courseId,
                    includeNavigation = false
                ).getOrThrow()
                
                // Extract all events from weeks/days
                calendarData.weeks.forEach { week ->
                    week.days.forEach { day ->
                        allEvents.addAll(day.events)
                    }
                }
                
                // Move to next month
                currentMonth++
                if (currentMonth > 12) {
                    currentMonth = 1
                    currentYear++
                }
            }
            
            // Remove duplicates based on event ID
            val uniqueEvents = allEvents
                .distinctBy { it.id }
                .sortedBy { it.timestart ?: 0L }
            
            Result.success(uniqueEvents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun close() {
        client.close()
    }
}

/**
 * Helper extension to convert JsonObject to Map<String, JsonElement>
 */
private fun JsonObject.toMap(): Map<String, JsonElement> {
    return this.entries.associate { it.key to it.value }
}
