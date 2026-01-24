package com.cipher.studio.data.local

import android.content.Context
import android.content.SharedPreferences
import com.cipher.studio.domain.model.Session
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalStorageManager @Inject constructor(
    @ApplicationContext context: Context
) {
    // Standard prefs for heavy data (Chat History)
    private val prefs: SharedPreferences = context.getSharedPreferences("cipher_data", Context.MODE_PRIVATE)
    
    // Secure prefs logic handled in ApiKeyManager, but we handle simple boolean flags here
    private val gson = Gson()

    companion object {
        private const val KEY_IS_AUTHORIZED = "is_elite_authorized"
        private const val KEY_SESSIONS = "chat_sessions_v2"
        private const val KEY_THEME = "app_theme"
    }

    // --- 1. AUTH PERSISTENCE ---
    fun setAuthorized(isAuthorized: Boolean) {
        prefs.edit().putBoolean(KEY_IS_AUTHORIZED, isAuthorized).apply()
    }

    fun isAuthorized(): Boolean {
        return prefs.getBoolean(KEY_IS_AUTHORIZED, false)
    }

    // --- 2. CHAT HISTORY PERSISTENCE ---
    fun saveSessions(sessions: List<Session>) {
        val json = gson.toJson(sessions)
        prefs.edit().putString(KEY_SESSIONS, json).apply()
    }

    fun getSessions(): List<Session> {
        val json = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<Session>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- 3. THEME PERSISTENCE ---
    fun saveTheme(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_THEME, isDark).apply()
    }

    fun isDarkTheme(): Boolean {
        // Default to Dark Mode
        return prefs.getBoolean(KEY_THEME, true) 
    }
    
    // Clear everything (Logout)
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}