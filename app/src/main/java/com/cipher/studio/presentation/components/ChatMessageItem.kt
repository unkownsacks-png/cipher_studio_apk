package com.cipher.studio.presentation.components

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
    onRegenerate: (() -> Unit)? = null,
    onEdit: ((String) -> Unit)? = null // Added Edit Callback
) {
    val isUser = msg.role == ChatRole.USER
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // --- GEMINI STYLE LAYOUT ---
    
    if (isUser) {
        // ==========================================
        // USER MESSAGE STYLE (Modern Pill + Edit)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                
                // FEATURE 1: EDIT BUTTON (For User)
                // Allows quick correction of prompts
                IconButton(
                    onClick = { onEdit?.invoke(msg.text) },
                    modifier = Modifier.size(32.dp).padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    // Attachments (Images) for User
                    if (msg.attachments.isNotEmpty()) {
                        UserAttachments(msg)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // The Text Pill
                    Surface(
                        shape = RoundedCornerShape(24.dp), 
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
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
        }

    } else {
        // ==========================================
        // AI MESSAGE STYLE (Clean, No Icon)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 20.dp), // Increased padding since icon is gone
            verticalAlignment = Alignment.Top 
        ) {
            // REMOVED: The "Sparkle" Icon is gone. 
            // Now it's just pure content flow like Gemini Web.

            // The Content & Actions
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

                // The Utility/Action Row (Floating below text)
                if (!isStreaming && msg.text.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AIActionRow(
                        text = msg.text,
                        onCopy = { 
                            clipboardManager.setText(AnnotatedString(msg.text))
                            // FEATURE 4: CLIPBOARD FEEDBACK
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onSpeak = { onSpeak?.invoke(msg.text) },
                        onRegenerate = onRegenerate,
                        onShare = {
                            // FEATURE 2: SHARE FUNCTIONALITY
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, msg.text)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }
                    )
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
private fun AIActionRow(
    text: String,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: (() -> Unit)?
) {
    // FEATURE 3: INTERACTIVE STATE
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Speak
        ActionIcon(icon = Icons.Outlined.VolumeUp, description = "Read Aloud", onClick = onSpeak)
        
        // Copy
        ActionIcon(icon = Icons.Outlined.ContentCopy, description = "Copy", onClick = onCopy)

        // Share (NEW)
        ActionIcon(icon = Icons.Outlined.Share, description = "Share", onClick = onShare)
        
        Spacer(modifier = Modifier.width(16.dp))

        // Thumbs Up (Interactive)
        IconButton(onClick = { isLiked = !isLiked; isDisliked = false }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                contentDescription = "Good",
                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Thumbs Down (Interactive)
        IconButton(onClick = { isDisliked = !isDisliked; isLiked = false }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                contentDescription = "Bad",
                tint = if (isDisliked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Regenerate
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
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}