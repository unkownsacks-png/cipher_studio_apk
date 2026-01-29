package com.cipher.studio.data.local

import android.content.Context
import android.content.SharedPreferences
import com.cipher.studio.data.local.room.CipherDao
import com.cipher.studio.data.local.room.CipherTypeConverters
import com.cipher.studio.data.local.room.MessageEntity
import com.cipher.studio.data.local.room.SessionEntity
import com.cipher.studio.domain.model.ChatMessage
import com.cipher.studio.domain.model.ChatRole
import com.cipher.studio.domain.model.Session
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ELITE STORAGE MANAGER (ROOM EDITION)
 * 
 * Bridges the gap between Domain Models and Room Database.
 * Handles extensive data mapping efficiently.
 */
@Singleton
class LocalStorageManager @Inject constructor(
    @ApplicationContext context: Context,
    private val cipherDao: CipherDao
) {
    // Legacy Prefs for simple flags
    private val prefs: SharedPreferences = context.getSharedPreferences("cipher_data", Context.MODE_PRIVATE)
    private val converters = CipherTypeConverters()

    companion object {
        private const val KEY_IS_AUTHORIZED = "is_elite_authorized"
        private const val KEY_THEME = "app_theme"
    }

    // --- 1. AUTH & THEME (Keep Simple) ---
    fun setAuthorized(isAuthorized: Boolean) = prefs.edit().putBoolean(KEY_IS_AUTHORIZED, isAuthorized).apply()
    fun isAuthorized(): Boolean = prefs.getBoolean(KEY_IS_AUTHORIZED, false)
    
    fun saveTheme(isDark: Boolean) = prefs.edit().putBoolean(KEY_THEME, isDark).apply()
    fun isDarkTheme(): Boolean = prefs.getBoolean(KEY_THEME, true)

    // --- 2. SESSION MANAGEMENT (POWERED BY ROOM) ---

    // Save a list of sessions (Usually called when updating history)
    // We use runBlocking here to be compatible with the ViewModel's structure,
    // but the internal DAO call handles concurrency safely.
    fun saveSessions(sessions: List<Session>) {
        runBlocking {
            withContext(Dispatchers.IO) {
                sessions.forEach { session ->
                    val sessionEntity = SessionEntity(
                        id = session.id,
                        title = session.title,
                        lastModified = session.lastModified,
                        configJson = converters.fromModelConfig(session.config)
                    )

                    val messageEntities = session.history.map { msg ->
                        MessageEntity(
                            id = msg.id ?: java.util.UUID.randomUUID().toString(),
                            sessionId = session.id,
                            role = if (msg.role == ChatRole.USER) "user" else "model",
                            text = msg.text ?: "",
                            timestamp = msg.timestamp,
                            attachmentsJson = converters.fromAttachmentList(msg.attachments)
                        )
                    }

                    cipherDao.saveFullSession(sessionEntity, messageEntities)
                }
            }
        }
    }

    // Load all sessions with their messages
    // Optimized to load fast.
    fun getSessions(): List<Session> {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val sessionEntities = cipherDao.getAllSessions()
                
                sessionEntities.map { entity ->
                    val messageEntities = cipherDao.getMessagesForSession(entity.id)
                    
                    val history = messageEntities.map { msgEntity ->
                        ChatMessage(
                            id = msgEntity.id,
                            role = if (msgEntity.role == "user") ChatRole.USER else ChatRole.MODEL,
                            text = msgEntity.text,
                            timestamp = msgEntity.timestamp,
                            attachments = converters.toAttachmentList(msgEntity.attachmentsJson)
                        )
                    }

                    Session(
                        id = entity.id,
                        title = entity.title,
                        history = history,
                        config = converters.toModelConfig(entity.configJson),
                        lastModified = entity.lastModified
                    )
                }
            }
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        // Database clear logic would go here if needed (nuke table)
    }
}