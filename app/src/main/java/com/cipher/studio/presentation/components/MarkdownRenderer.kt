package com.cipher.studio.presentation.components

import android.util.Log
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
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.ContentCopy
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
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.cipher.studio.domain.model.Theme
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * CIPHER STUDIO: ULTIMATE MARKDOWN RENDERER (PRODUCTION READY)
 *
 * UPDATES:
 * 1. Infinite Loop Guard: Prevents app freeze/crash on malformed markdown.
 * 2. HTML Support: Renders <b>, <i>, <u>, <s>, <br> tags.
 * 3. Strict List Parsing: Handles cases like "1.Text" (no space) gracefully.
 */

// --- COMPATIBILITY WRAPPER ---
@Composable
fun MarkdownRenderer(
    content: String,
    theme: Theme,
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Generate a static ID to avoid UI flickering
    val baseId = remember { UUID.randomUUID().toString() }
    
    // Optimized: Only re-parse if content actually changes significantly to reduce lag
    val blocks = remember(content) { 
        parseMarkdownBlocks(content, baseId) 
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        blocks.forEach { block ->
            AiBlockRenderer(block, isDark = theme == Theme.DARK)
        }
    }
}

// --- DATA STRUCTURES (Stable IDs) ---
sealed class MarkdownBlock {
    abstract val id: String

    data class Header(val text: String, val level: Int, override val id: String) : MarkdownBlock()
    data class Text(val content: String, override val id: String) : MarkdownBlock()
    data class Code(val language: String, val content: String, override val id: String) : MarkdownBlock()
    data class Table(val rows: List<List<String>>, override val id: String) : MarkdownBlock()
    data class Image(val url: String, val altText: String, override val id: String) : MarkdownBlock()
    data class Quote(val content: String, override val id: String) : MarkdownBlock()
    data class ListItem(val content: String, val isOrdered: Boolean, override val id: String) : MarkdownBlock()
    data class TaskItem(val content: String, val isChecked: Boolean, override val id: String) : MarkdownBlock()
    object Rule : MarkdownBlock() {
        override val id: String = "rule_static"
    }
}

// --- BULLETPROOF PARSER LOGIC ---
fun parseMarkdownBlocks(text: String, baseId: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()

    // Safety check: Empty text
    if (text.isBlank()) return emptyList()

    val lines = text.lines()
    var i = 0
    var blockIndex = 0

    fun getStableId() = "${baseId}_blk_${blockIndex++}"

    while (i < lines.size) {
        // [CRITICAL FIX]: Loop Guard Variable
        val startIndex = i

        try {
            val line = lines[i]
            val trimmed = line.trim()

            // 1. CODE BLOCK
            if (trimmed.startsWith("```")) {
                val language = trimmed.removePrefix("```").trim()
                val codeBuilder = StringBuilder()
                i++ 

                while (i < lines.size) {
                    if (lines[i].trim().startsWith("```")) {
                        i++ 
                        break
                    }
                    codeBuilder.append(lines[i]).append("\n")
                    i++
                }
                blocks.add(MarkdownBlock.Code(language, codeBuilder.toString().trimEnd(), getStableId()))
                continue
            }

            // 2. TABLE
            if (trimmed.startsWith("|")) {
                // Peek next line safely
                if (i + 1 < lines.size && lines[i+1].trim().contains("---")) {
                    val tableRows = mutableListOf<List<String>>()
                    while (i < lines.size && lines[i].trim().startsWith("|")) {
                        val rowContent = lines[i].trim()
                        if (!rowContent.contains("---")) {
                            val cells = rowContent.split("|")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            if (cells.isNotEmpty()) {
                                tableRows.add(cells)
                            }
                        }
                        i++
                    }
                    if (tableRows.isNotEmpty()) {
                        blocks.add(MarkdownBlock.Table(tableRows, getStableId()))
                        continue
                    }
                }
            }

            // 3. HORIZONTAL RULE
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                blocks.add(MarkdownBlock.Rule)
                i++
                continue
            }

            // 4. TASK LIST
            val taskRegex = Regex("^[-*]\\s\\[([ xX])\\]\\s(.*)")
            val taskMatch = taskRegex.find(trimmed)
            if (taskMatch != null) {
                val isChecked = taskMatch.groupValues[1].equals("x", ignoreCase = true)
                val content = taskMatch.groupValues[2]
                blocks.add(MarkdownBlock.TaskItem(content, isChecked, getStableId()))
                i++
                continue
            }

            // 5. LISTS
            if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("+ ")) {
                val content = trimmed.substring(2).trim()
                blocks.add(MarkdownBlock.ListItem(content, isOrdered = false, getStableId()))
                i++
                continue
            }
            // [FIX]: Ensure space exists after number to avoid conflict with text
            val orderedRegex = Regex("^\\d+\\.\\s(.*)")
            val orderedMatch = orderedRegex.find(trimmed)
            if (orderedMatch != null) {
                blocks.add(MarkdownBlock.ListItem(orderedMatch.groupValues[1], isOrdered = true, getStableId()))
                i++
                continue
            }

            // 6. HEADERS
            if (trimmed.startsWith("#")) {
                val level = trimmed.takeWhile { it == '#' }.length
                val content = trimmed.removePrefix("#".repeat(level)).trim()
                blocks.add(MarkdownBlock.Header(content, level, getStableId()))
                i++
                continue
            }

            // 7. BLOCK QUOTES
            if (trimmed.startsWith(">")) {
                val content = trimmed.removePrefix(">").trim()
                blocks.add(MarkdownBlock.Quote(content, getStableId()))
                i++
                continue
            }

            // 8. IMAGES
            // Try-catch specific to regex to avoid crashes on partial streaming
            try {
                val imgMatch = Regex("^!\\[(.*?)\\]\\((.*?)\\)$").find(trimmed)
                if (imgMatch != null) {
                    blocks.add(MarkdownBlock.Image(imgMatch.groupValues[2], imgMatch.groupValues[1], getStableId()))
                    i++
                    continue
                }
            } catch (e: Exception) {
                // Ignore regex error and treat as text
            }

            // 9. REGULAR TEXT
            val textBuilder = StringBuilder()
            while (i < lines.size) {
                val nextTrim = lines[i].trim()
                // [FIX]: Lookahead includes \\s to ensure text blocks consume "1.Text" (no space)
                // This prevents the infinite loop where text says "It's a list" but list says "No space".
                if (nextTrim.startsWith("```") || 
                    (nextTrim.startsWith("|") && i + 1 < lines.size && lines[i+1].contains("---")) || 
                    nextTrim.startsWith("#") || 
                    nextTrim.startsWith(">") || 
                    nextTrim == "---" || 
                    nextTrim.startsWith("- [") || 
                    nextTrim.startsWith("* ") || 
                    nextTrim.startsWith("- ") || 
                    nextTrim.matches(Regex("^\\d+\\.\\s.*"))) {
                    break
                }
                textBuilder.append(lines[i]).append("\n")
                i++
            }

            if (textBuilder.isNotEmpty()) {
                blocks.add(MarkdownBlock.Text(textBuilder.toString().trimEnd(), getStableId()))
            } else if (i < lines.size && lines[i].isBlank()) {
                i++
            }

            // [CRITICAL FIX]: THE SAFETY NET
            // If logic failed to advance 'i', forcefully consume the line as text.
            // This guarantees the loop will NEVER be infinite.
            if (i == startIndex) {
                Log.w("MarkdownParser", "Loop stuck at line $i. Force consuming.")
                blocks.add(MarkdownBlock.Text(lines[i], getStableId()))
                i++
            }

        } catch (e: Exception) {
            // GLOBAL SAFETY NET: If any line crashes the parser, skip the line or render as text
            // instead of crashing the app.
            Log.e("MarkdownParser", "Error parsing line $i: ${e.message}")
            try {
                if (i < lines.size) {
                    blocks.add(MarkdownBlock.Text(lines[i], getStableId()))
                    i++
                } else {
                    break
                }
            } catch (e2: Exception) {
                break // Give up if fallback fails
            }
        }
    }
    return blocks
}

// --- SHARED BLOCK RENDERER ---
@Composable
fun AiBlockRenderer(block: MarkdownBlock, isDark: Boolean) {
    val theme = if(isDark) Theme.DARK else Theme.LIGHT

    Column(modifier = Modifier.fillMaxWidth()) {
        when (block) {
            is MarkdownBlock.Header -> MarkdownHeader(block.text, block.level, theme)
            is MarkdownBlock.Code -> EliteCodeBlock(block.language, block.content, isDark)
            is MarkdownBlock.Text -> StyledText(block.content, isDark)
            is MarkdownBlock.Table -> MarkdownTable(block.rows, isDark)
            is MarkdownBlock.Quote -> MarkdownQuote(block.content, theme)
            is MarkdownBlock.ListItem -> MarkdownListItem(block.content, block.isOrdered, theme)
            is MarkdownBlock.TaskItem -> MarkdownTaskItem(block.content, block.isChecked, theme)
            is MarkdownBlock.Image -> EliteImage(block.url, block.altText)
            is MarkdownBlock.Rule -> Divider(color = Color.Gray.copy(0.3f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

// --- UI COMPONENTS ---

@Composable
fun MarkdownHeader(text: String, level: Int, theme: Theme) {
    Text(
        text = text,
        style = when (level) {
            1 -> MaterialTheme.typography.headlineMedium
            2 -> MaterialTheme.typography.titleLarge
            else -> MaterialTheme.typography.titleMedium
        }.copy(fontWeight = FontWeight.Bold),
        color = if (theme == Theme.DARK) Color(0xFFE3E3E3) else Color(0xFF1F1F1F),
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

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
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
    ) {
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
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                        isCopied = true
                    }
                    .padding(4.dp)
            ) {
                Crossfade(targetState = isCopied, label = "Copy") { copied ->
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy",
                        tint = if (copied) Color(0xFF4CAF50) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = syntaxHighlight(code),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFFC9D1D9)
                    )
                )
            }
        }
    }
}

@Composable
fun MarkdownQuote(content: String, theme: Theme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color(0xFF6B7280), RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        StyledText(text = content, isDark = theme == Theme.DARK, fontSize = 16.sp, isItalic = true)
    }
}

@Composable
fun MarkdownListItem(content: String, isOrdered: Boolean, theme: Theme) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (theme == Theme.DARK) Color(0xFFE3E3E3) else Color(0xFF1F1F1F),
            modifier = Modifier.padding(end = 12.dp)
        )
        StyledText(text = content, isDark = theme == Theme.DARK)
    }
}

@Composable
fun MarkdownTaskItem(content: String, isChecked: Boolean, theme: Theme) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (isChecked) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        StyledText(
            text = content,
            isDark = theme == Theme.DARK,
            isStrikethrough = isChecked
        )
    }
}

// --- SAFEST SYNTAX HIGHLIGHTER ---
fun syntaxHighlight(code: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        append(code) // Base text first

        try {
            val keywords = listOf("fun", "val", "var", "return", "if", "else", "for", "while", "class", "object", "package", "import", "def", "function", "const", "let", "echo", "cd", "sudo", "docker", "pip", "npm", "public", "private", "protected", "void", "int", "string", "boolean")
            val keywordsRegex = "\\b(${keywords.joinToString("|")})\\b".toRegex()
            val stringRegex = "\".*?\"|\'.*?\'".toRegex()
            val numberRegex = "\\b\\d+\\b".toRegex()
            val commentRegex = "//.*|#.*|/\\*.*?\\*/".toRegex(RegexOption.DOT_MATCHES_ALL)

            // Safe application of styles
            stringRegex.findAll(code).forEach { match ->
                if (match.range.last < length) addStyle(SpanStyle(color = Color(0xFFA5D6FF)), match.range.first, match.range.last + 1)
            }
            keywordsRegex.findAll(code).forEach { match ->
                if (match.range.last < length) addStyle(SpanStyle(color = Color(0xFFFF7B72), fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
            }
            numberRegex.findAll(code).forEach { match ->
                if (match.range.last < length) addStyle(SpanStyle(color = Color(0xFF79C0FF)), match.range.first, match.range.last + 1)
            }
            commentRegex.findAll(code).forEach { match ->
                if (match.range.last < length) addStyle(SpanStyle(color = Color(0xFF8B949E)), match.range.first, match.range.last + 1)
            }
        } catch (e: Exception) {
            // If syntax highlighting fails, we just show plain code. No crash.
        }
    }
}

// --- ELITE IMAGE ---
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
                Spacer(modifier = Modifier.height(8.dp))
                Text("Failed to load image", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        },
        contentScale = ContentScale.Crop
    )
}

// --- STYLED TEXT (With Crash Protection & HTML Support) ---
@Composable
fun StyledText(
    text: String,
    isDark: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    isItalic: Boolean = false,
    isStrikethrough: Boolean = false
) {
    val uriHandler = LocalUriHandler.current
    val color = if (isDark) Color(0xFFE3E3E3) else Color(0xFF1F1F1F)
    val lineHeight = 32.sp 

    val annotatedString = remember(text, isDark) {
        try {
            buildAnnotatedString {
                val linkRegex = "\\[([^\\]]+)\\]\\(([^\\)]+)\\)".toRegex()
                var lastIndex = 0

                linkRegex.findAll(text).forEach { matchResult ->
                    val start = matchResult.range.first
                    val end = matchResult.range.last + 1

                    if (start > lastIndex) {
                        appendStyles(text.substring(lastIndex, start), isDark)
                    }

                    val linkText = matchResult.groupValues[1]
                    val linkUrl = matchResult.groupValues[2]

                    pushStringAnnotation(tag = "URL", annotation = linkUrl)
                    withStyle(SpanStyle(color = Color(0xFF3B82F6), textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)) {
                        append(linkText)
                    }
                    pop()
                    lastIndex = end
                }

                if (lastIndex < text.length) {
                    appendStyles(text.substring(lastIndex), isDark)
                }
            }
        } catch (e: Exception) {
            // Fallback to plain text if parsing crashes
            buildAnnotatedString { append(text) }
        }
    }

    ClickableText(
        text = annotatedString,
        style = TextStyle(
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontFamily = FontFamily.Default,
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (isStrikethrough) TextDecoration.LineThrough else TextDecoration.None
        ),
        onClick = { offset ->
            try {
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        uriHandler.openUri(annotation.item)
                    }
            } catch (e: Exception) {
                // Ignore click errors
            }
        }
    )
}

// --- HELPER: SAFE STYLE PARSER (NOW WITH HTML SUPPORT) ---
fun androidx.compose.ui.text.AnnotatedString.Builder.appendStyles(rawText: String, isDark: Boolean) {
    try {
        // UPDATED REGEX: Matches Markdown and HTML tags
        val pattern = "(`[^`]+`|\\*\\*[^*]+\\*\\*|\\*[^*]+\\*|~~[^~]+~~|<b>.*?</b>|<strong>.*?</strong>|<i>.*?</i>|<em>.*?</em>|<u>.*?</u>|<s>.*?</s>|<strike>.*?</strike>|<br\\s*/?>)".toRegex(RegexOption.IGNORE_CASE)
        var lastIndex = 0

        pattern.findAll(rawText).forEach { match ->
            if (match.range.first > lastIndex) {
                append(rawText.substring(lastIndex, match.range.first))
            }

            val content = match.value
            when {
                // Markdown Code
                content.startsWith("`") -> {
                    val cleanText = content.removeSurrounding("`")
                    withStyle(SpanStyle(background = if (isDark) Color(0xFF3E3E3E) else Color(0xFFE5E7EB), color = if (isDark) Color(0xFFE3E3E3) else Color(0xFF1F1F1F), fontFamily = FontFamily.Monospace, fontSize = 14.sp)) { 
                        append(" $cleanText ") 
                    }
                }
                // Markdown Bold
                content.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(content.removeSurrounding("**")) }
                // Markdown Italic
                content.startsWith("*") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(content.removeSurrounding("*")) }
                // Markdown Strikethrough
                content.startsWith("~~") -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(content.removeSurrounding("~~")) }
                
                // HTML Bold
                content.startsWith("<b>", ignoreCase = true) -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(content.replace(Regex("</?b>", RegexOption.IGNORE_CASE), "")) }
                content.startsWith("<strong>", ignoreCase = true) -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(content.replace(Regex("</?strong>", RegexOption.IGNORE_CASE), "")) }
                
                // HTML Italic
                content.startsWith("<i>", ignoreCase = true) -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(content.replace(Regex("</?i>", RegexOption.IGNORE_CASE), "")) }
                content.startsWith("<em>", ignoreCase = true) -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(content.replace(Regex("</?em>", RegexOption.IGNORE_CASE), "")) }
                
                // HTML Underline
                content.startsWith("<u>", ignoreCase = true) -> withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(content.replace(Regex("</?u>", RegexOption.IGNORE_CASE), "")) }

                // HTML Strikethrough
                content.startsWith("<s>", ignoreCase = true) -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(content.replace(Regex("</?s>", RegexOption.IGNORE_CASE), "")) }
                content.startsWith("<strike>", ignoreCase = true) -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(content.replace(Regex("</?strike>", RegexOption.IGNORE_CASE), "")) }

                // HTML Line Break
                content.startsWith("<br", ignoreCase = true) -> append("\n")
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < rawText.length) {
            append(rawText.substring(lastIndex))
        }
    } catch (e: Exception) {
        append(rawText) // Fallback
    }
}

// --- MARKDOWN TABLE ---
@Composable
fun MarkdownTable(rows: List<List<String>>, isDark: Boolean) {
    val borderColor = if (isDark) Color.Gray.copy(alpha = 0.3f) else Color.LightGray
    val headerColor = if (isDark) Color(0xFF2D2E35) else Color(0xFFF3F4F6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .background(if (index == 0) headerColor else Color.Transparent)
                    .fillMaxWidth()
            ) {
                row.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .widthIn(min = 120.dp)
                            .padding(12.dp)
                            .border(width = 0.5.dp, color = borderColor.copy(alpha = 0.1f))
                    ) {
                        StyledText(text = cell, isDark = isDark, fontSize = 14.sp)
                    }
                }
            }
            if (index < rows.size - 1) {
                Divider(color = borderColor, thickness = 1.dp)
            }
        }
    }
}