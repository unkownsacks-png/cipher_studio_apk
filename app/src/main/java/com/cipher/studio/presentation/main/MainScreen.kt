package com.cipher.studio.presentation.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.R
import com.cipher.studio.domain.model.ChatMessage
import com.cipher.studio.domain.model.ChatRole
import com.cipher.studio.domain.model.Session
import com.cipher.studio.domain.model.Theme
import com.cipher.studio.domain.model.ViewMode
import com.cipher.studio.presentation.about.AboutScreen
import com.cipher.studio.presentation.auth.EliteAuthScreen
import com.cipher.studio.presentation.codelab.CodeLabScreen
import com.cipher.studio.presentation.components.ChatMessageItem // IMPORTING OUR NEW COMPONENT
import com.cipher.studio.presentation.components.ControlPanel
import com.cipher.studio.presentation.components.SettingsDialog
import com.cipher.studio.presentation.cyberhouse.CyberHouseScreen
import com.cipher.studio.presentation.dataanalyst.DataAnalystScreen
import com.cipher.studio.presentation.docintel.DocIntelScreen
import com.cipher.studio.presentation.prompt.PromptStudioScreen
import com.cipher.studio.presentation.visionhub.VisionHubScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    var showSplash by remember { mutableStateOf(true) }

    // Splash Sequence
    LaunchedEffect(Unit) {
        delay(1500) // Slightly faster for snappier feel
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        val isAuthorized by viewModel.isAuthorized.collectAsState()

        if (!isAuthorized) {
            EliteAuthScreen(onLoginSuccess = { viewModel.setAuthorized(true) })
        } else {
            CipherEliteSystem(viewModel)
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Minimalist Splash
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = "Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CipherEliteSystem(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // States
    val theme by viewModel.theme.collectAsState()
    val isDark = theme == Theme.DARK
    val currentView by viewModel.currentView.collectAsState()
    val config by viewModel.config.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()

    // UI Controllers
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var isControlsOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    BackHandler(enabled = drawerState.isOpen || isControlsOpen) {
        if (isControlsOpen) isControlsOpen = false
        else scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(300.dp)
            ) {
                InternalSidebar(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    onSelectSession = { id -> 
                        val session = sessions.find { it.id == id }
                        if (session != null) viewModel.loadSession(session)
                        scope.launch { drawerState.close() }
                    },
                    onNewSession = { 
                        viewModel.createNewSession()
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = { id -> Toast.makeText(context, "Hold to delete", Toast.LENGTH_SHORT).show() },
                    onToggleSidebar = { scope.launch { drawerState.close() } },
                    currentView = currentView,
                    onViewChange = { 
                        viewModel.setViewMode(it)
                        scope.launch { drawerState.close() }
                    },
                    onLogout = { viewModel.setAuthorized(false) },
                    onOpenSettings = { 
                        showSettings = true 
                        scope.launch { drawerState.close() }
                    },
                    isDark = isDark
                )
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    
                    // 1. GEMINI STYLE HEADER (Minimal & Floating)
                    GeminiTopBar(
                        currentView = currentView,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSettingsClick = { isControlsOpen = true }
                    )

                    // 2. CONTENT AREA
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = currentView,
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                            label = "ViewTransition"
                        ) { targetView ->
                            when (targetView) {
                                ViewMode.CHAT -> ChatView(viewModel, isDark)
                                // Keep other screens as is, or refactor later
                                ViewMode.CODE_LAB -> CodeLabScreen(theme = theme, viewModel = hiltViewModel())
                                ViewMode.VISION_HUB -> VisionHubScreen(theme = theme, viewModel = hiltViewModel())
                                ViewMode.PROMPT_STUDIO -> PromptStudioScreen(theme = theme, onUsePrompt = { viewModel.updatePrompt(it); viewModel.setViewMode(ViewMode.CHAT) })
                                ViewMode.CYBER_HOUSE -> CyberHouseScreen(theme = theme, viewModel = hiltViewModel())
                                ViewMode.DATA_ANALYST -> DataAnalystScreen(theme = theme, viewModel = hiltViewModel())
                                ViewMode.DOC_INTEL -> DocIntelScreen(theme = theme, viewModel = hiltViewModel())
                                ViewMode.ABOUT -> AboutScreen(theme = theme, onBack = { viewModel.setViewMode(ViewMode.CHAT) })
                            }
                        }
                    }
                }

                // 3. RIGHT CONTROL PANEL (Overlay)
                ControlPanel(
                    config = config,
                    onChange = { /* Update Config in VM */ },
                    isOpen = isControlsOpen,
                    onClose = { isControlsOpen = false },
                    theme = theme
                )
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentKey = null,
            onSave = { viewModel.saveApiKey(it) },
            onClear = { viewModel.removeApiKey() },
            onDismiss = { showSettings = false },
            isDark = isDark
        )
    }
}

// --- GEMINI STYLE HEADER (Transparent) ---
@Composable
fun GeminiTopBar(
    currentView: ViewMode,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // No background, no shadows. Just icons floating.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Rounded.Menu, "Menu", tint = MaterialTheme.colorScheme.onBackground)
        }

        // Only show Title if not in Chat mode (Chat has its own greeting)
        if (currentView != ViewMode.CHAT) {
            Text(
                text = currentView.name.replace("_", " "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Outlined.Tune, "Config", tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

// --- THE MAIN CHAT VIEW ---
@Composable
fun ChatView(viewModel: MainViewModel, isDark: Boolean) {
    val history by viewModel.history.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val attachments by viewModel.attachments.collectAsState()

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.addAttachment(it) } }

    LaunchedEffect(history.size, isStreaming) {
        if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            // Add padding at bottom so text isn't hidden behind input bar
            contentPadding = PaddingValues(top = 0.dp, bottom = 100.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Greeting (Empty State)
            if (history.isEmpty()) {
                item { GreetingHeader() }
            }

            // Messages
            items(history) { message ->
                // USE THE NEW COMPONENT WE CREATED BEFORE
                ChatMessageItem(
                    msg = message,
                    isDark = isDark,
                    isStreaming = isStreaming && message == history.last() && message.role == ChatRole.MODEL,
                    onSpeak = { viewModel.speakText(it) },
                    onPin = { viewModel.togglePin(it) },
                    onRegenerate = { /* Call regenerate in VM */ }
                )
            }
            
            if (isStreaming) {
                item { StreamingIndicator() }
            }
        }

        // --- GEMINI INPUT BAR (Floating at Bottom) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    // Fade out effect for background behind input
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                    )
                )
        ) {
            GeminiInputBar(
                prompt = prompt,
                onPromptChange = { viewModel.updatePrompt(it) },
                onSend = { viewModel.handleRun(); focusManager.clearFocus() },
                onAttach = { galleryLauncher.launch("image/*") },
                isStreaming = isStreaming,
                attachmentCount = attachments.size
            )
        }
    }
}

@Composable
fun GreetingHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        // Gradient Text Effect
        Text(
            text = "Hello, Creator",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) // Or use a ShaderBrush for gradient
        )
        Text(
            text = "How can I help you today?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

// --- NEW GEMINI INPUT BAR ---
@Composable
fun GeminiInputBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    isStreaming: Boolean,
    attachmentCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 12.dp)
    ) {
        // Attachment Indicator
        if (attachmentCount > 0) {
            Row(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Image, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(4.dp))
                Text("$attachmentCount attached", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        // The Capsule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(28.dp)) // Full Rounded Capsule
                .background(MaterialTheme.colorScheme.surfaceVariant) // Greyish background
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attach Button
            IconButton(
                onClick = onAttach,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape) // Contrast circle
            ) {
                Icon(Icons.Rounded.Add, "Attach", tint = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Input Field (Transparent)
            Box(modifier = Modifier.weight(1f)) {
                if (prompt.isEmpty()) {
                    Text(
                        "Message Cipher...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                BasicTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp
                    ),
                    maxLines = 4,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            val isEnabled = prompt.isNotBlank() || isStreaming
            IconButton(
                onClick = onSend,
                enabled = isEnabled,
                modifier = Modifier.size(28.dp)
            ) {
                if (isStreaming) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Rounded.ArrowUpward,
                        "Send",
                        tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun StreamingIndicator() {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 16.dp)) {
        // Minimal pulsating dot or text
        Text(
            "Cipher is thinking...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --- CLEAN SIDEBAR (Minimalist) ---
@Composable
fun InternalSidebar(
    sessions: List<Session>,
    currentSessionId: String?,
    onSelectSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onToggleSidebar: () -> Unit,
    currentView: ViewMode,
    onViewChange: (ViewMode) -> Unit,
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Simple Text Header
        Text(
            "Cipher Studio",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)
        )

        // New Chat Button (Prominent)
        Button(
            onClick = onNewSession,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(Icons.Rounded.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Chat")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Recent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // Session List
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sessions) { session ->
                val isSelected = session.id == currentSessionId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .clickable { onSelectSession(session.id) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Footer Settings
        Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        
        Row(modifier = Modifier.clickable { onOpenSettings() }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Settings", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}