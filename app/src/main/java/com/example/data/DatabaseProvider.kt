package com.example.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    lateinit var database: AppDatabase
        private set

    fun init(context: Context) {
        if (!::database.isInitialized) {
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "astrolog_db"
            ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        }
    }
}
