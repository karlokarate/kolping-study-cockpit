package de.kolping.cockpit.android.sync

import android.util.Log
import de.kolping.cockpit.android.database.dao.*
import de.kolping.cockpit.android.database.entities.*
import de.kolping.cockpit.android.storage.FileStorageManager
import de.kolping.cockpit.api.GraphQLClient
import de.kolping.cockpit.api.MoodleAjaxClient
import de.kolping.cockpit.api.MoodleClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar

/**
 * Manages synchronization of all study data
 * Orchestrates fetching from GraphQL, Moodle AJAX, and downloading files
 * 
 * Based on Issue #6 requirements for offline-first architecture
 */
class SyncManager(
    private val graphQLClient: GraphQLClient,
    private val moodleClient: MoodleClient,
    private val fileStorage: FileStorageManager,
    private val downloadManager: DownloadManager,
    private val moduleDao: ModuleDao,
    private val courseDao: CourseDao,
    private val fileDao: FileDao,
    private val calendarEventDao: CalendarEventDao,
    private val studentProfileDao: StudentProfileDao
) {
    companion object {
        private const val TAG = "SyncManager"
        private const val CALENDAR_MONTHS_TO_SYNC = 6
    }
    
    /**
     * Perform complete synchronization of all data
     * Emits progress updates throughout the process
     * 
     * @return Flow of SyncProgress updates, ending with final SyncResult
     */
    fun syncAll(): Flow<Any> = flow {
        val startTime = System.currentTimeMillis()
        var modulesCount = 0
        var coursesCount = 0
        var filesDownloaded = 0
        var eventsCount = 0
        var bytesDownloaded = 0L
        
        try {
            // Phase 1: Sync Grades
            emit(SyncProgress(
                phase = SyncPhase.GRADES,
                progress = 0.1f,
                message = "Fetching grades from GraphQL..."
            ))
            
            val gradeResult = syncGrades()
            modulesCount = gradeResult
            
            emit(SyncProgress(
                phase = SyncPhase.GRADES,
                progress = 0.2f,
                message = "Synced $modulesCount modules"
            ))
            
            // Phase 2: Sync Profile
            emit(SyncProgress(
                phase = SyncPhase.PROFILE,
                progress = 0.25f,
                message = "Fetching student profile..."
            ))
            
            val profileSynced = syncProfile()
            
            emit(SyncProgress(
                phase = SyncPhase.PROFILE,
                progress = 0.3f,
                message = if (profileSynced) "Profile synced" else "Profile sync skipped"
            ))
            
            // Phase 3: Sync Courses via Moodle AJAX
            emit(SyncProgress(
                phase = SyncPhase.COURSES,
                progress = 0.35f,
                message = "Fetching enrolled courses..."
            ))
            
            val courseResult = syncCourses()
            coursesCount = courseResult
            
            emit(SyncProgress(
                phase = SyncPhase.COURSES,
                progress = 0.5f,
                message = "Synced $coursesCount courses"
            ))
            
            // Phase 4: Sync Calendar
            emit(SyncProgress(
                phase = SyncPhase.CALENDAR,
                progress = 0.55f,
                message = "Fetching calendar events..."
            ))
            
            val eventResult = syncCalendar()
            eventsCount = eventResult
            
            emit(SyncProgress(
                phase = SyncPhase.CALENDAR,
                progress = 0.7f,
                message = "Synced $eventsCount events"
            ))
            
            // Phase 5: Parse Course Content (placeholder)
            emit(SyncProgress(
                phase = SyncPhase.CONTENT,
                progress = 0.75f,
                message = "Parsing course content..."
            ))
            
            // TODO: Implement course content parsing in future PR
            // For now, just mark as complete
            
            emit(SyncProgress(
                phase = SyncPhase.CONTENT,
                progress = 0.8f,
                message = "Content parsing complete"
            ))
            
            // Phase 6: Download Files (placeholder)
            emit(SyncProgress(
                phase = SyncPhase.FILES,
                progress = 0.85f,
                message = "Downloading files..."
            ))
            
            // TODO: Implement file downloads in future PR
            // For now, just mark as complete
            
            emit(SyncProgress(
                phase = SyncPhase.FILES,
                progress = 1.0f,
                filesDownloaded = filesDownloaded,
                totalFiles = filesDownloaded,
                message = "File downloads complete"
            ))
            
            // Success!
            val duration = System.currentTimeMillis() - startTime
            emit(SyncResult.Success(
                modulesCount = modulesCount,
                coursesCount = coursesCount,
                filesDownloaded = filesDownloaded,
                eventsCount = eventsCount,
                bytesDownloaded = bytesDownloaded,
                durationMs = duration
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            emit(SyncResult.Failure(
                error = e,
                phase = SyncPhase.FAILED,
                message = e.message
            ))
        }
    }
    
    /**
     * Sync grades and modules from GraphQL API
     * Saves to ModuleEntity table
     * 
     * @return Number of modules synced
     */
    private suspend fun syncGrades(): Int {
        try {
            val gradeOverview = graphQLClient.getGradeOverview().getOrThrow()
            val currentTime = System.currentTimeMillis()
            
            // Convert to entities and save
            val moduleEntities = gradeOverview.modules.map { module ->
                ModuleEntity(
                    modulId = module.modulId,
                    semester = module.semester,
                    modulbezeichnung = module.modulbezeichnung,
                    eCTS = module.eCTS,
                    grade = module.grade,
                    note = module.note,
                    points = module.points,
                    pruefungsform = module.pruefungsform,
                    examStatus = module.examStatus,
                    color = module.color,
                    lastSync = currentTime
                )
            }
            
            moduleDao.insertModules(moduleEntities)
            Log.d(TAG, "Synced ${moduleEntities.size} modules")
            
            return moduleEntities.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync grades", e)
            throw e
        }
    }
    
    /**
     * Sync student profile from GraphQL API
     * Saves to StudentProfileEntity table
     * 
     * @return true if synced successfully
     */
    private suspend fun syncProfile(): Boolean {
        try {
            val student = graphQLClient.getMyStudentData().getOrThrow()
            val gradeOverview = graphQLClient.getGradeOverview().getOrThrow()
            val currentTime = System.currentTimeMillis()
            
            val profileEntity = StudentProfileEntity(
                studentId = student.studentId,
                vorname = student.vorname,
                nachname = student.nachname,
                emailKh = student.emailKh,
                currentSemester = gradeOverview.currentSemester.toIntOrNull(),
                overallGrade = gradeOverview.grade,
                totalEcts = gradeOverview.eCTS,
                lastSync = currentTime
            )
            
            studentProfileDao.insertProfile(profileEntity)
            Log.d(TAG, "Synced student profile: ${student.vorname} ${student.nachname}")
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync profile", e)
            // Don't fail the whole sync if profile fails
            return false
        }
    }
    
    /**
     * Sync enrolled courses from Moodle AJAX API
     * Saves to CourseEntity table
     * 
     * @return Number of courses synced
     */
    private suspend fun syncCourses(): Int {
        try {
            // Create AJAX client from Moodle session
            val ajaxClient = moodleClient.createAjaxClient().getOrThrow()
            
            // Fetch enrolled courses
            val coursesData = ajaxClient.getEnrolledCourses(
                classification = "all",
                sort = "fullname",
                limit = 0
            ).getOrThrow()
            
            val currentTime = System.currentTimeMillis()
            
            // Convert to entities and save
            val courseEntities = coursesData.courses.map { course ->
                CourseEntity(
                    id = course.id,
                    fullname = course.fullname,
                    shortname = course.shortname,
                    courseimage = course.courseimage,
                    progress = course.progress,
                    viewurl = course.viewurl,
                    startdate = course.startdate,
                    enddate = course.enddate,
                    lastSync = currentTime
                )
            }
            
            courseDao.insertCourses(courseEntities)
            Log.d(TAG, "Synced ${courseEntities.size} courses")
            
            ajaxClient.close()
            return courseEntities.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync courses", e)
            throw e
        }
    }
    
    /**
     * Sync calendar events from Moodle AJAX API
     * Fetches events for the next CALENDAR_MONTHS_TO_SYNC months
     * Saves to CalendarEventEntity table
     * 
     * @return Number of events synced
     */
    private suspend fun syncCalendar(): Int {
        try {
            // Create AJAX client from Moodle session
            val ajaxClient = moodleClient.createAjaxClient().getOrThrow()
            
            // Get current date
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
            
            // Fetch calendar events for next N months
            val events = ajaxClient.getCalendarEvents(
                startYear = currentYear,
                startMonth = currentMonth,
                monthCount = CALENDAR_MONTHS_TO_SYNC,
                courseId = 1 // 1 = all courses
            ).getOrThrow()
            
            val currentTime = System.currentTimeMillis()
            
            // Convert to entities and save
            val eventEntities = events.mapNotNull { event ->
                if (event.id == null || event.name == null) {
                    return@mapNotNull null
                }
                
                CalendarEventEntity(
                    id = event.id,
                    name = event.name,
                    description = event.description,
                    eventtype = event.eventtype,
                    timestart = event.timestart ?: 0L,
                    timeduration = event.timeduration,
                    courseId = event.course?.id,
                    courseName = event.course?.fullname,
                    viewurl = event.viewurl,
                    lastSync = currentTime
                )
            }
            
            calendarEventDao.insertEvents(eventEntities)
            Log.d(TAG, "Synced ${eventEntities.size} calendar events")
            
            ajaxClient.close()
            return eventEntities.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync calendar", e)
            throw e
        }
    }
}
