package com.cipher.studio.presentation.main

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.data.local.ApiKeyManager
import com.cipher.studio.domain.model.*
import com.cipher.studio.domain.service.GenerativeAIService
import com.cipher.studio.domain.service.StreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val aiService: GenerativeAIService,
    private val apiKeyManager: ApiKeyManager, // Injected Secure Manager
    application: Application
) : AndroidViewModel(application) {

    // --- State ---
    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized = _isAuthorized.asStateFlow()

    private val _hasApiKey = MutableStateFlow(apiKeyManager.hasKey())
    val hasApiKey = _hasApiKey.asStateFlow()

    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    val history = _history.asStateFlow()

    private val _prompt = MutableStateFlow("")
    val prompt = _prompt.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()

    private val _currentView = MutableStateFlow(ViewMode.CHAT)
    val currentView = _currentView.asStateFlow()
    
    private val _isSidebarOpen = MutableStateFlow(false)
    val isSidebarOpen = _isSidebarOpen.asStateFlow()

    private val _theme = MutableStateFlow(Theme.DARK)
    val theme = _theme.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions = _sessions.asStateFlow()
    
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()
    
    private val _config = MutableStateFlow(AppConstants.DEFAULT_CONFIG)
    val config = _config.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        createNewSession()
        tts = TextToSpeech(application) { status ->
            if (status != TextToSpeech.ERROR) tts?.language = Locale.US
        }
    }

    // --- API Key Management ---
    fun saveApiKey(key: String) {
        apiKeyManager.saveApiKey(key)
        _hasApiKey.value = true
    }

    fun removeApiKey() {
        apiKeyManager.clearApiKey()
        _hasApiKey.value = false
    }

    // --- Navigation ---
    fun setAuthorized(auth: Boolean) { _isAuthorized.value = auth }
    fun toggleSidebar() { _isSidebarOpen.value = !_isSidebarOpen.value }
    fun setViewMode(mode: ViewMode) { _currentView.value = mode }
    fun updatePrompt(text: String) { _prompt.value = text }

    // --- Chat Logic ---
    fun createNewSession() {
        val newSession = Session(UUID.randomUUID().toString(), "New Chat", emptyList(), AppConstants.DEFAULT_CONFIG, System.currentTimeMillis())
        _sessions.value = listOf(newSession) + _sessions.value
        loadSession(newSession)
    }

    fun loadSession(session: Session) {
        _currentSessionId.value = session.id
        _history.value = session.history
    }

    fun handleRun(overridePrompt: String? = null) {
        val textToRun = overridePrompt ?: _prompt.value
        if (textToRun.isBlank() || _isStreaming.value) return
        
        // Check API Key
        val apiKey = apiKeyManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            _history.value = _history.value + ChatMessage(
                role = ChatRole.MODEL,
                text = "⚠️ SYSTEM ALERT: No API Key found. Please go to Settings in the Sidebar and add your Gemini API Key.",
                timestamp = System.currentTimeMillis()
            )
            return
        }

        val userMsg = ChatMessage(UUID.randomUUID().toString(), ChatRole.USER, textToRun, System.currentTimeMillis())
        
        // 1. Add User Message
        val currentHistory = _history.value + userMsg
        _history.value = currentHistory
        
        if (overridePrompt == null) _prompt.value = ""
        _isStreaming.value = true

        // 2. Add Placeholder for Model Response
        val modelMsgId = UUID.randomUUID().toString()
        val placeholderMsg = ChatMessage(modelMsgId, ChatRole.MODEL, "", System.currentTimeMillis())
        _history.value = currentHistory + placeholderMsg

        viewModelScope.launch {
            var fullResponse = ""
            
            aiService.generateContentStream(
                apiKey = apiKey,
                prompt = userMsg.text,
                attachments = emptyList(),
                history = currentHistory, // Pass strict context
                config = _config.value
            ).collect { result ->
                when (result) {
                    is StreamResult.Content -> {
                        fullResponse += result.text
                        updateLastMessage(fullResponse)
                    }
                    is StreamResult.Error -> {
                        updateLastMessage("Error: ${result.message}\n\n$fullResponse")
                        _isStreaming.value = false
                    }
                    else -> {}
                }
            }
            _isStreaming.value = false
        }
    }

    private fun updateLastMessage(newText: String) {
        val list = _history.value.toMutableList()
        if (list.isNotEmpty()) {
            val last = list.last()
            if (last.role == ChatRole.MODEL) {
                list[list.lastIndex] = last.copy(text = newText)
                // Using a new list instance triggers StateFlow emission
                _history.value = list.toList() 
            }
        }
    }
    
    fun speakText(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) }
    fun togglePin(id: String) { /* Pin Logic */ }
}