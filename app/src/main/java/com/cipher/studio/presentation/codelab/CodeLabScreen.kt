package com.cipher.studio.presentation.codelab

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme

@Composable
fun CodeLabScreen(
    theme: Theme,
    viewModel: CodeLabViewModel = hiltViewModel()
) {
    val isDark = theme == Theme.DARK
    val prompt by viewModel.prompt.collectAsState()
    val code by viewModel.code.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    // Colors
    val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC) // Matches MainScreen bg
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.2f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(bgColor)
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Code Lab",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Generate and preview web apps instantly.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            // View Toggle Buttons
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f))
                    .padding(4.dp)
            ) {
                CodeLabViewMode.values().forEach { mode ->
                    val isSelected = viewMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) (if (isDark) Color.White.copy(0.1f) else Color.White) else Color.Transparent)
                            .clickable { viewModel.setViewMode(mode) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode.name, // CODE, PREVIEW, SPLIT
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) textColor else Color.Gray
                        )
                    }
                }
            }
        }

        // --- Input Area ---
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            TextField(
                value = prompt,
                onValueChange = { viewModel.updatePrompt(it) },
                placeholder = { Text("e.g., A calculator with a dark neon theme...", color = Color.Gray) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (isDark) Color.Black.copy(0.2f) else Color.White,
                    unfocusedContainerColor = if (isDark) Color.Black.copy(0.2f) else Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = { viewModel.handleGenerate() },
                enabled = prompt.isNotBlank() && !isGenerating,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PlayArrow, "Build")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Build")
                }
            }
        }

        // --- Content Area (Split / Code / Preview) ---
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Code Editor Section
            if (viewMode == CodeLabViewMode.CODE || viewMode == CodeLabViewMode.SPLIT) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF1E1E1E) else Color.White)
                ) {
                    // Editor Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(if (isDark) Color.White.copy(0.05f) else Color(0xFFF3F4F6))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, "Code", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("index.html", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        }
                        // Copy Button placeholder
                        Text("Copy", fontSize = 12.sp, color = Color(0xFF2563EB), modifier = Modifier.clickable { /* Copy Logic */ })
                    }

                    // Raw Code Text Area
                    Box(modifier = Modifier.padding(16.dp).weight(1f)) {
                        BasicTextField(
                            value = code,
                            onValueChange = {},
                            readOnly = true,
                            textStyle = TextStyle(
                                color = if (isDark) Color(0xFFD4D4D4) else Color(0xFF1F2937),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(textColor),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (viewMode == CodeLabViewMode.SPLIT) {
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Preview Section (WebView)
            if (viewMode == CodeLabViewMode.PREVIEW || viewMode == CodeLabViewMode.SPLIT) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .background(Color.White) // Webview usually needs white/neutral bg
                ) {
                    // Preview Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(Color(0xFFF3F4F6))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Web, "Preview", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Preview", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        }
                        // Browser Dots
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFF87171), CircleShape))
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFACC15), CircleShape))
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF4ADE80), CircleShape))
                        }
                    }

                    // Live WebView
                    if (code.isNotEmpty()) {
                        WebViewContainer(htmlContent = code)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Code, "Empty", tint = Color.Gray.copy(0.2f), modifier = Modifier.size(48.dp))
                                Text("Generated app will appear here", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(htmlContent: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                
                // Security: Restrict file access if needed, but for generated code usually ok
                // loadDataWithBaseURL needed for some CSS/JS to render properly
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    )
}