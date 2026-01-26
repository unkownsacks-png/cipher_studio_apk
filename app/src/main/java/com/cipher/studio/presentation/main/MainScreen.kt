package com.cipher.studio.presentation.main

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable 
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.cipher.studio.domain.model.ChatRole
import com.cipher.studio.domain.model.Session
import com.cipher.studio.domain.model.Theme
import com.cipher.studio.domain.model.ViewMode
import com.cipher.studio.presentation.about.AboutScreen
import com.cipher.studio.presentation.auth.EliteAuthScreen
import com.cipher.studio.presentation.codelab.CodeLabScreen
import com.cipher.studio.presentation.components.ChatMessageItem
import com.cipher.studio.presentation.components.ControlPanel
import com.cipher.studio.presentation.components.SettingsDialog
import com.cipher.studio.presentation.cyberhouse.CyberHouseScreen
import com.cipher.studio.presentation.dataanalyst.DataAnalystScreen
import com.cipher.studio.presentation.docintel.DocIntelScreen
import com.cipher.studio.presentation.prompt.PromptStudioScreen
import com.cipher.studio.presentation.visionhub.VisionHubScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1500)
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
        Image(
            painter = painterResource(id = R.drawable.my_logo),
            contentDescription = "Logo",
            modifier = Modifier.size(80.dp)
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CipherEliteSystem(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State Collectors
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

    // Feature: Real Delete Confirmation State
    var sessionToDelete by remember { mutableStateOf<String?>(null) }

    // Back Handler
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
                modifier = Modifier.width(320.dp).windowInsetsPadding(WindowInsets.safeDrawing)
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
                    onRequestDelete = { id -> sessionToDelete = id },
                    onToggleSidebar = { scope.launch { drawerState.close() } },
                    currentView = currentView,
                    onViewChange = { 
                        viewModel.setViewMode(it)
                        scope.launch { drawerState.close() }
                    },
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
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // 1. HEADER
                    GeminiTopBar(
                        currentView = currentView,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSettingsClick = { isControlsOpen = true }
                    )

                    // 2. CONTENT AREA (All Modules)
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = currentView,
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
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
                    }
                }

                // 3. RIGHT CONTROL PANEL
                ControlPanel(
                    config = config,
                    onChange = { viewModel.updateConfig(it) }, 
                    isOpen = isControlsOpen,
                    onClose = { isControlsOpen = false },
                    theme = theme
                )
            }
        }
    }

    // DELETE DIALOG
    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Chat?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove this conversation from your history.") },
            confirmButton = {
                Button(
                    onClick = {
                        sessionToDelete?.let { id ->
                            viewModel.deleteSession(id) 
                            Toast.makeText(context, "Conversation deleted", Toast.LENGTH_SHORT).show()
                        }
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") }
            }
        )
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

@Composable
fun GeminiTopBar(
    currentView: ViewMode,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
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
// --- CHAT VIEW (Main Conversation Area) ---
@Composable
fun ChatView(viewModel: MainViewModel, isDark: Boolean) {
    val history by viewModel.history.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val isVoiceActive by viewModel.isVoiceActive.collectAsState()
    val isExpanded by viewModel.isInputExpanded.collectAsState()

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Image Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.addAttachment(it) } }

    // Voice Permission Launcher
    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.toggleVoiceInput() 
        else Toast.makeText(context, "Microphone permission required for voice input", Toast.LENGTH_SHORT).show()
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(history.size, isStreaming) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Chat History List (Hidden when editor is fullscreen)
        if (!isExpanded) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 0.dp, bottom = 120.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (history.isEmpty()) {
                    item { GreetingHeader() }
                }

                items(history) { message ->
                    ChatMessageItem(
                        msg = message,
                        isDark = isDark,
                        isStreaming = isStreaming && message == history.last() && message.role == ChatRole.MODEL,
                        onSpeak = { viewModel.speakText(it) },
                        onPin = { viewModel.togglePin(it) },
                        onRegenerate = { viewModel.handleRun(message.text) },
                        onEdit = { viewModel.updatePrompt(it) }
                    )
                }

                if (isStreaming) {
                    item { StreamingIndicator() }
                }
            }
        }

        // --- SMART DYNAMIC INPUT BAR ---
        Box(
            modifier = Modifier
                .align(if (isExpanded) Alignment.TopCenter else Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(if (isExpanded) 1f else Float.NaN)
                .background(if (isExpanded) MaterialTheme.colorScheme.background else Color.Transparent)
                .imePadding() 
        ) {
            // Shadow Scrim for bottom bar mode
            if (!isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                            )
                        )
                )
            }

            GeminiSmartInputBar(
                prompt = prompt,
                onPromptChange = { viewModel.updatePrompt(it) },
                onSend = { 
                    viewModel.handleRun()
                    focusManager.clearFocus()
                    viewModel.setInputExpanded(false)
                },
                onAttach = { galleryLauncher.launch("image/*") },
                isStreaming = isStreaming,
                attachmentCount = attachments.size,
                isExpanded = isExpanded,
                onExpandToggle = { viewModel.toggleFullscreenInput() },
                isVoiceActive = isVoiceActive,
                onMicClick = { voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
        }
    }
}

// --- THE SMART INPUT COMPONENT ---
@Composable
fun GeminiSmartInputBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    isStreaming: Boolean,
    attachmentCount: Int,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    isVoiceActive: Boolean,
    onMicClick: () -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isExpanded) 0.6f else 0.95f)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    
    // Voice Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "VoicePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "Pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isExpanded) 0.dp else 8.dp)
            .animateContentSize(animationSpec = tween(300))
    ) {
        // Attachment Indicator
        if (attachmentCount > 0 && !isExpanded) {
            Row(
                modifier = Modifier
                    .padding(bottom = 8.dp, start = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text("$attachmentCount Media Ready", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        // Main Editor Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (isExpanded) 1f else Float.NaN)
                .shadow(if (isExpanded) 0.dp else 16.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clip(RoundedCornerShape(topStart = if (isExpanded) 0.dp else 28.dp, topEnd = if (isExpanded) 0.dp else 28.dp))
                .background(bgColor)
                .border(if (isExpanded) 0.dp else 1.dp, borderColor, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        ) {
            // Fullscreen Header
            if (isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onExpandToggle) {
                        Icon(Icons.Rounded.Close, "Close Editor")
                    }
                    Text("Cipher Code Editor", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Button(onClick = onSend, enabled = prompt.isNotBlank()) {
                        Text("Run Code")
                    }
                }
                Divider(color = borderColor)
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = if (isExpanded) Alignment.Top else Alignment.Bottom
            ) {
                if (!isExpanded) {
                    // Actions: Attach & Mic
                    IconButton(
                        onClick = onAttach,
                        modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.background, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Add, "Attach", modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (isVoiceActive) MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha) 
                                else MaterialTheme.colorScheme.background, 
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Rounded.Mic, "Voice", 
                            tint = if (isVoiceActive) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Field Wrapper
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .heightIn(max = if (isExpanded) Int.MAX_VALUE.dp else 180.dp)
                ) {
                    if (prompt.isEmpty()) {
                        Text(
                            text = if (isVoiceActive) "Listening closely..." else "Ask Cipher anything...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    BasicTextField(
                        value = prompt,
                        onValueChange = onPromptChange,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            lineHeight = 26.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().ifTrue(isExpanded) { fillMaxHeight() }
                    )
                }

                if (!isExpanded) {
                    Spacer(modifier = Modifier.width(8.dp))
                    // Actions: Fullscreen & Send
                    IconButton(onClick = onExpandToggle) {
                        Icon(Icons.Rounded.Fullscreen, "Expand", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    val isSendEnabled = prompt.isNotBlank() || isStreaming
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable(enabled = isSendEnabled) { onSend() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isStreaming) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.ArrowUpward, "Send", tint = if (isSendEnabled) Color.White else Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
// --- HELPER EXTENSION: Conditional Modifier ---
fun Modifier.ifTrue(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

// --- GREETING HEADER (Smart Greeting) ---
@Composable
fun GreetingHeader() {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 60.dp)
    ) {
        // App Identity Branding
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.my_logo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "$greeting, Creator",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your digital empire is ready. What shall we architect today?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            lineHeight = 24.sp
        )
    }
}

// --- STREAMING INDICATOR ---
@Composable
fun StreamingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 8.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(12.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "Cipher is processing your request...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}

// --- INTERNAL SIDEBAR: THE NAVIGATION ENGINE ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InternalSidebar(
    sessions: List<Session>,
    currentSessionId: String?,
    onSelectSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onRequestDelete: (String) -> Unit,
    onToggleSidebar: () -> Unit,
    currentView: ViewMode,
    onViewChange: (ViewMode) -> Unit,
    onOpenSettings: () -> Unit,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // 1. Sidebar Top Identity
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.padding(bottom = 28.dp, start = 8.dp)
        ) {
             Image(
                 painter = painterResource(id = R.drawable.my_logo),
                 contentDescription = "Logo",
                 modifier = Modifier.size(36.dp)
             )
             Spacer(modifier = Modifier.width(14.dp))
             Text(
                 "Cipher Studio", 
                 style = MaterialTheme.typography.titleLarge, 
                 fontWeight = FontWeight.ExtraBold,
                 letterSpacing = 0.5.sp
             )
        }

        // 2. Core Action: New Session
        Surface(
            onClick = onNewSession,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            tonalElevation = 4.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(Icons.Rounded.Add, "New", modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Start New Conversation", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. ECOSYSTEM MODULES (Scrollable if needed)
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                "ECOSYSTEM", 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.primary, 
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
            )

            SidebarItem(Icons.Default.ChatBubbleOutline, "Cipher Chat", currentView == ViewMode.CHAT) { onViewChange(ViewMode.CHAT) }
            SidebarItem(Icons.Default.Code, "Code Lab", currentView == ViewMode.CODE_LAB) { onViewChange(ViewMode.CODE_LAB) }
            SidebarItem(Icons.Default.Visibility, "Vision Hub", currentView == ViewMode.VISION_HUB) { onViewChange(ViewMode.VISION_HUB) }
            SidebarItem(Icons.Default.Lightbulb, "Prompt Studio", currentView == ViewMode.PROMPT_STUDIO) { onViewChange(ViewMode.PROMPT_STUDIO) }
            SidebarItem(Icons.Default.Security, "Cyber House", currentView == ViewMode.CYBER_HOUSE) { onViewChange(ViewMode.CYBER_HOUSE) }
            SidebarItem(Icons.Default.Analytics, "Data Analyst", currentView == ViewMode.DATA_ANALYST) { onViewChange(ViewMode.DATA_ANALYST) }
            SidebarItem(Icons.Default.Description, "Doc Intel", currentView == ViewMode.DOC_INTEL) { onViewChange(ViewMode.DOC_INTEL) }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(20.dp))

        // 4. CHAT HISTORY (Scrollable)
        Text(
            "RECENT SESSIONS", 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sessions) { session ->
                val isSelected = session.id == currentSessionId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .combinedClickable(
                            onClick = { onSelectSession(session.id) },
                            onLongClick = { onRequestDelete(session.id) }
                        )
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Chat, 
                        null, 
                        modifier = Modifier.size(18.dp), 
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = session.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 5. FOOTER: Utilities
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        // About
        Surface(
            onClick = { onViewChange(ViewMode.ABOUT) },
            shape = RoundedCornerShape(12.dp),
            color = if(currentView == ViewMode.ABOUT) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("About Cipher", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Settings
        Surface(
            onClick = { onOpenSettings() },
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text("System Settings", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// --- REUSABLE SIDEBAR ITEM COMPONENT ---
@Composable
fun SidebarItem(
    icon: ImageVector, 
    label: String, 
    isSelected: Boolean, 
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = label, 
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label, 
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 15.sp
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
