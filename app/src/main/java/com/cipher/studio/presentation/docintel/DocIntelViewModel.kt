package com.cipher.studio.presentation.docintel

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

enum class DocAnalysisMode { 
    SUMMARY, RISKS, INSIGHTS, 
    // New Modes
    CONTRACT_REVIEW, ACADEMIC_PAPER, EMAIL_TONE 
}

// FEATURE 2: Tabs for organized output
enum class ResultTab { OVERVIEW, KEY_POINTS, ACTION_ITEMS, RAW }

@HiltViewModel
class DocIntelViewModel @Inject constructor(
    private val aiService: GenerativeAIService,
    private val apiKeyManager: ApiKeyManager
) : ViewModel() {

    private val _docText = MutableStateFlow("")
    val docText = _docText.asStateFlow()

    private val _analysis = MutableStateFlow("")
    val analysis = _analysis.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    // FEATURE 2: Active Tab State
    private val _activeTab = MutableStateFlow(ResultTab.OVERVIEW)
    val activeTab = _activeTab.asStateFlow()

    // FEATURE 4: Sentiment Score (0 to 100)
    private val _sentimentScore = MutableStateFlow(50)
    val sentimentScore = _sentimentScore.asStateFlow()

    // FEATURE 5: Extracted Keywords
    private val _keywords = MutableStateFlow<List<String>>(emptyList())
    val keywords = _keywords.asStateFlow()

    // FEATURE 7: Q&A Chat State
    private val _qaQuery = MutableStateFlow("")
    val qaQuery = _qaQuery.asStateFlow()
    private val _qaAnswer = MutableStateFlow("")
    val qaAnswer = _qaAnswer.asStateFlow()

    fun updateDocText(text: String) { _docText.value = text }
    fun setActiveTab(tab: ResultTab) { _activeTab.value = tab }
    fun updateQaQuery(text: String) { _qaQuery.value = text }

    fun runAnalysis(mode: DocAnalysisMode) {
        if (_docText.value.isBlank()) return
        
        _isProcessing.value = true
        _analysis.value = ""
        _keywords.value = emptyList() // Clear previous
        _sentimentScore.value = 50 // Reset neutral
        
        val apiKey = apiKeyManager.getApiKey() ?: return

        // ADVANCED PROMPT ENGINEERING
        val systemPrompt = """
            You are an elite Document Intelligence AI.
            Analyze the provided text based on the requested mode.
            
            IMPORTANT FORMATTING:
            1. Return the main analysis in Markdown.
            2. At the very end, append a JSON-like block for metadata:
               [[METADATA]]
               KEYWORDS: key1, key2, key3, key4
               SENTIMENT: 0-100 (where 0 is negative, 100 is positive)
        """.trimIndent()

        val userPrompt = when(mode) {
            DocAnalysisMode.SUMMARY -> "Provide a comprehensive Executive Summary."
            DocAnalysisMode.RISKS -> "Identify all Legal Risks, Liabilities, and Red Flags."
            DocAnalysisMode.INSIGHTS -> "Extract hidden insights, trends, and strategic value."
            DocAnalysisMode.CONTRACT_REVIEW -> "Review this contract. Highlight missing clauses and unfair terms."
            DocAnalysisMode.ACADEMIC_PAPER -> "Simplify this academic paper. Explain methodology and conclusion."
            DocAnalysisMode.EMAIL_TONE -> "Analyze the tone of this email. Suggest improvements for clarity/politeness."
        }

        val fullPrompt = "TEXT:\n${_docText.value}\n\nTASK: $userPrompt"

        viewModelScope.launch {
            val config = ModelConfig(
                model = ModelName.FLASH,
                temperature = 0.3, // Low temp for accuracy
                systemInstruction = systemPrompt
            )

            var fullResponse = ""

            aiService.generateContentStream(
                apiKey = apiKey,
                prompt = fullPrompt,
                attachments = emptyList(),
                history = emptyList(),
                config = config
            ).collect { result ->
                when(result) {
                    is StreamResult.Content -> {
                        fullResponse += result.text
                        // Filter out metadata from live display
                        if (!fullResponse.contains("[[METADATA]]")) {
                            _analysis.value = fullResponse
                        } else {
                            val parts = fullResponse.split("[[METADATA]]")
                            _analysis.value = parts[0].trim()
                            parseMetadata(parts[1])
                        }
                    }
                    is StreamResult.Error -> {
                        _analysis.value = "Error: ${result.message}"
                        _isProcessing.value = false
                    }
                    else -> {}
                }
            }
            _isProcessing.value = false
        }
    }

    // FEATURE 7: Ask Question about Doc
    fun askQuestion() {
        if (_qaQuery.value.isBlank() || _docText.value.isBlank()) return
        
        viewModelScope.launch {
            val apiKey = apiKeyManager.getApiKey() ?: return
            val prompt = "CONTEXT:\n${_docText.value}\n\nQUESTION: ${_qaQuery.value}\n\nAnswer briefly based ONLY on the context."
            
            val config = ModelConfig(model = ModelName.GEMINI_PRO, temperature = 0.1)
            
            aiService.generateContentStream(
                apiKey = apiKey,
                prompt = prompt,
                attachments = emptyList(),
                history = emptyList(),
                config = config
            ).collect { result ->
                if (result is StreamResult.Content) {
                    _qaAnswer.value += result.text
                }
            }
        }
    }

    private fun parseMetadata(meta: String) {
        try {
            val lines = meta.lines()
            lines.forEach { line ->
                if (line.contains("KEYWORDS:")) {
                    val keys = line.substringAfter("KEYWORDS:").split(",").map { it.trim() }
                    _keywords.value = keys.take(5)
                }
                if (line.contains("SENTIMENT:")) {
                    val score = line.substringAfter("SENTIMENT:").trim().toIntOrNull() ?: 50
                    _sentimentScore.value = score
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
    }
}