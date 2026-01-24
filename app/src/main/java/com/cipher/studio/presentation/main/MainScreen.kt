package com.cipher.studio.presentation.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.R
import com.cipher.studio.domain.model.ChatRole
import com.cipher.studio.domain.model.Theme
import com.cipher.studio.domain.model.ViewMode
import com.cipher.studio.presentation.about.AboutScreen
import com.cipher.studio.presentation.auth.EliteAuthScreen
import com.cipher.studio.presentation.codelab.CodeLabScreen
import com.cipher.studio.presentation.components.ChatMessageItem
import com.cipher.studio.presentation.components.ControlPanel
import com.cipher.studio.presentation.components.SettingsDialog
import com.cipher.studio.presentation.components.Sidebar
import com.cipher.studio.presentation.cyberhouse.CyberHouseScreen
import com.cipher.studio.presentation.dataanalyst.DataAnalystScreen
import com.cipher.studio.presentation.docintel.DocIntelScreen
import com.cipher.studio.presentation.prompt.PromptStudioScreen
import com.cipher.studio.presentation.visionhub.VisionHubScreen
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    var showSplash by remember { mutableStateOf(true) }

    // Splash Screen Timer
    LaunchedEffect(Unit) {
        delay(2500) // 2.5 seconds
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        val isAuthorized by viewModel.isAuthorized.collectAsState()

        // 1. ELITE AUTH CHECK (The Gatekeeper)
        if (!isAuthorized) {
            EliteAuthScreen(onLoginSuccess = { viewModel.setAuthorized(true) })
        } else {
            // 2. THE MAIN APP SYSTEM
            CipherAppSystem(viewModel)
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617)), // Dark Background
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // YOUR LOGO
            Image(
                painter = painterResource(id = R.drawable.my_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "CIPHER STUDIO",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                color = Color(0xFF10B981),
                trackColor = Color.White.copy(0.1f),
                modifier = Modifier.width(150.dp)
            )
        }
    }
}

@Composable
fun CipherAppSystem(viewModel: MainViewModel) {
    // Collect States
    val theme by viewModel.theme.collectAsState()
    val isDark = theme == Theme.DARK
    val currentView by viewModel.currentView.collectAsState()
    val isSidebarOpen by viewModel.isSidebarOpen.collectAsState()
    val config by viewModel.config.collectAsState()
    
    // Session States
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    
    // UI State for Overlays
    var isControlsOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Dynamic Background Colors
    val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)

    // Handle Back Press
    BackHandler(enabled = isSidebarOpen || isControlsOpen) {
        if (isControlsOpen) isControlsOpen = false
        else if (isSidebarOpen) viewModel.toggleSidebar()
    }

    // --- ROOT LAYOUT ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        
        // --- MAIN CONTENT ROW ---
        Row(modifier = Modifier.fillMaxSize()) {
            
            // A. SIDEBAR NAVIGATION
            Sidebar(
                sessions = sessions,
                currentSessionId = currentSessionId,
                onSelectSession = { sessionId ->
                    val session = sessions.find { it.id == sessionId }
                    if (session != null) viewModel.loadSession(session)
                },
                onNewSession = { viewModel.createNewSession() },
                onDeleteSession = { /* Implement delete logic */ },
                isOpen = isSidebarOpen,
                onToggleSidebar = { viewModel.toggleSidebar() },
                theme = theme,
                currentView = currentView,
                onViewChange = { viewModel.setViewMode(it) },
                onLogout = { viewModel.setAuthorized(false) },
                onOpenSettings = { showSettings = true }
            )

            // B. WORKSPACE AREA
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                // 1. Header
                AppHeader(
                    isDark = isDark,
                    currentView = currentView,
                    onToggleSidebar = { viewModel.toggleSidebar() },
                    onToggleControls = { isControlsOpen = !isControlsOpen }
                )

                // 2. Dynamic Module Switcher
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    when (currentView) {
                        // --- HERE IS WHERE CHATVIEW IS CALLED ---
                        ViewMode.CHAT -> ChatView(viewModel, isDark)
                        
                        ViewMode.CODE_LAB -> CodeLabScreen(theme = theme)
                        ViewMode.VISION_HUB -> VisionHubScreen(theme = theme)
                        ViewMode.PROMPT_STUDIO -> PromptStudioScreen(
                            theme = theme,
                            onUsePrompt = { prompt ->
                                viewModel.updatePrompt(prompt)
                                viewModel.setViewMode(ViewMode.CHAT)
                            }
                        )
                        ViewMode.CYBER_HOUSE -> CyberHouseScreen(theme = theme)
                        ViewMode.DATA_ANALYST -> DataAnalystScreen(theme = theme)
                        ViewMode.DOC_INTEL -> DocIntelScreen(theme = theme)
                        ViewMode.ABOUT -> AboutScreen(
                            theme = theme,
                            onBack = { viewModel.setViewMode(ViewMode.CHAT) }
                        )
                    }
                }
            }
        }

        // --- C. CONTROL PANEL OVERLAY ---
        ControlPanel(
            config = config,
            onChange = { /* Update config in VM */ },
            isOpen = isControlsOpen,
            onClose = { isControlsOpen = false },
            theme = theme
        )

        // --- D. SETTINGS DIALOG ---
        if (showSettings) {
            SettingsDialog(
                currentKey = null, // Security: Don't expose key
                onSave = { viewModel.saveApiKey(it) },
                onClear = { viewModel.removeApiKey() },
                onDismiss = { showSettings = false },
                isDark = isDark
            )
        }
    }
}

// --- SUB-COMPONENT: HEADER ---
@Composable
fun AppHeader(
    isDark: Boolean,
    currentView: ViewMode,
    onToggleSidebar: () -> Unit,
    onToggleControls: () -> Unit
) {
    val glassColor = if (isDark) Color(0x1AFFFFFF) else Color(0xCCFFFFFF)
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f)
    val iconColor = if (isDark) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(glassColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Menu, Logo & Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleSidebar) {
                Icon(Icons.Default.Menu, "Menu", tint = iconColor)
            }
            Spacer(modifier = Modifier.width(8.dp))
            
            // LOGO IN HEADER
            Image(
                painter = painterResource(id = R.drawable.my_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // View Badge
            Surface(
                color = if(isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(0.3f))
            ) {
                Text(
                    text = currentView.name.replace("_", " "),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        
        // Right: Elite Status & Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .background(Color(0x1A10B981), CircleShape)
                    .border(1.dp, Color(0x3310B981), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ELITE ACTIVE", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Black, 
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onToggleControls) {
                Icon(Icons.Default.Tune, "Settings", tint = iconColor)
            }
        }
    }
}

// --- IMPORTANT: CHAT VIEW DEFINITION ---
// This was likely missing or out of scope in the previous file.
@Composable
fun ChatView(viewModel: MainViewModel, isDark: Boolean) {
    val history by viewModel.history.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    // Mock attachments state for now, assuming logic exists in VM
    // val attachments by viewModel.attachments.collectAsState() 
    
    val listState = rememberLazyListState()

    // Auto Scroll Logic
    LaunchedEffect(history.size, isStreaming) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        
        // 1. CHAT LIST AREA
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (history.isEmpty()) {
                item { EmptyState(isDark) }
            } else {
                items(history) { message ->
                    ChatMessageItem(
                        msg = message,
                        isDark = isDark,
                        isStreaming = isStreaming && message == history.last() && message.role == ChatRole.MODEL,
                        onSpeak = { text -> viewModel.speakText(text) },
                        onPin = { id -> viewModel.togglePin(id) }
                    )
                }
            }
            
            if (isStreaming) {
                item { StreamingIndicator() }
            }
            
            // Spacer for input area visibility
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // 2. INPUT AREA (Glassmorphism 3D)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.98f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) Color(0xFF0F172A).copy(0.9f) else Color.White.copy(0.9f))
                    .border(1.dp, if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f), RoundedCornerShape(24.dp))
                    .padding(8.dp)
                    .animateContentSize()
            ) {
                // Attachments Area (Placeholder if you implemented attachments in VM)
                /* if (attachments.isNotEmpty()) { ... } */

                Row(verticalAlignment = Alignment.Bottom) {
                    // File Upload
                    IconButton(onClick = { /* File Picker logic */ }) {
                        Icon(Icons.Default.AddPhotoAlternate, "Upload", tint = Color.Gray)
                    }
                    
                    // Text Field
                    TextField(
                        value = prompt,
                        onValueChange = { viewModel.updatePrompt(it) },
                        placeholder = { Text("Ask Cipher Omni-Mind...", fontSize = 14.sp, color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = if (isDark) Color.White else Color.Black,
                            unfocusedTextColor = if (isDark) Color.White else Color.Black
                        ),
                        maxLines = 5
                    )

                    // Send / Stop Button
                    IconButton(
                        onClick = { viewModel.handleRun() },
                        enabled = prompt.isNotBlank() || isStreaming,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = if (isStreaming) 
                                        listOf(Color(0xFFEF4444), Color(0xFFDC2626)) // Red for Stop
                                    else 
                                        listOf(Color(0xFF2563EB), Color(0xFF3B82F6)) // Blue for Send
                                ),
                                shape = CircleShape
                            )
                            .size(42.dp)
                    ) {
                        if (isStreaming) {
                            Icon(Icons.Default.Stop, "Stop", tint = Color.White)
                        } else {
                            Icon(Icons.Default.ArrowUpward, "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-COMPONENT: EMPTY STATE ---
@Composable
fun EmptyState(isDark: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp)
    ) {
        // Glowing Logo Placeholder
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
             // Main Logo
             Image(
                painter = painterResource(id = R.drawable.my_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(100.dp)
             )
             // Glow effect behind
             Box(
                 modifier = Modifier
                     .size(100.dp)
                     .background(Color(0xFF2563EB).copy(0.1f), CircleShape)
                     .clip(CircleShape)
             )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Cipher Studio",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = if (isDark) Color.White else Color.Black,
            letterSpacing = (-1).sp
        )
        
        Text(
            text = "Advanced reasoning. Beautifully designed.",
            color = Color.Gray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Light
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Suggestion Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionChip(text = "Analyze this code", isDark)
            SuggestionChip(text = "Write a python script", isDark)
        }
    }
}

@Composable
fun SuggestionChip(text: String, isDark: Boolean) {
    Surface(
        color = if(isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if(isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f))
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// --- SUB-COMPONENT: STREAMING INDICATOR ---
@Composable
fun StreamingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 48.dp, bottom = 16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = Color(0xFF2563EB)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Cipher Omni-Mind is thinking...",
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}