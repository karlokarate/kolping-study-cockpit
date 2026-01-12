package de.kolping.cockpit.api

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import de.kolping.cockpit.models.*
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Moodle client for Kolping portal
 * Ported from Python moodle_client.py
 * 
 * Uses session cookie authentication.
 */
class MoodleClient(
    private val sessionCookie: String? = null,
    private val baseUrl: String = "https://portal.kolping-hochschule.de"
) {
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        
        install(HttpCookies) {
            sessionCookie?.let {
                storage = object : CookiesStorage {
                    override suspend fun get(requestUrl: Url): List<Cookie> {
                        return if (requestUrl.host.contains("kolping-hochschule.de")) {
                            listOf(
                                Cookie(
                                    name = "MoodleSession",
                                    value = it,
                                    domain = "portal.kolping-hochschule.de",
                                    path = "/"
                                )
                            )
                        } else {
                            emptyList()
                        }
                    }
                    
                    // No-op implementations: we manually set the session cookie
                    // and don't need to persist cookies from server responses
                    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
                        // Intentionally empty - session cookie is pre-configured
                    }
                    
                    override fun close() {
                        // Intentionally empty - no resources to clean up
                    }
                }
            }
        }
        
        install(Logging) {
            level = LogLevel.INFO
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
        
        followRedirects = true
        
        defaultRequest {
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header("Accept-Language", "de-DE,de;q=0.9,en;q=0.8")
        }
    }
    
    val isAuthenticated: Boolean
        get() = sessionCookie != null
    
    /**
     * Test if the current session is valid
     */
    suspend fun testSession(): Result<Boolean> {
        return try {
            val response = client.get("$baseUrl/my/")
            val html = response.bodyAsText()
            
            val isValid = when {
                "login" in response.request.url.toString() -> false
                "Weiterleiten" in html -> false
                "user-menu" in html || "usermenu" in html -> true
                "Dashboard" in html || "Meine Kurse" in html -> true
                else -> false
            }
            
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Fetch and parse the Moodle dashboard
     */
    suspend fun getDashboard(): Result<MoodleDashboard> {
        return try {
            val response = client.get("$baseUrl/my/")
            val html = response.bodyAsText()
            val document = Ksoup.parse(html)
            
            val dashboard = MoodleDashboard(
                userName = extractUserName(document),
                courses = extractCourses(document),
                events = extractEvents(document),
                rawHtml = html
            )
            
            Result.success(dashboard)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun extractUserName(document: Document): String? {
        return document.select("[class*=user][class*=menu], [class*=usermenu]")
            .firstOrNull()
            ?.text()
            ?.take(50)
    }
    
    private fun extractCourses(document: Document): List<MoodleCourse> {
        val courses = mutableListOf<MoodleCourse>()
        val seenIds = mutableSetOf<String>()
        
        document.select("a[href*='/course/view.php?id=']").forEach { element ->
            val href = element.attr("href")
            val idMatch = Regex("id=(\\d+)").find(href)
            val courseId = idMatch?.groupValues?.get(1) ?: return@forEach
            
            if (courseId in seenIds) return@forEach
            seenIds.add(courseId)
            
            val name = element.text().takeIf { it.length >= 3 }
                ?: element.parent()?.text()?.take(100)
                ?: return@forEach
            
            val url = if (href.startsWith("http")) href else "$baseUrl$href"
            
            courses.add(
                MoodleCourse(
                    id = courseId,
                    name = name.take(200),
                    url = url
                )
            )
        }
        
        return courses
    }
    
    private fun extractEvents(document: Document): List<MoodleEvent> {
        val events = mutableListOf<MoodleEvent>()
        
        // Look for event blocks
        document.select("div.event[data-region='event-item']").forEach { element ->
            val link = element.selectFirst("a[data-event-id]")
                ?: element.selectFirst("a[href*='event'], a[href*='calendar'], a[href*='mod']")
            
            val eventId = link?.attr("data-event-id")
                ?: Regex("id=(\\d+)").find(link?.attr("href") ?: "")?.groupValues?.get(1)
                ?: "unknown"
            
            val title = link?.text()
                ?: element.text().take(200)
            
            if (title.isBlank()) return@forEach
            
            val url = link?.attr("href")?.let { href ->
                if (href.startsWith("http")) href else "$baseUrl$href"
            }
            
            val dateDiv = element.selectFirst("div.date")
            val startTime = dateDiv?.text()?.replace("»", "→")?.replace("&raquo;", "→")
            
            events.add(
                MoodleEvent(
                    id = eventId,
                    title = title.take(100),
                    startTime = startTime,
                    url = url
                )
            )
        }
        
        return events
    }
    
    /**
     * Fetch list of enrolled courses
     */
    suspend fun getCourses(): Result<List<MoodleCourse>> {
        return try {
            val response = client.get("$baseUrl/my/courses.php")
            val html = response.bodyAsText()
            val document = Ksoup.parse(html)
            
            Result.success(extractCourses(document))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Fetch all assignments
     */
    suspend fun getAssignments(): Result<List<MoodleAssignment>> {
        return try {
            val response = client.get("$baseUrl/mod/assign/index.php")
            if (response.status != HttpStatusCode.OK) {
                return Result.success(emptyList())
            }
            
            val html = response.bodyAsText()
            val document = Ksoup.parse(html)
            
            val assignments = mutableListOf<MoodleAssignment>()
            
            document.select("a[href*='/mod/assign/view.php?id=']").forEach { link ->
                val href = link.attr("href")
                val idMatch = Regex("id=(\\d+)").find(href)
                val assignId = idMatch?.groupValues?.get(1) ?: return@forEach
                
                val name = link.text().takeIf { it.isNotBlank() } ?: return@forEach
                val url = if (href.startsWith("http")) href else "$baseUrl$href"
                
                assignments.add(
                    MoodleAssignment(
                        id = assignId,
                        name = name,
                        url = url
                    )
                )
            }
            
            Result.success(assignments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Fetch upcoming deadlines from calendar
     */
    suspend fun getUpcomingDeadlines(): Result<List<MoodleEvent>> {
        return try {
            val response = client.get("$baseUrl/calendar/view.php?view=upcoming")
            if (response.status != HttpStatusCode.OK) {
                return Result.success(emptyList())
            }
            
            val html = response.bodyAsText()
            val document = Ksoup.parse(html)
            
            Result.success(extractEvents(document))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Extract sesskey from Moodle HTML page
     * The sesskey is required for AJAX API calls
     * 
     * Looks for patterns like:
     * - "sesskey":"abc123def456"
     * - M.cfg.sesskey = "abc123def456"
     * - sesskey=abc123def456 (in URLs)
     * 
     * @return sesskey string or null if not found
     */
    suspend fun extractSesskey(): Result<String> {
        return try {
            val response = client.get("$baseUrl/my/")
            val html = response.bodyAsText()
            
            // Try multiple patterns to find sesskey
            val patterns = listOf(
                Regex(""""sesskey"\s*:\s*"([a-zA-Z0-9]+)""""),
                Regex("""M\.cfg\.sesskey\s*=\s*"([a-zA-Z0-9]+)""""),
                Regex("""sesskey=([a-zA-Z0-9]+)"""),
                Regex("""'sesskey'\s*:\s*'([a-zA-Z0-9]+)'""")
            )
            
            for (pattern in patterns) {
                val match = pattern.find(html)
                if (match != null && match.groupValues.size > 1) {
                    val sesskey = match.groupValues[1]
                    if (sesskey.isNotBlank()) {
                        return Result.success(sesskey)
                    }
                }
            }
            
            Result.failure(Exception("Could not extract sesskey from HTML"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a MoodleAjaxClient using the current session
     * Automatically extracts sesskey from the current session
     * 
     * @return MoodleAjaxClient configured with session cookie and sesskey
     */
    suspend fun createAjaxClient(): Result<MoodleAjaxClient> {
        if (!isAuthenticated) {
            return Result.failure(Exception("Not authenticated - session cookie required"))
        }
        
        return try {
            val cookie = sessionCookie
                ?: return Result.failure(Exception("Not authenticated - session cookie missing"))
            val sesskey = extractSesskey().getOrThrow()
            Result.success(MoodleAjaxClient(cookie, sesskey, baseUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun close() {
        client.close()
    }
}
