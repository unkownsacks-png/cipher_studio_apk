package com.cipher.studio.presentation.main

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.data.local.ApiKeyManager
import com.cipher.studio.data.local.LocalStorageManager // Added Import
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
    private val apiKeyManager: ApiKeyManager,
    private val storageManager: LocalStorageManager, // Injected Storage
    application: Application
) : AndroidViewModel(application) {

    // --- State ---
    // Initialize Auth state directly from storage!
    private val _isAuthorized = MutableStateFlow(storageManager.isAuthorized())
    val isAuthorized = _isAuthorized.asStateFlow()

    private val _hasApiKey = MutableStateFlow(apiKeyManager.hasKey())
    val hasApiKey = _hasApiKey.asStateFlow()

    // Initialize Theme from storage
    private val _theme = MutableStateFlow(if(storageManager.isDarkTheme()) Theme.DARK else Theme.LIGHT)
    val theme = _theme.asStateFlow()

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

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions = _sessions.asStateFlow()
    
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()
    
    private val _config = MutableStateFlow(AppConstants.DEFAULT_CONFIG)
    val config = _config.asStateFlow()

    // Attachments State (For VisionHub sharing to Chat)
    private val _attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val attachments = _attachments.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        // 1. Load Sessions from Disk
        val savedSessions = storageManager.getSessions()
        if (savedSessions.isNotEmpty()) {
            _sessions.value = savedSessions
            // Load the most recent session
            loadSession(savedSessions.first())
        } else {
            createNewSession()
        }

        // 2. Initialize TTS
        tts = TextToSpeech(application) { status ->
            if (status != TextToSpeech.ERROR) tts?.language = Locale.US
        }
    }

    // --- Actions ---

    // Auth Update
    fun setAuthorized(auth: Boolean) {
        _isAuthorized.value = auth
        storageManager.setAuthorized(auth) // Persist!
    }

    // Session Management
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
        saveSessionsToDisk() // Persist!
    }

    fun loadSession(session: Session) {
        _currentSessionId.value = session.id
        _history.value = session.history
        _config.value = session.config
        // Close sidebar on mobile when selecting
        _isSidebarOpen.value = false
    }

    private fun saveSessionsToDisk() {
        // Update the current session in the list before saving
        val currentId = _currentSessionId.value
        if (currentId != null) {
            val updatedList = _sessions.value.map { 
                if (it.id == currentId) it.copy(history = _history.value, config = _config.value, lastModified = System.currentTimeMillis()) 
                else it 
            }
            _sessions.value = updatedList
            storageManager.saveSessions(updatedList) // Write to Disk
        }
    }

    // Chat Logic
    fun handleRun(overridePrompt: String? = null) {
        val textToRun = overridePrompt ?: _prompt.value
        val attachmentsToUse = if (overridePrompt != null) emptyList() else _attachments.value

        if ((textToRun.isBlank() && attachmentsToUse.isEmpty()) || _isStreaming.value) return
        
        val apiKey = apiKeyManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            addSystemMessage("⚠️ SYSTEM ALERT: No API Key found. Please add it in Settings.")
            return
        }

        // User Message
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            text = textToRun,
            timestamp = System.currentTimeMillis(),
            attachments = attachmentsToUse
        )
        
        // Update State
        val currentHistory = _history.value + userMsg
        _history.value = currentHistory
        
        // Reset Inputs
        if (overridePrompt == null) {
            _prompt.value = ""
            _attachments.value = emptyList()
        }
        _isStreaming.value = true

        // Update Session Title if it's the first message
        if (currentHistory.size == 1) {
            val title = textToRun.take(30) + "..."
            val currentId = _currentSessionId.value
            if (currentId != null) {
                _sessions.value = _sessions.value.map { if (it.id == currentId) it.copy(title = title) else it }
            }
        }

        // Placeholder for AI Response
        val modelMsgId = UUID.randomUUID().toString()
        val placeholderMsg = ChatMessage(modelMsgId, ChatRole.MODEL, "", System.currentTimeMillis())
        _history.value = currentHistory + placeholderMsg

        // Save immediately so user sees their message if app crashes
        saveSessionsToDisk()

        viewModelScope.launch {
            var fullResponse = ""
            
            aiService.generateContentStream(
                apiKey = apiKey,
                prompt = userMsg.text,
                attachments = userMsg.attachments,
                history = currentHistory,
                config = _config.value
            ).collect { result ->
                when (result) {
                    is StreamResult.Content -> {
                        fullResponse += result.text
                        updateLastMessage(fullResponse)
                    }
                    is StreamResult.Error -> {
                        updateLastMessage(fullResponse + "\n\n[Error: ${result.message}]")
                        _isStreaming.value = false
                        saveSessionsToDisk() // Save error state
                    }
                    else -> {}
                }
            }
            _isStreaming.value = false
            saveSessionsToDisk() // Save final response
        }
    }

    private fun updateLastMessage(newText: String) {
        val list = _history.value.toMutableList()
        if (list.isNotEmpty()) {
            val last = list.last()
            if (last.role == ChatRole.MODEL) {
                list[list.lastIndex] = last.copy(text = newText)
                _history.value = list.toList()
            }
        }
    }

    private fun addSystemMessage(text: String) {
        _history.value = _history.value + ChatMessage(role = ChatRole.MODEL, text = text, timestamp = System.currentTimeMillis())
    }

    // Other Actions
    fun saveApiKey(key: String) { apiKeyManager.saveApiKey(key); _hasApiKey.value = true }
    fun removeApiKey() { apiKeyManager.clearApiKey(); _hasApiKey.value = false }
    fun toggleSidebar() { _isSidebarOpen.value = !_isSidebarOpen.value }
    fun setViewMode(mode: ViewMode) { _currentView.value = mode }
    fun updatePrompt(text: String) { _prompt.value = text }
    fun speakText(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) }
    fun togglePin(id: String) { 
        _history.value = _history.value.map { if (it.id == id) it.copy(pinned = !it.pinned) else it }
        saveSessionsToDisk()
    }
}