package com.cipher.studio.presentation.dataanalyst

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme

@Composable
fun DataAnalystScreen(
    theme: Theme,
    viewModel: DataAnalystViewModel = hiltViewModel()
) {
    val isDark = theme == Theme.DARK
    
    // State
    val dataInput by viewModel.dataInput.collectAsState()
    val chartCode by viewModel.chartCode.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    // Colors
    val bgColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color.Black
    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.2f)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = "Data Analyst",
                tint = Color(0xFF22C55E), // Green-500
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Data Analyst",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Turn raw numbers into interactive visual insights.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // --- Main Content (Split View) ---
        // On Mobile, we stack them. Input on Top, Chart on Bottom.
        
        // 1. Input Section
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxWidth()
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DATA SOURCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    TextField(
                        value = dataInput,
                        onValueChange = { viewModel.updateDataInput(it) },
                        placeholder = { 
                            Text(
                                "Paste CSV, JSON, or describe data (e.g., 'Q1 Sales: 50k, Q2: 75k...')",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            ) 
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.handleVisualize(theme) },
                enabled = dataInput.isNotBlank() && !isGenerating,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16A34A), // Green-600
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.PlayArrow, "Visualize")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Visualize Data", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Output Section (WebView Chart)
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0xFF121212) else Color(0xFFF9FAFB))
        ) {
            if (chartCode.isNotBlank()) {
                ChartWebView(htmlContent = chartCode)
                
                // Copy Code Button (Overlay)
                val clipboardManager = LocalClipboardManager.current
                val context = LocalContext.current
                
                IconButton(
                    onClick = { 
                        clipboardManager.setText(AnnotatedString(chartCode))
                        Toast.makeText(context, "HTML Code Copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Code, "Copy Code", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            } else {
                // Placeholder
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.PieChart, null, tint = Color.Gray.copy(0.3f), modifier = Modifier.size(48.dp))
                        Icon(Icons.Default.TrendingUp, null, tint = Color.Gray.copy(0.3f), modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Visualization Dashboard",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray.copy(0.5f)
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChartWebView(htmlContent: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(0x00000000) // Transparent background
            }
        },
        update = { webView ->
            // Using loadDataWithBaseURL ensures better compatibility for local rendering
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}