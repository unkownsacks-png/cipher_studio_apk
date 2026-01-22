package com.cipher.studio.presentation.dataanalyst

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.domain.model.ModelConfig
import com.cipher.studio.domain.model.ModelName
import com.cipher.studio.domain.model.Theme
import com.cipher.studio.domain.service.GenerativeAIService
import com.cipher.studio.domain.service.StreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataAnalystViewModel @Inject constructor(
    private val aiService: GenerativeAIService
) : ViewModel() {

    // State
    private val _dataInput = MutableStateFlow("")
    val dataInput = _dataInput.asStateFlow()

    private val _chartCode = MutableStateFlow("")
    val chartCode = _chartCode.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    // Configuration
    private val systemInstruction = """
        You are a Data Visualization Expert. 
        Your goal is to take raw text, CSV, or JSON data provided by the user and turn it into a beautiful, modern HTML Chart using Chart.js.

        RULES:
        1. Output a SINGLE HTML file containing the Chart.js CDN logic.
        2. The design must be modern. Use a dark theme if specified, otherwise light.
        3. Make the charts interactive and animated.
        4. Do not include markdown ticks like ```html. Just return the raw HTML code.
        5. Ensure the chart takes up the full width/height of the window (Use CSS: body, html { margin:0; height:100%; overflow:hidden; }).
        6. Use nice colors (gradients if possible) that match a professional dashboard.
    """.trimIndent()

    fun updateDataInput(input: String) {
        _dataInput.value = input
    }

    fun handleVisualize(theme: Theme) {
        if (_dataInput.value.isBlank() || _isGenerating.value) return

        _isGenerating.value = true
        _chartCode.value = ""

        val prompt = "Create a visualization for this data. If the user didn't specify chart type, pick the best one: ${_dataInput.value}. Theme: ${if (theme == Theme.DARK) "dark" else "light"}"

        val config = ModelConfig(
            model = ModelName.FLASH, // Fast model for code generation
            temperature = 1.0,
            systemInstruction = systemInstruction
        )

        // API Key Placeholder
        val apiKey = "YOUR_API_KEY_HERE"

        viewModelScope.launch {
            var fullHtml = ""
            
            aiService.generateContentStream(
                apiKey = apiKey,
                prompt = prompt,
                attachments = emptyList(),
                history = emptyList(),
                config = config
            ).collect { result ->
                when (result) {
                    is StreamResult.Content -> {
                        fullHtml += result.text
                        // Clean markdown ticks if AI adds them despite instructions
                        val cleanCode = fullHtml
                            .replace("```html", "")
                            .replace("```", "")
                        _chartCode.value = cleanCode
                    }
                    is StreamResult.Error -> {
                        _chartCode.value = "<html><body><h2 style='color:red'>Error generating chart: ${result.message}</h2></body></html>"
                        _isGenerating.value = false
                    }
                    is StreamResult.Metadata -> { /* Ignore */ }
                }
            }
            _isGenerating.value = false
        }
    }
}