package com.cipher.studio.domain.model

/**
 * The specific AI model versions available in the application.
 * Maps to the API string values.
 */
enum class ModelName(val value: String) {
    FLASH("gemini-2.5-flash"),
    PRO("gemini-3-pro-preview"),
    FLASH_LITE("gemini-flash-lite-latest");

    // Helper to find enum by string value (useful for JSON parsing)
    companion object {
        fun fromValue(value: String): ModelName? = entries.find { it.value == value }
    }
}

/**
 * Configuration for the Generative AI Model.
 */
data class ModelConfig(
    val model: ModelName = ModelName.FLASH,
    val temperature: Double = 1.0,
    val topK: Int = 64,
    val topP: Double = 0.95,
    val maxOutputTokens: Int = 32000,
    val systemInstruction: String = "You are a helpful and expert AI assistant."
)

/**
 * Represents a file attachment (usually Image or PDF) sent to the model.
 */
data class Attachment(
    val mimeType: String,
    val data: String // base64 encoded string
)

/**
 * Metadata for search grounding (citations/sources).
 */
data class GroundingMetadata(
    val groundingChunks: List<GroundingChunk> = emptyList()
)

data class GroundingChunk(
    val web: WebSource? = null
)

data class WebSource(
    val uri: String,
    val title: String
)

/**
 * Role of the message sender.
 */
enum class ChatRole(val value: String) {
    USER("user"),
    MODEL("model");
    
    companion object {
        fun fromValue(value: String): ChatRole = entries.find { it.value == value } ?: USER
    }
}

/**
 * A single message in the chat history.
 */
data class ChatMessage(
    val id: String? = null,
    val role: ChatRole, // Converted String literal to Enum for safety
    val text: String,
    val timestamp: Long,
    val attachments: List<Attachment> = emptyList(),
    val pinned: Boolean = false,
    val groundingMetadata: GroundingMetadata? = null // New: For Search Sources
)

/**
 * Represents a full Chat Session.
 */
data class Session(
    val id: String,
    val title: String,
    val history: List<ChatMessage> = emptyList(),
    val config: ModelConfig,
    val lastModified: Long
)

/**
 * AI Persona definitions.
 * Note: 'icon' is converted to String (URL or Resource Name) as we cannot store 'any' component in Data Class.
 */
data class Persona(
    val id: String,
    val name: String,
    val icon: String, // Changed from 'any' to String for Android compatibility
    val systemInstruction: String,
    val temperature: Double
)

/**
 * UI Theme preference.
 */
enum class Theme {
    LIGHT,
    DARK
}

/**
 * Text-to-Speech Voice options.
 */
enum class Voice {
    MALE,
    FEMALE,
    ROBOT
}

/**
 * Navigation destinations (Routes).
 */
enum class ViewMode(val route: String) {
    CHAT("chat"),
    CODE_LAB("codelab"),
    PROMPT_STUDIO("prompt-studio"),
    VISION_HUB("vision-hub"),
    DATA_ANALYST("data-analyst"),
    DOC_INTEL("doc-intel"),
    CYBER_HOUSE("cyber-house"),
    ABOUT("about")
}

/**
 * Global Constants
 */
object AppConstants {
    val DEFAULT_CONFIG = ModelConfig(
        model = ModelName.FLASH,
        temperature = 1.0,
        topK = 64,
        topP = 0.95,
        maxOutputTokens = 32000,
        systemInstruction = "You are a helpful and expert AI assistant."
    )
}
