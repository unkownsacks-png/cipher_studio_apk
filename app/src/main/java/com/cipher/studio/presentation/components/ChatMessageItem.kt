package com.cipher.studio.presentation.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cipher.studio.domain.model.ChatMessage
import com.cipher.studio.domain.model.ChatRole
import com.cipher.studio.domain.model.Theme

@Composable
fun ChatMessageItem(
    msg: ChatMessage,
    isDark: Boolean,
    isStreaming: Boolean = false,
    onSpeak: ((String) -> Unit)? = null,
    onPin: ((String) -> Unit)? = null
) {
    val isUser = msg.role == ChatRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val clipboardManager = LocalClipboardManager.current

    // --- ELITE BUBBLE SHAPES ---
    // User: Rounded everywhere except Bottom-Right (Tail effect)
    // AI: Rounded everywhere except Top-Left
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
    }

    // --- COLORS ---
    // User: Gradient Blue (Telegram Style)
    // AI: Surface Color (Clean Look)
    val backgroundModifier = if (isUser) {
        Modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF2563EB), Color(0xFF3B82F6)) // Cipher Blue Gradient
            ),
            shape = bubbleShape
        )
    } else {
        Modifier.background(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = bubbleShape
        )
    }

    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom, // Align avatar to bottom of message
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(if (isUser) 1f else 0.95f)
        ) {
            // AI Avatar (Only show for model)
            if (!isUser) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.SmartToy,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // --- THE BUBBLE ---
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .then(backgroundModifier)
                    .padding(2.dp) // Slight border effect padding if needed
            ) {
                // 1. Attachments
                if (msg.attachments.isNotEmpty()) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        msg.attachments.forEach { attachment ->
                            val bitmap = remember(attachment.data) {
                                try {
                                    val decodedString = Base64.decode(attachment.data, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size).asImageBitmap()
                                } catch (e: Exception) { null }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Attachment",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                // 2. Text Content
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // We use our existing MarkdownRenderer, but ensure it handles colors correctly
                    MarkdownRenderer(
                        content = msg.text,
                        theme = if (isDark) Theme.DARK else Theme.LIGHT,
                        isStreaming = isStreaming
                    )
                }

                // 3. Footer Actions (Only for AI messages that are finished)
                if (!isUser && !isStreaming) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Copy
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(msg.text)) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copy", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                        }
                        // Speak
                        IconButton(
                            onClick = { onSpeak?.invoke(msg.text) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, "Speak", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}