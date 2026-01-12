package de.kolping.cockpit.android.sync

/**
 * Phases of the synchronization process
 * Used to track progress and display appropriate status messages
 */
enum class SyncPhase {
    /** Fetching grades from GraphQL API */
    GRADES,
    
    /** Fetching student profile from GraphQL API */
    PROFILE,
    
    /** Fetching enrolled courses from Moodle AJAX API */
    COURSES,
    
    /** Fetching calendar events from Moodle AJAX API */
    CALENDAR,
    
    /** Parsing course content for file URLs */
    CONTENT,
    
    /** Downloading files (PDFs, documents, etc.) */
    FILES,
    
    /** Sync completed successfully */
    COMPLETED,
    
    /** Sync failed with error */
    FAILED
}
