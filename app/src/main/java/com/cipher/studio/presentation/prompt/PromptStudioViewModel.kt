package com.cipher.studio.presentation.prompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.data.local.ApiKeyManager // IMPORT ADDED
import com.cipher.studio.domain.model.ModelConfig
import com.cipher.studio.domain.model.ModelName
import com.cipher.studio.domain.service.GenerativeAIService
import com.cipher.studio.domain.service.StreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PromptStudioViewModel @Inject constructor(
    private val aiService: GenerativeAIService,
    private val apiKeyManager: ApiKeyManager // FIX 1: Dependency Injection
) : ViewModel() {

    // State
    private val _inputPrompt = MutableStateFlow("")
    val inputPrompt = _inputPrompt.asStateFlow()

    private val _optimizedPrompt = MutableStateFlow("")
    val optimizedPrompt = _optimizedPrompt.asStateFlow()

    private val _explanation = MutableStateFlow("")
    val explanation = _explanation.asStateFlow()

    private val _isOptimizing = MutableStateFlow(false)
    val isOptimizing = _isOptimizing.asStateFlow()

    // ENHANCED SYSTEM INSTRUCTION: Uses CO-STAR Framework for elite prompting
    private val systemInstruction = """
        You are a World-Class Prompt Engineer specializing in LLM optimization.
        Your goal is to transform basic user inputs into high-performance prompts using the CO-STAR framework (Context, Objective, Style, Tone, Audience, Response).
        
        OUTPUT RULES:
        1. Return the response in two distinct sections separated EXACTLY by "|||SEPARATOR|||".
        2. Section 1: The Optimized Prompt. It must be ready-to-use, enclosed in clear delimiters (like ###), and utilize Chain-of-Thought prompting where necessary.
        3. Section 2: Strategy Breakdown. Explain *why* you made changes (e.g., "Added constraints to prevent hallucinations", "Specified output format for parsing").
        
        DO NOT include markdown code blocks (```) for the separator, just raw text.
    """.trimIndent()

    fun updateInput(text: String) {
        _inputPrompt.value = text
    }

    fun handleOptimize() {
        if (_inputPrompt.value.isBlank() || _isOptimizing.value) return

        _isOptimizing.value = true
        _optimizedPrompt.value = ""
        _explanation.value = ""

        // FIX 2: Retrieve API Key
        val apiKey = apiKeyManager.getApiKey()

        // FIX 3: Validate Key
        if (apiKey.isNullOrBlank()) {
            _optimizedPrompt.value = "API Key Missing"
            _explanation.value = "⚠️ SYSTEM ALERT: Please configure your Gemini API Key in the Settings menu to unlock Prompt Studio."
            _isOptimizing.value = false
            return
        }

        viewModelScope.launch {
            val config = ModelConfig(
                model = ModelName.PRO, // Pro model is better for reasoning/instruction following
                temperature = 0.7, // Balanced creativity
                systemInstruction = systemInstruction
            )

            var fullText = ""

            // FIX 4: Use validated apiKey
            aiService.generateContentStream(
                apiKey = apiKey,
                prompt = "Optimize this prompt: \"${_inputPrompt.value}\"",
                attachments = emptyList(),
                history = emptyList(),
                config = config
            ).collect { result ->
                when (result) {
                    is StreamResult.Content -> {
                        fullText += result.text
                        parseResult(fullText)
                    }
                    is StreamResult.Error -> {
                        _explanation.value = "Optimization Failed: ${result.message}"
                        _isOptimizing.value = false
                    }
                    is StreamResult.Metadata -> { /* Ignore */ }
                }
            }
            _isOptimizing.value = false
        }
    }

    private fun parseResult(text: String) {
        // Split based on the separator defined in the system instruction
        val parts = text.split("|||SEPARATOR|||")

        if (parts.isNotEmpty()) {
            _optimizedPrompt.value = parts[0].trim()
        }

        if (parts.size > 1) {
            _explanation.value = parts[1].trim()
        }
    }
}