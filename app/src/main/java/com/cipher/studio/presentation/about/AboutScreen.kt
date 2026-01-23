package com.cipher.studio.presentation.about

import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cipher.studio.domain.model.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AboutScreen(
    theme: Theme,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Tilt State
    var rotateX by remember { mutableStateOf(0f) }
    var rotateY by remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    // --- Feature 1: Warp Speed Background Logic ---
    // Using a simple Canvas animation loop
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { size = it.size }
            // Capture touch for 3D Tilt Effect
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        val x = event.x - size.width / 2
                        val y = event.y - size.height / 2
                        rotateY = (x / (size.width / 2)) * 10f // Max 10 degrees
                        rotateX = (y / (size.height / 2)) * -10f
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        rotateX = 0f
                        rotateY = 0f
                    }
                }
                true
            }
    ) {
        // 1. Warp Speed Background
        WarpSpeedBackground()

        // 2. Floating Runes (Simplified as random text)
        FloatingRunes()

        // 3. Return Button
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .border(1.dp, Color(0xFF22C55E).copy(0.3f), RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF4ADE80))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SYSTEM RETURN", color = Color(0xFF4ADE80), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }

        // 4. Main Holographic Card (3D Tilt Container)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .aspectRatio(0.8f) // Card aspect ratio
                .graphicsLayer {
                    rotationX = rotateX
                    rotationY = rotateY
                    cameraDistance = 12f * density
                }
                .background(Color.Black.copy(0.6f), RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(24.dp))
                //.blur(if (android.os.Build.VERSION.SDK_INT >= 31) 20.dp else 0.dp) // Blur requires S+
        ) {
            // Inner Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo with Spin
                Box(modifier = Modifier.size(100.dp)) {
                    val rotation = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        rotation.animateTo(
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing))
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color(0xFF3B82F6).copy(0.3f), CircleShape)
                            .graphicsLayer { rotationZ = rotation.value }
                    )
                    
                    // Center Logo
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.Center)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF111827), Color.Black)),
                                CircleShape
                            )
                            .border(1.dp, Color.White.copy(0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code, // Placeholder for Cipher Logo
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Text Scramble Effect
                var scrambledText by remember { mutableStateOf("CIPHER ATTACK") }
                LaunchedEffect(Unit) {
                    val target = "CIPHER ATTACK"
                    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()"
                    for (i in 0..15) {
                        scrambledText = target.map { if (Random().nextBoolean()) it else chars.random() }.joinToString("")
                        delay(50)
                    }
                    scrambledText = target
                }

                Text(
                    text = scrambledText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    brush = Brush.verticalGradient(listOf(Color.White, Color.Gray)),
                    style = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF3B82F6), CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BIRUK GETACHEW",
                        color = Color(0xFF60A5FA),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Red Teaming the Metaverse.\nCyber Security Specialist & Creative Web Designer.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Social Shards Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SocialShard("X", "https://x.com/Cipher_attacks", Color(0xFF60A5FA))
                    SocialShard("YT", "https://www.youtube.com/@cipher-atack", Color(0xFFEF4444))
                    SocialShard("GH", "https://github.com/cipher-attack", Color.White)
                    SocialShard("LI", "https://et.linkedin.com/in/cipher-attack-93582433b", Color(0xFF2563EB))
                }
            }
            
            // Scanline Effect Overlay
            ScanLine()
        }
    }
}

// --- Components ---

@Composable
fun SocialShard(label: String, url: String, color: Color) {
    val context = LocalContext.current
    var isShattered by remember { mutableStateOf(false) }
    
    // Shatter Animation logic would go here, simpler version:
    val scale by animateFloatAsState(targetValue = if (isShattered) 1.2f else 1f)
    val alpha by animateFloatAsState(targetValue = if (isShattered) 0f else 1f)

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(Color.Black.copy(0.4f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
            .clickable {
                isShattered = true
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
                // Reset after delay handled by LaunchedEffect in real app
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun WarpSpeedBackground() {
    val stars = remember { List(200) { Star(
        x = (Math.random() * 2 - 1).toFloat(),
        y = (Math.random() * 2 - 1).toFloat(),
        z = Math.random().toFloat()
    ) } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        
        stars.forEach { star ->
            star.z -= 0.01f // Speed
            if (star.z <= 0) {
                star.z = 1f
                star.x = (Math.random() * 2 - 1).toFloat()
                star.y = (Math.random() * 2 - 1).toFloat()
            }

            val x = (star.x / star.z) * cx + cx
            val y = (star.y / star.z) * cy + cy
            val radius = (1f - star.z) * 3f

            if (x in 0f..size.width && y in 0f..size.height) {
                drawCircle(
                    color = Color.White.copy(alpha = (1f - star.z).coerceIn(0f, 1f)),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }
    }
    
    // Force recomposition loop
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis { }
        }
    }
}

class Star(var x: Float, var y: Float, var z: Float)

@Composable
fun FloatingRunes() {
    // Simplified: Just static random chars for now, animating text in Canvas is complex
}

@Composable
fun ScanLine() {
    val infiniteTransition = rememberInfiniteTransition()
    val yPercent by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .offset(y = maxHeight * yPercent)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF3B82F6).copy(0.5f), Color.Transparent)
                    )
                )
        )
    }
}