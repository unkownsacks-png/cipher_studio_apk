package com.cipher.studio.presentation.visionhub

import android.content.Intent
import android.graphics.BitmapFactory
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.SpanStyle // FIX: Added Import
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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

    // FEATURE 1: Text To Speech Engine
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

    // Modern Color Palette
    val bgColor = if (isDark) Color(0xFF0B0F19) else Color(0xFFF1F5F9)
    val cardBg = if (isDark) Color(0xFF151C2C) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val accentColor = Color(0xFFA855F7) // Purple accent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- Header ---
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp, top = 8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Visibility, "Vision", tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Vision Hub", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text("AI-Powered Visual Analysis", fontSize = 13.sp, color = if(isDark) Color.Gray else Color.DarkGray)
            }
        }

        // --- FEATURE 2 & 3 & 4: ADVANCED IMAGE PREVIEW (Zoom, Pan, Reset, Scan) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = accentColor.copy(alpha = 0.1f))
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDark) Color.Black else Color(0xFFE2E8F0))
                .border(1.dp, if (isDark) Color.White.copy(0.1f) else Color.White, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (selectedImage != null) {
                val bitmap = remember(selectedImage) {
                    val decodedBytes = Base64.decode(selectedImage!!.data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size).asImageBitmap()
                }

                // Zoom & Pan State
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                if (scale == 1f) offset = Offset.Zero
                                else {
                                    val newOffset = offset + pan
                                    offset = newOffset
                                }
                            }
                        }
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Preview",
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

                    // FEATURE 2: Scanning Laser Animation
                    if (isAnalyzing) {
                        ScannerOverlay()
                    }
                }

                // FEATURE 4: Image Controls (Remove & Reset Zoom)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    // Reset Zoom Button
                    if (scale > 1f) {
                        IconButton(
                            onClick = { scale = 1f; offset = Offset.Zero },
                            modifier = Modifier
                                .background(Color.Black.copy(0.6f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Rounded.ZoomInMap, "Reset", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Remove/Replace Button
                    IconButton(
                        onClick = { viewModel.clearSelection() },
                        modifier = Modifier
                            .background(Color.Red.copy(0.8f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Close, "Remove", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

            } else {
                // Empty State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { launcher.launch("image/*") },
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, "Upload", tint = Color.Gray, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tap to upload Image", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)
                    Text("Supports JPG, PNG", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- FEATURE 5: Modern Action Grid ---
        Text("AI TOOLS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2
        ) {
            val itemMod = Modifier.weight(1f).height(50.dp)
            val hasImg = selectedImage != null && !isAnalyzing

            VisionToolButton("Describe", Icons.Rounded.AutoAwesome, Color(0xFF3B82F6), hasImg, { viewModel.analyzeImage(VisionMode.DESCRIBE) }, itemMod)
            VisionToolButton("OCR Text", Icons.Rounded.TextFields, Color(0xFF22C55E), hasImg, { viewModel.analyzeImage(VisionMode.EXTRACT) }, itemMod)
            VisionToolButton("Code Ext.", Icons.Rounded.Code, Color(0xFFF97316), hasImg, { viewModel.analyzeImage(VisionMode.CODE) }, itemMod)
            VisionToolButton("Security", Icons.Rounded.Security, Color(0xFFEF4444), hasImg, { viewModel.analyzeImage(VisionMode.THREAT) }, itemMod)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- FEATURE 6: Result Card with Markdown & Toolbar ---
        if (result.isNotEmpty() || isAnalyzing) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth().animateContentSize()
            ) {
                Column {
                    // Result Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color.White.copy(0.05f) else Color.Gray.copy(0.05f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OUTPUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        
                        if (!isAnalyzing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val clipboardManager = LocalClipboardManager.current
                                SmallIconBtn(Icons.Rounded.VolumeUp) { tts?.speak(result, TextToSpeech.QUEUE_FLUSH, null, null) }
                                SmallIconBtn(Icons.Rounded.ContentCopy) { 
                                    clipboardManager.setText(AnnotatedString(result))
                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                }
                                SmallIconBtn(Icons.Rounded.Share) {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, result)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share"))
                                }
                            }
                        }
                    }

                    // Result Body
                    Box(modifier = Modifier.padding(16.dp).heightIn(min = 100.dp)) {
                        if (isAnalyzing) {
                            Row(modifier = Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = accentColor)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Analyzing pixels...", fontSize = 14.sp, color = Color.Gray)
                            }
                        } else {
                            // FEATURE 7: Markdown Rendering
                            Text(
                                text = formatMarkdown(result, isDark),
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- HELPER COMPONENTS ---

@Composable
fun VisionToolButton(
    text: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.1f),
            contentColor = color,
            disabledContainerColor = Color.Gray.copy(alpha = 0.05f),
            disabledContentColor = Color.Gray.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
fun SmallIconBtn(icon: ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
    }
}

@Composable
fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanY"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val yPos = size.height * scanY
        val gradient = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color(0xFF22C55E).copy(alpha = 0.5f)),
            startY = yPos - 150f,
            endY = yPos
        )
        
        drawRect(brush = gradient, topLeft = Offset(0f, yPos - 150f), size = androidx.compose.ui.geometry.Size(size.width, 150f))
        drawLine(
            color = Color(0xFF22C55E),
            start = Offset(0f, yPos),
            end = Offset(size.width, yPos),
            strokeWidth = 4f
        )
    }
}

// FEATURE 7: Markdown Parser (Simple Bold & Code handling)
fun formatMarkdown(text: String, isDark: Boolean): AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Bold text
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = if(isDark) Color(0xFF60A5FA) else Color(0xFF2563EB))) {
                    append(part)
                }
            } else {
                // Normal text (Check for simple code blocks ` `)
                val codeParts = part.split("`")
                codeParts.forEachIndexed { cIndex, cPart ->
                    if (cIndex % 2 == 1) {
                        withStyle(style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = if(isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                            color = if(isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                        )) {
                            append(" $cPart ")
                        }
                    } else {
                        append(cPart)
                    }
                }
            }
        }
    }
}