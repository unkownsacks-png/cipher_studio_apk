package com.cipher.studio.presentation.dataanalyst

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.data.local.ApiKeyManager // IMPORT ADDED
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
    private val aiService: GenerativeAIService,
    private val apiKeyManager: ApiKeyManager // FIX 1: Dependency Injection
) : ViewModel() {

    // State
    private val _dataInput = MutableStateFlow("")
    val dataInput = _dataInput.asStateFlow()

    private val _chartCode = MutableStateFlow("")
    val chartCode = _chartCode.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    // Configuration - ENHANCED for Real World Utility
    private val systemInstruction = """
        You are a Data Visualization Expert and Frontend Engineer. 
        Your goal is to take raw text, CSV, or JSON data provided by the user and turn it into a beautiful, modern HTML Chart using Chart.js.

        RULES:
        1. Output a SINGLE HTML file containing the Chart.js CDN logic.
        2. The design must be modern. Use a dark theme if specified, otherwise light.
        3. Make the charts interactive and animated.
        4. Do not include markdown ticks like ```html. Just return the raw HTML code.
        5. Ensure the chart takes up the full width/height of the window (Use CSS: body, html { margin:0; height:100%; overflow:hidden; display:flex; flex-direction:column; }).
        6. Use nice colors (gradients if possible) that match a professional dashboard.
        
        REAL-WORLD FEATURES:
        7. DATA PARSING: The user input might be messy. Write robust JavaScript to parse CSV or JSON strings automatically before rendering.
        8. DOWNLOAD BUTTON: Include a stylish, floating button (bottom-right) in the HTML that allows the user to download the chart as a PNG image using `chart.toBase64Image()`.
        9. SUMMARY: Add a small, semi-transparent overlay div at the top displaying a 1-sentence insight about the data.
    """.trimIndent()

    fun updateDataInput(input: String) {
        _dataInput.value = input
    }

    fun handleVisualize(theme: Theme) {
        if (_dataInput.value.isBlank() || _isGenerating.value) return

        _isGenerating.value = true
        _chartCode.value = ""

        // FIX 2: Retrieve API Key
        val apiKey = apiKeyManager.getApiKey()

        // FIX 3: Validate Key
        if (apiKey.isNullOrBlank()) {
            _chartCode.value = """
                <html>
                <body style="display:flex; justify-content:center; align-items:center; height:100%; background:#111; color:white; font-family:sans-serif;">
                    <div style="text-align:center;">
                        <h2 style='color:#EF4444'>ACCESS DENIED</h2>
                        <p>API Key not found. Please add your key in the Settings menu.</p>
                    </div>
                </body>
                </html>
            """.trimIndent()
            _isGenerating.value = false
            return
        }

        val prompt = "Create a visualization for this data. If the user didn't specify chart type, pick the best one (Bar, Line, Pie, Radar). Theme: ${if (theme == Theme.DARK) "dark" else "light"}. \n\nDATA:\n${_dataInput.value}"

        val config = ModelConfig(
            model = ModelName.FLASH, // Fast model for code generation
            temperature = 0.5, // Reduced temp for more accurate code generation
            systemInstruction = systemInstruction
        )

        viewModelScope.launch {
            var fullHtml = ""

            // FIX 4: Use validated apiKey
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