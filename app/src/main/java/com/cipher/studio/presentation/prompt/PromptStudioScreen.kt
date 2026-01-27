package com.cipher.studio.presentation.prompt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme

@Composable
fun PromptStudioScreen(
    theme: Theme,
    onUsePrompt: (String) -> Unit, 
    viewModel: PromptStudioViewModel = hiltViewModel()
) {
    val isDark = theme == Theme.DARK
    val inputPrompt by viewModel.inputPrompt.collectAsState()
    val optimizedPrompt by viewModel.optimizedPrompt.collectAsState()
    val explanation by viewModel.explanation.collectAsState()
    val isOptimizing by viewModel.isOptimizing.collectAsState()
    val quality by viewModel.quality.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Modern Colors
    val bgColor = if (isDark) Color(0xFF0B0F19) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF151C2C) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val accentGradient = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899)))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // --- FEATURE 1: Header with Animated Icon ---
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoFixHigh, 
                    contentDescription = null,
                    tint = Color(0xFFA855F7), 
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Prompt Studio",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Craft perfect AI instructions",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // --- FEATURE 3: Template Chips ---
        Text("QUICK STARTERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("Coding", "Creative", "Business", "Academic").forEach { template ->
                SuggestionChip(
                    onClick = { viewModel.applyTemplate(template) },
                    label = { Text(template, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if(isDark) Color(0xFF1E293B) else Color.White,
                        labelColor = textColor
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        borderColor = if(isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // --- FEATURE 1 & 7: Magical Input Card (Glassmorphism + Glow) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFFA855F7).copy(0.2f))
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .border(1.dp, if(isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.1f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "YOUR DRAFT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    // FEATURE 5: Analysis Badge (Only show if not empty)
                    if (inputPrompt.isNotEmpty() && !isOptimizing && optimizedPrompt.isNotEmpty()) {
                        QualityBadge(quality)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = inputPrompt,
                    onValueChange = { viewModel.updateInput(it) },
                    placeholder = { Text("Describe what you want the AI to do...", color = Color.Gray.copy(0.5f), fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFFA855F7),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Magic Button
                Button(
                    onClick = { viewModel.handleOptimize() },
                    enabled = inputPrompt.isNotBlank() && !isOptimizing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFFA855F7).copy(0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accentGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isOptimizing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Magic Optimize", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- FEATURE 2 & 4: RESULT SECTION (Typewriter & Comparison) ---
        AnimatedVisibility(
            visible = optimizedPrompt.isNotEmpty() || isOptimizing,
            enter = fadeIn() + expandVertically()
        ) {
            Column {
                // Explanation Card
                if (explanation.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if(isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF))
                            .padding(16.dp)
                    ) {
                        Row(crossAxisAlignment = Alignment.Start) {
                            Icon(Icons.Rounded.Lightbulb, null, tint = Color(0xFFF59E0B), modifier = Modifier.padding(top = 2.dp).size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = explanation,
                                color = if(isDark) Color(0xFFCBD5E1) else Color(0xFF1E3A8A),
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // OPTIMIZED RESULT CARD
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, Color(0xFF22C55E).copy(0.3f), RoundedCornerShape(20.dp))
                ) {
                    Column {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF22C55E).copy(0.1f))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("OPTIMIZED VERSION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                        }

                        // Content (Typewriter Effect managed by State changes)
                        SelectionContainer {
                            Text(
                                text = optimizedPrompt,
                                modifier = Modifier.padding(20.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = textColor
                            )
                        }

                        Divider(color = if(isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.1f))

                        // FEATURE 6: Action Footer
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { 
                                clipboardManager.setText(AnnotatedString(optimizedPrompt)) 
                            }) {
                                Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy", color = Color.Gray)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onUsePrompt(optimizedPrompt) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Use in Chat", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- HELPER COMPONENTS ---

@Composable
fun QualityBadge(quality: PromptQuality) {
    val (color, text) = when(quality) {
        PromptQuality.POOR -> Color(0xFFEF4444) to "Weak"
        PromptQuality.OKAY -> Color(0xFFF59E0B) to "Okay"
        PromptQuality.GREAT -> Color(0xFF3B82F6) to "Great"
        PromptQuality.PERFECT -> Color(0xFF22C55E) to "Perfect"
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}