// ... imports ...
// (ቀድሞ የነበሩ Imports እንዳሉ ሆነው፣ እነዚህን ጨምር)
import android.Manifest
import androidx.compose.foundation.interaction.MutableInteractionSource
// ... other imports

// --- CHAT VIEW (UPDATED) ---
@Composable
fun ChatView(viewModel: MainViewModel, isDark: Boolean) {
    val history by viewModel.history.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    
    // NEW STATES FROM VIEWMODEL
    val isExpanded by viewModel.isInputExpanded.collectAsState()
    val isVoiceActive by viewModel.isVoiceActive.collectAsState()

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // GALLERY LAUNCHER
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.addAttachment(it) } }

    // VOICE PERMISSION LAUNCHER
    val voicePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceInput()
        } else {
            Toast.makeText(context, "Permission needed for Voice Input", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(history.size, isStreaming) {
        if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1)
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        onRegenerate = { /* Call VM */ },
                        onEdit = { viewModel.updatePrompt(it) }
                    )
                }

                if (isStreaming) {
                    item { StreamingIndicator() }
                }
            }
        }

        // --- NEW SMART DYNAMIC INPUT BAR ---
        Box(
            modifier = Modifier
                .align(if (isExpanded) Alignment.TopCenter else Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(if (isExpanded) 1f else Float.NaN)
                .background(
                    if (isExpanded) MaterialTheme.colorScheme.background else Color.Transparent
                )
                .imePadding() 
        ) {
            if (!isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
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
                
                // NEW: Voice Logic
                isVoiceActive = isVoiceActive,
                onMicClick = { 
                    voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) 
                }
            )
        }
    }
}

// --- NEW COMPONENT: SMART INPUT BAR (UPDATED WITH VOICE UI) ---
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
    // Voice Params
    isVoiceActive: Boolean,
    onMicClick: () -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isExpanded) 0.5f else 0.9f)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    // Pulse Animation for Voice
    val pulseAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isExpanded) 0.dp else 12.dp,
                end = if (isExpanded) 0.dp else 12.dp,
                bottom = if (isExpanded) 0.dp else 16.dp,
                top = if (isExpanded) 0.dp else 0.dp
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
                .fillMaxHeight(if (isExpanded) 1f else Float.NaN)
                .shadow(if (isExpanded) 0.dp else 4.dp, RoundedCornerShape(if (isExpanded) 0.dp else 28.dp), spotColor = Color.Black.copy(0.1f))
                .clip(RoundedCornerShape(if (isExpanded) 0.dp else 28.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(if (isExpanded) 0.dp else 28.dp))
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
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // MIC BUTTON WITH PULSE ANIMATION
                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isVoiceActive) MaterialTheme.colorScheme.error.copy(alpha = pulseAlpha) else MaterialTheme.colorScheme.background, 
                                CircleShape
                            )
                            .border(1.dp, if(isVoiceActive) Color.Transparent else borderColor, CircleShape)
                    ) {
                        Icon(
                            if(isVoiceActive) Icons.Rounded.Stop else Icons.Rounded.Mic, 
                            "Voice", 
                            tint = if (isVoiceActive) Color.White else MaterialTheme.colorScheme.onSurface, 
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = if (isExpanded) 0.dp else 6.dp)
                        .heightIn(max = if (isExpanded) Int.MAX_VALUE.dp else 120.dp)
                ) {
                    if (prompt.isEmpty()) {
                        Text(
                            if (isExpanded) "Start coding or writing..." else if (isVoiceActive) "Listening..." else "Message Cipher...",
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

                    Spacer(modifier = Modifier.width(8.dp))

                    val isSendEnabled = prompt.isNotBlank() || isStreaming
                    val btnColor = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh

                    Box(
                        modifier = Modifier
                            .size(42.dp)
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