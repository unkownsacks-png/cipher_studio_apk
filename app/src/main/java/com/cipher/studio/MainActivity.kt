package com.cipher.studio.presentation.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.R
import com.cipher.studio.domain.model.ViewMode
import com.cipher.studio.presentation.auth.EliteAuthScreen
import com.cipher.studio.presentation.codelab.CodeLabScreen
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
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    var showSplash by remember { mutableStateOf(true) }

    // Splash Screen Logic
    LaunchedEffect(Unit) {
        delay(2500) // 2.5 seconds splash
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        val isAuthorized by viewModel.isAuthorized.collectAsState()
        
        if (!isAuthorized) {
            EliteAuthScreen(onLoginSuccess = { viewModel.setAuthorized(true) })
        } else {
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
            // YOUR LOGO HERE
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
    val theme by viewModel.theme.collectAsState()
    val isDark = com.cipher.studio.domain.model.Theme.DARK == theme
    val currentView by viewModel.currentView.collectAsState()
    val isSidebarOpen by viewModel.isSidebarOpen.collectAsState()
    val config by viewModel.config.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    
    // Settings Dialog State
    var showSettings by remember { mutableStateOf(false) }
    var isControlsOpen by remember { mutableStateOf(false) }

    val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(
                sessions = sessions,
                currentSessionId = currentSessionId,
                onSelectSession = { id -> sessions.find { it.id == id }?.let { viewModel.loadSession(it) } },
                onNewSession = { viewModel.createNewSession() },
                onDeleteSession = { /* */ },
                isOpen = isSidebarOpen,
                onToggleSidebar = { viewModel.toggleSidebar() },
                theme = theme,
                currentView = currentView,
                onViewChange = { viewModel.setViewMode(it) },
                onLogout = { viewModel.setAuthorized(false) },
                onOpenSettings = { showSettings = true } // Open Settings
            )

            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(12.dp)) {
                // Header with Logo
                AppHeader(isDark, currentView, onToggleSidebar = { viewModel.toggleSidebar() })

                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
                    when (currentView) {
                        ViewMode.CHAT -> ChatView(viewModel, isDark)
                        ViewMode.CODE_LAB -> CodeLabScreen(theme = theme, viewModel = hiltViewModel())
                        ViewMode.VISION_HUB -> VisionHubScreen(theme = theme, viewModel = hiltViewModel())
                        ViewMode.PROMPT_STUDIO -> PromptStudioScreen(theme = theme, onUsePrompt = { viewModel.updatePrompt(it); viewModel.setViewMode(ViewMode.CHAT) })
                        ViewMode.CYBER_HOUSE -> CyberHouseScreen(theme = theme, viewModel = hiltViewModel())
                        ViewMode.DATA_ANALYST -> DataAnalystScreen(theme = theme, viewModel = hiltViewModel())
                        ViewMode.DOC_INTEL -> DocIntelScreen(theme = theme, viewModel = hiltViewModel())
                        // About, etc.
                        else -> Box(modifier = Modifier.fillMaxSize()) { Text("Module Loading...") }
                    }
                }
            }
        }

        // Settings Dialog Overlay
        if (showSettings) {
            SettingsDialog(
                currentKey = null, // In real app, expose key via VM if needed, but safer to keep write-only mostly
                onSave = { viewModel.saveApiKey(it) },
                onClear = { viewModel.removeApiKey() },
                onDismiss = { showSettings = false },
                isDark = isDark
            )
        }
    }
}

@Composable
fun AppHeader(isDark: Boolean, currentView: ViewMode, onToggleSidebar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0x1AFFFFFF) else Color(0xCCFFFFFF))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleSidebar) {
                Icon(Icons.Default.Menu, "Menu", tint = if (isDark) Color.White else Color.Black)
            }
            Spacer(modifier = Modifier.width(12.dp))
            
            // LOGO IN HEADER
            Image(
                painter = painterResource(id = R.drawable.my_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentView.name.replace("_", " "),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3B82F6)
            )
        }
    }
}