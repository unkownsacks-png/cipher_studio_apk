package com.cipher.studio.presentation.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalFocusManager
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
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    var showSplash by remember { mutableStateOf(true) }

    // Professional Splash Sequence
    LaunchedEffect(Unit) {
        delay(2000) 
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        val isAuthorized by viewModel.isAuthorized.collectAsState()

        if (!isAuthorized) {
            EliteAuthScreen(onLoginSuccess = { viewModel.setAuthorized(true) })
        } else {
            // The New Elite System UI
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.my_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                modifier = Modifier.width(120.dp).clip(CircleShape)
            )
        }
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

    // Back Handler for Drawer
    BackHandler(enabled = drawerState.isOpen || isControlsOpen) {
        if (isControlsOpen) isControlsOpen = false
        else scope.launch { drawerState.close() }
    }

    // --- MODERN DRAWER LAYOUT ---
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(300.dp) // Standard Mobile Drawer Width
            ) {
                // Reusing Sidebar Logic but adapting for Drawer
                Sidebar(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    onSelectSession = { id -> 
                        sessions.find { it.id == id }?.let { viewModel.loadSession(it) }
                        scope.launch { drawerState.close() }
                    },
                    onNewSession = { 
                        viewModel.createNewSession()
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = { /* Delete logic */ },
                    isOpen = true, // Drawer is always "open" when visible
                    onToggleSidebar = { scope.launch { drawerState.close() } },
                    theme = theme,
                    currentView = currentView,
                    onViewChange = { 
                        viewModel.setViewMode(it)
                        scope.launch { drawerState.close() }
                    },
                    onLogout = { viewModel.setAuthorized(false) },
                    onOpenSettings = { 
                        showSettings = true 
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        // --- MAIN SCAFFOLD ---
        Scaffold(
            topBar = {
                // Collapsing-style simple header
                EliteTopBar(
                    currentView = currentView,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSettingsClick = { isControlsOpen = true },
                    isDark = isDark
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                
                // Animated View Transition
                AnimatedContent(
                    targetState = currentView,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "ViewTransition"
                ) { targetView ->
                    when (targetView) {
                        ViewMode.CHAT -> ChatView(viewModel, isDark)
                        ViewMode.CODE_LAB -> CodeLabScreen(theme = theme, viewModel = hiltViewModel())
                        ViewMode.VISION_HUB -> VisionHubScreen(theme = theme, viewModel = hiltViewModel())
                        ViewMode.PROMPT_STUDIO -> PromptStudioScreen(theme = theme, onUsePrompt = { viewModel.updatePrompt(it); viewModel.setViewMode(ViewMode.CHAT) })
                        ViewMode.CYBER_HOUSE -> CyberHouseScreen(theme = theme, viewModel = hiltViewModel())
                        ViewMode.DATA_ANALYST -> DataAnalystScreen(theme = theme, viewModel = hiltViewModel())
                        ViewMode.DOC_INTEL -> DocIntelScreen(theme = theme, viewModel = hiltViewModel())
                        ViewMode.ABOUT -> AboutScreen(theme = theme, onBack = { viewModel.setViewMode(ViewMode.CHAT) })
                    }
                }

                // Control Panel Overlay (Right Side)
                ControlPanel(
                    config = config,
                    onChange = { /* update config */ },
                    isOpen = isControlsOpen,
                    onClose = { isControlsOpen = false },
                    theme = theme
                )
            }
        }
    }

    // Dialogs
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

// --- REFINED HEADER ---
@Composable
fun EliteTopBar(
    currentView: ViewMode,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isDark: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        shadowElevation = 0.dp, // Flat modern look
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Menu & Brand
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                
                Image(
                    painter = painterResource(id = R.drawable.my_logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = currentView.name.replace("_", " "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Right: Controls
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Tune, "Config", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- ELITE CHAT VIEW (Floating Input) ---
@Composable
fun ChatView(viewModel: MainViewModel, isDark: Boolean) {
    val history by viewModel.history.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    
    // IMAGE PICKER LOGIC
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addAttachment(it) }
    }

    // Auto Scroll
    LaunchedEffect(history.size, isStreaming) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Chat List (Background)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp), // Space for floating input
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (history.isEmpty()) {
                item { EmptyState(isDark) }
            } else {
                items(history) { message ->
                    ChatMessageItem(
                        msg = message,
                        isDark = isDark,
                        isStreaming = isStreaming && message == history.last() && message.role == ChatRole.MODEL,
                        onSpeak = { viewModel.speakText(it) },
                        onPin = { viewModel.togglePin(it) }
                    )
                }
            }
            if (isStreaming) {
                item { StreamingIndicator() }
            }
        }

        // 2. Floating Input Bar (Foreground)
        AlignFloatingInput(
            modifier = Modifier.align(Alignment.BottomCenter),
            prompt = prompt,
            onPromptChange = { viewModel.updatePrompt(it) },
            onSend = { 
                viewModel.handleRun()
                focusManager.clearFocus() 
            },
            onAttach = { galleryLauncher.launch("image/*") }, // Trigger Gallery
            isStreaming = isStreaming,
            isDark = isDark,
            attachmentCount = attachments.size
        )
    }
}

@Composable
fun AlignFloatingInput(
    modifier: Modifier,
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    isStreaming: Boolean,
    isDark: Boolean,
    attachmentCount: Int
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .imePadding() // Move up with keyboard
    ) {
        // Show Attachment Indicator if images are selected
        if (attachmentCount > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$attachmentCount Image(s) Attached", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Attachment Button
                IconButton(
                    onClick = onAttach,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Attach",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text Input
                TextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    placeholder = { Text("Message Cipher...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 120.dp), // Auto-grow
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send Button (Floating Gradient)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isStreaming) MaterialTheme.colorScheme.error 
                            else MaterialTheme.colorScheme.primary
                        )
                        .clickable { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isStreaming) Icons.Default.Stop else Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ... EmptyState and StreamingIndicator reuse ...
@Composable
fun EmptyState(isDark: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp)
    ) {
        // Using Icon instead of Image to prevent crash if logo missing, 
        // replace with Image(painter...) if logo exists
        Icon(
            imageVector = Icons.Default.AutoAwesome, 
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Cipher Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("How can I help you today?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StreamingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Thinking...", style = MaterialTheme.typography.labelMedium)
    }
}