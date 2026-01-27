package com.cipher.studio.presentation.docintel

import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer // FIX: Added Import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cipher.studio.domain.model.Theme
import java.util.Locale

@Composable
fun DocIntelScreen(
    theme: Theme,
    viewModel: DocIntelViewModel = hiltViewModel()
) {
    val isDark = theme == Theme.DARK
    val docText by viewModel.docText.collectAsState()
    val analysis by viewModel.analysis.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val sentiment by viewModel.sentimentScore.collectAsState()
    val keywords by viewModel.keywords.collectAsState()
    val qaQuery by viewModel.qaQuery.collectAsState()
    val qaAnswer by viewModel.qaAnswer.collectAsState()

    // FEATURE 1: Text to Speech
    val context = LocalContext.current
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale.US }
        onDispose { tts?.shutdown() }
    }

    // Colors
    val bgColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF1E293B)
    val accentColor = Color(0xFFF97316) // Orange Brand

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Description, null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Doc Intel", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text("Enterprise Document Analysis", fontSize = 13.sp, color = Color.Gray)
            }
        }

        // --- FEATURE 3: DROP ZONE / INPUT ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(200.dp)
                .border(1.dp, Color.Gray.copy(0.2f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("SOURCE TEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    if (docText.isNotEmpty()) {
                        Icon(Icons.Rounded.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp).clickable { viewModel.updateDocText("") })
                    }
                }
                
                TextField(
                    value = docText,
                    onValueChange = { viewModel.updateDocText(it) },
                    placeholder = { 
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                            Icon(Icons.Rounded.CloudUpload, null, tint = accentColor.copy(0.5f), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Paste text or drag file here", color = Color.Gray.copy(0.5f))
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = accentColor
                    ),
                    textStyle = TextStyle(fontSize = 14.sp, color = textColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- ACTION GRID ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DocModeChip("Summarize", Icons.Rounded.ShortText, Color(0xFF3B82F6)) { viewModel.runAnalysis(DocAnalysisMode.SUMMARY) }
            DocModeChip("Risks", Icons.Rounded.Warning, Color(0xFFEF4444)) { viewModel.runAnalysis(DocAnalysisMode.RISKS) }
            DocModeChip("Insights", Icons.Rounded.Lightbulb, Color(0xFFEAB308)) { viewModel.runAnalysis(DocAnalysisMode.INSIGHTS) }
            DocModeChip("Contract", Icons.Rounded.Gavel, Color(0xFF8B5CF6)) { viewModel.runAnalysis(DocAnalysisMode.CONTRACT_REVIEW) }
            DocModeChip("Tone", Icons.Rounded.Mood, Color(0xFFEC4899)) { viewModel.runAnalysis(DocAnalysisMode.EMAIL_TONE) }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- FEATURE 2: SMART TABS & OUTPUT ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(cardBg)
        ) {
            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(if(isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResultTab.values().forEach { tab ->
                    val isSel = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if(isSel) cardBg else Color.Transparent)
                            .clickable { viewModel.setActiveTab(tab) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tab.name.take(4), 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = if(isSel) accentColor else Color.Gray
                        )
                    }
                }
            }

            // Content Area
            Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                if (isProcessing) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = accentColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing document structure...", fontSize = 12.sp, color = Color.Gray)
                    }
                } else if (analysis.isNotEmpty()) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        
                        // FEATURE 4: SENTIMENT GAUGE
                        SentimentGauge(sentiment)
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // FEATURE 5: KEYWORDS
                        if (keywords.isNotEmpty()) {
                            Row(modifier = Modifier.wrapContentWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                keywords.forEach { key ->
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, accentColor.copy(0.3f), CircleShape)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(key, fontSize = 10.sp, color = accentColor)
                                    }
                                }
                            }
                        }

                        // Main Text (FIX: Wrapped in SelectionContainer)
                        SelectionContainer {
                            Text(
                                text = analysis,
                                fontSize = 14.sp,
                                lineHeight = 24.sp,
                                color = textColor
                            )
                        }

                        // FEATURE 7: Q&A Section
                        Spacer(modifier = Modifier.height(24.dp))
                        Divider(color = Color.Gray.copy(0.1f))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("ASK THE DOCUMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = qaQuery,
                                onValueChange = { viewModel.updateQaQuery(it) },
                                placeholder = { Text("e.g. What is the deadline?", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            IconButton(onClick = { viewModel.askQuestion() }) {
                                Icon(Icons.Rounded.Send, null, tint = accentColor)
                            }
                        }
                        if (qaAnswer.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .background(accentColor.copy(0.1f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Text(qaAnswer, fontSize = 13.sp, color = textColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
            
            // FEATURE 6: EXPORT BAR
            if (analysis.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Gray.copy(0.1f))
                        .background(cardBg)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { tts?.speak(analysis, TextToSpeech.QUEUE_FLUSH, null, null) }) {
                        Icon(Icons.Rounded.VolumeUp, null, tint = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { /* Simulated Export */ },
                        colors = ButtonDefaults.buttonColors(containerColor = textColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Rounded.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export PDF", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- FEATURE 4: SENTIMENT GAUGE UI ---
@Composable
fun SentimentGauge(score: Int) {
    val animatedScore by animateFloatAsState(targetValue = score / 100f, label = "gauge")
    val color = when {
        score < 40 -> Color(0xFFEF4444) // Negative
        score > 60 -> Color(0xFF22C55E) // Positive
        else -> Color(0xFFEAB308) // Neutral
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("SENTIMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedScore)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text("$score%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun DocModeChip(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}