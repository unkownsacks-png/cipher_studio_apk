package com.cipher.studio.presentation.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em // FIXED: Added Import for em
import androidx.hilt.navigation.compose.hiltViewModel

// Colors from your CSS
val ColorBackgroundDark = Color(0xFF020617)
val ColorGreenPrimary = Color(0xFF10b981)
val ColorGreenLight = Color(0xFF34d399)
val ColorCardBg = Color(0x4D0F172A)
val ColorTextWhite = Color(0xFFECFDF5)
val ColorTextGray = Color(0xFF94A3B8)
val ColorInputBg = Color(0x99020617)

@Composable
fun EliteAuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val email by viewModel.email.collectAsState()
    val key by viewModel.key.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoginSuccess by viewModel.loginSuccess.collectAsState()

    LaunchedEffect(isLoginSuccess) {
        if (isLoginSuccess) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ColorBackgroundDark, Color.Black)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF064e3b), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                    radius = size.width * 0.8f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0f172a), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width, size.height),
                    radius = size.width * 0.8f
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(20.dp)
                .fillMaxWidth(0.95f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = ColorCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x3310B981), RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 48.dp, horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    CipherLogo(modifier = Modifier.size(90.dp))
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "CIPHER ELITE",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.04).em,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Secure Encrypted Access System",
                        fontSize = 15.sp,
                        color = ColorTextGray,
                        modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
                    )

                    EliteTextField(
                        value = email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = "Registered Email",
                        keyboardType = KeyboardType.Email
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    EliteTextField(
                        value = key,
                        onValueChange = viewModel::onKeyChange,
                        placeholder = "Access License Key",
                        keyboardType = KeyboardType.Password,
                        isPassword = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val buttonScale by animateFloatAsState(if (isLoading) 0.98f else 1f)
                    
                    Button(
                        onClick = { viewModel.handleAccess() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .scale(buttonScale)
                            .shadow(10.dp, spotColor = ColorGreenPrimary, ambientColor = ColorGreenPrimary),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Gray
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF10b981), Color(0xFF059669))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isLoading) "VERIFYING ACCESS..." else "UNLOCK SYSTEM",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = ColorBackgroundDark
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Cipher_attack"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Text(
                            text = "GET ELITE LICENSE KEY",
                            color = ColorGreenLight,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        modifier = Modifier.padding(top = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val statusColor = if (isLoading) Color(0xFFfbbf24) else ColorGreenPrimary
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLoading) "SYSTEM ENCRYPTING..." else "SYSTEM READY",
                            color = Color(0xFF475569),
                            fontSize = 12.sp,
                            letterSpacing = 0.1.em,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EliteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = ColorTextGray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ColorInputBg,
            unfocusedContainerColor = ColorInputBg,
            focusedBorderColor = ColorGreenPrimary.copy(alpha = 0.5f),
            unfocusedBorderColor = ColorGreenPrimary.copy(alpha = 0.2f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = ColorGreenPrimary
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = true
    )
}

@Composable
fun CipherLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val pathColor = ColorGreenPrimary
        val strokeWidth = 6.dp.toPx()
        val scaleX = size.width / 100f
        val scaleY = size.height / 100f

        val mainPath = Path().apply {
            moveTo(75f * scaleX, 25f * scaleY)
            lineTo(35f * scaleX, 25f * scaleY)
            lineTo(15f * scaleX, 50f * scaleY)
            lineTo(35f * scaleX, 75f * scaleY)
            lineTo(75f * scaleX, 75f * scaleY)
        }
        
        drawPath(
            path = mainPath,
            color = pathColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawCircle(
            color = pathColor,
            center = androidx.compose.ui.geometry.Offset(40f * scaleX, 50f * scaleY),
            radius = 10f * scaleX,
            style = Stroke(width = strokeWidth)
        )

        drawLine(
            color = pathColor,
            start = androidx.compose.ui.geometry.Offset(50f * scaleX, 50f * scaleY),
            end = androidx.compose.ui.geometry.Offset(85f * scaleX, 50f * scaleY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = ColorGreenLight.copy(alpha = 0.6f),
            start = androidx.compose.ui.geometry.Offset(68f * scaleX, 50f * scaleY),
            end = androidx.compose.ui.geometry.Offset(68f * scaleX, 65f * scaleY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}