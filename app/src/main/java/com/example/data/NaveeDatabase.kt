package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TripEntity::class, DiagnosticLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NaveeDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun diagnosticLogDao(): DiagnosticLogDao

    companion object {
        @Volatile
        private var INSTANCE: NaveeDatabase? = null

        fun getDatabase(context: Context): NaveeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NaveeDatabase::class.java,
                    "navee_companion.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
