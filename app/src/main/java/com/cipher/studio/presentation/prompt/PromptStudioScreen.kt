package com.cipher.studio.presentation.prompt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme

@Composable
fun PromptStudioScreen(
    theme: Theme,
    onUsePrompt: (String) -> Unit, // Callback to send prompt to Chat
    viewModel: PromptStudioViewModel = hiltViewModel()
) {
    val isDark = theme == Theme.DARK
    val inputPrompt by viewModel.inputPrompt.collectAsState()
    val optimizedPrompt by viewModel.optimizedPrompt.collectAsState()
    val explanation by viewModel.explanation.collectAsState()
    val isOptimizing by viewModel.isOptimizing.collectAsState()
    
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    // Colors
    val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.2f)
    val cardBg = if (isDark) Color.White.copy(0.05f) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // --- Header ---
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome, // Sparkles equivalent
                contentDescription = "Prompt Studio",
                tint = Color(0xFFEAB308), // Yellow-500
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Prompt Studio",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Transform basic ideas into professional AI instructions.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // --- Input Section ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ORIGINAL IDEA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = inputPrompt,
                    onValueChange = { viewModel.updateInput(it) },
                    placeholder = { Text("e.g., Write a blog post about coffee...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { viewModel.handleOptimize() },
                        enabled = inputPrompt.isNotBlank() && !isOptimizing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(), // Reset padding for gradient
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF2563EB), Color(0xFF9333EA)) // Blue to Purple
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isOptimizing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoFixHigh, "Enhance", tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enhance Prompt", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Explanation Section (Strategy) ---
        if (explanation.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E3A8A).copy(0.1f) else Color(0xFFEFF6FF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isDark) Color(0xFF3B82F6).copy(0.2f) else Color(0xFFDBEAFE), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, "Strategy", tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OPTIMIZATION STRATEGY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6),
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = explanation,
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Result Section ---
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF0A0A0A) else Color(0xFFF9FAFB)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column {
                // Label
                Box(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                ) {
                    Text(
                        text = "OPTIMIZED RESULT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22C55E), // Green
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .background(if (isDark) Color(0xFF0A0A0A) else Color(0xFFF9FAFB))
                            .padding(horizontal = 4.dp)
                    )
                }

                // Output Text
                Box(modifier = Modifier.padding(24.dp).fillMaxWidth().heightIn(min = 100.dp)) {
                    if (optimizedPrompt.isNotBlank()) {
                        Text(
                            text = optimizedPrompt,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = if (isDark) Color(0xFFD1D5DB) else Color(0xFF374151),
                            lineHeight = 22.sp
                        )
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.AutoAwesome, "Waiting", tint = Color.Gray.copy(0.2f), modifier = Modifier.size(48.dp))
                            Text("Waiting for input...", color = Color.Gray.copy(0.3f), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }

                // Action Footer
                if (optimizedPrompt.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 1.dp, color = borderColor) // Top border logic handled by box above implicitly
                            .background(if (isDark) Color.White.copy(0.05f) else Color.White)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Copy Button
                        TextButton(
                            onClick = { clipboardManager.setText(AnnotatedString(optimizedPrompt)) },
                            colors = ButtonDefaults.textButtonColors(contentColor = if (isDark) Color.LightGray else Color.Gray)
                        ) {
                            Text("Copy")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        // Use in Chat Button
                        Button(
                            onClick = { onUsePrompt(optimizedPrompt) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)), // Green
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, "Use", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use in Chat")
                        }
                    }
                }
            }
        }
    }
}