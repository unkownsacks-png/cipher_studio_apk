package com.cipher.studio.presentation.components

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.cipher.studio.domain.model.Theme
import kotlinx.coroutines.delay

/**
 * CIPHER STUDIO: ULTIMATE MARKDOWN RENDERER
 * Features:
 * 1. Syntax Highlighting (Keywords, Strings, Numbers)
 * 2. Full Inline Styles (Bold, Italic, Inline Code, Links)
 * 3. Amharic Optimization (LineHeight & Regex)
 * 4. macOS Style Code Blocks
 * 5. Optimized Tables & Headers
 */
@Composable
fun MarkdownRenderer(
    content: String,
    theme: Theme,
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier
) {
    // 1. Optimized Parsing with derivedStateOf to prevent jank during streaming
    val blocks by remember(content) {
        derivedStateOf { parseMarkdownBlocks(content) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp) // Better spacing for readability
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    // NEW: Header Support (# H1, ## H2)
                    Text(
                        text = block.text,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineMedium
                            2 -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.titleMedium
                        }.copy(fontWeight = FontWeight.Bold),
                        color = if (theme == Theme.DARK) Color(0xFFE3E3E3) else Color(0xFF1F1F1F)
                    )
                }
                is MarkdownBlock.Code -> {
                    EliteCodeBlock(
                        language = block.language,
                        code = block.content,
                        isDark = theme == Theme.DARK
                    )
                }
                is MarkdownBlock.Table -> {
                    MarkdownTable(
                        rows = block.rows,
                        isDark = theme == Theme.DARK
                    )
                }
                is MarkdownBlock.Image -> {
                    EliteImage(url = block.url, alt = block.altText)
                }
                is MarkdownBlock.Quote -> {
                    // NEW: Block Quote Support (>)
                    Row(modifier = Modifier.intrinsicHeight(IntrinsicSize.Min)) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(Color(0xFF6B7280), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        StyledText(text = block.content, isDark = theme == Theme.DARK, fontSize = 16.sp, isItalic = true)
                    }
                }
                is MarkdownBlock.Rule -> {
                    // NEW: Horizontal Rule (---)
                    Divider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
                }
                is MarkdownBlock.Text -> {
                    StyledText(
                        text = block.content,
                        isDark = theme == Theme.DARK
                    )
                }
            }
        }
    }
}

// --- DATA STRUCTURES ---
sealed class MarkdownBlock {
    data class Header(val text: String, val level: Int) : MarkdownBlock()
    data class Text(val content: String) : MarkdownBlock()
    data class Code(val language: String, val content: String) : MarkdownBlock()
    data class Table(val rows: List<List<String>>) : MarkdownBlock()
    data class Image(val url: String, val altText: String) : MarkdownBlock()
    data class Quote(val content: String) : MarkdownBlock()
    object Rule : MarkdownBlock()
}

// --- PARSER LOGIC ---
fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // 1. Code Block
        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val codeBuilder = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeBuilder.append(lines[i]).append("\n")
                i++
            }
            blocks.add(MarkdownBlock.Code(language, codeBuilder.toString().trimEnd()))
            i++
            continue
        }

        // 2. Table
        if (trimmed.startsWith("|")) {
            val tableRows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trim().startsWith("|")) {
                val row = lines[i].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                if (!row.any { it.contains("---") }) { // Skip separator
                    tableRows.add(row)
                }
                i++
            }
            if (tableRows.isNotEmpty()) blocks.add(MarkdownBlock.Table(tableRows))
            continue
        }

        // 3. Headers (#)
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            val content = trimmed.removePrefix("#".repeat(level)).trim()
            blocks.add(MarkdownBlock.Header(content, level))
            i++
            continue
        }

        // 4. Block Quotes (>)
        if (trimmed.startsWith(">")) {
            val content = trimmed.removePrefix(">").trim()
            blocks.add(MarkdownBlock.Quote(content))
            i++
            continue
        }

        // 5. Horizontal Rule (--- or ***)
        if (trimmed == "---" || trimmed == "***") {
            blocks.add(MarkdownBlock.Rule)
            i++
            continue
        }

        // 6. Images
        val imgMatch = Regex("^!\\[(.*?)\\]\\((.*?)\\)$").find(trimmed)
        if (imgMatch != null) {
            blocks.add(MarkdownBlock.Image(imgMatch.groupValues[2], imgMatch.groupValues[1]))
            i++
            continue
        }

        // 7. Regular Text (Grouped)
        val textBuilder = StringBuilder()
        while (i < lines.size) {
            val nextTrim = lines[i].trim()
            if (nextTrim.startsWith("```") || nextTrim.startsWith("|") || nextTrim.startsWith("#") || nextTrim.startsWith(">") || nextTrim == "---") break
            textBuilder.append(lines[i]).append("\n")
            i++
        }
        if (textBuilder.isNotEmpty()) {
            blocks.add(MarkdownBlock.Text(textBuilder.toString().trimEnd()))
        }
    }
    return blocks
}

// --- ELITE CODE BLOCK (With Syntax Highlighting) ---
@Composable
fun EliteCodeBlock(language: String, code: String, isDark: Boolean) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(isCopied) {
        if (isCopied) {
            delay(2000)
            isCopied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
        // macOS Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF5F56), CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFBD2E), CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(10.dp).background(Color(0xFF27C93F), CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = language.ifEmpty { "CODE" }.uppercase(),
                    style = TextStyle(color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                        isCopied = true
                    }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Crossfade(targetState = isCopied, label = "Copy") { copied ->
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy",
                        tint = if (copied) Color(0xFF4CAF50) else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Code Content with Syntax Highlighting
        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Apply Highlighting
                Text(
                    text = syntaxHighlight(code),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}

// --- NEW: SYNTAX HIGHLIGHTER LOGIC ---
fun syntaxHighlight(code: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val str = code
        // Simple Regex Tokenizer for common keywords
        val keywords = listOf("fun", "val", "var", "return", "if", "else", "for", "while", "class", "object", "package", "import", "def", "function", "const", "let")
        val keywordsRegex = "\\b(${keywords.joinToString("|")})\\b".toRegex()
        val stringRegex = "\".*?\"".toRegex()
        val numberRegex = "\\b\\d+\\b".toRegex()
        val commentRegex = "//.*".toRegex()

        var lastIndex = 0
        // We iterate char by char effectively (mocking a lexer) by matching all regexes
        // For simplicity in this robust version, we'll apply layers.
        
        append(str) // Base text

        // 1. Strings (Green)
        stringRegex.findAll(str).forEach { match ->
            addStyle(SpanStyle(color = Color(0xFFA5D6FF)), match.range.first, match.range.last + 1)
        }
        
        // 2. Keywords (Orange/Purple)
        keywordsRegex.findAll(str).forEach { match ->
            addStyle(SpanStyle(color = Color(0xFFFF7B72), fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
        }

        // 3. Numbers (Blue)
        numberRegex.findAll(str).forEach { match ->
            addStyle(SpanStyle(color = Color(0xFF79C0FF)), match.range.first, match.range.last + 1)
        }

        // 4. Comments (Grey) - Override everything else
        commentRegex.findAll(str).forEach { match ->
            addStyle(SpanStyle(color = Color(0xFF8B949E)), match.range.first, match.range.last + 1)
        }
        
        // Base Color
        addStyle(SpanStyle(color = Color(0xFFC9D1D9)), 0, str.length)
    }
}

// --- ELITE IMAGE (With Loading State) ---
@Composable
fun EliteImage(url: String, alt: String) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = alt,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Gray.copy(alpha = 0.1f)),
        loading = {
            Box(modifier = Modifier.height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        },
        error = {
            Column(
                modifier = Modifier.height(150.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.BrokenImage, null, tint = Color.Gray)
                Text("Failed to load image", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        contentScale = ContentScale.Crop
    )
}

// --- STYLED TEXT (Full Markdown Support) ---
@Composable
fun StyledText(
    text: String,
    isDark: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    isItalic: Boolean = false
) {
    val uriHandler = LocalUriHandler.current
    val color = if (isDark) Color(0xFFE3E3E3) else Color(0xFF1F1F1F)
    val lineHeight = 26.sp

    val annotatedString = buildAnnotatedString {
        // Recursively apply styles: Link -> Code -> Bold -> Italic
        
                // 1. LINKS [text](url) - handle Amharic chars [^\]]+
        val linkRegex = "\\[([^\\]]+)\\]\\(([^\\)]+)\\)".toRegex()
        var lastIndex = 0
        
        linkRegex.findAll(text).forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1
            val linkText = matchResult.groupValues[1]
            val linkUrl = matchResult.groupValues[2]

            // ከሊንኩ በፊት ያለውን ጽሁፍ በስታይል ጨምር
            appendStyles(text.substring(lastIndex, start))

            // ሊንኩን ጨምር
            pushStringAnnotation(tag = "URL", annotation = linkUrl)
            withStyle(SpanStyle(
                color = Color(0xFF3B82F6), 
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Medium
            )) {
                append(linkText)
            }
            pop()
            
            lastIndex = end
        }
        
        // ቀሪውን ጽሁፍ ጨምር
        if (lastIndex < text.length) {
            appendStyles(text.substring(lastIndex))
        }
    }

    ClickableText(
        text = annotatedString,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
            fontFamily = FontFamily.Default
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (e: Exception) {
                        // ሊንኩ ባይሰራ ዝም በል
                    }
                }
        }
    )
}

/**
 * Helper to process Bold (**), Italic (*), and Inline Code (`)
 */
fun androidx.compose.ui.text.AnnotatedString.Builder.appendStyles(rawText: String) {
    // Regex for Bold, Italic, and Inline Code
    val combinedRegex = "(\\*\\*.*?\\*\\*)|(\\*.*?\\*)|(`.*?`)".toRegex()
    var lastIndex = 0

    combinedRegex.findAll(rawText).forEach { match ->
        val start = match.range.first
        val end = match.range.last + 1
        
        // ከስታይሉ በፊት ያለውን ተራ ጽሁፍ ጨምር
        append(rawText.substring(lastIndex, start))
        
        val token = match.value
        when {
            // Bold: **text**
            token.startsWith("**") && token.endsWith("**") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(token.removeSurrounding("**"))
                }
            }
            // Italic: *text*
            token.startsWith("*") && token.endsWith("*") -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(token.removeSurrounding("*"))
                }
            }
            // Inline Code: `text`
            token.startsWith("`") && token.endsWith("`") -> {
                withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color.Gray.copy(alpha = 0.2f),
                    color = Color(0xFFE57373) // Slightly red/pink for inline code visibility
                )) {
                    append(token.removeSurrounding("`"))
                }
            }
        }
        lastIndex = end
    }
    // የቀረውን ጽሁፍ ጨምር
    append(rawText.substring(lastIndex))
}
