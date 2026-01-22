package com.cipher.studio.domain.service

import com.cipher.studio.domain.model.*
import kotlinx.coroutines.flow.Flow

interface GenerativeAIService {
    /**
     * Maps to: streamContent(...) in geminiService.ts
     */
    fun generateContentStream(
        apiKey: String,
        prompt: String,
        attachments: List<Attachment>,
        history: List<ChatMessage>,
        config: ModelConfig
    ): Flow<StreamResult>
}

// Helper wrapper for stream results
sealed class StreamResult {
    data class Content(val text: String) : StreamResult()
    data class Metadata(val metadata: GroundingMetadata) : StreamResult()
    data class Error(val message: String) : StreamResult()
}