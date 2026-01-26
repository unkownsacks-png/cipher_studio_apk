package com.cipher.studio.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cipher.studio.domain.model.ModelConfig
import com.cipher.studio.domain.model.ModelName
import com.cipher.studio.domain.model.Theme
import kotlin.math.roundToInt

@Composable
fun ControlPanel(
    config: ModelConfig,
    onChange: (ModelConfig) -> Unit,
    isOpen: Boolean,
    onClose: () -> Unit,
    theme: Theme
) {
    val isDark = theme == Theme.DARK
    val haptic = LocalHapticFeedback.current

    // Panel Colors
    val panelBg = if (isDark) Color(0xFF0F172A).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f)
    val textColor = if (isDark) Color.White else Color(0xFF1F2937)
    val labelColor = if (isDark) Color.Gray else Color.Gray

    // 1. STATE MANAGEMENT (CRITICAL FIX)
    // We use local state for sliders to ensure smooth UI updates, then sync on finish
    var localTemp by remember(config.temperature) { mutableFloatStateOf(config.temperature.toFloat()) }
    var localTokens by remember(config.maxOutputTokens) { mutableFloatStateOf(config.maxOutputTokens.toFloat()) }

    // Animation: Slide in from Right
    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        // Overlay (Click to close)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable { onClose() }
        ) {
            // The Actual Panel (Aligned Right)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(320.dp)
                    .fillMaxHeight()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(panelBg)
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .clickable(enabled = false) {} // Prevent click-through to overlay
                    .verticalScroll(rememberScrollState())
            ) {
                // --- Header ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Settings",
                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CONFIGURATION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textColor,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, "Close", tint = labelColor)
                    }
                }

                HorizontalDivider(color = borderColor)

                // --- Content ---
                Column(modifier = Modifier.padding(24.dp)) {

                    // 1. Model Selection (CRITICAL FIX)
                    Text(
                        text = "AI MODEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ModelDropdown(
                        currentModel = config.model,
                        onModelSelected = { 
                            onChange(config.copy(model = it))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- 2. ADVANCED FEATURE: AI PRESETS ---
                    Text(
                        text = "QUICK PRESETS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PresetButton("Precise", isDark) {
                            localTemp = 0.1f; localTokens = 2000f
                            onChange(config.copy(temperature = 0.1, maxOutputTokens = 2000))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        PresetButton("Balanced", isDark) {
                            localTemp = 0.7f; localTokens = 8000f
                            onChange(config.copy(temperature = 0.7, maxOutputTokens = 8000))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        PresetButton("Creative", isDark) {
                            localTemp = 1.5f; localTokens = 16000f
                            onChange(config.copy(temperature = 1.5, maxOutputTokens = 16000))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 3. Temperature Slider
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Creativity (Temp)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = labelColor
                        )
                        // Animated Number Display
                        AnimatedContent(targetState = localTemp, label = "temp") { targetTemp ->
                            Text(
                                text = String.format("%.1f", targetTemp),
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                            )
                        }
                    }

                    CustomSlider(
                        value = localTemp,
                        onValueChange = { 
                            localTemp = it 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onValueChangeFinished = {
                            onChange(config.copy(temperature = (localTemp * 10).roundToInt() / 10.0))
                        },
                        valueRange = 0f..2f,
                        isDark = isDark
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 4. Max Tokens Slider
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Max Length (Tokens)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = labelColor
                        )
                        AnimatedContent(targetState = localTokens, label = "tokens") { targetTokens ->
                            Text(
                                text = targetTokens.toInt().toString(),
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = if (isDark) Color(0xFFEC4899) else Color(0xFFDB2777)
                            )
                        }
                    }

                    CustomSlider(
                        value = localTokens,
                        onValueChange = { 
                            localTokens = it
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onValueChangeFinished = {
                            onChange(config.copy(maxOutputTokens = localTokens.toInt()))
                        },
                        valueRange = 100f..32000f,
                        isDark = isDark,
                        trackColor = if (isDark) Color(0xFFEC4899) else Color(0xFFDB2777)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 5. Safety Settings (Compliance)
                    HorizontalDivider(color = borderColor)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "COMPLIANCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = labelColor
                        )
                        Text(
                            text = "UNLOCKED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22C55E), // Green
                            modifier = Modifier
                                .background(Color(0xFF22C55E).copy(0.1f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFF22C55E).copy(0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .background(if (isDark) Color.White.copy(0.05f) else Color.White)
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security, 
                            contentDescription = "Alert",
                            tint = Color(0xFFF87171), 
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Safety filters disabled. The model will respond to all queries without refusal.",
                            fontSize = 12.sp,
                            color = if (isDark) Color.Gray else Color(0xFF4B5563),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// --- Helper Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDropdown(
    currentModel: ModelName,
    onModelSelected: (ModelName) -> Unit,
    isDark: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    val bgColor = if (isDark) Color.Black.copy(0.2f) else Color.White
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.2f)
    val textColor = if (isDark) Color.White else Color(0xFF111827)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when(currentModel) {
                    ModelName.FLASH -> "Gemini 2.5 Flash (Fast)"
                    ModelName.PRO -> "Gemini 3.0 Pro (Deep)"
                    ModelName.FLASH_LITE -> "Gemini Flash Lite (Eco)"
                },
                fontSize = 14.sp,
                color = textColor
            )
            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(if (isDark) Color(0xFF1E293B) else Color.White)
        ) {
            ModelName.values().forEach { model ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = when(model) {
                                ModelName.FLASH -> "Gemini 2.5 Flash"
                                ModelName.PRO -> "Gemini 3.0 Pro"
                                ModelName.FLASH_LITE -> "Gemini Flash Lite"
                            },
                            color = textColor 
                        ) 
                    },
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    isDark: Boolean,
    trackColor: Color? = null
) {
    val thumbColor = Color.White
    val activeTrackColor = trackColor ?: if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
    val inactiveTrackColor = if (isDark) Color.Black.copy(0.3f) else Color(0xFFE5E7EB)

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished, // CRITICAL FIX: Sync on release
        valueRange = valueRange,
        // steps removed for smooth sliding (UI Refinement)
        colors = SliderDefaults.colors(
            thumbColor = thumbColor,
            activeTrackColor = activeTrackColor,
            inactiveTrackColor = inactiveTrackColor
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PresetButton(text: String, isDark: Boolean, onClick: () -> Unit) {
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.2f)
    val textColor = if (isDark) Color.White else Color.Black
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}