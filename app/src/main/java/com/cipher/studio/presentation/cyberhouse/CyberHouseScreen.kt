package com.cipher.studio.presentation.cyberhouse

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme

@Composable
fun CyberHouseScreen(
    theme: Theme,
    viewModel: CyberHouseViewModel = hiltViewModel()
) {
    val isDark = theme == Theme.DARK
    
    // States
    val mode by viewModel.mode.collectAsState()
    val activeAiTool by viewModel.activeAiTool.collectAsState()
    val aiInputData by viewModel.aiInputData.collectAsState()
    val aiOutput by viewModel.aiOutput.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    
    val activeUtilTool by viewModel.activeUtilTool.collectAsState()
    val utilInput by viewModel.utilInput.collectAsState()
    val utilOutput by viewModel.utilOutput.collectAsState()

    // Colors (Hacker Theme)
    val bgColor = if (isDark) Color(0xFF050505) else Color(0xFFF9FAFB)
    val textColor = if (isDark) Color(0xFF22C55E) else Color(0xFF111827) // Green-500 or Gray-900
    val borderColor = if (isDark) Color(0xFF22C55E).copy(0.3f) else Color.Gray.copy(0.3f)
    val panelBg = if (isDark) Color.Black else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, "Shield", tint = if (isDark) Color(0xFF22C55E) else Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CYBER HOUSE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textColor,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ROOT ACCESS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color(0xFFDC2626), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "Advanced Security Operations & Red Teaming Console.",
                    fontSize = 12.sp,
                    color = if (isDark) Color.Gray else Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Mode Switcher
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .background(if (isDark) Color.Black.copy(0.4f) else Color.Gray.copy(0.1f))
                    .padding(4.dp)
            ) {
                ToolMode.values().forEach { m ->
                    val isSelected = mode == m
                    val bg = if (isSelected) (if (isDark) Color(0xFF22C55E).copy(0.2f) else Color.White) else Color.Transparent
                    val textCol = if (isSelected) (if (isDark) Color(0xFF4ADE80) else Color.Black) else Color.Gray
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(bg)
                            .clickable { viewModel.setMode(m) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (m == ToolMode.AI) Icons.Default.Memory else Icons.Default.Terminal, 
                                contentDescription = null,
                                tint = textCol,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (m == ToolMode.AI) "AI OPS" else "UTILS (JS)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textCol
                            )
                        }
                    }
                }
            }
        }

        // --- AI MODE ---
        if (mode == ToolMode.AI) {
            Row(modifier = Modifier.weight(1f)) {
                // Left: Input & Tools
                Column(modifier = Modifier.weight(0.35f).fillMaxHeight()) {
                    // Tool Selector
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiTool.values().forEach { tool ->
                            val isSelected = activeAiTool == tool
                            val bg = if (isSelected) (if (isDark) Color(0xFF22C55E).copy(0.1f) else Color.Black) else Color.Transparent
                            val border = if (isSelected) (if (isDark) Color(0xFF22C55E) else Color.Black) else borderColor
                            val textC = if (isSelected) (if (isDark) Color(0xFF4ADE80) else Color.White) else Color.Gray
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, border, RoundedCornerShape(4.dp))
                                    .background(bg, RoundedCornerShape(4.dp))
                                    .clickable { viewModel.setAiTool(tool) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = when(tool) {
                                            AiTool.PAYLOAD -> Icons.Default.Terminal
                                            AiTool.AUDIT -> Icons.Default.BugReport
                                            AiTool.LOGS -> Icons.Default.Search
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) textC else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tool.name,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) textC else Color.Gray,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                            .background(panelBg)
                    ) {
                        Text(
                            text = "TARGET INPUT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF22C55E) else Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDark) Color(0xFF22C55E).copy(0.05f) else Color.Gray.copy(0.1f))
                                .padding(8.dp)
                        )
                        
                        TextField(
                            value = aiInputData,
                            onValueChange = { viewModel.updateAiInput(it) },
                            placeholder = { 
                                Text(
                                    text = when(activeAiTool) {
                                        AiTool.PAYLOAD -> "e.g., SQL Injection for Login Page..."
                                        AiTool.AUDIT -> "// Paste vulnerable code..."
                                        AiTool.LOGS -> "Paste server logs..."
                                    },
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                ) 
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        )

                        // Execute Button
                        Button(
                            onClick = { viewModel.runAiTool() },
                            enabled = aiInputData.isNotEmpty() && !isProcessing,
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF16A34A) else Color.Black,
                                contentColor = if (isDark) Color.Black else Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("EXECUTE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right: Output
                Column(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                        .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0xFF050505) else Color.White)
                ) {
                    // Simple Matrix effect background could go here if using Canvas
                    Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                        val scrollState = rememberScrollState()
                        if (aiOutput.isNotEmpty()) {
                            Text(
                                text = aiOutput,
                                color = textColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                modifier = Modifier.verticalScroll(scrollState)
                            )
                        } else {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Security, null, tint = Color.Gray.copy(0.3f), modifier = Modifier.size(64.dp))
                                Text("SYSTEM READY", fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                                Text("AWAITING TARGET", fontSize = 10.sp, color = Color.Gray.copy(0.5f), fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // --- UTILITY MODE ---
        if (mode == ToolMode.UTILITY) {
            Column(modifier = Modifier.weight(1f)) {
                // Tool Select
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UtilityTool.values().forEach { tool ->
                        val isSelected = activeUtilTool == tool
                        Button(
                            onClick = { viewModel.setUtilTool(tool) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) (if (isDark) Color(0xFF22C55E).copy(0.2f) else Color.Black) else Color.Transparent
                            ),
                            border = if (isSelected) BorderStroke(1.dp, if (isDark) Color(0xFF22C55E) else Color.Black) else BorderStroke(1.dp, borderColor),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tool.name, 
                                fontSize = 12.sp, 
                                color = if (isSelected) (if (isDark) Color(0xFF4ADE80) else Color.White) else Color.Gray
                            )
                        }
                    }
                }

                Row(modifier = Modifier.weight(1f)) {
                    // Input
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                            .background(panelBg)
                    ) {
                        Text(
                            text = "INPUT STRING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp)
                        )
                        TextField(
                            value = utilInput,
                            onValueChange = { viewModel.updateUtilInput(it) },
                            placeholder = { Text("Type here...", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxSize(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Output
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                            .background(if (isDark) Color(0xFF050505) else Color(0xFFF9FAFB))
                    ) {
                        Text(
                            text = "PROCESSED OUTPUT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(8.dp)
                        )
                        TextField(
                            value = utilOutput,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxSize(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }
        }
    }
}