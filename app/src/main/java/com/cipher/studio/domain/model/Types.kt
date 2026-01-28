package com.cipher.studio.domain.model

enum class ChatRole { USER, MODEL }
enum class ViewMode { CHAT, CODE_LAB, VISION_HUB, PROMPT_STUDIO, CYBER_HOUSE, DATA_ANALYST, DOC_INTEL, ABOUT }
enum class Theme { LIGHT, DARK }
enum class AiModel(val value: String) {
    GEMINI_PRO("gemini-pro"),
    GEMINI_FLASH("gemini-1.5-flash"),
    CIPHER_ULTRA("gemini-1.5-pro")
}

data class ChatMessage(
    val id: String? = null,
    val role: ChatRole,
    val text: String? = null,
    val timestamp: Long,
    val attachments: List<Attachment> = emptyList(),
    val pinned: Boolean = false
)

data class Attachment(val mimeType: String, val data: String)

data class Session(
    val id: String,
    val title: String,
    val history: List<ChatMessage>,
    val config: ModelConfig,
    val lastModified: Long
)

data class ModelConfig(
    val model: AiModel = AiModel.GEMINI_PRO,
    val temperature: Double = 0.7,
    val topK: Int = 40,
    val topP: Double = 0.95,
    val maxOutputTokens: Int = 2048,
    val systemInstruction: String = "You are Cipher, an advanced AI assistant."
)

object AppConstants {
    val DEFAULT_CONFIG = ModelConfig()
}