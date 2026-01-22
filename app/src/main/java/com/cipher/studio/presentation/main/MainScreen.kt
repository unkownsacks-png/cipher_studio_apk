package com.cipher.studio.presentation.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.*
import com.cipher.studio.presentation.auth.EliteAuthScreen

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val isAuthorized by viewModel.isAuthorized.collectAsState()
    
    // Auth Check
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
    
    // Dynamic Background Colors
    val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)

    // Layout Structure
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Sidebar (Conditional)
        AnimatedVisibility(visible = isSidebarOpen) {
            Box(modifier = Modifier.width(280.dp).fillMaxHeight().background(Color.Black.copy(0.2f))) {
                // Placeholder for Sidebar Component
                Text("Sidebar Here", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }

        // Main Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(12.dp)
        ) {
            // Header
            AppHeader(
                isDark = isDark,
                currentView = currentView,
                onToggleSidebar = { viewModel.toggleSidebar() }
            )

            // Dynamic View Content
            Box(modifier = Modifier.weight(1f)) {
                when (currentView) {
                    ViewMode.CHAT -> ChatView(viewModel, isDark)
                    else -> PlaceholderView(currentView.name)
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
            IconButton(onClick = onToggleSidebar) {
                Icon(Icons.Default.Menu, "Menu", tint = if (isDark) Color.White else Color.Black)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentView.name.replace("_", " "),
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.Blue else Color.Blue,
                fontSize = 14.sp
            )
        }
        
        // Elite Badge
        Row(
            modifier = Modifier
                .background(Color(0x1A10B981), CircleShape)
                .border(1.dp, Color(0x3310B981), CircleShape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text("ELITE ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
        }
    }
}

@Composable
fun ChatView(viewModel: MainViewModel, isDark: Boolean) {
    val history by viewModel.history.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val listState = rememberLazyListState()

    // Auto Scroll
    LaunchedEffect(history.size, isStreaming) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat List
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
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
                item { StreamingIndicator(isDark) }
            }
        }

        // Input Area (3D Glass Look)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(32.dp))
                    .background(if (isDark) Color(0x33000000) else Color(0xCCFFFFFF))
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(32.dp))
                    .padding(8.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    IconButton(onClick = { /* File Upload */ }) {
                        Icon(Icons.Default.Image, "Upload", tint = Color.Gray)
                    }
                    
                    TextField(
                        value = prompt,
                        onValueChange = { viewModel.updatePrompt(it) },
                        placeholder = { Text("Ask anything...", fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = { viewModel.handleRun() },
                        enabled = prompt.isNotBlank() || isStreaming,
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF3B82F6))),
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
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!isUser) {
                // Bot Icon
                Box(modifier = Modifier.size(32.dp).background(Color.Blue, CircleShape).padding(6.dp)) {
                    // Cipher Logo Path would go here
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

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
                Text(text = message.text, color = textColor, fontSize = 15.sp, lineHeight = 22.sp)
            }
        }
    }
}

@Composable
fun EmptyState(isDark: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 100.dp)
    ) {
        Text("Cipher Studio", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
        Text("Advanced reasoning. Beautifully designed.", color = Color.Gray)
    }
}

@Composable
fun StreamingIndicator(isDark: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 40.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Blue)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Processing...", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun PlaceholderView(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("View: $name", fontSize = 24.sp, color = Color.Gray)
    }
}