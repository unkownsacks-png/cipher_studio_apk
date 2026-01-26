package com.cipher.studio.presentation.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged // FIXED: Added critical import
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

// --- ELITE THEME COLORS ---
val DeepSpaceBlack = Color(0xFF020617)
val CyberGreen = Color(0xFF10b981)
val CyberGreenGlow = Color(0xFF34d399)
val GlassSurface = Color(0xFF0F172A).copy(alpha = 0.7f)
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF64748B)

@Composable
fun EliteAuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // ViewModel State
    val email by viewModel.email.collectAsState()
    val key by viewModel.key.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoginSuccess by viewModel.loginSuccess.collectAsState()

    // Animation States
    var startAnimation by remember { mutableStateOf(false) }
    
    // FEATURE: Error Shake Animation
    val shakeOffset = remember { Animatable(0f) }
    
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            // Shake Effect
            for (i in 0..3) {
                shakeOffset.animateTo(10f, animationSpec = tween(50))
                shakeOffset.animateTo(-10f, animationSpec = tween(50))
            }
            shakeOffset.animateTo(0f)
            
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    LaunchedEffect(isLoginSuccess) {
        if (isLoginSuccess) onLoginSuccess()
    }

    // 2. ALIVE NEBULA BACKGROUND
    val infiniteTransition = rememberInfiniteTransition(label = "nebula")
    val nebulaOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceBlack)
    ) {
        // Dynamic Background Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF064e3b).copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(size.width * 0.2f + (nebulaOffset * 0.1f), size.height * 0.2f),
                    radius = size.width * 0.9f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1e1b4b).copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(size.width * 0.8f - (nebulaOffset * 0.1f), size.height * 0.9f),
                    radius = size.width * 0.8f
                )
            )
        }

        // Main Content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .fillMaxWidth()
                .offset(x = shakeOffset.value.dp), // Apply Shake
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 3. FROSTED GLASS CARD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassSurface)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    // 4. STAGGERED ENTRANCE (Logo)
                    AnimatedVisibility(
                        visible = startAnimation,
                        enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { -40 }
                    ) {
                        // FEATURE: Neon Glow Layer (FIXED SHADOW)
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .shadow(
                                    elevation = 25.dp, 
                                    shape = CircleShape, 
                                    spotColor = CyberGreen,
                                    ambientColor = CyberGreen
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CipherExactLogo(modifier = Modifier.fillMaxSize())
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 7. DECRYPTION TEXT EFFECT
                    DecryptionText(
                        targetText = "CIPHER ELITE",
                        modifier = Modifier.padding(bottom = 8.dp),
                        visible = startAnimation
                    )

                    AnimatedVisibility(
                        visible = startAnimation,
                        enter = fadeIn(tween(1000, delayMillis = 300))
                    ) {
                        Text(
                            text = "SECURE ACCESS TERMINAL",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.2.em,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // 6. MINIMALIST INPUTS
                    AnimatedVisibility(
                        visible = startAnimation,
                        enter = fadeIn(tween(800, delayMillis = 500)) + slideInVertically(tween(800)) { 40 }
                    ) {
                        val emailFocusRequester = remember { FocusRequester() }
                        
                        // FEATURE: Auto-Focus
                        LaunchedEffect(Unit) {
                            delay(600) // Wait for enter animation
                            emailFocusRequester.requestFocus()
                        }

                        Column {
                            MinimalistTextField(
                                value = email,
                                onValueChange = viewModel::onEmailChange,
                                label = "IDENTITY (EMAIL)",
                                keyboardType = KeyboardType.Email,
                                focusRequester = emailFocusRequester,
                                imeAction = ImeAction.Next
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            MinimalistTextField(
                                value = key,
                                onValueChange = viewModel::onKeyChange,
                                label = "LICENSE KEY",
                                keyboardType = KeyboardType.Password,
                                isPassword = true,
                                imeAction = ImeAction.Done,
                                onAction = { // FEATURE: Fast Access
                                    keyboardController?.hide()
                                    viewModel.handleAccess()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // 8. INTERACTIVE HAPTIC BUTTON
                    AnimatedVisibility(
                        visible = startAnimation,
                        enter = fadeIn(tween(800, delayMillis = 700)) + scaleIn(tween(800))
                    ) {
                        val buttonScale by animateFloatAsState(if (isLoading) 0.95f else 1f, label = "btnScale")
                        
                        Button(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.handleAccess() 
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .scale(buttonScale)
                                .shadow(
                                    elevation = 15.dp, 
                                    shape = RoundedCornerShape(12.dp),
                                    spotColor = CyberGreen.copy(alpha = 0.4f)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberGreen,
                                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = DeepSpaceBlack,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "INITIALIZE SYSTEM",
                                    color = DeepSpaceBlack,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.1.em
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Footer Link
                    AnimatedVisibility(
                        visible = startAnimation,
                        enter = fadeIn(tween(1000, delayMillis = 900))
                    ) {
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Cipher_attack"))
                                context.startActivity(intent)
                            }
                        ) {
                            Text(
                                text = "ACQUIRE ACCESS KEY",
                                color = CyberGreenGlow,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 9. STATUS HEARTBEAT
                    AnimatedVisibility(
                        visible = startAnimation,
                        enter = fadeIn(tween(1000, delayMillis = 1100))
                    ) {
                        SystemStatusIndicator(isLoading)
                    }
                }
            }
        }
    }
}

// --- CUSTOM COMPONENTS FOR ELITE FEEL ---

@Composable
fun CipherExactLogo(modifier: Modifier) {
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
            color = CyberGreen,
            style = Stroke(width = 6f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawCircle(
            color = CyberGreen,
            center = Offset(40f * scale, 50f * scale),
            radius = 10f * scale,
            style = Stroke(width = 6f * scale)
        )

        drawLine(
            color = CyberGreen,
            start = Offset(50f * scale, 50f * scale),
            end = Offset(85f * scale, 50f * scale),
            strokeWidth = 6f * scale,
            cap = StrokeCap.Round
        )

        drawLine(
            color = CyberGreen,
            start = Offset(68f * scale, 50f * scale),
            end = Offset(68f * scale, 62f * scale),
            strokeWidth = 6f * scale,
            cap = StrokeCap.Round
        )

        drawLine(
            color = CyberGreen,
            start = Offset(78f * scale, 50f * scale),
            end = Offset(78f * scale, 58f * scale),
            strokeWidth = 6f * scale,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun MinimalistTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
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
            letterSpacing = 0.1.em,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TextPrimary,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { onAction?.invoke() },
                onGo = { onAction?.invoke() }
            ),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .onFocusChanged { isFocused = it.isFocused } // FIXED: Using Native Import
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty() && !isFocused) {
                        Text(
                            text = "...", 
                            color = TextSecondary.copy(alpha = 0.3f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        val lineWidth by animateFloatAsState(targetValue = if (isFocused) 1f else 0.3f, label = "line")
        val lineColor = if (isFocused) CyberGreen else TextSecondary.copy(alpha = 0.3f)
        
        Box(
            modifier = Modifier
                .fillMaxWidth(lineWidth)
                .height(1.dp)
                .background(lineColor)
                .align(if (isFocused) Alignment.CenterHorizontally else Alignment.Start)
        )
    }
}

@Composable
fun DecryptionText(targetText: String, modifier: Modifier = Modifier, visible: Boolean) {
    var displayText by remember { mutableStateOf("") }
    val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*"

    LaunchedEffect(visible) {
        if (visible) {
            for (i in 1..targetText.length) {
                repeat(3) {
                    displayText = targetText.take(i - 1) + characters.random()
                    delay(50)
                }
                displayText = targetText.take(i)
            }
        }
    }

    Text(
        text = displayText,
        color = TextPrimary,
        fontSize = 28.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.15.em,
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}

@Composable
fun SystemStatusIndicator(isLoading: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isLoading) Color(0xFFF59E0B) else CyberGreen.copy(alpha = alpha))
                .shadow(
                    elevation = 8.dp, 
                    shape = CircleShape,
                    spotColor = if (isLoading) Color(0xFFF59E0B) else CyberGreen
                )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (isLoading) "ESTABLISHING CONNECTION..." else "SYSTEM ONLINE :: READY",
            color = if (isLoading) Color(0xFFF59E0B) else TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.15.em
        )
    }
}