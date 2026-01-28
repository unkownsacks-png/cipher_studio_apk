package com.cipher.studio.data.local

import android.content.Context
import android.content.SharedPreferences
import com.cipher.studio.domain.model.Session
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FIXED LOCAL STORAGE MANAGER
 * 
 * CHANGE: Moved 'Session' storage from SharedPreferences to Internal File Storage.
 * REASON: SharedPreferences causes crashes with large data (images/long text).
 * Files handle megabytes of JSON smoothly.
 */

@Singleton
class LocalStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Standard prefs for LIGHTWEIGHT data (Auth, Theme)
    private val prefs: SharedPreferences = context.getSharedPreferences("cipher_data", Context.MODE_PRIVATE)
    
    // Gson for serialization
    private val gson = Gson()

    // FILE STORAGE for HEAVY data (Sessions)
    private val sessionFile = File(context.filesDir, "cipher_sessions_db.json")

    companion object {
        private const val KEY_IS_AUTHORIZED = "is_elite_authorized"
        private const val KEY_THEME = "app_theme"
        // Note: KEY_SESSIONS is removed because we use a file now.
    }

    // --- 1. AUTH PERSISTENCE (Keep in Prefs - it's small) ---
    fun setAuthorized(isAuthorized: Boolean) {
        prefs.edit().putBoolean(KEY_IS_AUTHORIZED, isAuthorized).apply()
    }

    fun isAuthorized(): Boolean {
        return prefs.getBoolean(KEY_IS_AUTHORIZED, false)
    }

    // --- 2. CHAT HISTORY PERSISTENCE (Moved to File IO) ---
    // This solves the crash when saving 1000+ lines or images.
    
    @Synchronized // Prevents two threads from writing at the same time
    fun saveSessions(sessions: List<Session>) {
        try {
            // Convert to JSON
            val json = gson.toJson(sessions)
            
            // Write directly to a file (No XML overhead, No Size Limit)
            FileWriter(sessionFile).use { writer ->
                writer.write(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If writing fails, we don't crash the app, we just log it.
        }
    }

    fun getSessions(): List<Session> {
        if (!sessionFile.exists()) return emptyList()

        return try {
            val type = object : TypeToken<List<Session>>() {}.type
            
            // Read directly from file stream (Memory Efficient)
            FileReader(sessionFile).use { reader ->
                gson.fromJson(reader, type) ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- 3. THEME PERSISTENCE ---
    fun saveTheme(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_THEME, isDark).apply()
    }

    fun isDarkTheme(): Boolean {
        return prefs.getBoolean(KEY_THEME, true) 
    }

    // Clear everything (Logout)
    fun clearAll() {
        prefs.edit().clear().apply()
        if (sessionFile.exists()) {
            sessionFile.delete()
        }
    }
}