package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [OmnisRecord::class], version = 3, exportSchema = false)
abstract class OmnisDatabase : RoomDatabase() {
    abstract fun omnisDao(): OmnisDao

    companion object {
        @Volatile
        private var INSTANCE: OmnisDatabase? = null

        fun getDatabase(context: Context): OmnisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OmnisDatabase::class.java,
                    "omnis_local_db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }

        fun getInMemoryDatabase(context: Context): OmnisDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                OmnisDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
