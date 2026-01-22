package com.cipher.studio.presentation.main

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.ChatMessage
import com.cipher.studio.domain.model.ChatRole
import com.cipher.studio.domain.model.Theme
import com.cipher.studio.domain.model.ViewMode
import com.cipher.studio.presentation.auth.EliteAuthScreen
import com.cipher.studio.presentation.components.Sidebar

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    
    // Auth Check: If not authorized, show EliteAuthScreen
    if (!isAuthorized) {
        EliteAuthScreen(onLoginSuccess = { viewModel.setAuthorized(true) })
    } else {
        AppContent(viewModel)
    }
}

@Composable
fun AppContent(viewModel: MainViewModel) {
    val theme by viewModel.theme.collectAsState()
    val isDark = theme == Theme.DARK
    val currentView by viewModel.currentView.collectAsState()
    val isSidebarOpen by viewModel.isSidebarOpen.collectAsState()
    
    // Session State
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    
    // Dynamic Background Colors based on Theme
    val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // --- SIDEBAR INTEGRATION ---
        // This connects the logic from MainViewModel to the Sidebar UI
        Sidebar(
            sessions = sessions,
            currentSessionId = currentSessionId,
            onSelectSession = { sessionId ->
                // Find the session object by ID and load it
                val sessionToLoad = sessions.find { it.id == sessionId }
                if (sessionToLoad != null) {
                    viewModel.loadSession(sessionToLoad)
                }
            },
            onNewSession = { viewModel.createNewSession() },
            onDeleteSession = { sessionId ->
                // Note: Ensure deleteSession is implemented in ViewModel
                // viewModel.deleteSession(sessionId) 
            },
            isOpen = isSidebarOpen,
            onToggleSidebar = { viewModel.toggleSidebar() },
            theme = theme,
            currentView = currentView,
            onViewChange = { mode -> viewModel.setViewMode(mode) },
            onLogout = { viewModel.setAuthorized(false) }
        )

        // --- MAIN CONTENT AREA ---
        Column(
            modifier = Modifier
                .weight(1f) // Takes remaining width
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            // Header
            AppHeader(
                isDark = isDark,
                currentView = currentView,
                onToggleSidebar = { viewModel.toggleSidebar() }
            )

            // Dynamic View Switcher
            Box(modifier = Modifier.weight(1f)) {
                when (currentView) {
                    ViewMode.CHAT -> ChatView(viewModel, isDark)
                    ViewMode.CODE_LAB -> PlaceholderView("Code Lab")
                    ViewMode.PROMPT_STUDIO -> PlaceholderView("Prompt Studio")
                    ViewMode.VISION_HUB -> PlaceholderView("Vision Hub")
                    ViewMode.DATA_ANALYST -> PlaceholderView("Data Analyst")
                    ViewMode.DOC_INTEL -> PlaceholderView("Doc Intel")
                    ViewMode.CYBER_HOUSE -> PlaceholderView("Cyber House")
                    ViewMode.ABOUT -> PlaceholderView("About Developer")
                }
            }
        }
    }
}

@Composable
fun AppHeader(isDark: Boolean, currentView: ViewMode, onToggleSidebar: () -> Unit) {
    val glassColor = if (isDark) Color(0x1AFFFFFF) else Color(0xCCFFFFFF)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(glassColor)
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Sidebar Toggle Button (Visible on all screens in native logic)
            IconButton(onClick = onToggleSidebar) {
                Icon(Icons.Default.Menu, "Menu", tint = if (isDark) Color.White else Color.Black)
            }
            Spacer(modifier = Modifier.width(8.dp))
            
            // View Title
            Text(
                text = currentView.name.replace("_", " "),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB), // Blue Primary
                fontSize = 14.sp
            )
        }
        
        // Elite Status Badge
        Row(
            modifier = Modifier
                .background(Color(0x1A10B981), CircleShape)
                .border(1.dp, Color(0x3310B981), CircleShape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "ELITE ACTIVE", 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Black, 
                color = Color(0xFF10B981)
            )
        }
    }
}

@Composable
fun ChatView(viewModel: MainViewModel, isDark: Boolean) {
    val history by viewModel.history.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val listState = rememberLazyListState()

    // Auto Scroll to bottom when history changes or streaming updates
    LaunchedEffect(history.size, isStreaming) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat History List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (history.isEmpty()) {
                item { EmptyState(isDark) }
            } else {
                items(history) { message ->
                    ChatMessageItem(message, isDark)
                }
            }
            
            if (isStreaming) {
                item { StreamingIndicator() }
            }
        }

        // Input Area (3D Glassmorphism Look)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(32.dp))
                    .background(if (isDark) Color(0x33000000) else Color(0xCCFFFFFF))
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(32.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    // File Upload Button (Placeholder logic)
                    IconButton(onClick = { /* Implement File Upload */ }) {
                        Icon(Icons.Default.Image, "Upload", tint = Color.Gray)
                    }
                    
                    // Text Input
                    TextField(
                        value = prompt,
                        onValueChange = { viewModel.updatePrompt(it) },
                        placeholder = { Text("Ask anything...", fontSize = 14.sp) },
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
                        maxLines = 4
                    )

                    // Send / Stop Button
                    IconButton(
                        onClick = { viewModel.handleRun() },
                        enabled = prompt.isNotBlank() || isStreaming,
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF2563EB), Color(0xFF3B82F6))
                                ),
                                shape = CircleShape
                            )
                    ) {
                        if (isStreaming) {
                            Icon(Icons.Default.Stop, "Stop", tint = Color.White)
                        } else {
                            Icon(Icons.Default.Send, "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage, isDark: Boolean) {
    val isUser = message.role == ChatRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) Color(0xFF2563EB) else if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isUser) Color.White else if (isDark) Color.White else Color.Black

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            // Model Icon
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF2563EB), CircleShape)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Bot",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Message Bubble
            Column(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isUser) 20.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 20.dp
                        )
                    )
                    .background(bubbleColor)
                    .padding(16.dp)
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                
                // Grounding / Sources (If available)
                if (message.groundingMetadata?.groundingChunks?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sources available",
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(isDark: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 100.dp)
    ) {
        // Logo Placeholder with Glow
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                imageVector = Icons.Default.Hexagon,
                contentDescription = "Logo",
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(80.dp)
             )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Cipher Studio",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color.Black
        )
        
        Text(
            text = "Advanced reasoning. Beautifully designed.",
            color = Color.Gray,
            fontSize = 14.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Shortcut Hint
        Text(
            text = "Type and press Send to execute",
            fontSize = 12.sp,
            color = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier
                .background(
                    if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f), 
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StreamingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 48.dp) // Align with bot text
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = Color(0xFF2563EB)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Processing...",
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlaceholderView(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Text(
                text = "Under Construction",
                fontSize = 14.sp,
                color = Color.Gray.copy(alpha = 0.5f)
            )
        }
    }
}