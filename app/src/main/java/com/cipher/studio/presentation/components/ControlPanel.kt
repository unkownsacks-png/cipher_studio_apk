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

    // DESIGN UPDATE: Faint/Pale Colors (Clean Aesthetic)
    val panelBg = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.98f) else Color(0xFFF9F9FB).copy(alpha = 0.98f)
    val borderColor = if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.06f)
    val textColor = if (isDark) Color(0xFFE5E5E5) else Color(0xFF333333)
    val labelColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF8E8E93)
    val accentColor = if (isDark) Color(0xFFE2E2E2) else Color(0xFF555555) // Desaturated Accent

    // 1. STATE MANAGEMENT (CRITICAL FIX)
    // We use local state for sliders to ensure smooth UI updates
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
                .background(Color.Black.copy(alpha = 0.2f)) // Softer Overlay
                .clickable { onClose() }
        ) {
            // The Actual Panel (Aligned Right)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(340.dp) // Slightly wider for elegance
                    .fillMaxHeight()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp)) // Softer Corners
                    .background(panelBg)
                    .border(1.dp, borderColor, RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {} // Prevent click-through
                    .verticalScroll(rememberScrollState())
            ) {
                // --- Header ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(if(isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Settings",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Model Config",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = textColor
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
                        text = "INTELLIGENCE MODEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                        modifier = Modifier.padding(bottom = 12.dp),
                        letterSpacing = 0.5.sp
                    )

                    ModelDropdown(
                        currentModel = config.model,
                        onModelSelected = { newModel ->
                            // CRITICAL: Link to ViewModel
                            onChange(config.copy(model = newModel))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        isDark = isDark,
                        textColor = textColor,
                        borderColor = borderColor
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- 2. ADVANCED FEATURE: AI PRESETS ---
                    Text(
                        text = "QUICK PRESETS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = labelColor,
                        modifier = Modifier.padding(bottom = 12.dp),
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetButton("Precise", isDark, borderColor, textColor) {
                            localTemp = 0.1f; localTokens = 2000f
                            onChange(config.copy(temperature = 0.1, maxOutputTokens = 2000))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        PresetButton("Balanced", isDark, borderColor, textColor) {
                            localTemp = 0.7f; localTokens = 8000f
                            onChange(config.copy(temperature = 0.7, maxOutputTokens = 8000))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        PresetButton("Creative", isDark, borderColor, textColor) {
                            localTemp = 1.5f; localTokens = 16000f
                            onChange(config.copy(temperature = 1.5, maxOutputTokens = 16000))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // 3. Temperature Slider
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Temperature",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Text(
                                text = "Creativity & Randomness",
                                fontSize = 11.sp,
                                color = labelColor
                            )
                        }
                        // Animated Number Display
                        Box(
                            modifier = Modifier
                                .background(if(isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            AnimatedContent(targetState = localTemp, label = "temp") { targetTemp ->
                                Text(
                                    text = String.format("%.1f", targetTemp),
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                        isDark = isDark,
                        activeColor = accentColor
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // 4. Max Tokens Slider
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Max Length",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Text(
                                text = "Response Token Limit",
                                fontSize = 11.sp,
                                color = labelColor
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(if(isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            AnimatedContent(targetState = localTokens, label = "tokens") { targetTokens ->
                                Text(
                                    text = targetTokens.toInt().toString(),
                                    fontSize = 12.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
                        activeColor = accentColor
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    // 5. Safety Settings (Compliance)
                    HorizontalDivider(color = borderColor)
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SAFETY PROTOCOLS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = labelColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "DISABLED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE57373), // Soft Red
                            modifier = Modifier
                                .background(Color(0xFFE57373).copy(0.1f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0xFFE57373).copy(0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF2C2C2E).copy(0.5f) else Color(0xFFF2F2F7))
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = "Alert",
                            tint = labelColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Standard filters are bypassed. The model will process all queries without restrictions.",
                            fontSize = 12.sp,
                            color = labelColor,
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
    isDark: Boolean,
    textColor: Color,
    borderColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    val bg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                // CRITICAL FIX: Enum Mapping Check
                text = when(currentModel) {
                    ModelName.FLASH -> "Gemini 2.5 Flash"
                    ModelName.PRO -> "Gemini 3.0 Pro"
                    ModelName.FLASH_LITE -> "Gemini Flash Lite"
                },
                fontSize = 14.sp,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }, // Close on outside click
            modifier = Modifier.background(bg).border(1.dp, borderColor, RoundedCornerShape(4.dp))
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
                        // CRITICAL FIX: Close expanded inside selection
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
    activeColor: Color
) {
    val thumbColor = Color.White
    val inactiveTrackColor = if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.05f)

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished, // CRITICAL FIX: Sync on release
        valueRange = valueRange,
        // steps removed for smooth sliding (UI Refinement)
        colors = SliderDefaults.colors(
            thumbColor = thumbColor,
            activeTrackColor = activeColor,
            inactiveTrackColor = inactiveTrackColor
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun PresetButton(text: String, isDark: Boolean, borderColor: Color, textColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = text, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}