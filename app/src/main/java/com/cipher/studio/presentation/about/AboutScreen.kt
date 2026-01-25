package com.cipher.studio.presentation.about

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.view.MotionEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cipher.studio.domain.model.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AboutScreen(
    theme: Theme,
    onBack: () -> Unit
) {
    // 1. System Return Button Removed. Only Gesture/Hardware back works.
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // God Mode State
    var secretTapCount by remember { mutableIntStateOf(0) }
    val isRedMode = secretTapCount >= 5
    
    // Aesthetic Colors
    val primaryColor = if (isRedMode) Color(0xFFEF4444) else Color(0xFF00F0FF) // Cyber Cyan
    val secondaryColor = if (isRedMode) Color(0xFF7F1D1D) else Color(0xFF0077B6)
    
    val accentColor by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = tween(1000)
    )

    // 3D & Gyro Logic
    var rotateX by remember { mutableStateOf(0f) }
    var rotateY by remember { mutableStateOf(0f) }
    
    // -- 1. SMOOTH ROTATION FIX --
    // These animated values make the card tilt smoothly instead of jerking
    val animatedRotateX by animateFloatAsState(targetValue = rotateX, animationSpec = tween(100))
    val animatedRotateY by animateFloatAsState(targetValue = rotateY, animationSpec = tween(100))

    var size by remember { mutableStateOf(IntSize.Zero) }
    var isTouching by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val listener = object : SensorEventListener {
            var gravity: FloatArray? = null
            var geomagnetic: FloatArray? = null

            override fun onSensorChanged(event: SensorEvent?) {
                if (isTouching) return
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity = it.values
                    if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = it.values
                    if (gravity != null && geomagnetic != null) {
                        val R = FloatArray(9)
                        val I = FloatArray(9)
                        if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(R, orientation)
                            rotateY = (rotateY * 0.9f) + ((-orientation[2] * 20f) * 0.1f)
                            rotateX = (rotateX * 0.9f) + ((orientation[1] * 20f) * 0.1f)
                        }
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Glitch Logic
    var glitchOffsetX by remember { mutableStateOf(0f) }
    var glitchColorOffset by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(Random.nextLong(3000, 8000))
            repeat(5) {
                glitchOffsetX = Random.nextInt(-10, 10).toFloat()
                glitchColorOffset = true
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(50)
                glitchOffsetX = 0f
                glitchColorOffset = false
                delay(30)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { size = it.size }
            .pointerInteropFilter { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> isTouching = true
                    MotionEvent.ACTION_MOVE -> {
                        val x = event.x - size.width / 2
                        val y = event.y - size.height / 2
                        rotateY = (x / (size.width / 2)) * 10f
                        rotateX = (y / (size.height / 2)) * -10f
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isTouching = false
                    }
                }
                true
            }
    ) {
        // Feature 4: Hexagon Grid + Stars
        WarpSpeedBackground(isRedMode)
        HexagonGridOverlay(accentColor.copy(alpha = 0.1f))

        // Main Glass Card
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .aspectRatio(0.75f) // Taller card for better layout
                .graphicsLayer {
                    // -- SMOOTH ROTATION APPLIED HERE --
                    rotationX = animatedRotateX
                    rotationY = animatedRotateY
                    cameraDistance = 16f * density // Improved depth
                    translationX = glitchOffsetX
                }
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF111827).copy(0.9f),
                            Color.Black.copy(0.95f)
                        )
                    ), 
                    RoundedCornerShape(32.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(0.5f),
                            Color.Transparent,
                            accentColor.copy(0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            // Feature 3: Nano Particles inside card
            NanoParticles(accentColor)

            if (glitchColorOffset) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(0.05f)).offset(x = 2.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // --- PROFILE SECTION ---
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            secretTapCount++
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Feature 1: Holographic Rotating Ring
                    val infiniteTransition = rememberInfiniteTransition()
                    val angle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing))
                    )
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.sweepGradient(listOf(Color.Transparent, accentColor, Color.Transparent)),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f)))
                        )
                    }

                    // Inner Avatar
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(Color.Black, CircleShape)
                            .border(2.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = null,
                            tint = if(isRedMode) Color.Red else Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // --- TEXT SECTION ---
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    var scrambledText by remember { mutableStateOf("CIPHER ATTACK") }
                    LaunchedEffect(isRedMode) {
                        val target = if(isRedMode) "SYSTEM OVERRIDE" else "CIPHER ATTACK"
                        val chars = "010101XYZA_"
                        repeat(20) {
                            scrambledText = target.map { if (Random.nextBoolean()) it else chars.random() }.joinToString("")
                            delay(40)
                        }
                        scrambledText = target
                    }

                    Text(
                        text = scrambledText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            brush = Brush.horizontalGradient(listOf(accentColor, Color.White)),
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Feature 2: Live Status Pulse
                        val alpha by rememberInfiniteTransition().animateFloat(
                            initialValue = 0.3f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
                        )
                        Box(modifier = Modifier.size(8.dp).background(accentColor.copy(alpha = alpha), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BIRUK GETACHEW",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Typewriter Description
                    var typeWriterText by remember { mutableStateOf("") }
                    val desc = "Red Teaming the Metaverse.\nCyber Security & Creative Dev."
                    LaunchedEffect(desc) {
                        desc.forEach { char ->
                            typeWriterText += char
                            delay(20)
                        }
                    }
                    Text(
                        text = typeWriterText,
                        color = Color.White.copy(0.7f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // --- SOCIALS SECTION (FIXED & MODERN) ---
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // -- 3. SOCIAL ICONS FIX --
                    // Using 'Close' for X (Cross), 'PlayCircle' for YT, 'Code' for GitHub, 'BusinessCenter' for LinkedIn
                    ModernSocialLink(Icons.Outlined.Close, "https://x.com/Cipher_attacks", accentColor) 
                    ModernSocialLink(Icons.Outlined.PlayCircle, "https://www.youtube.com/@cipher-atack", Color(0xFFEF4444)) 
                    ModernSocialLink(Icons.Outlined.Code, "https://github.com/cipher-attack", Color.White) // Fixed: Changed DataObject to Code
                    ModernSocialLink(Icons.Outlined.BusinessCenter, "https://et.linkedin.com/in/cipher-attack-93582433b", Color(0xFF0A66C2)) 
                }
            }
        }

        // -- 2. SCANLINE ACTIVATION --
        // Added at the very end of the main Box to overlay everything
        ScanLine(accentColor.copy(alpha = 0.2f))
    }
}

// 2. Modern Smart Social Button (Line Icon Style)
@Composable
fun ModernSocialLink(icon: ImageVector, url: String, color: Color) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(if (isPressed) 0.8f else 1f)

    Box(
        modifier = Modifier
            .size(50.dp)
            .scale(scale)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable {
                isPressed = true
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                
                // Fixed Link Logic: Launch immediately with slight UI delay
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Reset animation shortly after
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
    
    LaunchedEffect(isPressed) {
        if(isPressed) {
            delay(200)
            isPressed = false
        }
    }
}

// Feature 3: Nano Particles inside card
@Composable
fun NanoParticles(color: Color) {
    val particles = remember { List(15) { Particle() } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            p.update(size.width, size.height)
            drawCircle(
                color = color.copy(alpha = p.alpha),
                radius = p.radius,
                center = Offset(p.x, p.y)
            )
        }
    }
    LaunchedEffect(Unit) {
        while(isActive) {
            withFrameMillis { }
        }
    }
}

class Particle {
    var x = Random.nextFloat() * 500
    var y = Random.nextFloat() * 800
    var radius = Random.nextFloat() * 3
    var alpha = Random.nextFloat() * 0.5f
    var speed = Random.nextFloat() * 0.5f
    
    fun update(w: Float, h: Float) {
        y -= speed
        if (y < 0) y = h
    }
}

// Feature 4: Hexagon Grid Overlay
@Composable
fun HexagonGridOverlay(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val hexSize = 100f
        for (i in 0..(size.width / hexSize).toInt()) {
            for (j in 0..(size.height / hexSize).toInt()) {
                val x = i * hexSize * 1.5f
                val y = j * hexSize * 1.732f + (if (i % 2 == 1) hexSize * 0.866f else 0f)
                drawCircle(color = color, radius = 1f, center = Offset(x, y))
            }
        }
    }
}

// Existing Warp Speed (Optimized)
@Composable
fun WarpSpeedBackground(isRedMode: Boolean = false) {
    // -- 4. PERFORMANCE FIX --
    // Reduced star count from 150 to 80 for better FPS on older devices
    val stars = remember { List(80) { Star(
        x = (Math.random() * 2 - 1).toFloat(),
        y = (Math.random() * 2 - 1).toFloat(),
        z = Math.random().toFloat()
    ) } }

    val starColor = if(isRedMode) Color.Red else Color.Cyan

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2

        stars.forEach { star ->
            star.z -= 0.02f // Faster warp
            if (star.z <= 0) {
                star.z = 1f
                star.x = (Math.random() * 2 - 1).toFloat()
                star.y = (Math.random() * 2 - 1).toFloat()
            }

            val x = (star.x / star.z) * cx + cx
            val y = (star.y / star.z) * cy + cy
            val radius = (1f - star.z) * 2f

            if (x in 0f..size.width && y in 0f..size.height) {
                drawCircle(
                    color = starColor.copy(alpha = (1f - star.z).coerceIn(0f, 1f)),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameMillis { }
        }
    }
}

class Star(var x: Float, var y: Float, var z: Float)

// Scanline
@Composable
fun ScanLine(color: Color) {
    val infiniteTransition = rememberInfiniteTransition()
    val yPercent by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .offset(y = 800.dp * yPercent) // Approximate height
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, color, Color.Transparent)
                )
            )
    )
}