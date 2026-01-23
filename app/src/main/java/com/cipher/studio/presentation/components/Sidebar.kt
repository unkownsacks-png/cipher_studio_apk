package com.cipher.studio.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cipher.studio.R
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
    onLogout: () -> Unit,
    onOpenSettings: () -> Unit // NEW CALLBACK
) {
    val isDark = theme == Theme.DARK
    val width = if (isOpen) 280.dp else 72.dp // Adjusted width
    val surfaceColor = if (isDark) Color(0xFF0F172A).copy(alpha = 0.98f) else Color.White
    val contentColor = if (isDark) Color.White else Color.Black

    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(surfaceColor)
            .padding(vertical = 12.dp)
            .animateContentSize()
    ) {
        // --- Header with Logo ---
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
                    // YOUR LOGO HERE
                    Image(
                        painter = painterResource(id = R.drawable.my_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Cipher Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = contentColor
                    )
                }
                IconButton(onClick = onToggleSidebar) {
                    Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                }
            } else {
                IconButton(onClick = onToggleSidebar) {
                    Icon(Icons.Default.Menu, "Menu", tint = contentColor)
                }
            }
        }

        HorizontalDivider(color = Color.Gray.copy(0.1f), modifier = Modifier.padding(vertical = 8.dp))

        // --- Modules ---
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            NavItem(ViewMode.CHAT, Icons.Default.ChatBubbleOutline, "Chat", isOpen, currentView, onViewChange, isDark)
            NavItem(ViewMode.CODE_LAB, Icons.Default.Code, "Code Lab", isOpen, currentView, onViewChange, isDark)
            NavItem(ViewMode.VISION_HUB, Icons.Default.Visibility, "Vision Hub", isOpen, currentView, onViewChange, isDark)
            NavItem(ViewMode.PROMPT_STUDIO, Icons.Default.AutoAwesome, "Prompt Studio", isOpen, currentView, onViewChange, isDark)
            NavItem(ViewMode.CYBER_HOUSE, Icons.Default.Security, "Cyber House", isOpen, currentView, onViewChange, isDark)
            NavItem(ViewMode.DATA_ANALYST, Icons.Default.Analytics, "Data Analyst", isOpen, currentView, onViewChange, isDark)
            NavItem(ViewMode.DOC_INTEL, Icons.Default.Description, "Doc Intel", isOpen, currentView, onViewChange, isDark)
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- Footer (Settings & Logout) ---
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            
            // Settings Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenSettings() }
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isOpen) Arrangement.Start else Arrangement.Center
            ) {
                Icon(Icons.Default.Settings, "Settings", tint = Color(0xFF60A5FA), modifier = Modifier.size(22.dp))
                if (isOpen) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("API Settings", fontSize = 14.sp, color = contentColor)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Logout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onLogout() }
                    .background(Color(0xFFEF4444).copy(0.1f))
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isOpen) Arrangement.Start else Arrangement.Center
            ) {
                Icon(Icons.Outlined.Logout, "Logout", tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                if (isOpen) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Logout Elite", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
            }
        }
    }
}

// Reuse NavItem and SessionItem from previous code...
@Composable
fun NavItem(
    view: ViewMode,
    icon: ImageVector,
    label: String,
    isOpen: Boolean,
    currentView: ViewMode,
    onViewChange: (ViewMode) -> Unit,
    isDark: Boolean
) {
    val isSelected = currentView == view
    val bgColor = if (isSelected) (if (isDark) Color(0xFF2563EB).copy(0.2f) else Color(0xFFEFF6FF)) else Color.Transparent
    val iconColor = if (isSelected) (if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)) else Color.Gray

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
        Icon(icon, label, tint = iconColor, modifier = Modifier.size(22.dp))
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