package com.cipher.studio.presentation.visionhub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.domain.model.*
import com.cipher.studio.domain.service.GenerativeAIService
import com.cipher.studio.domain.service.StreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

enum class VisionMode {
    DESCRIBE, EXTRACT, ANALYZE, CODE, THREAT
}

@HiltViewModel
class VisionHubViewModel @Inject constructor(
    private val aiService: GenerativeAIService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // State
    private val _selectedImage = MutableStateFlow<Attachment?>(null)
    val selectedImage = _selectedImage.asStateFlow()

    private val _result = MutableStateFlow("")
    val result = _result.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    // Helper: Handle URI selection from Gallery
    fun onImageSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                if (bitmap != null) {
                    val outputStream = ByteArrayOutputStream()
                    // Compress to JPEG to save size, quality 80
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val byteArray = outputStream.toByteArray()
                    val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                    
                    val attachment = Attachment(
                        mimeType = "image/jpeg",
                        data = base64String
                    )
                    _selectedImage.value = attachment
                    _result.value = "" // Reset result on new image
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun analyzeImage(mode: VisionMode) {
        val image = _selectedImage.value ?: return
        if (_isAnalyzing.value) return

        _isAnalyzing.value = true
        _result.value = ""

        var prompt = ""
        var systemInfo = "You are an expert Computer Vision assistant."

        when (mode) {
            VisionMode.DESCRIBE -> prompt = "Describe this image in vivid detail. What is happening?"
            VisionMode.EXTRACT -> prompt = "Extract all visible text from this image exactly as it appears. Format it cleanly."
            VisionMode.ANALYZE -> prompt = "Analyze this image professionally. Identify objects, artistic style, colors, and potential context."
            VisionMode.CODE -> prompt = "Extract the code from this image. Return ONLY the valid code block, formatted correctly for the detected language."
            VisionMode.THREAT -> {
                prompt = "Perform a SECURITY AUDIT on this image. Identify: 1. Phishing indicators (URL mismatches, fake logos). 2. Sensitive data exposure (PII, Credentials). 3. Physical security risks (if a photo). Format as a Threat Report."
                systemInfo = "You are a Cyber Security Image Analyst. Detect threats, phishing attempts, and sensitive data leakage."
            }
        }

        viewModelScope.launch {
            val config = ModelConfig(
                model = ModelName.PRO, // Use Pro for better vision capabilities
                temperature = 0.4, // Lower temp for more accurate analysis
                systemInstruction = systemInfo
            )

            // NOTE: API Key should ideally be injected or retrieved from a secure repository
            val apiKey = "YOUR_API_KEY_PLACEHOLDER"

            aiService.generateContentStream(
                apiKey = apiKey,
                prompt = prompt,
                attachments = listOf(image),
                history = emptyList(), // Vision tasks are usually single-shot
                config = config
            ).collect { streamResult ->
                when (streamResult) {
                    is StreamResult.Content -> {
                        _result.value += streamResult.text
                    }
                    is StreamResult.Error -> {
                        _result.value += "\n\nError: ${streamResult.message}"
                        _isAnalyzing.value = false
                    }
                    is StreamResult.Metadata -> { /* Vision usually doesn't return grounding metadata yet */ }
                }
            }
            _isAnalyzing.value = false
        }
    }
}