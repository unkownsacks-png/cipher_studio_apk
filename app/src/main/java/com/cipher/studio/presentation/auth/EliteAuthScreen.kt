package com.cipher.studio.presentation.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Easing
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import android.view.animation.OvershootInterpolator

// --- ELITE THEME COLORS ---
val DeepSpaceBlack = Color(0xFF020617)
val CyberGreen = Color(0xFF10b981)
val CyberGreenGlow = Color(0xFF34d399)
val GlassSurface = Color(0xFF0F172A).copy(alpha = 0.65f) // Slightly more transparent for effect
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val ErrorRed = Color(0xFFEF4444)

@Composable
fun EliteAuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // ViewModel State
    val email by viewModel.email.collectAsState()
    val key by viewModel.key.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoginSuccess by viewModel.loginSuccess.collectAsState()

    // Animation States
    var startAnimation by remember { mutableStateOf(false) }
    var showSuccessBlast by remember { mutableStateOf(false) }

    // FEATURE 1: Error Shake & Haptic
    val shakeOffset = remember { Animatable(0f) }
    
    // FEATURE 4: Interactive Particles State
    var touchPosition by remember { mutableStateOf(Offset.Zero) }
    val particles = remember { List(20) { Particle() } }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            // Shake Effect
            repeat(3) {
                shakeOffset.animateTo(15f, animationSpec = tween(50))
                shakeOffset.animateTo(-15f, animationSpec = tween(50))
            }
            shakeOffset.animateTo(0f)
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    // FEATURE 6: Success Blast Logic
    LaunchedEffect(isLoginSuccess) {
        if (isLoginSuccess) {
            showSuccessBlast = true
            delay(800) // Wait for blast animation
            onLoginSuccess()
        }
    }

    // FEATURE 1: Parallax Nebula Background
    val infiniteTransition = rememberInfiniteTransition(label = "nebula")
    val nebulaMove by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 200f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBlack)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    touchPosition = offset
                    // Trigger particle burst on tap (simulated by state update)
                }
            }
    ) {
        // 2. Dynamic Background Canvas (Nebula & Particles)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Nebula 1
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF064e3b).copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(size.width * 0.2f + nebulaMove, size.height * 0.3f),
                    radius = size.width * 1.2f
                )
            )
            // Nebula 2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1e1b4b).copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.8f - nebulaMove, size.height * 0.8f),
                    radius = size.width * 1.0f
                )
            )

            // FEATURE 4: Interactive Particles
            particles.forEach { particle ->
                // Simple physics simulation for visual flair
                val x = (particle.initialX + nebulaMove * particle.speed) % size.width
                val y = (particle.initialY + nebulaMove * particle.speed * 0.5f) % size.height
                
                drawCircle(
                    color = CyberGreen.copy(alpha = particle.alpha),
                    radius = particle.size,
                    center = Offset(x, y)
                )
            }
        }

        // Main Content Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = shakeOffset.value.dp),
            contentAlignment = Alignment.Center
        ) {
            // 3. FROSTED GLASS CARD (Glassmorphism 2.0)
            Box(
                modifier = Modifier
                    .width(360.dp) // Fixed width for cleaner look on tablets
                    .clip(RoundedCornerShape(32.dp))
                    .background(GlassSurface)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    // FEATURE 3: Backdrop Blur Simulation (Visual trick via layer transparency)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // 4. LOGO WITH GLITCH EFFECT
                   AnimatedVisibility(
    visible = startAnimation,
    enter = scaleIn(
        animationSpec = tween(
            durationMillis = 600,
            // OvershootInterpolatorን ወደ Compose Easing የሚቀይር ማስተካከያ
            easing = Easing { fraction -> 
                OvershootInterpolator(1.2f).getInterpolation(fraction) 
            }
        )
    ) + fadeIn(animationSpec = tween(400))
) {
    GlitchLogo(isLoading = isLoading || showSuccessBlast)
}

Spacer(modifier = Modifier.height(32.dp))

                    // 7. DECRYPTION TEXT EFFECT
                    DecryptionText(
                        targetText = "CIPHER ELITE",
                        modifier = Modifier.padding(bottom = 6.dp),
                        visible = startAnimation,
                        color = if(showSuccessBlast) CyberGreen else TextPrimary
                    )

                    AnimatedVisibility(
                        visible = startAnimation,
                        enter = fadeIn(tween(1000, delayMillis = 300))
                    ) {
                        Text(
                            text = if(showSuccessBlast) "ACCESS GRANTED" else "SECURE TERMINAL V2.0",
                            fontSize = 10.sp,
                            color = if(showSuccessBlast) CyberGreen else TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.3.em,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // 6. MINIMALIST INPUTS (Smart Focus)
                    AnimatedVisibility(
                        visible = startAnimation && !showSuccessBlast,
                        enter = slideInVertically(tween(600, delayMillis = 500)) { 50 } + fadeIn(tween(600))
                    ) {
                        val emailFocusRequester = remember { FocusRequester() }

                        LaunchedEffect(Unit) {
                            delay(800) 
                            emailFocusRequester.requestFocus()
                        }

                        Column {
                            MinimalistTextField(
                                value = email,
                                onValueChange = viewModel::onEmailChange,
                                label = "IDENTITY",
                                placeholder = "Enter Email",
                                keyboardType = KeyboardType.Email,
                                focusRequester = emailFocusRequester,
                                imeAction = ImeAction.Next
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            MinimalistTextField(
                                value = key,
                                onValueChange = viewModel::onKeyChange,
                                label = "PASSPHRASE",
                                placeholder = "••••••••",
                                keyboardType = KeyboardType.Password,
                                isPassword = true,
                                imeAction = ImeAction.Done,
                                onAction = {
                                    keyboardController?.hide()
                                    viewModel.handleAccess()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // 8. INTERACTIVE BUTTON
                    AnimatedVisibility(
                        visible = startAnimation && !showSuccessBlast,
                        enter = fadeIn(tween(600, delayMillis = 700)) + scaleIn(tween(600))
                    ) {
                        LoginButton(
                            isLoading = isLoading,
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.handleAccess()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Footer Link
                    AnimatedVisibility(
                        visible = startAnimation && !showSuccessBlast,
                        enter = fadeIn(tween(1000, delayMillis = 900))
                    ) {
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Cipher_attack"))
                                context.startActivity(intent)
                            }
                        ) {
                            Text(
                                text = "REQUEST ACCESS KEY",
                                color = CyberGreenGlow.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // FEATURE 6: LOGIN SUCCESS BLAST OVERLAY
        if (showSuccessBlast) {
            val transition = updateTransition(targetState = true, label = "blast")
            val radius by transition.animateFloat(
                transitionSpec = { tween(800, easing = FastOutSlowInEasing) },
                label = "radius"
            ) { state -> if (state) 2000f else 0f }
            
            val alpha by transition.animateFloat(
                transitionSpec = { tween(800) },
                label = "alpha"
            ) { state -> if (state) 0f else 0.8f }

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = CyberGreen.copy(alpha = alpha),
                    radius = radius,
                    center = center
                )
            }
        }
    }
}

// --- HELPER CLASSES & COMPONENTS ---

// Simple particle data class
data class Particle(
    val initialX: Float = Random.nextFloat() * 1000f,
    val initialY: Float = Random.nextFloat() * 2000f,
    val size: Float = Random.nextFloat() * 3f + 1f,
    val speed: Float = Random.nextFloat() * 0.5f + 0.1f,
    val alpha: Float = Random.nextFloat() * 0.5f + 0.1f
)

@Composable
fun GlitchLogo(isLoading: Boolean) {
    // FEATURE 2: Glitch Effect Logic
    val infiniteTransition = rememberInfiniteTransition(label = "glitch")
    
    // Slight random offset for "glitch" look when loading
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -2f, targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glitchX"
    )
    
    // Scale pulse for heartbeat
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(if(isLoading) scale else 1f)
            .offset(x = if(isLoading) offsetX.dp else 0.dp)
            .shadow(
                elevation = 30.dp, 
                shape = CircleShape, 
                spotColor = CyberGreen,
                ambientColor = CyberGreen
            ),
        contentAlignment = Alignment.Center
    ) {
        CipherExactLogo(modifier = Modifier.fillMaxSize())
        
        // Glitch Overlay (Optional red/blue shift for true glitch feeling)
        if(isLoading) {
             CipherExactLogo(modifier = Modifier.fillMaxSize().offset(x = 2.dp).alpha(0.3f), color = Color.Red)
             CipherExactLogo(modifier = Modifier.fillMaxSize().offset(x = (-2).dp).alpha(0.3f), color = Color.Blue)
        }
    }
}

@Composable
fun CipherExactLogo(modifier: Modifier, color: Color = CyberGreen) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 100f
        val shieldPath = Path().apply {
            moveTo(75f * scale, 25f * scale)
            lineTo(35f * scale, 25f * scale)
            lineTo(15f * scale, 50f * scale)
            lineTo(35f * scale, 75f * scale)
            lineTo(75f * scale, 75f * scale)
        }

        drawPath(
            path = shieldPath,
            color = color,
            style = Stroke(width = 5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawCircle(
            color = color,
            center = Offset(40f * scale, 50f * scale),
            radius = 8f * scale,
            style = Stroke(width = 5f * scale)
        )

        // Data Lines
        drawLine(color = color, start = Offset(50f * scale, 50f * scale), end = Offset(85f * scale, 50f * scale), strokeWidth = 5f * scale, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(68f * scale, 50f * scale), end = Offset(68f * scale, 62f * scale), strokeWidth = 5f * scale, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(78f * scale, 50f * scale), end = Offset(78f * scale, 58f * scale), strokeWidth = 5f * scale, cap = StrokeCap.Round)
    }
}

@Composable
fun MinimalistTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Next,
    onAction: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = if (isFocused) CyberGreen else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.15.em,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TextPrimary,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onAction?.invoke() }, onGo = { onAction?.invoke() }),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if(isFocused) Color.White.copy(0.05f) else Color.Transparent) // Highlight on focus
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && !isFocused) {
                        Text(placeholder, color = TextSecondary.copy(alpha = 0.4f), fontSize = 16.sp)
                    }
                    innerTextField()
                }
            }
        )

        // Animated Bottom Line
        val lineWidth by animateFloatAsState(targetValue = if (isFocused) 1f else 0f, label = "lineW")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TextSecondary.copy(0.2f))
        ) {
             Box(
                modifier = Modifier
                    .fillMaxWidth(lineWidth)
                    .height(2.dp) // Thicker when active
                    .background(CyberGreen)
            )
        }
    }
}

@Composable
fun LoginButton(isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = 20.dp, 
                shape = RoundedCornerShape(16.dp),
                spotColor = CyberGreen.copy(alpha = 0.5f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CyberGreen,
            disabledContainerColor = CyberGreen.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = DeepSpaceBlack,
                strokeWidth = 3.dp
            )
        } else {
            Text(
                text = "INITIALIZE SYSTEM",
                color = DeepSpaceBlack,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.1.em
            )
        }
    }
}

@Composable
fun DecryptionText(targetText: String, modifier: Modifier = Modifier, visible: Boolean, color: Color) {
    var displayText by remember { mutableStateOf("") }
    val characters = "01010101XYZ#@!"

    LaunchedEffect(visible) {
        if (visible) {
            for (i in 1..targetText.length) {
                repeat(2) {
                    displayText = targetText.take(i - 1) + characters.random()
                    delay(30) // Faster decryption
                }
                displayText = targetText.take(i)
            }
        }
    }

    Text(
        text = displayText,
        color = color,
        fontSize = 26.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.15.em,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}