package com.cipher.studio.presentation.docintel

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme
import com.cipher.studio.presentation.components.MarkdownRenderer

@Composable
fun DocIntelScreen(
    theme: Theme,
    viewModel: DocIntelViewModel = hiltViewModel()
) {
    val isDark = theme == Theme.DARK
    val docText by viewModel.docText.collectAsState()
    val analysis by viewModel.analysis.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    // Colors
    val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.2f)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description, // FileText equiv
                contentDescription = "Doc Intel",
                tint = Color(0xFFF97316), // Orange-500
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Doc Intel",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Deep document analysis & intelligence extraction.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // --- Main Content ---
        // Input Section (Top 40%)
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxWidth()
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(0.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.dp, Color.Transparent) // Bottom border simulated via padding
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "SOURCE DOCUMENT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Divider(color = borderColor, thickness = 1.dp)

                    TextField(
                        value = docText,
                        onValueChange = { viewModel.updateDocText(it) },
                        placeholder = { 
                            Text(
                                "Paste contracts, reports, articles, or essays here...",
                                color = Color.Gray.copy(0.6f),
                                fontSize = 14.sp
                            ) 
                        },
                        modifier = Modifier
                            .fillMaxSize(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 22.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DocActionButton(
                    text = "Summarize",
                    icon = Icons.Default.List,
                    color = Color(0xFF3B82F6), // Blue
                    isEnabled = docText.isNotBlank() && !isProcessing,
                    onClick = { viewModel.runAnalysis(DocAnalysisMode.SUMMARY) },
                    modifier = Modifier.weight(1f)
                )
                DocActionButton(
                    text = "Audit & Risk",
                    icon = Icons.Default.VerifiedUser, // ShieldCheck equiv
                    color = Color(0xFFEF4444), // Red
                    isEnabled = docText.isNotBlank() && !isProcessing,
                    onClick = { viewModel.runAnalysis(DocAnalysisMode.AUDIT) },
                    modifier = Modifier.weight(1f)
                )
                DocActionButton(
                    text = "Deep Insights",
                    icon = Icons.Default.Lightbulb,
                    color = Color(0xFFF97316), // Orange
                    isEnabled = docText.isNotBlank() && !isProcessing,
                    onClick = { viewModel.runAnalysis(DocAnalysisMode.INSIGHTS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Output Section (Bottom 55%) ---
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0xFF0A0A0A) else Color(0xFFF9FAFB))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .border(0.dp, Color.Transparent), // Bottom border via divider
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ANALYSIS REPORT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFFF97316))
                }
            }
            
            Divider(color = borderColor, thickness = 1.dp)

            // Content
            Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                val scrollState = rememberScrollState()
                
                if (analysis.isNotEmpty()) {
                    Column(modifier = Modifier.verticalScroll(scrollState)) {
                         MarkdownRenderer(content = analysis, theme = theme)
                    }
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Search, null, tint = Color.Gray.copy(0.3f), modifier = Modifier.size(48.dp))
                        Text("Awaiting content...", color = Color.Gray.copy(0.5f), fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DocActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.1f),
            contentColor = color,
            disabledContainerColor = Color.Gray.copy(alpha = 0.1f),
            disabledContentColor = Color.Gray
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(56.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}