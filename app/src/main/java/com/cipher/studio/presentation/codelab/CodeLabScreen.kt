package com.cipher.studio.presentation.codelab

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.2f)
    
    // Feature 5: Fullscreen Preview Mode State
    var isFullscreen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Mobile-friendly scrolling
    ) {
        // --- Header (Minimalist) ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Feature 1: Animated Icon
                Icon(Icons.Rounded.AutoAwesome, "AI", tint = Color(0xFF3B82F6), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Code Lab",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium, // Reduced weight for minimalist look
                    color = textColor
                )
            }

            // Minimalist View Toggles (Subtle)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f))
                    .padding(2.dp)
            ) {
                CodeLabViewMode.values().forEach { mode ->
                    val isSelected = viewMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) (if (isDark) Color.White.copy(0.1f) else Color.White) else Color.Transparent)
                            .clickable { viewModel.setViewMode(mode) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = if(isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) textColor else Color.Gray
                        )
                    }
                }
            }
        }

        // --- Feature 2: Smart Templates (Horizontal Scroll) ---
        if (code.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val templates = listOf("Login Page", "Portfolio", "Calculator", "To-Do List", "Landing Page")
                templates.forEach { temp ->
                    SuggestionChip(
                        onClick = { viewModel.updatePrompt("Create a modern $temp with dark mode support") },
                        label = { Text(temp, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                        ),
                        border = BorderStroke(1.dp, borderColor),
                        shape = CircleShape
                    )
                }
            }
        }

        // --- Minimalist Input Area (Floating Bar) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(0.1f))
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color.White)
                .border(0.5.dp, borderColor, RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = prompt,
                    onValueChange = { viewModel.updatePrompt(it) },
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .heightIn(min = 24.dp),
                    cursorBrush = SolidColor(Color(0xFF3B82F6)),
                    decorationBox = { innerTextField ->
                        if (prompt.isEmpty()) {
                            Text("Describe your app concept...", color = Color.Gray.copy(0.6f), fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                )

                Button(
                    onClick = { viewModel.handleGenerate() },
                    enabled = prompt.isNotBlank() && !isGenerating,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color.White else Color.Black,
                        contentColor = if (isDark) Color.Black else Color.White
                    )
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = if (isDark) Color.Black else Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // --- Main Workspace (Vertical Stacking for Mobile) ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Code Editor Section
            if (viewMode == CodeLabViewMode.CODE || viewMode == CodeLabViewMode.SPLIT) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(0.5.dp, borderColor, RoundedCornerShape(24.dp))
                        .background(if (isDark) Color(0xFF0F172A) else Color.White)
                ) {
                    // Subtle Header with Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFEF4444), CircleShape))
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFEAB308), CircleShape))
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF22C55E), CircleShape))
                        }
                        
                        // Feature 3: Copy Code Action
                        IconButton(
                            onClick = { 
                                clipboardManager.setText(AnnotatedString(code)) 
                                Toast.makeText(context, "Code Copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, "Copy", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }

                    Divider(color = borderColor, thickness = 0.5.dp)

                    // Editor
                    Box(modifier = Modifier.padding(16.dp).weight(1f)) {
                        BasicTextField(
                            value = code,
                            onValueChange = {},
                            readOnly = true,
                            textStyle = TextStyle(
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(textColor),
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        )
                    }
                }
            }

            // 2. Live Preview Section
            if (viewMode == CodeLabViewMode.PREVIEW || viewMode == CodeLabViewMode.SPLIT) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(0.5.dp, borderColor, RoundedCornerShape(24.dp))
                        .background(Color.White) // Preview always white bg
                ) {
                    // Subtle Header with Fullscreen Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Preview", fontSize = 12.sp, color = Color.Gray.copy(0.7f), fontWeight = FontWeight.Bold)
                        
                        // Feature 5: Fullscreen Toggle (Mock logic)
                        IconButton(onClick = { isFullscreen = !isFullscreen }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                if(isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, 
                                "Toggle", 
                                tint = Color.Gray, 
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Divider(color = Color.Gray.copy(0.1f), thickness = 0.5.dp)

                    // WebView
                    if (code.isNotEmpty()) {
                        WebViewContainer(htmlContent = code)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No preview available", fontSize = 12.sp, color = Color.Gray.copy(0.4f))
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
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    )
}