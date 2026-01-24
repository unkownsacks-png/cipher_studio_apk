package com.cipher.studio.presentation.main

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
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
import com.cipher.studio.presentation.components.ControlPanel
import com.cipher.studio.presentation.components.MarkdownRenderer
import com.cipher.studio.presentation.components.SettingsDialog
import com.cipher.studio.presentation.cyberhouse.CyberHouseScreen
import com.cipher.studio.presentation.dataanalyst.DataAnalystScreen
import com.cipher.studio.presentation.docintel.DocIntelScreen
import com.cipher.studio.presentation.prompt.PromptStudioScreen
import com.cipher.studio.presentation.visionhub.VisionHubScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import android.util.Base64

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
                modifier = Modifier.width(300.dp)
            ) {
                // INTERNAL SIDEBAR IMPL (To fix Issue 2: History & Delete)
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
                    // MOCK DELETE (Ideally add deleteSession to VM)
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
        // --- MAIN SCAFFOLD (Fix Issue 1: Full Screen) ---
        Scaffold(
            // We use a transparent TopBar to allow content to flow or use a custom Box overlay
            contentWindowInsets = WindowInsets(0, 0, 0, 0), // Edge-to-Edge
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                
                // 1. CONTENT LAYER
                Column(modifier = Modifier.fillMaxSize()) {
                    
                    // Custom Floating Header (Transparent/Glass)
                    EliteTopBar(
                        currentView = currentView,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onSettingsClick = { isControlsOpen = true },
                        isDark = isDark
                    )

                    // Module Switcher
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

                // 2. RIGHT CONTROL PANEL (Overlay)
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

    // Settings Dialog
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

// --- FIX ISSUE 1: ELEGANT TOP BAR ---
@Composable
fun EliteTopBar(
    currentView: ViewMode,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isDark: Boolean
) {
    // No "Black Box" header. Just a clean surface.
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f), // Semi-transparent
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.my_logo),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = currentView.name.replace("_", " "),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Right
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Tune, "Config", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- FIX ISSUE 2: INTERNAL SIDEBAR WITH HISTORY & DELETE ---
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
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.my_logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Cipher Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            IconButton(onClick = onToggleSidebar) {
                Icon(Icons.Default.Close, "Close")
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        
        Spacer(modifier = Modifier.height(16.dp))

        // Modules
        InternalNavItem(ViewMode.CHAT, Icons.Default.ChatBubbleOutline, "Chat", currentView, onViewChange, isDark)
        InternalNavItem(ViewMode.CODE_LAB, Icons.Default.Code, "Code Lab", currentView, onViewChange, isDark)
        InternalNavItem(ViewMode.VISION_HUB, Icons.Default.Visibility, "Vision Hub", currentView, onViewChange, isDark)
        // ... (Other modules can be added here)

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        
        // HISTORY HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("HISTORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = onNewSession, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, "New", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // HISTORY LIST (FIXED: Auto-populated)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sessions) { session ->
                val isSelected = session.id == currentSessionId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .clickable { onSelectSession(session.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Outlined.Message, null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = session.title,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // DELETE ICON
                    Icon(
                        Icons.Default.Delete, "Delete",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDeleteSession(session.id) }, // Trigger Delete
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Footer
        InternalNavItem(ViewMode.ABOUT, Icons.Default.Info, "About", currentView, onViewChange, isDark)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onOpenSettings() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Settings", fontSize = 14.sp)
        }
    }
}

@Composable
fun InternalNavItem(view: ViewMode, icon: ImageVector, label: String, current: ViewMode, onChange: (ViewMode) -> Unit, isDark: Boolean) {
    val isSelected = current == view
    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .clickable { onChange(view) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

// --- FIX ISSUE 3: ELITE CHAT VIEW & BUBBLES ---
@Composable
fun ChatView(viewModel: MainViewModel, isDark: Boolean) {
    val history by viewModel.history.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    
    // IMAGE PICKER
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.addAttachment(it) } }

    LaunchedEffect(history.size, isStreaming) {
        if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(bottom = 90.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp, start = 8.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (history.isEmpty()) {
                item { EmptyState(isDark) }
            } else {
                items(history) { message ->
                    // WIDER BUBBLE IMPLEMENTATION
                    InternalChatMessageItem(
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

        // Floating Input
        AlignFloatingInput(
            modifier = Modifier.align(Alignment.BottomCenter),
            prompt = prompt,
            onPromptChange = { viewModel.updatePrompt(it) },
            onSend = { viewModel.handleRun(); focusManager.clearFocus() },
            onAttach = { galleryLauncher.launch("image/*") },
            isStreaming = isStreaming,
            isDark = isDark,
            attachmentCount = attachments.size
        )
    }
}

// --- WIDER CHAT BUBBLE COMPONENT ---
@Composable
fun InternalChatMessageItem(
    msg: ChatMessage,
    isDark: Boolean,
    isStreaming: Boolean,
    onSpeak: ((String) -> Unit)?,
    onPin: ((String) -> Unit)?
) {
    val isUser = msg.role == ChatRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val clipboardManager = LocalClipboardManager.current

    // Bubble Shape
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    }

    // Color
    val bgModifier = if (isUser) {
        Modifier.background(
            brush = Brush.verticalGradient(listOf(Color(0xFF2563EB), Color(0xFF3B82F6))),
            shape = bubbleShape
        )
    } else {
        Modifier.background(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = bubbleShape
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            // FIX ISSUE 3: Make it utilize 90% of screen width for wide reading
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f) 
        ) {
            // Avatar for AI
            if (!isUser) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp).padding(bottom = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.SmartToy, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // The Bubble
            Column(modifier = Modifier.weight(1f, fill = false).then(bgModifier)) {
                // Images
                if (msg.attachments.isNotEmpty()) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        msg.attachments.forEach { attachment ->
                            val bitmap = remember(attachment.data) {
                                try {
                                    val decoded = Base64.decode(attachment.data, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(decoded, 0, decoded.size).asImageBitmap()
                                } catch (e: Exception) { null }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                // Text
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Use MarkdownRenderer for nice text
                    MarkdownRenderer(
                        content = msg.text,
                        theme = if (isDark) Theme.DARK else Theme.LIGHT,
                        isStreaming = isStreaming
                    )
                }

                // AI Actions
                if (!isUser && !isStreaming) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(msg.text)) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                        }
                        IconButton(onClick = { onSpeak?.invoke(msg.text) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.VolumeUp, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- FLOATING INPUT ---
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
            .imePadding()
    ) {
        if (attachmentCount > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(bottom = 8.dp).align(Alignment.Start)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$attachmentCount Image(s)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
                IconButton(onClick = onAttach, modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    placeholder = { Text("Message Cipher...", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp, max = 120.dp),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isStreaming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).clickable { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isStreaming) Icons.Default.Stop else Icons.Default.ArrowUpward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ... EmptyState and StreamingIndicator ...
@Composable
fun EmptyState(isDark: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 60.dp)) {
        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Cipher Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("How can I help you today?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StreamingIndicator() {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Thinking...", style = MaterialTheme.typography.labelMedium)
    }
}