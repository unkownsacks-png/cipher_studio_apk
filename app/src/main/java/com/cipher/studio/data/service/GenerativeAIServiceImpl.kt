package com.cipher.studio.data.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.cipher.studio.domain.model.*
import com.cipher.studio.domain.service.GenerativeAIService
import com.cipher.studio.domain.service.StreamResult
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerativeAIServiceImpl @Inject constructor() : GenerativeAIService {

    override fun generateContentStream(
        apiKey: String,
        prompt: String,
        attachments: List<Attachment>,
        history: List<ChatMessage>,
        config: ModelConfig
    ): Flow<StreamResult> = flow {

        if (apiKey.isBlank()) {
            emit(StreamResult.Error("No API Key found."))
            return@flow
        }

        // 1. Configure Model
        val generativeModel = GenerativeModel(
            modelName = config.model.value, // FIX: Use .value from ModelName Enum
            apiKey = apiKey,
            systemInstruction = content { text(config.systemInstruction) },
            generationConfig = generationConfig {
                temperature = config.temperature.toFloat()
                topK = config.topK
                topP = config.topP.toFloat()
                maxOutputTokens = config.maxOutputTokens
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
            )
        )

        // 2. Build History
        val sdkHistory = mutableListOf<Content>()
        val pastHistory = if (history.isNotEmpty()) history.dropLast(1) else emptyList()

        pastHistory.forEach { msg ->
            val roleStr = msg.role.value // FIX: Use .value ("user" or "model")
            
            // Fix User->User collision
            if (sdkHistory.isNotEmpty() && sdkHistory.last().role == "user" && roleStr == "user") {
                sdkHistory.add(content("model") { text("...") })
            }

            sdkHistory.add(content(roleStr) {
                text(msg.text)
                msg.attachments.forEach { att ->
                    val bmp = base64ToBitmap(att.data)
                    if (bmp != null) image(bmp)
                }
            })
        }

        // Ensure history ends with model if sending user prompt
        if (sdkHistory.isNotEmpty() && sdkHistory.last().role == "user") {
            sdkHistory.add(content("model") { text("Ready.") })
        }

        try {
            val chat = generativeModel.startChat(history = sdkHistory)
            
            val inputContent = content {
                text(prompt)
                attachments.forEach { att ->
                    val bmp = base64ToBitmap(att.data)
                    if (bmp != null) image(bmp)
                }
            }

            val responseStream = chat.sendMessageStream(inputContent)

            responseStream.collect { chunk ->
                if (chunk.text != null) {
                    emit(StreamResult.Content(chunk.text!!))
                }
            }
        } catch (e: Exception) {
            emit(StreamResult.Error("AI Error: ${e.message}"))
        }

    }.catch { e ->
        emit(StreamResult.Error("Critical Error: ${e.message}"))
    }.flowOn(Dispatchers.IO) // Keeps it off main thread

    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
    }
}