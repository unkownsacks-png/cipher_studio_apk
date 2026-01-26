package com.cipher.studio.presentation.codelab

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme
import java.util.regex.Pattern

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
    val deviceFrame by viewModel.deviceFrame.collectAsState()
    val consoleLogs by viewModel.consoleLogs.collectAsState()
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Colors
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val editorBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val accentColor = Color(0xFF3B82F6)

    // Layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- 1. TOP BAR (Tools & Modes) ---
        CodeLabTopBar(
            isDark = isDark,
            viewMode = viewMode,
            onModeChange = { viewModel.setViewMode(it) },
            onUndo = { viewModel.undo() },
            onCopy = { 
                clipboardManager.setText(AnnotatedString(code))
                Toast.makeText(context, "Code Copied!", Toast.LENGTH_SHORT).show()
            }
        )

        // --- 2. MAIN WORKSPACE ---
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            
            // EDITOR MODE
            if (viewMode == CodeViewMode.EDITOR || viewMode == CodeViewMode.SPLIT) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(editorBg)
                    ) {
                        // FEATURE 2: Syntax Highlighting Editor
                        CodeEditor(
                            code = code,
                            isDark = isDark,
                            isReadOnly = true // AI writes, user reads/copies for now
                        )
                        
                        // Line Numbers (Visual)
                        Column(modifier = Modifier.padding(top = 16.dp, start = 8.dp)) {
                            (1..20).forEach { 
                                Text("$it", color = Color.Gray.copy(0.4f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                    // If Split, Preview is on the right/bottom
                    if (viewMode == CodeViewMode.SPLIT) {
                        Box(modifier = Modifier.width(8.dp)) // Spacer
                    }
                }
            }

            // PREVIEW MODE (Overlays if Preview, side-by-side if Split)
            if (viewMode == CodeViewMode.PREVIEW || viewMode == CodeViewMode.SPLIT) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if(viewMode == CodeViewMode.SPLIT) 8.dp else 0.dp)
                        .padding(start = if(viewMode == CodeViewMode.SPLIT) 50.dp else 0.dp) // Offset for split
                        .background(if(viewMode == CodeViewMode.SPLIT) Color.Transparent else bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    // FEATURE 3: Device Frame Simulation
                    DeviceFrameContainer(
                        frame = deviceFrame,
                        onFrameChange = { viewModel.setDeviceFrame(it) },
                        content = {
                            WebViewContainer(
                                htmlContent = code,
                                onConsoleMessage = { viewModel.addConsoleLog(it) }
                            )
                        }
                    )
                }
            }
            
            // FEATURE 4: Live Console Bottom Sheet (Overlay)
            if (consoleLogs.isNotEmpty()) {
                ConsoleOverlay(
                    logs = consoleLogs, 
                    onClear = { viewModel.clearConsole() },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 16.dp)
                )
            }
        }

        // --- 3. BOTTOM CONTROL BAR (Advanced Input) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF0B0F19) else Color.White)
                .shadow(16.dp)
        ) {
            // Feature 5: Smart Refactor Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Dark Mode", "Animate", "Fix Bug", "Beautify").forEach { action ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.applyQuickAction(action) },
                        label = { Text(action, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Rounded.AutoFixHigh, null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if(isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            labelColor = textColor
                        )
                    )
                }
            }

            // Input Field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.2f), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = prompt,
                    onValueChange = { viewModel.updatePrompt(it) },
                    textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .heightIn(max = 100.dp),
                    cursorBrush = SolidColor(accentColor),
                    decorationBox = { innerTextField ->
                        if (prompt.isEmpty()) Text("Describe UI to build...", color = Color.Gray)
                        innerTextField()
                    }
                )

                // Generate Button
                Button(
                    onClick = { viewModel.handleGenerate() },
                    enabled = prompt.isNotBlank() && !isGenerating,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

// --- FEATURE 2: SYNTAX HIGHLIGHTING (Regex Based) ---
@Composable
fun CodeEditor(code: String, isDark: Boolean, isReadOnly: Boolean) {
    val scrollState = rememberScrollState()
    
    // Simple Syntax Highlighting Logic
    val annotatedString = remember(code, isDark) {
        highlightCode(code, isDark)
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(start = 32.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
        .verticalScroll(scrollState)
    ) {
        Text(
            text = annotatedString,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

fun highlightCode(code: String, isDark: Boolean): AnnotatedString {
    return buildAnnotatedString {
        val tagColor = if(isDark) Color(0xFFF87171) else Color(0xFFD32F2F) // Red for tags
        val attrColor = if(isDark) Color(0xFFFBBF24) else Color(0xFFF57C00) // Orange for attributes
        val strColor = if(isDark) Color(0xFF4ADE80) else Color(0xFF388E3C) // Green for strings
        val baseColor = if(isDark) Color(0xFFE2E8F0) else Color(0xFF334155)

        withStyle(SpanStyle(color = baseColor)) {
            append(code)
        }

        // Very basic regex for HTML tags (Optimized for performance)
        val tagPattern = Pattern.compile("(<\\/?[a-z]+>?)", Pattern.CASE_INSENSITIVE)
        val matcher = tagPattern.matcher(code)
        while (matcher.find()) {
            addStyle(SpanStyle(color = tagColor, fontWeight = FontWeight.Bold), matcher.start(), matcher.end())
        }
        
        // Strings
        val strPattern = Pattern.compile("(\"[^\"]*\")|('[^']*')")
        val strMatcher = strPattern.matcher(code)
        while (strMatcher.find()) {
            addStyle(SpanStyle(color = strColor), strMatcher.start(), strMatcher.end())
        }
    }
}

// --- FEATURE 3: DEVICE FRAME SIMULATION ---
@Composable
fun DeviceFrameContainer(
    frame: DeviceFrame,
    onFrameChange: (DeviceFrame) -> Unit,
    content: @Composable () -> Unit
) {
    val widthFactor by animateFloatAsState(
        targetValue = when(frame) {
            DeviceFrame.PHONE -> 0.45f
            DeviceFrame.TABLET -> 0.75f
            DeviceFrame.DESKTOP -> 0.95f
        }, label = "width"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Frame Selector
        Row(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .background(Color.Black.copy(0.1f), CircleShape)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeviceFrame.values().forEach { 
                val isSel = it == frame
                Icon(
                    imageVector = when(it) {
                        DeviceFrame.PHONE -> Icons.Rounded.Smartphone
                        DeviceFrame.TABLET -> Icons.Rounded.TabletMac
                        DeviceFrame.DESKTOP -> Icons.Rounded.DesktopMac
                    },
                    contentDescription = null,
                    tint = if(isSel) Color(0xFF3B82F6) else Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onFrameChange(it) }
                        .padding(4.dp)
                )
            }
        }

        // The Device Bezel
        Box(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth(widthFactor)
                .shadow(20.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .padding(
                    top = if(frame == DeviceFrame.PHONE) 30.dp else 12.dp, 
                    bottom = if(frame == DeviceFrame.PHONE) 30.dp else 12.dp,
                    start = 12.dp, 
                    end = 12.dp
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
            ) {
                content()
            }
            
            // Fake Camera Notch for Phone
            if(frame == DeviceFrame.PHONE) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-20).dp)
                        .width(60.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                        .background(Color.Black)
                )
            }
        }
    }
}

// --- FEATURE 4: LIVE CONSOLE OVERLAY ---
@Composable
fun ConsoleOverlay(logs: List<String>, onClear: () -> Unit, modifier: Modifier) {
    Card(
        modifier = modifier.width(300.dp).heightIn(max = 200.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Terminal", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp).clickable { onClear() })
            }
            Divider(color = Color.Gray.copy(0.3f))
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                items(logs) { log ->
                    Text(
                        "> $log",
                        color = Color(0xFFFF5555), // Red for errors
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// --- TOP BAR COMPONENT ---
@Composable
fun CodeLabTopBar(
    isDark: Boolean,
    viewMode: CodeViewMode,
    onModeChange: (CodeViewMode) -> Unit,
    onUndo: () -> Unit,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Code, null, tint = Color(0xFF3B82F6))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Code Lab", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if(isDark) Color.White else Color.Black)
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if(isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                .padding(4.dp)
        ) {
            CodeViewMode.values().forEach { mode ->
                val isSel = mode == viewMode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if(isSel) if(isDark) Color(0xFF334155) else Color.White else Color.Transparent)
                        .clickable { onModeChange(mode) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        mode.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if(isSel) (if(isDark) Color.White else Color.Black) else Color.Gray
                    )
                }
            }
        }

        Row {
            IconButton(onClick = onUndo) { Icon(Icons.Rounded.Undo, "Undo", tint = Color.Gray) }
            IconButton(onClick = onCopy) { Icon(Icons.Rounded.ContentCopy, "Copy", tint = Color.Gray) }
        }
    }
}

// --- WEBVIEW ---
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(htmlContent: String, onConsoleMessage: (String) -> Unit) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                
                webViewClient = WebViewClient()
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                        message?.message()?.let { onConsoleMessage(it) }
                        return true
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    )
}