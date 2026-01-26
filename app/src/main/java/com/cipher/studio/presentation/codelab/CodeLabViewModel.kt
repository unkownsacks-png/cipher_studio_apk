package com.cipher.studio.presentation.codelab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.data.local.ApiKeyManager
import com.cipher.studio.domain.model.AppConstants
import com.cipher.studio.domain.model.ModelConfig
import com.cipher.studio.domain.model.ModelName
import com.cipher.studio.domain.service.GenerativeAIService
import com.cipher.studio.domain.service.StreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CodeViewMode { EDITOR, PREVIEW, SPLIT }
enum class DeviceFrame { PHONE, TABLET, DESKTOP }

@HiltViewModel
class CodeLabViewModel @Inject constructor(
    private val aiService: GenerativeAIService,
    private val apiKeyManager: ApiKeyManager
) : ViewModel() {

    // --- State ---
    private val _code = MutableStateFlow(
        "<!DOCTYPE html>\n<html>\n<body style='display:flex;justify-content:center;align-items:center;height:100vh;margin:0;font-family:sans-serif;background:#f0f0f0;'>\n<div style='text-align:center'>\n<h2 style='color:#333'>Ready to Code</h2>\n<p style='color:#666'>Ask me to build something...</p>\n</div>\n</body>\n</html>"
    )
    val code = _code.asStateFlow()

    private val _prompt = MutableStateFlow("")
    val prompt = _prompt.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _viewMode = MutableStateFlow(CodeViewMode.SPLIT)
    val viewMode = _viewMode.asStateFlow()

    // FEATURE 3: Device Frame State
    private val _deviceFrame = MutableStateFlow(DeviceFrame.PHONE)
    val deviceFrame = _deviceFrame.asStateFlow()

    // FEATURE 4: Console Logs
    private val _consoleLogs = MutableStateFlow<List<String>>(emptyList())
    val consoleLogs = _consoleLogs.asStateFlow()

    // FEATURE 6: History (Simple Undo)
    private val _codeHistory =  mutableListOf<String>()

    fun updatePrompt(text: String) { _prompt.value = text }
    fun setViewMode(mode: CodeViewMode) { _viewMode.value = mode }
    fun setDeviceFrame(frame: DeviceFrame) { _deviceFrame.value = frame }
    fun addConsoleLog(log: String) { _consoleLogs.value = _consoleLogs.value + log }
    fun clearConsole() { _consoleLogs.value = emptyList() }

    // FEATURE 5: Smart Refactor Shortcuts
    fun applyQuickAction(action: String) {
        val currentCode = _code.value
        val enhancementPrompt = when(action) {
            "Dark Mode" -> "Update this code to use a modern Dark Mode color scheme. Keep functionality same."
            "Animate" -> "Add smooth CSS animations to elements (fade-in, slide-up). Keep functionality same."
            "Fix Bug" -> "Analyze this code for bugs, fix them, and return the corrected full code."
            "Beautify" -> "Refactor the UI to be highly professional, using shadows, gradients, and rounded corners."
            else -> return
        }
        _prompt.value = enhancementPrompt
        handleGenerate(isRefactor = true, previousCode = currentCode)
    }

    fun handleGenerate(isRefactor: Boolean = false, previousCode: String = "") {
        val userPrompt = _prompt.value
        if (userPrompt.isBlank()) return

        // Save history before change
        if (_code.value.isNotEmpty()) {
            _codeHistory.add(_code.value)
            if (_codeHistory.size > 10) _codeHistory.removeAt(0)
        }

        _isGenerating.value = true
        _consoleLogs.value = emptyList() // Clear logs

        val apiKey = apiKeyManager.getApiKey() ?: return

        // SYSTEM INSTRUCTION: STRICT HTML OUTPUT
        val systemInstruction = """
            You are an expert Frontend Developer.
            1. Return ONLY valid, single-file HTML (including CSS in <style> and JS in <script>).
            2. NO markdown backticks (```html). Just the raw code.
            3. Use modern CSS (Flexbox, Grid) and clean Design.
            4. If refining code, keep the previous logic but apply the requested changes.
        """.trimIndent()

        val fullPrompt = if (isRefactor) {
            "Original Code:\n$previousCode\n\nTask: $userPrompt"
        } else {
            userPrompt
        }

        viewModelScope.launch {
            val config = ModelConfig(
                model = ModelName.FLASH,
                temperature = 0.5, // Balanced creativity
                systemInstruction = systemInstruction
            )

            // FEATURE 1: STREAMING (Wipe code and rebuild it live)
            var streamedCode = ""
            
            aiService.generateContentStream(
                apiKey = apiKey,
                prompt = fullPrompt,
                attachments = emptyList(),
                history = emptyList(),
                config = config
            ).collect { result ->
                when (result) {
                    is StreamResult.Content -> {
                        streamedCode += result.text
                        // Clean up markdown if AI adds it despite instructions
                        _code.value = streamedCode.replace("```html", "").replace("```", "")
                    }
                    is StreamResult.Error -> {
                        addConsoleLog("AI Error: ${result.message}")
                        _isGenerating.value = false
                    }
                    else -> {}
                }
            }
            _isGenerating.value = false
            _prompt.value = "" // Clear prompt after success
        }
    }

    fun undo() {
        if (_codeHistory.isNotEmpty()) {
            _code.value = _codeHistory.removeAt(_codeHistory.lastIndex)
        }
    }
}