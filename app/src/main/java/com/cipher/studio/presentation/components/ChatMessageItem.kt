package com.cipher.studio.presentation.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.cipher.studio.domain.model.ChatMessage
import com.cipher.studio.domain.model.ChatRole
import com.cipher.studio.domain.model.Theme

@Composable
fun ChatMessageItem(
    msg: ChatMessage,
    isDark: Boolean,
    isStreaming: Boolean = false,
    onSpeak: ((String) -> Unit)? = null,
    onPin: ((String) -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null
) {
    val isUser = msg.role == ChatRole.USER
    val clipboardManager = LocalClipboardManager.current

    // --- GEMINI STYLE LAYOUT ---
    
    if (isUser) {
        // ==========================================
        // USER MESSAGE STYLE (Modern Pill)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(horizontalAlignment = Alignment.End) {
                // Attachments (Images) for User
                if (msg.attachments.isNotEmpty()) {
                    UserAttachments(msg)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // The Text Pill
                Surface(
                    shape = RoundedCornerShape(24.dp), // Pill Shape
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), // Subtle Gray/Dark
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.widthIn(max = 340.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

    } else {
        // ==========================================
        // AI MESSAGE STYLE (Clean, No Bubble)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            // FIXED: Used verticalAlignment instead of crossAxisAlignment
            verticalAlignment = Alignment.Top 
        ) {
            // 1. The Gemini Icon (Sparkles)
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "AI",
                tint = if (isStreaming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 4.dp) // Align with first line of text
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 2. The Content & Actions
            Column(modifier = Modifier.weight(1f)) {
                
                // AI Attachments (if any)
                if (msg.attachments.isNotEmpty()) {
                    UserAttachments(msg)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // The Text (Markdown) - No Container!
                SelectionContainer {
                    MarkdownRenderer(
                        content = msg.text,
                        theme = if (isDark) Theme.DARK else Theme.LIGHT,
                        isStreaming = isStreaming
                    )
                }

                // 3. The Utility/Action Row (Floating below text)
                // Only show if not streaming and text is not empty
                if (!isStreaming && msg.text.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AIActionRow(
                        onCopy = { clipboardManager.setText(AnnotatedString(msg.text)) },
                        onSpeak = { onSpeak?.invoke(msg.text) },
                        onRegenerate = onRegenerate
                    )
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
private fun AIActionRow(
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onRegenerate: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Speak
        ActionIcon(icon = Icons.Outlined.VolumeUp, description = "Read Aloud", onClick = onSpeak)
        
        // Copy
        ActionIcon(icon = Icons.Outlined.ContentCopy, description = "Copy", onClick = onCopy)
        
        // Thumbs (Placeholder for feedback)
        ActionIcon(icon = Icons.Outlined.ThumbUp, description = "Good Response", onClick = {})
        ActionIcon(icon = Icons.Outlined.ThumbDown, description = "Bad Response", onClick = {})
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Regenerate (Optional)
        if (onRegenerate != null) {
            ActionIcon(icon = Icons.Default.Refresh, description = "Regenerate", onClick = onRegenerate)
        }
    }
}

@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp) // Smaller touch target
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp) // Elegant small icons
        )
    }
}

@Composable
private fun UserAttachments(msg: ChatMessage) {
    Column {
        msg.attachments.forEach { attachment ->
            val bitmap = remember(attachment.data) {
                try {
                    val decodedString = Base64.decode(attachment.data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size).asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Attachment",
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(12.dp)) // Modern rounded corners
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}