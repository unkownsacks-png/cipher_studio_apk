package com.cipher.studio.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cipher.studio.domain.model.Session
import com.cipher.studio.domain.model.Theme
import com.cipher.studio.domain.model.ViewMode

@Composable
fun Sidebar(
    sessions: List<Session>,
    currentSessionId: String?,
    onSelectSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onDeleteSession: (String) -> Unit,
    isOpen: Boolean,
    onToggleSidebar: () -> Unit,
    theme: Theme,
    currentView: ViewMode,
    onViewChange: (ViewMode) -> Unit,
    onLogout: () -> Unit
) {
    val isDark = theme == Theme.DARK
    val width = if (isOpen) 280.dp else 70.dp
    
    // Background Colors
    val surfaceColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.95f) else Color.White
    val contentColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f)

    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(surfaceColor)
            .padding(vertical = 12.dp)
            .animateContentSize() // Smooth width transition
    ) {
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = if (isOpen) 16.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isOpen) Arrangement.SpaceBetween else Arrangement.Center
        ) {
            if (isOpen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hexagon, // Placeholder for Cipher Logo
                        contentDescription = "Logo",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cipher Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = contentColor
                    )
                }
            } else {
                IconButton(onClick = onToggleSidebar) {
                    Icon(Icons.Default.Menu, "Menu", tint = contentColor)
                }
            }
            
            if (isOpen) {
                IconButton(onClick = onToggleSidebar) {
                    Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                }
            }
        }

        Divider(color = borderColor, modifier = Modifier.padding(vertical = 8.dp))

        // --- Modules Navigation ---
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            if (isOpen) {
                Text(
                    text = "MODULES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            }

            NavItem(ViewMode.CHAT, Icons.Default.Message, "Chat & Reason", isOpen, currentView, onViewChange, isDark)
            NavItem(ViewMode.PROMPT_STUDIO, Icons.Default.AutoAwesome, "Prompt Studio", isOpen, currentView, onViewChange, isDark)
            NavItem(ViewMode.CODE_LAB, Icons.Default.Code, "Code Lab", isOpen, currentView, onViewChange, isDark)
            NavItem(ViewMode.VISION_HUB, Icons.Default.Visibility, "Vision Hub", isOpen, currentView, onViewChange, isDark)
            
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = borderColor)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cyber House Highlight
            NavItem(ViewMode.CYBER_HOUSE, Icons.Default.Security, "Cyber House", isOpen, currentView, onViewChange, isDark, isHighlight = true)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- History List ---
        if (isOpen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("HISTORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Chat",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.clickable { onNewSession() }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            items(sessions) { session ->
                val isSelected = currentSessionId == session.id && currentView == ViewMode.CHAT
                SessionItem(
                    session = session,
                    isSelected = isSelected,
                    isOpen = isOpen,
                    isDark = isDark,
                    onSelect = { 
                        onSelectSession(session.id)
                        onViewChange(ViewMode.CHAT)
                    },
                    onDelete = { onDeleteSession(session.id) }
                )
            }
        }

        // --- Footer (About & Logout) ---
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            NavItem(ViewMode.ABOUT, Icons.Default.Person, "About Dev", isOpen, currentView, onViewChange, isDark)
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Logout Button
            val logoutBg = if (isDark) Color(0x1AFF0000) else Color(0x1AFF0000)
            val logoutText = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onLogout() }
                    .background(logoutBg)
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isOpen) Arrangement.Start else Arrangement.Center
            ) {
                Icon(Icons.Outlined.Logout, "Logout", tint = logoutText, modifier = Modifier.size(20.dp))
                if (isOpen) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "LOGOUT ELITE",
                        color = logoutText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NavItem(
    view: ViewMode,
    icon: ImageVector,
    label: String,
    isOpen: Boolean,
    currentView: ViewMode,
    onViewChange: (ViewMode) -> Unit,
    isDark: Boolean,
    isHighlight: Boolean = false
) {
    val isSelected = currentView == view
    
    // Dynamic Colors
    val bgColor = when {
        isSelected -> if (isDark) Color(0xFF2563EB).copy(0.2f) else Color(0xFFEFF6FF)
        else -> Color.Transparent
    }
    
    val iconColor = when {
        isHighlight -> Color(0xFFEF4444) // Red for Cyber House
        isSelected -> if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
        else -> if (isDark) Color.Gray else Color(0xFF4B5563)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onViewChange(view) }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isOpen) Arrangement.Start else Arrangement.Center
    ) {
        Icon(icon, label, tint = iconColor, modifier = Modifier.size(20.dp))
        if (isOpen) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isDark) Color.White else Color.Black
            )
        }
    }
}

@Composable
fun SessionItem(
    session: Session,
    isSelected: Boolean,
    isOpen: Boolean,
    isDark: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val bgColor = if (isSelected) {
        if (isDark) Color.White.copy(0.1f) else Color.White
    } else Color.Transparent
    
    val textColor = if (isSelected) {
        if (isDark) Color.White else Color.Black
    } else Color.Gray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onSelect() }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isOpen) Arrangement.SpaceBetween else Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                "Chat",
                tint = if (isSelected) Color(0xFF2563EB) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            
            if (isOpen) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = session.title,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
            }
        }
        
        if (isOpen && isSelected) {
            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
            }
        }
    }
}