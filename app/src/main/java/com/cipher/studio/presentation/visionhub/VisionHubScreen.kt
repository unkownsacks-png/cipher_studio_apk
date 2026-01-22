package com.cipher.studio.presentation.visionhub

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme

@Composable
fun VisionHubScreen(
    theme: Theme,
    viewModel: VisionHubViewModel = hiltViewModel()
) {
    val isDark = theme == Theme.DARK
    val selectedImage by viewModel.selectedImage.collectAsState()
    val result by viewModel.result.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()

    // Gallery Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    // Colors
    val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.2f)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(bgColor)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
            Icon(Icons.Default.Visibility, "Vision", tint = Color(0xFFA855F7), modifier = Modifier.size(28.dp)) // Purple
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Vision Hub", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text("Advanced image analysis & extraction.", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Row(modifier = Modifier.weight(1f)) {
            // --- LEFT COLUMN: Upload & Actions ---
            Column(modifier = Modifier.weight(0.4f).fillMaxHeight()) {
                // Image Placeholder / Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.3f), RoundedCornerShape(16.dp))
                        .clickable { launcher.launch("image/*") }
                        .background(if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImage != null) {
                        val bitmap = remember(selectedImage) {
                            val decodedBytes = Base64.decode(selectedImage!!.data, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size).asImageBitmap()
                        }
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Uploaded Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudUpload, "Upload", tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Text("Click to Upload Image", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("JPG, PNG, WEBP", fontSize = 12.sp, color = Color.Gray.copy(0.7f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VisionActionButton(
                            text = "Describe",
                            icon = Icons.Default.Image,
                            color = Color(0xFF3B82F6), // Blue
                            isEnabled = selectedImage != null && !isAnalyzing,
                            onClick = { viewModel.analyzeImage(VisionMode.DESCRIBE) },
                            modifier = Modifier.weight(1f)
                        )
                        VisionActionButton(
                            text = "OCR Text",
                            icon = Icons.Default.Description, // ScanText equiv
                            color = Color(0xFF22C55E), // Green
                            isEnabled = selectedImage != null && !isAnalyzing,
                            onClick = { viewModel.analyzeImage(VisionMode.EXTRACT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VisionActionButton(
                            text = "Code Ext.",
                            icon = Icons.Default.Code,
                            color = Color(0xFFF97316), // Orange
                            isEnabled = selectedImage != null && !isAnalyzing,
                            onClick = { viewModel.analyzeImage(VisionMode.CODE) },
                            modifier = Modifier.weight(1f)
                        )
                        VisionActionButton(
                            text = "Threat Detect",
                            icon = Icons.Default.Security, // ShieldAlert equiv
                            color = Color(0xFFEF4444), // Red
                            isEnabled = selectedImage != null && !isAnalyzing,
                            onClick = { viewModel.analyzeImage(VisionMode.THREAT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- RIGHT COLUMN: Result Area ---
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .background(cardBg)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(if (isDark) Color.White.copy(0.05f) else Color.Gray.copy(0.1f))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "ANALYSIS RESULT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                }

                // Content
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                    if (isAnalyzing) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF3B82F6))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Vision model is analyzing...", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else if (result.isNotEmpty()) {
                        val scrollState = rememberScrollState()
                        Text(
                            text = result,
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.verticalScroll(scrollState)
                        )
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Visibility, "Empty", tint = Color.Gray.copy(0.3f), modifier = Modifier.size(64.dp))
                            Text("Upload an image and select a tool", color = Color.Gray.copy(0.5f), fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VisionActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.1f),
            contentColor = color,
            disabledContainerColor = Color.Gray.copy(alpha = 0.1f),
            disabledContentColor = Color.Gray
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = modifier
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}