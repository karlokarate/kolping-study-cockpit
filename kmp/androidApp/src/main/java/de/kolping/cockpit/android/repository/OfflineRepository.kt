package de.kolping.cockpit.android.repository

import de.kolping.cockpit.android.database.dao.*
import de.kolping.cockpit.android.database.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * Unified repository for offline data access
 * Provides a single access point to all cached study data
 * 
 * This repository follows offline-first architecture:
 * - All read operations query local Room database
 * - Data is populated via SyncManager
 * - UI observes Flow to get real-time updates
 */
class OfflineRepository(
    private val moduleDao: ModuleDao,
    private val courseDao: CourseDao,
    private val fileDao: FileDao,
    private val calendarEventDao: CalendarEventDao,
    private val studentProfileDao: StudentProfileDao
) {
    
    // ==================== Student Profile ====================
    
    /**
     * Get student profile as Flow
     * Automatically updates when data changes
     */
    fun getStudentProfile(): Flow<StudentProfileEntity?> {
        return studentProfileDao.getProfile()
    }
    
    /**
     * Get student profile by ID
     */
    suspend fun getStudentProfileById(studentId: String): StudentProfileEntity? {
        return studentProfileDao.getProfileById(studentId)
    }
    
    // ==================== Modules ====================
    
    /**
     * Get all modules as Flow
     * Sorted by semester (descending) and module name
     */
    fun getAllModules(): Flow<List<ModuleEntity>> {
        return moduleDao.getAllModules()
    }
    
    /**
     * Get modules for a specific semester as Flow
     */
    fun getModulesBySemester(semester: Int): Flow<List<ModuleEntity>> {
        return moduleDao.getModulesBySemester(semester)
    }
    
    /**
     * Get a specific module by ID
     */
    suspend fun getModuleById(modulId: String): ModuleEntity? {
        return moduleDao.getModuleById(modulId)
    }
    
    // ==================== Courses ====================
    
    /**
     * Get all courses as Flow
     * Sorted by full name
     */
    fun getAllCourses(): Flow<List<CourseEntity>> {
        return courseDao.getAllCourses()
    }
    
    /**
     * Get a specific course by ID
     */
    suspend fun getCourseById(courseId: Int): CourseEntity? {
        return courseDao.getCourseById(courseId)
    }
    
    // ==================== Files ====================
    
    /**
     * Get all files as Flow
     * Sorted by download time (most recent first)
     */
    fun getAllFiles(): Flow<List<FileEntity>> {
        return fileDao.getAllFiles()
    }
    
    /**
     * Get files for a specific module as Flow
     */
    fun getFilesByModule(moduleId: String): Flow<List<FileEntity>> {
        return fileDao.getFilesByModule(moduleId)
    }
    
    /**
     * Get files for a specific course as Flow
     */
    fun getFilesByCourse(courseId: Int): Flow<List<FileEntity>> {
        return fileDao.getFilesByCourse(courseId)
    }
    
    /**
     * Get a specific file by ID
     */
    suspend fun getFileById(fileId: String): FileEntity? {
        return fileDao.getFileById(fileId)
    }
    
    // ==================== Calendar Events ====================
    
    /**
     * Get all calendar events as Flow
     * Sorted by start time
     */
    fun getAllEvents(): Flow<List<CalendarEventEntity>> {
        return calendarEventDao.getAllEvents()
    }
    
    /**
     * Get events within a date range as Flow
     */
    fun getEventsByDateRange(startTime: Long, endTime: Long): Flow<List<CalendarEventEntity>> {
        return calendarEventDao.getEventsByDateRange(startTime, endTime)
    }
    
    /**
     * Get events for a specific course as Flow
     */
    fun getEventsByCourse(courseId: Int): Flow<List<CalendarEventEntity>> {
        return calendarEventDao.getEventsByCourse(courseId)
    }
    
    /**
     * Get a specific event by ID
     */
    suspend fun getEventById(eventId: Int): CalendarEventEntity? {
        return calendarEventDao.getEventById(eventId)
    }
    
    /**
     * Get upcoming events (after current time)
     */
    fun getUpcomingEvents(limit: Int = 10): Flow<List<CalendarEventEntity>> {
        val now = System.currentTimeMillis() / 1000 // Convert to Unix timestamp
        return calendarEventDao.getUpcomingEventsWithLimit(now, limit)
    }
    
    /**
     * Get modules for current semester
     * Returns all modules sorted by semester (most recent first)
     * Note: Filtering by specific semester should be done in the UI layer
     * based on StudentProfile.currentSemester
     */
    fun getCurrentSemesterModules(): Flow<List<ModuleEntity>> {
        return moduleDao.getAllModules()
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Delete all cached data
     * Useful for logout or full re-sync
     */
    suspend fun clearAllData() {
        moduleDao.deleteAllModules()
        courseDao.deleteAllCourses()
        fileDao.deleteAllFiles()
        calendarEventDao.deleteAllEvents()
        studentProfileDao.deleteAllProfiles()
    }
}
