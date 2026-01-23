package com.cipher.studio.presentation.docintel

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

enum class DocAnalysisMode {
    SUMMARY, AUDIT, INSIGHTS
}

@HiltViewModel
class DocIntelViewModel @Inject constructor(
    private val aiService: GenerativeAIService
) : ViewModel() {

    // State
    private val _docText = MutableStateFlow("")
    val docText = _docText.asStateFlow()

    private val _analysis = MutableStateFlow("")
    val analysis = _analysis.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    fun updateDocText(text: String) {
        _docText.value = text
    }

    fun runAnalysis(mode: DocAnalysisMode) {
        if (_docText.value.isBlank() || _isProcessing.value) return

        _isProcessing.value = true
        _analysis.value = ""

        var prompt = ""
        when (mode) {
            DocAnalysisMode.SUMMARY -> prompt = "Provide a comprehensive executive summary of this document. Bullet points for key takeaways."
            DocAnalysisMode.AUDIT -> prompt = "Audit this text. Find potential risks, contradictions, legal loopholes, or weak arguments."
            DocAnalysisMode.INSIGHTS -> prompt = "Extract hidden insights, patterns, and actionable intelligence from this text that might not be immediately obvious."
        }

        // Include the text in the prompt context
        val fullPrompt = "$prompt\n\nDOCUMENT:\n${_docText.value}"

        val config = ModelConfig(
            model = ModelName.PRO, // Pro is better for large context/reasoning
            temperature = 0.5,
            systemInstruction = "You are a Senior Document Analyst. Your output should be structured, professional, and incredibly detailed."
        )

        // API Key Placeholder
        val apiKey = "YOUR_API_KEY_HERE"

        viewModelScope.launch {
            aiService.generateContentStream(
                apiKey = apiKey,
                prompt = fullPrompt,
                attachments = emptyList(),
                history = emptyList(), // Single shot analysis
                config = config
            ).collect { result ->
                when (result) {
                    is StreamResult.Content -> {
                        _analysis.value += result.text
                    }
                    is StreamResult.Error -> {
                        _analysis.value = "Error processing document: ${result.message}"
                        _isProcessing.value = false
                    }
                    is StreamResult.Metadata -> { /* Ignore */ }
                }
            }
            _isProcessing.value = false
        }
    }
}