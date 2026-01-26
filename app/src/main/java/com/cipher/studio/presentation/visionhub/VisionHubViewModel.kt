package com.cipher.studio.presentation.visionhub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.data.local.ApiKeyManager // IMPORT ADDED
import com.cipher.studio.domain.model.*
import com.cipher.studio.domain.service.GenerativeAIService
import com.cipher.studio.domain.service.StreamResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

enum class VisionMode {
    DESCRIBE, EXTRACT, ANALYZE, CODE, THREAT,
    // --- NEW ADVANCED FEATURES ---
    UI_AUDIT,       // Criticize App/Web UI Designs
    MATH_SOLVER,    // Solve handwritten math problems
    GEOLOCATE,      // OSINT: Guess location from visual cues
    NUTRITION,      // Estimate calories/macros from food photos
    DIAGNOSIS,      // Identify plant diseases or machinery faults
    SHOPPING,       // Identify products/brands
    HANDWRITING     // Digitize handwritten notes
}

@HiltViewModel
class VisionHubViewModel @Inject constructor(
    private val aiService: GenerativeAIService,
    @ApplicationContext private val context: Context,
    private val apiKeyManager: ApiKeyManager // FIX 1: Dependency Injection
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

        // FIX 2: Retrieve API Key
        val apiKey = apiKeyManager.getApiKey()

        // FIX 3: Validate Key
        if (apiKey.isNullOrBlank()) {
            _result.value = "⚠️ ACCESS DENIED: API Key not found. Please navigate to Settings and configure your Gemini API Key to unlock Vision Hub."
            _isAnalyzing.value = false
            return
        }

        var prompt = ""
        var systemInfo = "You are an expert Computer Vision assistant."

        // Enhanced Prompts for Advanced Features
        when (mode) {
            VisionMode.DESCRIBE -> prompt = "Describe this image in vivid detail. What is happening?"
            VisionMode.EXTRACT -> prompt = "Extract all visible text from this image exactly as it appears. Format it cleanly."
            VisionMode.ANALYZE -> prompt = "Analyze this image professionally. Identify objects, artistic style, colors, and potential context."
            VisionMode.CODE -> prompt = "Extract the code from this image. Return ONLY the valid code block, formatted correctly for the detected language."
            VisionMode.THREAT -> {
                prompt = "Perform a SECURITY AUDIT on this image. Identify: 1. Phishing indicators (URL mismatches, fake logos). 2. Sensitive data exposure (PII, Credentials). 3. Physical security risks. Format as a Threat Report."
                systemInfo = "You are a Cyber Security Image Analyst."
            }
            // --- NEW FEATURES ---
            VisionMode.UI_AUDIT -> {
                prompt = "Act as a Senior UX/UI Designer. Audit this interface screenshot. Critique: 1. Accessibility (Color Contrast). 2. Layout & Spacing. 3. User Flow issues. Provide actionable improvements."
                systemInfo = "You are a Product Designer specializing in Accessibility and UX."
            }
            VisionMode.MATH_SOLVER -> {
                prompt = "Solve this mathematical problem shown in the image. Show your work STEP-BY-STEP. Finally, provide the answer in bold."
                systemInfo = "You are a Mathematics Professor. Solve equations accurately."
            }
            VisionMode.GEOLOCATE -> {
                prompt = "Analyze this image for GEOLOCATION cues. Look at: vegetation, road signs, architecture, power outlets, and sun position. Estimate the country or city with reasoning."
                systemInfo = "You are an OSINT (Open Source Intelligence) Investigator."
            }
            VisionMode.NUTRITION -> {
                prompt = "Analyze the food in this image. Estimate the portion size, total calories, and macro breakdown (Protein, Carbs, Fats). Warn if it contains common allergens."
                systemInfo = "You are a certified Nutritionist and Dietician."
            }
            VisionMode.DIAGNOSIS -> {
                prompt = "Identify the subject (Plant or Machine). If Plant: Identify the species and any visible disease. If Machine: Identify the part and signs of wear/damage. Provide a diagnosis."
                systemInfo = "You are an expert Diagnostician."
            }
            VisionMode.SHOPPING -> {
                prompt = "Identify the main products (Fashion, Furniture, Electronics) in this image. List the potential Brand, Model, and style name to help me find it online."
                systemInfo = "You are a Personal Shopper and Stylist."
            }
            VisionMode.HANDWRITING -> {
                prompt = "Transcribe this handwritten note into clean, formatted digital text (Markdown). Fix minor spelling errors if obvious, but keep the original meaning."
            }
        }

        viewModelScope.launch {
            val config = ModelConfig(
                model = ModelName.FLASH, // Always use Pro for vision tasks
                temperature = 0.4,
                systemInstruction = systemInfo
            )

            // FIX 4: Use validated apiKey
            aiService.generateContentStream(
                apiKey = apiKey,
                prompt = prompt,
                attachments = listOf(image),
                history = emptyList(), 
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
                    is StreamResult.Metadata -> { /* Ignore */ }
                }
            }
            _isAnalyzing.value = false
        }
    }
}