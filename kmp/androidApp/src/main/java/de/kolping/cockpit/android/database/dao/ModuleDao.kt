package de.kolping.cockpit.android.database.dao

import androidx.room.*
import de.kolping.cockpit.android.database.entities.ModuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Module entities.
 * Provides methods for CRUD operations on modules.
 */
@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules ORDER BY semester DESC, modulbezeichnung ASC")
    fun getAllModules(): Flow<List<ModuleEntity>>
    
    @Query("SELECT * FROM modules WHERE semester = :semester ORDER BY modulbezeichnung ASC")
    fun getModulesBySemester(semester: Int): Flow<List<ModuleEntity>>
    
    @Query("SELECT * FROM modules WHERE modulId = :modulId")
    suspend fun getModuleById(modulId: String): ModuleEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: ModuleEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(modules: List<ModuleEntity>)
    
    @Update
    suspend fun updateModule(module: ModuleEntity)
    
    @Delete
    suspend fun deleteModule(module: ModuleEntity)
    
    @Query("DELETE FROM modules")
    suspend fun deleteAllModules()
}
