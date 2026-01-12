package de.kolping.cockpit.android.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import de.kolping.cockpit.android.database.dao.*
import de.kolping.cockpit.android.database.entities.*

/**
 * Main Room database for offline storage.
 * Manages all entities and provides access to DAOs.
 */
@Database(
    entities = [
        ModuleEntity::class,
        CourseEntity::class,
        FileEntity::class,
        CalendarEventEntity::class,
        StudentProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KolpingDatabase : RoomDatabase() {
    abstract fun moduleDao(): ModuleDao
    abstract fun courseDao(): CourseDao
    abstract fun fileDao(): FileDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun studentProfileDao(): StudentProfileDao
    
    companion object {
        private const val DATABASE_NAME = "kolping_database"
        
        @Volatile
        private var INSTANCE: KolpingDatabase? = null
        
        fun getInstance(context: Context): KolpingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KolpingDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
