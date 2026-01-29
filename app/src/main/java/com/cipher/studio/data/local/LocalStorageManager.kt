package com.cipher.studio.data.local

import android.content.Context
import android.content.SharedPreferences
import com.cipher.studio.data.local.room.*
import com.cipher.studio.domain.model.*
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalStorageManager @Inject constructor(
    @ApplicationContext context: Context,
    private val cipherDao: CipherDao
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cipher_data", Context.MODE_PRIVATE)
    private val converters = CipherTypeConverters()
    private val gson = Gson()

    companion object {
        private const val KEY_IS_AUTHORIZED = "is_elite_authorized"
        private const val KEY_THEME = "app_theme"
    }

    // --- AUTH & THEME ---
    fun setAuthorized(isAuthorized: Boolean) = prefs.edit().putBoolean(KEY_IS_AUTHORIZED, isAuthorized).apply()
    fun isAuthorized(): Boolean = prefs.getBoolean(KEY_IS_AUTHORIZED, false)
    fun saveTheme(isDark: Boolean) = prefs.edit().putBoolean(KEY_THEME, isDark).apply()
    fun isDarkTheme(): Boolean = prefs.getBoolean(KEY_THEME, true)

    // --- SESSION MANAGEMENT (ROOM) ---
    fun saveSessions(sessions: List<Session>) {
        runBlocking {
            withContext(Dispatchers.IO) {
                sessions.forEach { session ->
                    // 1. Save Session
                    val sessionEntity = SessionEntity(
                        id = session.id,
                        title = session.title,
                        lastModified = session.lastModified,
                        configJson = converters.fromModelConfig(session.config)
                    )

                    // 2. Map Messages
                    val messageEntities = session.history.map { msg ->
                        MessageEntity(
                            id = msg.id ?: java.util.UUID.randomUUID().toString(),
                            sessionId = session.id,
                            role = msg.role.value, // FIX: Use .value property from your Enum
                            text = msg.text,
                            timestamp = msg.timestamp,
                            attachmentsJson = converters.fromAttachmentList(msg.attachments),
                            groundingJson = if (msg.groundingMetadata != null) gson.toJson(msg.groundingMetadata) else null
                        )
                    }

                    // 3. Save to DB
                    cipherDao.saveFullSession(sessionEntity, messageEntities)
                }
            }
        }
    }

    fun getSessions(): List<Session> {
        return runBlocking {
            withContext(Dispatchers.IO) {
                cipherDao.getAllSessions().map { entity ->
                    val messages = cipherDao.getMessagesForSession(entity.id).map { msgEntity ->
                        
                        // Recover Grounding Metadata
                        val grounding = if (msgEntity.groundingJson != null) {
                            try {
                                gson.fromJson(msgEntity.groundingJson, GroundingMetadata::class.java)
                            } catch (e: Exception) { null }
                        } else null

                        ChatMessage(
                            id = msgEntity.id,
                            role = ChatRole.fromValue(msgEntity.role), // FIX: Use helper from your Enum
                            text = msgEntity.text,
                            timestamp = msgEntity.timestamp,
                            attachments = converters.toAttachmentList(msgEntity.attachmentsJson),
                            pinned = false, 
                            groundingMetadata = grounding
                        )
                    }

                    Session(
                        id = entity.id,
                        title = entity.title,
                        history = messages,
                        config = converters.toModelConfig(entity.configJson),
                        lastModified = entity.lastModified
                    )
                }
            }
        }
    }
    
    fun clearAll() {
        prefs.edit().clear().apply()
        runBlocking {
            withContext(Dispatchers.IO) {
                // In a real app, you might want a DAO method to nuke tables
            }
        }
    }
}