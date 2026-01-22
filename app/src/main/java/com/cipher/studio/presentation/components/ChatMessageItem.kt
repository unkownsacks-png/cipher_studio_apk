package com.cipher.studio.presentation.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cipher.studio.domain.model.ChatMessage
import com.cipher.studio.domain.model.ChatRole
import com.cipher.studio.domain.model.Theme

@Composable
fun ChatMessageItem(
    msg: ChatMessage,
    isDark: Boolean,
    isStreaming: Boolean = false,
    isLast: Boolean = false,
    onSpeak: ((String) -> Unit)? = null,
    onPin: ((String) -> Unit)? = null
) {
    val isUser = msg.role == ChatRole.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    
    // Theme Colors
    val bubbleColor = if (isUser) Color(0xFF2563EB) else if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isUser) Color.White else if (isDark) Color(0xFFF1F5F9) else Color(0xFF1F2937)
    val borderColor = if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(if (isUser) 1f else 0.95f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // --- Model Avatar ---
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color(0xFF2563EB), Color(0xFF9333EA))
                            ),
                            shape = CircleShape
                        )
                        .padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SmartToy,
                        contentDescription = "AI",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            // --- Bubble Content ---
            Column(
                modifier = Modifier
                    .widthIn(max = 340.dp) // Max width constraint
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 18.dp
                        )
                    )
                    .background(bubbleColor)
            ) {
                
                // 1. Attachments (Images)
                if (msg.attachments.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(msg.attachments) { attachment ->
                            // Convert Base64 to Bitmap safely
                            // Note: In a real production app, try to use URIs, but since we have base64:
                            val bitmap = remember(attachment.data) {
                                try {
                                    val decodedString = Base64.decode(attachment.data, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                        .asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "Attachment",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // 2. Text Content (Markdown)
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    // Custom Markdown Renderer
                    MarkdownRenderer(
                        content = msg.text,
                        theme = if (isDark) Theme.DARK else Theme.LIGHT,
                        isStreaming = isStreaming
                    )
                }

                // 3. Footer (Grounding & Actions)
                if (!isUser && !isStreaming) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                    ) {
                        // Grounding (Citations)
                        if (msg.groundingMetadata?.groundingChunks?.isNotEmpty() == true) {
                            Text(
                                text = "Sources: " + msg.groundingMetadata.groundingChunks
                                    .mapNotNull { it.web?.title }
                                    .joinToString(", "),
                                fontSize = 11.sp,
                                color = textColor.copy(alpha = 0.6f),
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // Actions Row (Divider + Icons)
                        HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 0.5.dp)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // TTS
                            IconButton(
                                onClick = { onSpeak?.invoke(msg.text) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, "Speak", tint = textColor.copy(0.5f), modifier = Modifier.size(14.dp))
                            }
                            
                            // Pin
                            IconButton(
                                onClick = { onPin?.invoke(msg.id ?: "") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (msg.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = "Pin",
                                    tint = if (msg.pinned) Color(0xFFEAB308) else textColor.copy(0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}