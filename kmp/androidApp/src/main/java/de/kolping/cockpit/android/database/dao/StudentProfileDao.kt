package de.kolping.cockpit.android.database.dao

import androidx.room.*
import de.kolping.cockpit.android.database.entities.StudentProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for StudentProfile entity.
 * Provides methods for CRUD operations on student profile.
 */
@Dao
interface StudentProfileDao {
    @Query("SELECT * FROM student_profile LIMIT 1")
    fun getProfile(): Flow<StudentProfileEntity?>
    
    @Query("SELECT * FROM student_profile WHERE studentId = :studentId")
    suspend fun getProfileById(studentId: String): StudentProfileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: StudentProfileEntity)
    
    @Update
    suspend fun updateProfile(profile: StudentProfileEntity)
    
    @Delete
    suspend fun deleteProfile(profile: StudentProfileEntity)
    
    @Query("DELETE FROM student_profile")
    suspend fun deleteAllProfiles()
}
