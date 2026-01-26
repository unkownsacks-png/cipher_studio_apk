package com.cipher.studio.presentation.main

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.data.local.ApiKeyManager
import com.cipher.studio.data.local.LocalStorageManager
import com.cipher.studio.domain.model.*
import com.cipher.studio.domain.service.GenerativeAIService
import com.cipher.studio.domain.service.StreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val aiService: GenerativeAIService,
    private val apiKeyManager: ApiKeyManager,
    private val storageManager: LocalStorageManager,
    private val application: Application
) : AndroidViewModel(application) {

    // --- Core State ---
    private val _isAuthorized = MutableStateFlow(storageManager.isAuthorized())
    val isAuthorized = _isAuthorized.asStateFlow()

    private val _theme = MutableStateFlow(if(storageManager.isDarkTheme()) Theme.DARK else Theme.LIGHT)
    val theme = _theme.asStateFlow()

    // --- Chat State ---
    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    val history = _history.asStateFlow()

    private val _prompt = MutableStateFlow("")
    val prompt = _prompt.asStateFlow()

    private val _attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val attachments = _attachments.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming = _isStreaming.asStateFlow()

    // --- UI Layout State ---
    private val _currentView = MutableStateFlow(ViewMode.CHAT)
    val currentView = _currentView.asStateFlow()

    private val _isSidebarOpen = MutableStateFlow(false)
    val isSidebarOpen = _isSidebarOpen.asStateFlow()

    // --- Advanced UI States (NEW & UPDATED) ---
    private val _isVoiceActive = MutableStateFlow(false)
    val isVoiceActive = _isVoiceActive.asStateFlow()

    private val _isInputExpanded = MutableStateFlow(false)
    val isInputExpanded = _isInputExpanded.asStateFlow()

    // NEW: Model Name for Top Bar Badge
    private val _currentModelName = MutableStateFlow("Cipher 1.0 Ultra") 
    val currentModelName = _currentModelName.asStateFlow()

    // NEW: Suggestion Chips Data
    val suggestionChips = listOf(
        "Generate Kotlin Code for MVVM",
        "Analyze this UI Layout",
        "Debug my crash log",
        "Write a creative story"
    )

    // --- Session & Config ---
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _config = MutableStateFlow(AppConstants.DEFAULT_CONFIG)
    val config = _config.asStateFlow()

    // --- System Services ---
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        // Load Data
        val savedSessions = storageManager.getSessions()
        if (savedSessions.isNotEmpty()) {
            _sessions.value = savedSessions
            loadSession(savedSessions.first())
        } else {
            createNewSession()
        }

        // Init Text-to-Speech
        tts = TextToSpeech(application) { status ->
            if (status != TextToSpeech.ERROR) tts?.language = Locale.US
        }

        // Init Speech Recognition (SAFE MODE)
        initSpeechRecognizer()
    }

    // --- VOICE INPUT LOGIC (REAL IMPLEMENTATION) ---
    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(application)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() { _isVoiceActive.value = true }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { _isVoiceActive.value = false }

                    override fun onError(error: Int) {
                        _isVoiceActive.value = false
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val spokenText = matches[0]
                            val currentText = _prompt.value
                            _prompt.value = if (currentText.isBlank()) spokenText else "$currentText $spokenText"
                        }
                        _isVoiceActive.value = false
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleVoiceInput() {
        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }

        if (_isVoiceActive.value) {
            speechRecognizer?.stopListening()
            _isVoiceActive.value = false
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            try {
                speechRecognizer?.startListening(intent)
                _isVoiceActive.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _isVoiceActive.value = false
            }
        }
    }

    // --- FULLSCREEN EDITOR LOGIC ---
    fun toggleFullscreenInput() {
        _isInputExpanded.value = !_isInputExpanded.value
    }

    fun setInputExpanded(expanded: Boolean) {
        _isInputExpanded.value = expanded
    }

    // --- CONFIGURATION LINK ---
    fun updateConfig(newConfig: ModelConfig) {
        _config.value = newConfig
        // Update model name based on config if needed
        _currentModelName.value = if(newConfig.modelName.contains("gemini")) "Gemini Pro" else "Cipher Ultra"
        saveSessionsToDisk()
    }

    // --- SESSION MANAGEMENT ---
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
        saveSessionsToDisk()
    }

    fun loadSession(session: Session) {
        _currentSessionId.value = session.id
        _history.value = session.history
        _config.value = session.config
        _isSidebarOpen.value = false
        _isInputExpanded.value = false 
    }

    fun deleteSession(sessionId: String) {
        val updatedList = _sessions.value.filter { it.id != sessionId }
        _sessions.value = updatedList
        storageManager.saveSessions(updatedList)

        if (_currentSessionId.value == sessionId) {
            if (updatedList.isNotEmpty()) {
                loadSession(updatedList.first())
            } else {
                createNewSession()
            }
        }
    }

    private fun saveSessionsToDisk() {
        val currentId = _currentSessionId.value
        if (currentId != null) {
            val updatedList = _sessions.value.map { 
                if (it.id == currentId) it.copy(
                    history = _history.value, 
                    config = _config.value, 
                    lastModified = System.currentTimeMillis()
                ) 
                else it 
            }
            _sessions.value = updatedList
            storageManager.saveSessions(updatedList)
        }
    }

    // --- CHAT LOGIC ---
    fun handleRun(overridePrompt: String? = null) {
        val textToRun = overridePrompt ?: _prompt.value
        val attachmentsToUse = if (overridePrompt != null) emptyList() else _attachments.value

        if ((textToRun.isBlank() && attachmentsToUse.isEmpty()) || _isStreaming.value) return

        val apiKey = apiKeyManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            addSystemMessage("⚠️ SYSTEM ALERT: No API Key found. Please add it in Settings.")
            return
        }

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            text = textToRun,
            timestamp = System.currentTimeMillis(),
            attachments = attachmentsToUse
        )

        val currentHistory = _history.value + userMsg
        _history.value = currentHistory

        if (overridePrompt == null) {
            _prompt.value = ""
            clearAttachments()
        }
        _isStreaming.value = true

        if (currentHistory.size == 1) {
            val title = textToRun.take(30) + "..."
            val currentId = _currentSessionId.value
            if (currentId != null) {
                _sessions.value = _sessions.value.map { if (it.id == currentId) it.copy(title = title) else it }
            }
        }

        val modelMsgId = UUID.randomUUID().toString()
        val placeholderMsg = ChatMessage(modelMsgId, ChatRole.MODEL, "", System.currentTimeMillis())
        _history.value = currentHistory + placeholderMsg

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
                        saveSessionsToDisk()
                    }
                    else -> {}
                }
            }
            _isStreaming.value = false
            saveSessionsToDisk()
        }
    }

    // --- HELPER FUNCTIONS ---
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

    fun addAttachment(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = application.applicationContext
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)

                if (bitmap != null) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val byteArray = outputStream.toByteArray()
                    val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                    val newAttachment = Attachment(mimeType = "image/jpeg", data = base64String)
                    withContext(Dispatchers.Main) {
                        _attachments.value = _attachments.value + newAttachment
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAttachments() { _attachments.value = emptyList() }
    fun setAuthorized(auth: Boolean) { _isAuthorized.value = auth; storageManager.setAuthorized(auth) }
    fun saveApiKey(key: String) { apiKeyManager.saveApiKey(key) }
    fun removeApiKey() { apiKeyManager.clearApiKey() }
    fun toggleSidebar() { _isSidebarOpen.value = !_isSidebarOpen.value }
    fun setViewMode(mode: ViewMode) { _currentView.value = mode }
    fun updatePrompt(text: String) { _prompt.value = text }
    fun speakText(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null) }
    fun togglePin(id: String) { 
        _history.value = _history.value.map { if (it.id == id) it.copy(pinned = !it.pinned) else it }
        saveSessionsToDisk()
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}