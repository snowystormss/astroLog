package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserProfile::class, AstrologyInsight::class, SavedProfile::class, ChatSession::class, ChatMessageData::class, AstrologyEventNote::class, UserSettings::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun astrologyDao(): AstrologyDao
}
