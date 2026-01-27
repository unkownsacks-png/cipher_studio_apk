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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Stop
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
import kotlin.random.Random

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    var showSplash by remember { mutableStateOf(true) }

    // Professional Splash Sequence
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
    val currentModelName by viewModel.currentModelName.collectAsState()

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

                    // 1. HEADER (With Model Badge)
                    GeminiTopBar(
                        currentView = currentView,
                        modelName = currentModelName,
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

    // DIALOGS
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
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
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

// --- HEADER (Updated with Model Badge) ---
@Composable
fun GeminiTopBar(
    currentView: ViewMode,
    modelName: String,
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

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (currentView == ViewMode.CHAT) modelName else currentView.name.replace("_", " "),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Outlined.Tune, "Config", tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

// --- CHAT VIEW (FIXED: Smart Auto-Scroll with Content Key) ---
@Composable
fun ChatView(viewModel: MainViewModel, isDark: Boolean) {
    val history by viewModel.history.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val isExpanded by viewModel.isInputExpanded.collectAsState()
    val isVoiceActive by viewModel.isVoiceActive.collectAsState()

    val suggestions = viewModel.suggestionChips

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.addAttachment(it) } }

    val voicePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.toggleVoiceInput() else Toast.makeText(context, "Permission needed for Voice", Toast.LENGTH_SHORT).show()
    }

    // FIXED: Now observing 'lastMessageContent' to scroll while AI is typing
    val lastMessageContent = history.lastOrNull()?.text ?: ""

    LaunchedEffect(history.size, isStreaming, lastMessageContent) {
        if (history.isNotEmpty()) {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            
            // Check if user is near the bottom (within 2-3 items)
            val isNearBottom = lastVisibleIndex >= (totalItems - 3)
            
            val lastMessage = history.last()
            val isUserMessage = lastMessage.role == ChatRole.USER

            // Scroll if user just sent a message OR if they are already reading at the bottom
            if (isUserMessage || isNearBottom) {
                listState.animateScrollToItem(history.size - 1)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isExpanded) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 0.dp, bottom = 140.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (history.isEmpty()) {
                    item { GreetingHeader() }
                    item {
                        SuggestionGrid(
                            suggestions = suggestions,
                            onSuggestionClick = { viewModel.updatePrompt(it) }
                        )
                    }
                }

                items(
                    items = history,
                    key = { it.id ?: "" } // Performance Optimization
                ) { message ->
                    ChatMessageItem(
                        msg = message,
                        isDark = isDark,
                        isStreaming = isStreaming && message == history.last() && message.role == ChatRole.MODEL,
                        onSpeak = { viewModel.speakText(it) },
                        onPin = { viewModel.togglePin(it) },
                        onRegenerate = { /* Call VM */ },
                        onEdit = { viewModel.updatePrompt(it) }
                    )
                }

                if (isStreaming) {
                    item { StreamingIndicator() }
                }
            }
        }

        // --- UPDATED INPUT BAR CONTAINER ---
        Box(
            modifier = Modifier
                .align(if (isExpanded) Alignment.TopCenter else Alignment.BottomCenter)
                .fillMaxWidth()
                .ifTrue(isExpanded) { fillMaxHeight() }
                .background(
                    if (isExpanded) MaterialTheme.colorScheme.background else Color.Transparent
                )
                .imePadding() 
        ) {
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
// --- UPDATED: SMART INPUT BAR (With Waveform & Typewriter) ---
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
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isExpanded) 1f else 0.95f)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    val shadowElevation = if (isExpanded) 0.dp else 12.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isExpanded) 0.dp else 8.dp, 
                end = if (isExpanded) 0.dp else 8.dp,
                bottom = if (isExpanded) 0.dp else 16.dp,
                top = 0.dp
            )
            .animateContentSize()
    ) {
        if (attachmentCount > 0 && !isExpanded) {
            Row(
                modifier = Modifier
                    .padding(bottom = 8.dp, start = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Image, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.width(6.dp))
                Text("$attachmentCount Image Attached", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .ifTrue(isExpanded) { fillMaxHeight() }
                .shadow(shadowElevation, RoundedCornerShape(if (isExpanded) 0.dp else 24.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(if (isExpanded) 0.dp else 24.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(if (isExpanded) 0.dp else 24.dp))
        ) {
            if (isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onExpandToggle) {
                        Icon(Icons.Rounded.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = onSend,
                        enabled = prompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Send")
                    }
                }
                Divider(color = borderColor)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isExpanded) 16.dp else 12.dp),
                verticalAlignment = if (isExpanded) Alignment.Top else Alignment.Bottom
            ) {
                if (!isExpanded) {
                    IconButton(
                        onClick = onAttach,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.background, CircleShape)
                            .border(1.dp, borderColor, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Add, "Attach", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // MIC / WAVEFORM BUTTON
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            // Width expands when active to show waveform
                            .width(if (isVoiceActive) 80.dp else 36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isVoiceActive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background
                            )
                            .border(1.dp, if(isVoiceActive) MaterialTheme.colorScheme.error else borderColor, CircleShape)
                            .clickable { onMicClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isVoiceActive) {
                            // FEATURE 1: VOICE WAVEFORM
                            VoiceWaveform()
                        } else {
                            Icon(
                                Icons.Rounded.Mic, 
                                "Voice", 
                                tint = MaterialTheme.colorScheme.onSurface, 
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = if (isExpanded) 0.dp else 6.dp)
                        .heightIn(max = if (isExpanded) Int.MAX_VALUE.dp else 120.dp)
                ) {
                    if (prompt.isEmpty() && !isVoiceActive) {
                        // FEATURE 3: TYPEWRITER PLACEHOLDER
                        TypewriterPlaceholder(isExpanded)
                    } else if (prompt.isEmpty() && isVoiceActive) {
                        Text(
                            "Listening...",
                            color = MaterialTheme.colorScheme.error,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .ifTrue(isExpanded) { fillMaxHeight() }
                    )
                }

                if (!isExpanded) {
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onExpandToggle,
                        modifier = Modifier.size(32.dp).padding(bottom = 2.dp)
                    ) {
                        Icon(Icons.Rounded.Fullscreen, "Expand", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val isSendEnabled = prompt.isNotBlank() || isStreaming
                    val btnColor = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(btnColor)
                            .clickable(enabled = isSendEnabled) { onSend() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isStreaming) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(
                                Icons.Rounded.ArrowUpward,
                                "Send",
                                tint = if (isSendEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- FEATURE 1 IMPLEMENTATION: VOICE WAVEFORM ---
@Composable
fun VoiceWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(5) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 10f,
                targetValue = 24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, delayMillis = index * 50, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.error)
            )
        }
    }
}

// --- FEATURE 3 IMPLEMENTATION: TYPEWRITER PLACEHOLDER ---
@Composable
fun TypewriterPlaceholder(isExpanded: Boolean) {
    val hints = listOf(
        "Ask Cipher to code...",
        "Write a creative story...",
        "Debug my application...",
        "Explain Quantum Physics...",
        "Generate a business plan..."
    )

    var displayedText by remember { mutableStateOf("") }
    var hintIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            val targetText = hints[hintIndex]

            // Type out
            for (i in 1..targetText.length) {
                displayedText = targetText.take(i)
                delay(50)
            }

            delay(2000) // Wait

            // Delete
            for (i in targetText.length downTo 0) {
                displayedText = targetText.take(i)
                delay(30)
            }

            delay(500)
            hintIndex = (hintIndex + 1) % hints.size
        }
    }

    Text(
        text = if (isExpanded) "Start coding or writing..." else displayedText,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// --- GREETING ---
@Composable
fun GreetingHeader() {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val pastelGreen = Color(0xFF81C784) 

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 60.dp, bottom = 20.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.my_logo),
            contentDescription = null,
            modifier = Modifier.size(48.dp).padding(bottom = 20.dp)
        )

        Text(
            text = "$greeting, Creator",
            style = MaterialTheme.typography.headlineMedium.copy(lineHeight = 40.sp),
            fontWeight = FontWeight.Bold,
            color = pastelGreen 
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ready to build something extraordinary?",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// --- SUGGESTION GRID ---
@Composable
fun SuggestionGrid(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        suggestions.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    SuggestionChip(
                        label = item,
                        onClick = { onSuggestionClick(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper Extension
fun Modifier.ifTrue(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}

@Composable
fun StreamingIndicator() {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 16.dp)) {
        Text(
            "Cipher is thinking...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --- SIDEBAR ---
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
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
             Image(
                 painter = painterResource(id = R.drawable.my_logo),
                 contentDescription = "Cipher Logo",
                 modifier = Modifier.size(32.dp)
             )
             Spacer(modifier = Modifier.width(12.dp))
             Text("Cipher Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

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

        Text("APPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        SidebarItem(Icons.Default.ChatBubbleOutline, "Chat", currentView == ViewMode.CHAT) { onViewChange(ViewMode.CHAT) }
        SidebarItem(Icons.Default.Code, "Code Lab", currentView == ViewMode.CODE_LAB) { onViewChange(ViewMode.CODE_LAB) }
        SidebarItem(Icons.Default.Visibility, "Vision Hub", currentView == ViewMode.VISION_HUB) { onViewChange(ViewMode.VISION_HUB) }
        SidebarItem(Icons.Default.Lightbulb, "Prompt Studio", currentView == ViewMode.PROMPT_STUDIO) { onViewChange(ViewMode.PROMPT_STUDIO) }
        SidebarItem(Icons.Default.Security, "Cyber House", currentView == ViewMode.CYBER_HOUSE) { onViewChange(ViewMode.CYBER_HOUSE) }
        SidebarItem(Icons.Default.Analytics, "Data Analyst", currentView == ViewMode.DATA_ANALYST) { onViewChange(ViewMode.DATA_ANALYST) }
        SidebarItem(Icons.Default.Description, "Doc Intel", currentView == ViewMode.DOC_INTEL) { onViewChange(ViewMode.DOC_INTEL) }

        Spacer(modifier = Modifier.height(24.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Text("RECENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sessions) { session ->
                val isSelected = session.id == currentSessionId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .combinedClickable(
                            onClick = { onSelectSession(session.id) },
                            onLongClick = { onRequestDelete(session.id) }
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Message, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
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

        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

        Row(modifier = Modifier.clickable { onViewChange(ViewMode.ABOUT) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Text("About", color = MaterialTheme.colorScheme.onSurface)
        }

        Row(modifier = Modifier.clickable { onOpenSettings() }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Settings", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SidebarItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}