package com.cipher.studio.presentation.visionhub

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisionHubScreen(
    theme: Theme,
    viewModel: VisionHubViewModel = hiltViewModel()
) {
    val isDark = theme == Theme.DARK
    val selectedImage by viewModel.selectedImage.collectAsState()
    val result by viewModel.result.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val context = LocalContext.current

    // -- FEATURE 3: Text To Speech Engine Initialization --
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        onDispose { tts?.shutdown() }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.2f)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White

    // Main Container
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- Header ---
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
            Icon(Icons.Default.Visibility, "Vision", tint = Color(0xFFA855F7), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Vision Hub", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text("Advanced image analysis & extraction.", fontSize = 14.sp, color = Color.Gray)
            }
        }

        // --- UPLOAD BOX (Top) with Feature 2 & 5 ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.3f), RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (selectedImage != null) {
                val bitmap = remember(selectedImage) {
                    val decodedBytes = Base64.decode(selectedImage!!.data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size).asImageBitmap()
                }

                // -- FEATURE 5: Zoom & Pan State --
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)) // Clip ensures zoomed image doesn't overflow
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f) // Limit zoom between 1x and 4x
                                if (scale == 1f) offset = Offset.Zero // Reset offset if zoomed out
                                else offset += pan
                            }
                        }
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Uploaded Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )

                    // -- FEATURE 2: Scanning Animation Overlay --
                    if (isAnalyzing) {
                        ScannerOverlay()
                    }
                }
                
                // Reset Zoom Button (Optional Helper)
                if (scale > 1f) {
                    IconButton(
                        onClick = { scale = 1f; offset = Offset.Zero },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(0.5f), RoundedCornerShape(4.dp))
                    ) {
                        Icon(Icons.Default.ZoomOutMap, "Reset", tint = Color.White)
                    }
                }

            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { launcher.launch("image/*") } // Click logic moved here when empty
                ) {
                    Icon(Icons.Default.CloudUpload, "Upload", tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Text("Click to Upload Image", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text("JPG, PNG, WEBP", fontSize = 12.sp, color = Color.Gray.copy(0.7f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- ACTION BUTTONS ---
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            val gridItemModifier = Modifier.weight(1f).fillMaxWidth()

            VisionActionButton("Describe", Icons.Default.Image, Color(0xFF3B82F6), selectedImage != null && !isAnalyzing, { viewModel.analyzeImage(VisionMode.DESCRIBE) }, gridItemModifier)
            VisionActionButton("OCR Text", Icons.Default.Description, Color(0xFF22C55E), selectedImage != null && !isAnalyzing, { viewModel.analyzeImage(VisionMode.EXTRACT) }, gridItemModifier)
            VisionActionButton("Code Ext.", Icons.Default.Code, Color(0xFFF97316), selectedImage != null && !isAnalyzing, { viewModel.analyzeImage(VisionMode.CODE) }, gridItemModifier)
            VisionActionButton("Threat Detect", Icons.Default.Security, Color(0xFFEF4444), selectedImage != null && !isAnalyzing, { viewModel.analyzeImage(VisionMode.THREAT) }, gridItemModifier)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ANALYSIS RESULT (Bottom) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .background(cardBg)
        ) {
            // -- FEATURE 3: Result Header with Actions --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(if (isDark) Color.White.copy(0.05f) else Color.Gray.copy(0.1f))
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ANALYSIS RESULT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )

                // Action Icons (Copy, Speak, Share)
                if (result.isNotEmpty() && !isAnalyzing) {
                    Row {
                        val clipboardManager = LocalClipboardManager.current
                        
                        // Speak
                        IconButton(onClick = { tts?.speak(result, TextToSpeech.QUEUE_FLUSH, null, null) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.VolumeUp, "Speak", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        // Copy
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(result))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.ContentCopy, "Copy", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        // Share
                        IconButton(onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, result)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Analysis"))
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Share, "Share", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Divider(color = borderColor, thickness = 1.dp)

            // Result Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                if (isAnalyzing) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Vision model is analyzing...", color = Color.Gray, fontSize = 14.sp)
                    }
                } else if (result.isNotEmpty()) {
                    val textScrollState = rememberScrollState()
                    Text(
                        text = result,
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.verticalScroll(textScrollState)
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Visibility, "Empty", tint = Color.Gray.copy(0.3f), modifier = Modifier.size(64.dp))
                        Text("Upload an image and select a tool", color = Color.Gray.copy(0.5f), fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

// -- FEATURE 2 Implementation: Cyber Scanning Animation --
@Composable
fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition()
    val scanPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val yPos = size.height * scanPosition
        
        // 1. Draw the laser line
        drawLine(
            color = Color(0xFF22C55E), // Matrix Green
            start = Offset(0f, yPos),
            end = Offset(size.width, yPos),
            strokeWidth = 4f
        )

        // 2. Draw the fading trail (gradient) behind the line
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF22C55E).copy(alpha = 0f), Color(0xFF22C55E).copy(alpha = 0.3f)),
                startY = yPos - 100f,
                endY = yPos
            ),
            topLeft = Offset(0f, yPos - 100f),
            size = androidx.compose.ui.geometry.Size(size.width, 100f)
        )
    }
}

@Composable
fun VisionActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.1f),
            contentColor = color,
            disabledContainerColor = Color.Gray.copy(alpha = 0.1f),
            disabledContentColor = Color.Gray
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = modifier
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}