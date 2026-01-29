package com.cipher.studio.data.local.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.cipher.studio.domain.model.Attachment
import com.cipher.studio.domain.model.ModelConfig
import com.cipher.studio.domain.model.ModelName
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// --- 1. SESSION TABLE ---
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val lastModified: Long,
    val configJson: String
)

// --- 2. MESSAGE TABLE ---
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String, // Stored as "user" or "model"
    val text: String,
    val timestamp: Long,
    val attachmentsJson: String,
    val groundingJson: String?
)

// --- 3. CONVERTERS (Independent) ---
class CipherTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromModelConfig(config: ModelConfig): String {
        return gson.toJson(config)
    }

    @TypeConverter
    fun toModelConfig(json: String): ModelConfig {
        return try {
            gson.fromJson(json, ModelConfig::class.java)
        } catch (e: Exception) {
            // FIX: Removed dependency on AppConstants to prevent KAPT crash
            // We create a fresh default instance here locally.
            ModelConfig(
                model = ModelName.FLASH,
                temperature = 1.0,
                topK = 64,
                topP = 0.95,
                maxOutputTokens = 32000,
                systemInstruction = "You are a helpful and expert AI assistant."
            )
        }
    }

    @TypeConverter
    fun fromAttachmentList(attachments: List<Attachment>): String {
        return gson.toJson(attachments)
    }

    @TypeConverter
    fun toAttachmentList(json: String): List<Attachment> {
        return try {
            val type = object : TypeToken<List<Attachment>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}