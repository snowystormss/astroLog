package com.example.data

import android.content.Context
import android.content.SharedPreferences

object LocalStorageCache {
    private const val PREFS_NAME = "local_storage_cache"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun get(key: String): String? {
        return prefs.getString(key, null)
    }

    fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
