package com.cipher.studio.presentation.main

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1500); showSplash = false }

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
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Image(painter = painterResource(id = R.drawable.my_logo), contentDescription = "Logo", modifier = Modifier.size(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CipherEliteSystem(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme by viewModel.theme.collectAsState()
    val isDark = theme == Theme.DARK
    val currentView by viewModel.currentView.collectAsState()
    val config by viewModel.config.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var isControlsOpen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<String?>(null) }

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
                    sessions = sessions, currentSessionId = currentSessionId,
                    onSelectSession = { id -> sessions.find { it.id == id }?.let { viewModel.loadSession(it) }; scope.launch { drawerState.close() } },
                    onNewSession = { viewModel.createNewSession(); scope.launch { drawerState.close() } },
                    onRequestDelete = { sessionToDelete = it },
                    onToggleSidebar = { scope.launch { drawerState.close() } },
                    currentView = currentView,
                    onViewChange = { viewModel.setViewMode(it); scope.launch { drawerState.close() } },
                    onOpenSettings = { showSettings = true; scope.launch { drawerState.close() } },
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
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).windowInsetsPadding(WindowInsets.safeDrawing)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    GeminiTopBar(currentView, { scope.launch { drawerState.open() } }, { isControlsOpen = true })
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(targetState = currentView, label = "ViewTransition") { targetView ->
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
                ControlPanel(config, { viewModel.updateConfig(it) }, isControlsOpen, { isControlsOpen = false }, theme)
            }
        }
    }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Chat?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove this conversation.") },
            confirmButton = {
                Button(onClick = { sessionToDelete?.let { viewModel.deleteSession(it) }; sessionToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") } }
        )
    }
    if (showSettings) SettingsDialog(null, { viewModel.saveApiKey(it) }, { viewModel.removeApiKey() }, { showSettings = false }, isDark)
}

@Composable
fun GeminiTopBar(currentView: ViewMode, onMenuClick: () -> Unit, onSettingsClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onMenuClick) { Icon(Icons.Rounded.Menu, "Menu", tint = MaterialTheme.colorScheme.onBackground) }
        if (currentView != ViewMode.CHAT) {
            Text(text = currentView.name.replace("_", " "), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }
        IconButton(onClick = onSettingsClick) { Icon(Icons.Outlined.Tune, "Config", tint = MaterialTheme.colorScheme.onBackground) }
    }
}

// --- UPDATED CHAT VIEW (WITH ALL ENHANCEMENTS) ---
@Composable
fun ChatView(viewModel: MainViewModel, isDark: Boolean) {
    val history by viewModel.history.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val attachments by viewModel.attachments.collectAsState()

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Launcher for images
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { viewModel.addAttachment(it) } }

    // NEW STATES: Local control to prevent ViewModel-sync crashes
    var isExpanded by remember { mutableStateOf(false) }
    var isVoiceActive by remember { mutableStateOf(false) }

    val voicePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) { isVoiceActive = !isVoiceActive } else { Toast.makeText(context, "Mic access required", Toast.LENGTH_SHORT).show() }
    }

    LaunchedEffect(history.size, isStreaming) { if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isExpanded) {
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 120.dp), modifier = Modifier.fillMaxSize()) {
                if (history.isEmpty()) item { GreetingHeader() }
                items(history) { msg -> ChatMessageItem(msg, isDark, isStreaming && msg == history.last() && msg.role == ChatRole.MODEL, { viewModel.speakText(it) }, { viewModel.togglePin(it) }, {}, { viewModel.updatePrompt(it) }) }
                if (isStreaming) item { StreamingIndicator() }
            }
        }

        // --- SMART DYNAMIC INPUT AREA ---
        Box(
            modifier = Modifier
                .align(if (isExpanded) Alignment.TopCenter else Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(if (isExpanded) 1f else Float.NaN)
                .background(if (isExpanded) MaterialTheme.colorScheme.background else Color.Transparent)
                .imePadding() 
        ) {
            GeminiSmartInputBar(
                prompt = prompt,
                onPromptChange = { viewModel.updatePrompt(it) },
                onSend = { viewModel.handleRun(); focusManager.clearFocus(); isExpanded = false },
                onAttach = { galleryLauncher.launch("image/*") },
                isStreaming = isStreaming,
                attachmentCount = attachments.size,
                isExpanded = isExpanded,
                onExpandToggle = { isExpanded = !isExpanded },
                isVoiceActive = isVoiceActive,
                onMicClick = { voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
        }
    }
}

@Composable
fun GeminiSmartInputBar(
    prompt: String, onPromptChange: (String) -> Unit, onSend: () -> Unit,
    onAttach: () -> Unit, isStreaming: Boolean, attachmentCount: Int,
    isExpanded: Boolean, onExpandToggle: () -> Unit, isVoiceActive: Boolean, onMicClick: () -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isExpanded) 0.5f else 0.95f)
    val pulseAlpha by rememberInfiniteTransition().animateFloat(0.4f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isExpanded) 0.dp else 4.dp) // Edge-to-Edge look
            .animateContentSize()
    ) {
        if (attachmentCount > 0 && !isExpanded) {
            Row(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Icon(Icons.Default.Image, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(6.dp))
                Text("$attachmentCount Attached", style = MaterialTheme.typography.labelSmall)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (isExpanded) 1f else Float.NaN)
                .shadow(if (isExpanded) 0.dp else 8.dp, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clip(RoundedCornerShape(topStart = if (isExpanded) 0.dp else 28.dp, topEnd = if (isExpanded) 0.dp else 28.dp))
                .background(bgColor)
        ) {
            if (isExpanded) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onExpandToggle) { Icon(Icons.Rounded.Close, "Close") }
                    Button(onClick = onSend, enabled = prompt.isNotBlank()) { Text("Send") }
                }
                Divider(alpha = 0.1f)
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
                if (!isExpanded) {
                    IconButton(onClick = onAttach, modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.background, CircleShape)) {
                        Icon(Icons.Rounded.Add, "Attach", modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onMicClick, modifier = Modifier.size(40.dp).background(if (isVoiceActive) MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha) else MaterialTheme.colorScheme.background, CircleShape)) {
                        Icon(Icons.Rounded.Mic, "Voice", tint = if (isVoiceActive) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Box(modifier = Modifier.weight(1f).padding(vertical = 8.dp).heightIn(max = if (isExpanded) Int.MAX_VALUE.dp else 150.dp)) {
                    if (prompt.isEmpty()) {
                        Text(if (isVoiceActive) "Listening..." else "Message Cipher...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    BasicTextField(
                        value = prompt, onValueChange = onPromptChange,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().ifTrue(isExpanded) { fillMaxHeight() }
                    )
                }

                if (!isExpanded) {
                    IconButton(onClick = onExpandToggle) { Icon(Icons.Rounded.Fullscreen, "Full Screen", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    
                    // FAB STYLE SEND BUTTON
                    val isSendEnabled = prompt.isNotBlank() || isStreaming
                    Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh).clickable(enabled = isSendEnabled) { onSend() }, contentAlignment = Alignment.Center) {
                        if (isStreaming) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        else Icon(Icons.Rounded.ArrowUpward, "Send", tint = if (isSendEnabled) Color.White else Color.Gray)
                    }
                }
            }
        }
    }
}

fun Modifier.ifTrue(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier = if (condition) then(modifier(Modifier)) else this

@Composable
fun GreetingHeader() {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) { in 5..11 -> "Good Morning" in 12..17 -> "Good Afternoon" else -> "Good Evening" }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 60.dp)) {
        Image(painter = painterResource(id = R.drawable.my_logo), contentDescription = null, modifier = Modifier.size(40.dp).padding(bottom = 16.dp))
        Text(text = "$greeting, Creator", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = "Ready to build something extraordinary?", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun StreamingIndicator() {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 16.dp)) {
        Text("Cipher is thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- SIDEBAR ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InternalSidebar(sessions: List<Session>, currentSessionId: String?, onSelectSession: (String) -> Unit, onNewSession: () -> Unit, onRequestDelete: (String) -> Unit, onToggleSidebar: () -> Unit, currentView: ViewMode, onViewChange: (ViewMode) -> Unit, onOpenSettings: () -> Unit, isDark: Boolean) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
             Image(painter = painterResource(id = R.drawable.my_logo), contentDescription = "Logo", modifier = Modifier.size(32.dp))
             Spacer(modifier = Modifier.width(12.dp))
             Text("Cipher Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Button(onClick = onNewSession, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
            Icon(Icons.Rounded.Add, null); Spacer(modifier = Modifier.width(8.dp)); Text("New Chat")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("APPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        SidebarItem(Icons.Default.ChatBubbleOutline, "Chat", currentView == ViewMode.CHAT) { onViewChange(ViewMode.CHAT) }
        SidebarItem(Icons.Default.Code, "Code Lab", currentView == ViewMode.CODE_LAB) { onViewChange(ViewMode.CODE_LAB) }
        SidebarItem(Icons.Default.Visibility, "Vision Hub", currentView == ViewMode.VISION_HUB) { onViewChange(ViewMode.VISION_HUB) }
        SidebarItem(Icons.Default.Lightbulb, "Prompt Studio", currentView == ViewMode.PROMPT_STUDIO) { onViewChange(ViewMode.PROMPT_STUDIO) }
        SidebarItem(Icons.Default.Security, "Cyber House", currentView == ViewMode.CYBER_HOUSE) { onViewChange(ViewMode.CYBER_HOUSE) }
        SidebarItem(Icons.Default.Analytics, "Data Analyst", currentView == ViewMode.DATA_ANALYST) { onViewChange(ViewMode.DATA_ANALYST) }
        SidebarItem(Icons.Default.Description, "Doc Intel", currentView == ViewMode.DOC_INTEL) { onViewChange(ViewMode.DOC_INTEL) }
        
        Spacer(modifier = Modifier.height(16.dp))
        Divider(alpha = 0.1f)
        Spacer(modifier = Modifier.height(16.dp))

        Text("RECENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sessions) { session ->
                val isSelected = session.id == currentSessionId
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent).combinedClickable(onClick = { onSelectSession(session.id) }, onLongClick = { onRequestDelete(session.id) }).padding(12.dp)) {
                    Icon(Icons.Outlined.Message, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(session.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Divider(modifier = Modifier.padding(vertical = 12.dp), alpha = 0.1f)
        Row(modifier = Modifier.clickable { onViewChange(ViewMode.ABOUT) }.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(12.dp)); Text("About")
        }
        Row(modifier = Modifier.clickable { onOpenSettings() }.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(12.dp)); Text("Settings")
        }
    }
}

@Composable
fun SidebarItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(8.dp)).background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent).clickable { onClick() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp)); Text(label, fontSize = 14.sp)
    }
}
