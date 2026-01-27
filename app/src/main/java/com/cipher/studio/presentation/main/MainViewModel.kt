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

    // --- Advanced UI States (Full Feature Set) ---
    private val _isVoiceActive = MutableStateFlow(false)
    val isVoiceActive = _isVoiceActive.asStateFlow()

    private val _isInputExpanded = MutableStateFlow(false)
    val isInputExpanded = _isInputExpanded.asStateFlow()

    // For the Top Bar Badge (Shows current active brain)
    private val _currentModelName = MutableStateFlow("Cipher 1.0 Ultra") 
    val currentModelName = _currentModelName.asStateFlow()

    // Suggestion Chips (To fill the dead space)
    val suggestionChips = listOf(
        "Generate Kotlin Code",
        "Analyze this UI",
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

        // Init Speech Recognition (Crash Safe Implementation)
        initSpeechRecognizer()
    }

    // --- VOICE INPUT LOGIC (Robust & Safe) ---
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
                        // Handle error silently or update UI state if needed
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val spokenText = matches[0]
                            val currentText = _prompt.value
                            // Smart append: adds space if needed
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
            // Fail gracefully if device doesn't support speech
        }
    }

    fun toggleVoiceInput() {
        if (speechRecognizer == null) {
            initSpeechRecognizer() // Retry init if failed previously
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

    // --- CONFIGURATION & MODEL SWITCHING ---
    fun updateConfig(newConfig: ModelConfig) {
        _config.value = newConfig

        // Update Badge Name based on model selection
        val name = newConfig.model.name.lowercase() 

        _currentModelName.value = when {
            name.contains("pro") -> "Gemini Pro"
            name.contains("flash") -> "Gemini 1.5 Flash"
            else -> "Cipher Ultra"
        }

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
        // Sync model name when loading session
        updateConfig(session.config)
    }

    fun deleteSession(sessionId: String) {
        val updatedList = _sessions.value.filter { it.id != sessionId }
        _sessions.value = updatedList
        
        // FIXED: Run heavy database operation on IO thread to prevent UI freeze
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.saveSessions(updatedList)
        }

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
            // Update UI State immediately (Fast)
            _sessions.value = updatedList
            
            // FIXED: Heavy JSON Serialization/Writing moved to Background Thread (IO)
            viewModelScope.launch(Dispatchers.IO) {
                storageManager.saveSessions(updatedList)
            }
        }
    }

    // --- CHAT EXECUTION LOGIC ---
    fun handleRun(overridePrompt: String? = null) {
        val textToRun = overridePrompt ?: _prompt.value
        val attachmentsToUse = if (overridePrompt != null) emptyList() else _attachments.value

        if ((textToRun.isBlank() && attachmentsToUse.isEmpty()) || _isStreaming.value) return

        val apiKey = apiKeyManager.getApiKey()
        if (apiKey.isNullOrBlank()) {
            addSystemMessage("⚠️ SYSTEM ALERT: No API Key found. Please add it in Settings.")
            return
        }

        // 1. Create User Message
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            text = textToRun,
            timestamp = System.currentTimeMillis(),
            attachments = attachmentsToUse
        )

        val currentHistory = _history.value + userMsg
        _history.value = currentHistory

        // 2. Clear Inputs
        if (overridePrompt == null) {
            _prompt.value = ""
            clearAttachments()
        }
        _isStreaming.value = true

        // 3. Update Title (First Message)
        if (currentHistory.size == 1) {
            val title = textToRun.take(30) + "..."
            val currentId = _currentSessionId.value
            if (currentId != null) {
                _sessions.value = _sessions.value.map { if (it.id == currentId) it.copy(title = title) else it }
            }
        }

        // 4. Add AI Placeholder
        val modelMsgId = UUID.randomUUID().toString()
        val placeholderMsg = ChatMessage(modelMsgId, ChatRole.MODEL, "", System.currentTimeMillis())
        _history.value = currentHistory + placeholderMsg

        saveSessionsToDisk()

        // 5. Start Streaming
        viewModelScope.launch {
            val fullResponse = StringBuilder()
            var lastUiUpdate = 0L // To track throttling time

            aiService.generateContentStream(
                apiKey = apiKey,
                prompt = userMsg.text,
                attachments = userMsg.attachments,
                history = currentHistory,
                config = _config.value 
            ).collect { result ->
                when (result) {
                    is StreamResult.Content -> {
                        fullResponse.append(result.text)
                        
                        // THROTTLING: Only update UI if > 100ms has passed
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUiUpdate >= 100) {
                            updateLastMessage(fullResponse.toString())
                            lastUiUpdate = currentTime
                        }
                    }
                    is StreamResult.Error -> {
                        fullResponse.append("\n\n[Error: ${result.message}]")
                        updateLastMessage(fullResponse.toString())
                        _isStreaming.value = false
                        saveSessionsToDisk()
                    }
                    else -> {}
                }
            }
            
            // FINAL UPDATE: Ensure the complete text is shown (in case the last chunk was throttled)
            updateLastMessage(fullResponse.toString())
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

    // --- CLEANUP ---
    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}