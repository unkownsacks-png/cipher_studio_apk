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
import java.util.Random
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AboutScreen(
    theme: Theme,
    onBack: () -> Unit
) {
    // FIX: Handle System Back Button
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current // FEATURE 1: Haptic Feedback Support

    // FEATURE 5: Secret "Red Team" Mode State
    var secretTapCount by remember { mutableIntStateOf(0) }
    val isRedMode = secretTapCount >= 5
    // Dynamic Accent Color (Blue normally, Red in God Mode)
    val accentColor by animateColorAsState(
        targetValue = if (isRedMode) Color(0xFFEF4444) else Color(0xFF3B82F6),
        animationSpec = tween(1000)
    )

    var rotateX by remember { mutableStateOf(0f) }
    var rotateY by remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var isTouching by remember { mutableStateOf(false) } // To coordinate Gyro vs Touch

    // FEATURE 2: Gyroscope Parallax
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val listener = object : SensorEventListener {
            var gravity: FloatArray? = null
            var geomagnetic: FloatArray? = null

            override fun onSensorChanged(event: SensorEvent?) {
                if (isTouching) return // Don't fight with user's finger
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) gravity = it.values
                    if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = it.values
                    if (gravity != null && geomagnetic != null) {
                        val R = FloatArray(9)
                        val I = FloatArray(9)
                        if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(R, orientation)
                            // Smooth interpolation for sensor data
                            rotateY = (rotateY * 0.9f) + ((-orientation[2] * 20f) * 0.1f) // Roll
                            rotateX = (rotateX * 0.9f) + ((orientation[1] * 20f) * 0.1f)  // Pitch
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

    // FEATURE 4: Glitch Effect (RGB Shift / Position Jitter)
    var glitchOffsetX by remember { mutableStateOf(0f) }
    var glitchColorOffset by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(Random().nextLong(3000, 8000)) // Random interval
            // Trigger Glitch
            repeat(5) {
                glitchOffsetX = Random().nextInt(-15, 15).toFloat()
                glitchColorOffset = true
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Haptic on glitch
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
                        // Don't reset to 0 immediately, let Gyro take over smoothly
                        isTouching = false
                    }
                }
                true
            }
    ) {
        WarpSpeedBackground(isRedMode) // Pass Red Mode to BG

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

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .aspectRatio(0.8f)
                .graphicsLayer {
                    rotationX = rotateX
                    rotationY = rotateY
                    cameraDistance = 12f * density
                    // Glitch application
                    translationX = glitchOffsetX
                }
                .background(Color.Black.copy(0.6f), RoundedCornerShape(24.dp))
                // Dynamic Border Color based on Red Mode
                .border(1.dp, if(glitchColorOffset) Color.Cyan else accentColor.copy(0.5f), RoundedCornerShape(24.dp))
        ) {
            // Glitch Shadow Layer (Cyan/Red split)
            if (glitchColorOffset) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(0.1f)).offset(x = 4.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        // FEATURE 5: Secret Click Listener
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            secretTapCount++
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (secretTapCount == 5) {
                                // Trigger God Mode Haptic
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                ) {
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
                            .border(1.dp, accentColor.copy(0.3f), CircleShape) // Dynamic Color
                            .graphicsLayer { rotationZ = rotation.value }
                    )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.Center)
                            .background(
                                Brush.linearGradient(
                                    // Dynamic Gradient
                                    if(isRedMode) listOf(Color(0xFF450a0a), Color.Black)
                                    else listOf(Color(0xFF111827), Color.Black)
                                ),
                                CircleShape
                            )
                            .border(1.dp, Color.White.copy(0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = if(isRedMode) Color.Red else Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                var scrambledText by remember { mutableStateOf("CIPHER ATTACK") }
                LaunchedEffect(Unit) {
                    val target = if(isRedMode) "ROOT ACCESS" else "CIPHER ATTACK" // Dynamic Text
                    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()"
                    for (i in 0..15) {
                        scrambledText = target.map { if (Random().nextBoolean()) it else chars.random() }.joinToString("")
                        if (i % 2 == 0) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Haptic on scramble
                        delay(50)
                    }
                    scrambledText = target
                }

                // If Red Mode is active, show warning text
                if(isRedMode) {
                     Text(
                        text = "GOD MODE ACTIVATED",
                        color = Color.Red,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 4.sp
                    )
                }

                Text(
                    text = scrambledText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        brush = Brush.verticalGradient(
                            if(isRedMode) listOf(Color.Red, Color.DarkGray)
                            else listOf(Color.White, Color.Gray)
                        ),
                        fontFamily = FontFamily.Monospace
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(accentColor, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BIRUK GETACHEW",
                        color = if(isRedMode) Color(0xFFFCA5A5) else Color(0xFF60A5FA),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // FEATURE 3: Typewriter Effect
                val fullDescription = "Red Teaming the Metaverse.\nCyber Security Specialist & Creative Web Designer."
                var typeWriterText by remember { mutableStateOf("") }
                
                LaunchedEffect(fullDescription) {
                    typeWriterText = ""
                    fullDescription.forEach { char ->
                        typeWriterText += char
                        delay(30) // Typing speed
                    }
                }

                Text(
                    text = typeWriterText + if(System.currentTimeMillis() % 1000 > 500) "_" else "", // Blinking cursor
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily.Monospace // Monospace fits typewriter better
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SocialShard("X", "https://x.com/Cipher_attacks", if(isRedMode) Color.Red else Color(0xFF60A5FA))
                    SocialShard("YT", "https://www.youtube.com/@cipher-atack", if(isRedMode) Color.Red else Color(0xFFEF4444))
                    SocialShard("GH", "https://github.com/cipher-attack", if(isRedMode) Color.Red else Color.White)
                    SocialShard("LI", "https://et.linkedin.com/in/cipher-attack-93582433b", if(isRedMode) Color.Red else Color(0xFF2563EB))
                }
            }

            ScanLine(accentColor)
        }
    }
}

@Composable
fun SocialShard(label: String, url: String, color: Color) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current // Haptic
    val scope = rememberCoroutineScope() // Use Scope for async delay
    var isShattered by remember { mutableStateOf(false) }

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
                haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Feature 1
                // FIX: Coroutine handling for visual feedback + stability
                scope.launch {
                    isShattered = true
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(1000)
                    isShattered = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun WarpSpeedBackground(isRedMode: Boolean = false) {
    val stars = remember { List(200) { Star(
        x = (Math.random() * 2 - 1).toFloat(),
        y = (Math.random() * 2 - 1).toFloat(),
        z = Math.random().toFloat()
    ) } }

    val starColor = if(isRedMode) Color.Red else Color.White // Red Mode support

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2

        stars.forEach { star ->
            star.z -= 0.01f
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

@Composable
fun ScanLine(color: Color = Color(0xFF3B82F6)) {
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
                        colors = listOf(Color.Transparent, color.copy(0.5f), Color.Transparent)
                    )
                )
        )
    }
}