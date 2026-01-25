package com.cipher.studio.presentation.cyberhouse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll // Added Import
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Added Import
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
// Removed explicit ArrowForward import to use Default accessor
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberHouseScreen(
    theme: Theme,
    viewModel: CyberHouseViewModel = hiltViewModel()
) {
    val isDark = theme == Theme.DARK

    val mode by viewModel.mode.collectAsState()
    val activeAiTool by viewModel.activeAiTool.collectAsState()
    val aiInputData by viewModel.aiInputData.collectAsState()
    val aiOutput by viewModel.aiOutput.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    val activeUtilTool by viewModel.activeUtilTool.collectAsState()
    val utilInput by viewModel.utilInput.collectAsState()
    val utilOutput by viewModel.utilOutput.collectAsState()

    // Minimalist Palette (Gemini Inspired)
    val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val accentColor = if (isDark) Color(0xFF10B981) else Color(0xFF059669) // Soft Emerald
    val borderColor = if (isDark) Color.White.copy(0.08f) else Color.Black.copy(0.08f)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val inputBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Mobile scroll enabled
    ) {
        // --- Header (Elegant & Minimal) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Security, "Shield", tint = accentColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Cyber House",
                        fontWeight = FontWeight.Medium, // Reduced weight
                        fontSize = 22.sp,
                        color = textColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Minimal Badge
                    Text(
                        text = "ROOT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier
                            .border(0.5.dp, accentColor.copy(0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "Advanced Red Teaming Console",
                    fontSize = 13.sp,
                    color = Color.Gray.copy(0.8f),
                    modifier = Modifier.padding(start = 36.dp, top = 4.dp)
                )
            }

            // Subtle Mode Switcher
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f))
                    .padding(2.dp)
            ) {
                ToolMode.values().forEach { m ->
                    val isSelected = mode == m
                    val textCol = if (isSelected) textColor else Color.Gray

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) cardBg else Color.Transparent)
                            .clickable { viewModel.setMode(m) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (m == ToolMode.AI) "AI Ops" else "Utils",
                            fontSize = 12.sp,
                            fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = textCol
                        )
                    }
                }
            }
        }

        // --- AI MODE ---
        if (mode == ToolMode.AI) {
            // 1. Tool Cards (Row)
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AiTool.values().forEach { tool ->
                    val isSelected = activeAiTool == tool
                    val bg = if (isSelected) accentColor.copy(0.1f) else cardBg
                    val borderCol = if (isSelected) accentColor else borderColor
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .border(0.5.dp, borderCol, RoundedCornerShape(16.dp))
                            .clickable { viewModel.setAiTool(tool) }
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when(tool) {
                                AiTool.PAYLOAD -> Icons.Outlined.Code
                                AiTool.AUDIT -> Icons.Outlined.BugReport
                                AiTool.LOGS -> Icons.Outlined.Search
                            },
                            contentDescription = null,
                            tint = if (isSelected) accentColor else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tool.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) accentColor else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Input Area (Gemini Style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                // Header inside card
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Input, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Target Parameter", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                }

                TextField(
                    value = aiInputData,
                    onValueChange = { viewModel.updateAiInput(it) },
                    placeholder = { 
                        Text(
                            text = when(activeAiTool) {
                                AiTool.PAYLOAD -> "e.g., SQL Injection pattern for login..."
                                AiTool.AUDIT -> "Paste vulnerable code block..."
                                AiTool.LOGS -> "Paste server logs here..."
                            },
                            color = Color.Gray.copy(0.6f),
                            fontSize = 13.sp
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(inputBg), // Soft background for input
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = accentColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // Execute Button (Floating Right)
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterEnd) {
                    Button(
                        onClick = { viewModel.runAiTool() },
                        enabled = aiInputData.isNotEmpty() && !isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Execute", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            // FIXED: Using Icons.Default.ArrowForward
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Output Console
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp) // Fixed height for mobile scroll
                    .clip(RoundedCornerShape(20.dp))
                    .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0xFF050505) else Color.White)
            ) {
                // Console Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color.White.copy(0.03f) else Color.Black.copy(0.03f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Terminal, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SYSTEM OUTPUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                }
                
                Divider(color = borderColor, thickness = 0.5.dp)

                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    if (aiOutput.isNotEmpty()) {
                        val scrollState = rememberScrollState()
                        Text(
                            text = aiOutput,
                            color = if(isDark) Color(0xFF4ADE80) else Color(0xFF047857), // Matrix Green for text only
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.verticalScroll(scrollState)
                        )
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.Security, null, tint = Color.Gray.copy(0.2f), modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Ready for Operations", fontSize = 13.sp, color = Color.Gray.copy(0.6f))
                        }
                    }
                }
            }
        }

        // --- UTILITY MODE ---
        if (mode == ToolMode.UTILITY) {
            // 1. Utility Chips (FlowRow alternative using Scrollable Row)
            // FIXED: Added horizontalScroll(rememberScrollState())
             Row(
                 modifier = Modifier
                     .fillMaxWidth()
                     .horizontalScroll(rememberScrollState())
                     .padding(bottom = 24.dp), 
                 horizontalArrangement = Arrangement.spacedBy(8.dp)
             ) {
                UtilityTool.values().forEach { tool ->
                    val isSelected = activeUtilTool == tool
                    
                    // FIXED: Added required parameters 'enabled', 'selected' and fixed border defaults
                    FilterChip(
                        selected = isSelected,
                        enabled = true, // Required Param Added
                        onClick = { viewModel.setUtilTool(tool) },
                        label = { Text(tool.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = textColor,
                            selectedContainerColor = accentColor.copy(0.1f),
                            selectedLabelColor = accentColor
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, // Required Param Added
                            selected = isSelected, // Required Param Added
                            borderColor = borderColor,
                            selectedBorderColor = accentColor,
                            borderWidth = 0.5.dp
                        )
                    )
                }
            }

            // 2. Input
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardBg)
                    .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
            ) {
                Text("INPUT STRING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(16.dp))
                TextField(
                    value = utilInput,
                    onValueChange = { viewModel.updateUtilInput(it) },
                    placeholder = { Text("Type here...", fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                    modifier = Modifier.fillMaxSize().background(Color.Transparent),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Output
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color(0xFF050505) else Color(0xFFF1F5F9))
                    .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
            ) {
                Text("PROCESSED RESULT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(16.dp))
                TextField(
                    value = utilOutput,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxSize(),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = textColor, unfocusedTextColor = textColor),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}