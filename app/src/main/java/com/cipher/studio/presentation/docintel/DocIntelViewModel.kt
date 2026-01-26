package com.cipher.studio.presentation.docintel

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

enum class DocAnalysisMode {
    SUMMARY, AUDIT, INSIGHTS
}

@HiltViewModel
class DocIntelViewModel @Inject constructor(
    private val aiService: GenerativeAIService,
    private val apiKeyManager: ApiKeyManager // FIX 1: Dependency Injection
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

        // FIX 2: Retrieve API Key
        val apiKey = apiKeyManager.getApiKey()

        // FIX 3: Validate Key
        if (apiKey.isNullOrBlank()) {
            _analysis.value = "## ⚠️ ACCESS DENIED\n\nAPI Key not found. Please navigate to Settings and configure your Gemini API Key to unlock Document Intelligence."
            _isProcessing.value = false
            return
        }

        var prompt = ""
        when (mode) {
            DocAnalysisMode.SUMMARY -> prompt = "Provide a comprehensive executive summary. Use headers, bullet points for key takeaways, and a final 'Actionable Verdict'."
            DocAnalysisMode.AUDIT -> prompt = "Perform a Forensic Audit on this text. Highlight:\n1. Potential Risks\n2. Legal Loopholes\n3. Contradictions\n4. Ambiguous Language."
            DocAnalysisMode.INSIGHTS -> prompt = "Extract hidden insights, patterns, and actionable intelligence. Identify key entities, sentiment, and implied intent that is not immediately obvious."
        }

        // Include the text in the prompt context
        val fullPrompt = "$prompt\n\n--- TARGET DOCUMENT ---\n${_docText.value}"

        // ENHANCED REAL-WORLD SYSTEM INSTRUCTION
        val systemInstruction = """
            You are a Senior Document Analyst & Intelligence Officer. 
            
            OUTPUT RULES:
            1. Format your response in clean Markdown (use ## for headers, **bold** for emphasis).
            2. Be objective, critical, and extremely detailed.
            3. If the document contains sensitive PII (Personally Identifiable Information), flag it in a warning block at the top.
            4. Never hallucinate facts not present in the text.
        """.trimIndent()

        val config = ModelConfig(
            model = ModelName.PRO, // Pro is better for large context/reasoning
            temperature = 0.3, // Lower temperature for factual accuracy
            systemInstruction = systemInstruction
        )

        viewModelScope.launch {
            // FIX 4: Use validated apiKey
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
                        _analysis.value = "## 🛑 System Error\n\nFailed to process document.\nError Details: ${result.message}"
                        _isProcessing.value = false
                    }
                    is StreamResult.Metadata -> { /* Ignore */ }
                }
            }
            _isProcessing.value = false
        }
    }
}