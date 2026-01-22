package com.cipher.studio.presentation.prompt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val aiService: GenerativeAIService
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

    // System Instruction specifically for Prompt Engineering
    private val systemInstruction = """
        You are a World-Class Prompt Engineer. Your goal is to take a basic user input and transform it into a highly effective, structured prompt for an LLM.
        
        OUTPUT FORMAT:
        Return the response in two distinct sections separated by "|||SEPARATOR|||".
        
        Section 1: The Optimized Prompt (Ready to Copy). Use techniques like Persona adoption, Chain of Thought, and Clear Constraints.
        Section 2: Brief explanation of what you changed and why (3-4 bullet points).

        Do not use markdown blocks for the optimized prompt section, just raw text.
    """.trimIndent()

    fun updateInput(text: String) {
        _inputPrompt.value = text
    }

    fun handleOptimize() {
        if (_inputPrompt.value.isBlank() || _isOptimizing.value) return

        _isOptimizing.value = true
        _optimizedPrompt.value = ""
        _explanation.value = ""

        viewModelScope.launch {
            val config = ModelConfig(
                model = ModelName.FLASH,
                temperature = 1.0,
                systemInstruction = systemInstruction
            )

            // NOTE: API Key placeholder. In a real app, inject via UseCase or Repository.
            val apiKey = "YOUR_API_KEY_HERE" 
            
            var fullText = ""

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
                        _explanation.value = "Error: ${result.message}"
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