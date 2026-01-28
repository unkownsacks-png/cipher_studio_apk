package com.cipher.studio.data.local.room

import androidx.room.*

@Dao
interface CipherDao {

    // --- SESSIONS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY lastModified DESC")
    fun getAllSessions(): List<SessionEntity> // Blocking for initial load (compatible with legacy architecture)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    // --- MESSAGES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    // --- TRANSACTION (ATOMIC OPERATION) ---
    // Session እና Messages በአንድ ጊዜ ማስገባት
    @Transaction
    suspend fun saveFullSession(session: SessionEntity, messages: List<MessageEntity>) {
        insertSession(session)
        // አሮጌ መልዕክቶችን አጥፍተን አዲሱን ማስገባት (To ensure sync)
        deleteMessagesForSession(session.id)
        insertMessages(messages)
    }
}