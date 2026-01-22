package com.cipher.studio.presentation.codelab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.domain.model.Attachment
import com.cipher.studio.domain.model.ChatMessage
import com.cipher.studio.domain.model.ChatRole
import com.cipher.studio.domain.model.ModelConfig
import com.cipher.studio.domain.model.ModelName
import com.cipher.studio.domain.service.GenerativeAIService
import com.cipher.studio.domain.service.StreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CodeLabViewMode {
    CODE,
    PREVIEW,
    SPLIT
}

@HiltViewModel
class CodeLabViewModel @Inject constructor(
    private val aiService: GenerativeAIService
) : ViewModel() {

    // State
    private val _prompt = MutableStateFlow("")
    val prompt = _prompt.asStateFlow()

    private val _code = MutableStateFlow("")
    val code = _code.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _viewMode = MutableStateFlow(CodeLabViewMode.SPLIT)
    val viewMode = _viewMode.asStateFlow()

    // Configuration for CodeLab
    private val systemInstruction = """
        You are an expert Frontend Developer. 
        The user wants you to build a single-file HTML application (HTML + CSS + JS).
        
        RULES:
        1. Return ONLY the raw HTML code. Do not wrap it in markdown blocks (like ```html).
        2. The code must be a complete, working HTML file.
        3. Use Tailwind CSS via CDN for styling if needed.
        4. Make it look modern, clean, and professional.
        5. Do not add explanations. Just the code.
    """.trimIndent()

    fun updatePrompt(text: String) {
        _prompt.value = text
    }

    fun setViewMode(mode: CodeLabViewMode) {
        _viewMode.value = mode
    }

    fun handleGenerate() {
        if (_prompt.value.isBlank() || _isGenerating.value) return

        _isGenerating.value = true
        _code.value = "" // Clear previous code

        viewModelScope.launch {
            val config = ModelConfig(
                model = ModelName.FLASH, // Using Flash for speed as per TS file
                temperature = 1.0,
                systemInstruction = systemInstruction
            )

            // We mimic the chat structure but it's a single-shot request here
            val history = emptyList<ChatMessage>() // CodeLab doesn't seem to maintain long history in your TS file
            
            // NOTE: API Key handling should be centralized. 
            // Assuming we pass it or the service handles it from a secure repo.
            // For now, using a placeholder or injected key logic from MainViewModel context if linked.
            val apiKey = "YOUR_API_KEY_OR_INJECTED_PREFERENCE" 

            var accumulatedCode = ""

            aiService.generateContentStream(
                apiKey = apiKey, 
                prompt = _prompt.value, 
                attachments = emptyList(), 
                history = history, 
                config = config
            ).collect { result ->
                when (result) {
                    is StreamResult.Content -> {
                        // Simple cleaning if the model accidentally adds markdown despite instructions
                        val cleanChunk = result.text.replace("```html", "").replace("```", "")
                        accumulatedCode += cleanChunk
                        _code.value = accumulatedCode
                    }
                    is StreamResult.Error -> {
                        _code.value = "<!-- Error generating code: ${result.message} -->"
                        _isGenerating.value = false
                    }
                    is StreamResult.Metadata -> { /* Ignore metadata for CodeLab */ }
                }
            }
            _isGenerating.value = false
        }
    }
}