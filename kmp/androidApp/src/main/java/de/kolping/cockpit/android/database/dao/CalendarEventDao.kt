package de.kolping.cockpit.android.database.dao

import androidx.room.*
import de.kolping.cockpit.android.database.entities.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for CalendarEvent entities.
 * Provides methods for CRUD operations on calendar events.
 */
@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events ORDER BY timestart ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>
    
    @Query("SELECT * FROM calendar_events WHERE timestart >= :startTime AND timestart <= :endTime ORDER BY timestart ASC")
    fun getEventsByDateRange(startTime: Long, endTime: Long): Flow<List<CalendarEventEntity>>
    
    @Query("SELECT * FROM calendar_events WHERE timestart >= :startTime ORDER BY timestart ASC LIMIT :limit")
    fun getUpcomingEventsWithLimit(startTime: Long, limit: Int): Flow<List<CalendarEventEntity>>
    
    @Query("SELECT * FROM calendar_events WHERE courseId = :courseId ORDER BY timestart ASC")
    fun getEventsByCourse(courseId: Int): Flow<List<CalendarEventEntity>>
    
    @Query("SELECT * FROM calendar_events WHERE id = :eventId")
    suspend fun getEventById(eventId: Int): CalendarEventEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CalendarEventEntity>)
    
    @Update
    suspend fun updateEvent(event: CalendarEventEntity)
    
    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)
    
    @Query("DELETE FROM calendar_events WHERE timestart < :beforeTime")
    suspend fun deleteEventsOlderThan(beforeTime: Long)
    
    @Query("DELETE FROM calendar_events")
    suspend fun deleteAllEvents()
}
