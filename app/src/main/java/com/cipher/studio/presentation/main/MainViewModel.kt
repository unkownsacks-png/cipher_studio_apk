package com.cipher.studio.presentation.main

import android.app.Application
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.domain.model.*
import com.cipher.studio.domain.repository.AuthRepository
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
    private val authRepository: AuthRepository,
    application: Application
) : AndroidViewModel(application) {

    // --- State Management (React useState equivalents) ---
    
    // Auth State
    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized = _isAuthorized.asStateFlow()

    // Session State
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    // Chat Data State
    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    val history = _history.asStateFlow()

    private val _prompt = MutableStateFlow("")
    val prompt = _prompt.asStateFlow()

    private val _attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val attachments = _attachments.asStateFlow()

    // UI/Config State
    private val _config = MutableStateFlow(AppConstants.DEFAULT_CONFIG)
    val config = _config.asStateFlow()

    private val _currentView = MutableStateFlow(ViewMode.CHAT)
    val currentView = _currentView.asStateFlow()

    private val _theme = MutableStateFlow(Theme.DARK) // Default dark
    val theme = _theme.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()
    
    private val _isSidebarOpen = MutableStateFlow(false)
    val isSidebarOpen = _isSidebarOpen.asStateFlow()

    private val _activePersona = MutableStateFlow("default")
    val activePersona = _activePersona.asStateFlow()

    private val _tokenCount = MutableStateFlow(0.0)
    val tokenCount = _tokenCount.asStateFlow()

    // Text To Speech Engine
    private var tts: TextToSpeech? = null
    private val _selectedVoice = MutableStateFlow(Voice.MALE)

    init {
        // Initialize TTS
        tts = TextToSpeech(application) { status ->
            if (status != TextToSpeech.ERROR) {
                tts?.language = Locale.US
            }
        }
        
        // Initial Session Load (Equiv to useEffect on mount)
        createNewSession()
    }

    // --- Actions (Logic from App.tsx) ---

    fun setAuthorized(authorized: Boolean) {
        _isAuthorized.value = authorized
        // Logic to persist authorization would go here (DataStore)
    }

    fun toggleSidebar() {
        _isSidebarOpen.value = !_isSidebarOpen.value
    }

    fun setViewMode(mode: ViewMode) {
        _currentView.value = mode
    }
    
    fun updatePrompt(text: String) {
        _prompt.value = text
        _tokenCount.value = text.length / 4.0 // Rough estimation
    }

    fun createNewSession() {
        val newSession = Session(
            id = UUID.randomUUID().toString(),
            title = "New Chat",
            history = emptyList(),
            config = AppConstants.DEFAULT_CONFIG,
            lastModified = System.currentTimeMillis()
        )
        _sessions.value = listOf(newSession) + _sessions.value
        loadSession(newSession)
    }

    fun loadSession(session: Session) {
        _currentSessionId.value = session.id
        _history.value = session.history
        _config.value = session.config
        _prompt.value = ""
        _attachments.value = emptyList()
    }

    fun handleRun(overridePrompt: String? = null) {
        val textToRun = overridePrompt ?: _prompt.value
        if (textToRun.isBlank() && _attachments.value.isEmpty()) return
        if (_isStreaming.value) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            text = textToRun,
            timestamp = System.currentTimeMillis(),
            attachments = if (overridePrompt != null) emptyList() else _attachments.value
        )

        // Update History
        val currentHistory = _history.value + userMsg
        _history.value = currentHistory
        
        // Clear input
        if (overridePrompt == null) {
            _prompt.value = ""
            _attachments.value = emptyList()
        }

        _isStreaming.value = true

        // Placeholder for model response
        val modelMsgId = UUID.randomUUID().toString()
        _history.value = currentHistory + ChatMessage(
            id = modelMsgId,
            role = ChatRole.MODEL,
            text = "",
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            // Provide context (last 30 messages)
            val contextHistory = currentHistory.takeLast(30)
            
            // NOTE: API Key should come from Secured Storage, currently placeholder
            val apiKey = "YOUR_API_KEY_HERE" 

            var fullResponse = ""
            var metadata: GroundingMetadata? = null

            aiService.generateContentStream(
                apiKey, userMsg.text, userMsg.attachments, contextHistory, _config.value
            ).collect { result ->
                when (result) {
                    is StreamResult.Content -> {
                        fullResponse += result.text
                        updateLastMessage(fullResponse, metadata)
                    }
                    is StreamResult.Metadata -> {
                        metadata = result.metadata
                        updateLastMessage(fullResponse, metadata)
                    }
                    is StreamResult.Error -> {
                        // Handle Error (Update UI or Show Toast)
                        _isStreaming.value = false
                    }
                }
            }
            _isStreaming.value = false
            
            // Save Session update
            updateSessionHistory()
        }
    }

    private fun updateLastMessage(text: String, metadata: GroundingMetadata?) {
        val currentList = _history.value.toMutableList()
        if (currentList.isNotEmpty()) {
            val lastMsg = currentList.last()
            if (lastMsg.role == ChatRole.MODEL) {
                currentList[currentList.lastIndex] = lastMsg.copy(
                    text = text,
                    groundingMetadata = metadata
                )
                _history.value = currentList
            }
        }
    }

    private fun updateSessionHistory() {
        val currentId = _currentSessionId.value ?: return
        val updatedSessions = _sessions.value.map { session ->
            if (session.id == currentId) {
                session.copy(history = _history.value, lastModified = System.currentTimeMillis())
            } else {
                session
            }
        }
        _sessions.value = updatedSessions
    }
    
    fun togglePin(messageId: String) {
        // Implementation for pinning messages
    }

    fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }
}