package de.kolping.cockpit.android.database.dao

import androidx.room.*
import de.kolping.cockpit.android.database.entities.CourseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Course entities.
 * Provides methods for CRUD operations on courses.
 */
@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY fullname ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>
    
    @Query("SELECT * FROM courses WHERE id = :courseId")
    suspend fun getCourseById(courseId: Int): CourseEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>)
    
    @Update
    suspend fun updateCourse(course: CourseEntity)
    
    @Delete
    suspend fun deleteCourse(course: CourseEntity)
    
    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()
}
