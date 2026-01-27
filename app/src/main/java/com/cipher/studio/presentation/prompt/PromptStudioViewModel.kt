package com.cipher.studio.presentation.prompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.data.local.ApiKeyManager
import com.cipher.studio.domain.model.ModelConfig
import com.cipher.studio.domain.model.ModelName
import com.cipher.studio.domain.service.GenerativeAIService
import com.cipher.studio.domain.service.StreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// FEATURE 5: Analysis Badges
enum class PromptQuality { POOR, OKAY, GREAT, PERFECT }

@HiltViewModel
class PromptStudioViewModel @Inject constructor(
    private val aiService: GenerativeAIService,
    private val apiKeyManager: ApiKeyManager
) : ViewModel() {

    private val _inputPrompt = MutableStateFlow("")
    val inputPrompt = _inputPrompt.asStateFlow()

    private val _optimizedPrompt = MutableStateFlow("")
    val optimizedPrompt = _optimizedPrompt.asStateFlow()

    private val _explanation = MutableStateFlow("")
    val explanation = _explanation.asStateFlow()

    private val _isOptimizing = MutableStateFlow(false)
    val isOptimizing = _isOptimizing.asStateFlow()

    // FEATURE 5: Quality Analysis
    private val _quality = MutableStateFlow(PromptQuality.OKAY)
    val quality = _quality.asStateFlow()

    fun updateInput(text: String) { _inputPrompt.value = text }
    
    // FEATURE 3: Template Chips Logic
    fun applyTemplate(template: String) {
        val base = when(template) {
            "Coding" -> "Write a Python script to..."
            "Creative" -> "Write a dystopian story about..."
            "Business" -> "Draft a professional email to..."
            "Academic" -> "Summarize the key concepts of..."
            else -> ""
        }
        _inputPrompt.value = base
    }

    fun handleOptimize() {
        if (_inputPrompt.value.isBlank()) return
        
        _isOptimizing.value = true
        _optimizedPrompt.value = "" // Clear previous
        _explanation.value = ""
        
        val apiKey = apiKeyManager.getApiKey() ?: return

        // ADVANCED SYSTEM PROMPT
        val systemPrompt = """
            You are a Prompt Engineer Expert. 
            1. Analyze the user's prompt.
            2. REWRITE it to be highly detailed, structured, and optimized for LLMs (using personas, constraints, steps).
            3. Provide a short explanation of WHY you made changes.
            4. Rate the original prompt (POOR, OKAY, GREAT, PERFECT).
            
            Format output as:
            [RATING]
            ...
            [EXPLANATION]
            ...
            [OPTIMIZED]
            ...
        """.trimIndent()

        viewModelScope.launch {
            val config = ModelConfig(
                model = ModelName.FLASH,
                temperature = 0.7,
                systemInstruction = systemPrompt
            )

            // FEATURE 2: Streaming Logic
            var fullResponse = ""
            
            aiService.generateContentStream(
                apiKey = apiKey,
                prompt = _inputPrompt.value,
                attachments = emptyList(),
                history = emptyList(),
                config = config
            ).collect { result ->
                when(result) {
                    is StreamResult.Content -> {
                        fullResponse += result.text
                        parseResponse(fullResponse)
                    }
                    is StreamResult.Error -> {
                        _explanation.value = "Error: ${result.message}"
                        _isOptimizing.value = false
                    }
                    else -> {}
                }
            }
            _isOptimizing.value = false
        }
    }

    private fun parseResponse(response: String) {
        // Simple parser to split the AI response into sections
        try {
            if (response.contains("[OPTIMIZED]")) {
                val parts = response.split("[OPTIMIZED]")
                val prePart = parts[0]
                val optimizedPart = parts[1]

                _optimizedPrompt.value = optimizedPart.trim()

                if (prePart.contains("[EXPLANATION]")) {
                    val expParts = prePart.split("[EXPLANATION]")
                    val ratingPart = expParts[0]
                    _explanation.value = expParts[1].trim()
                    
                    // Parse Rating
                    if (ratingPart.contains("POOR")) _quality.value = PromptQuality.POOR
                    else if (ratingPart.contains("OKAY")) _quality.value = PromptQuality.OKAY
                    else if (ratingPart.contains("GREAT")) _quality.value = PromptQuality.GREAT
                    else if (ratingPart.contains("PERFECT")) _quality.value = PromptQuality.PERFECT
                }
            } else {
                // Fallback if streaming is partial
                _explanation.value = "Optimizing..."
            }
        } catch (e: Exception) {
            // Ignore parse errors during streaming
        }
    }
}