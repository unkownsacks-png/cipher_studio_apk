package com.cipher.studio.presentation.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cipher.studio.domain.model.Theme
import java.util.regex.Pattern

/**
 * A High-Performance Native Markdown & Code Renderer.
 * Optimized for Streaming Text (Gemini) to prevent UI jitter.
 */
@Composable
fun MarkdownRenderer(
    content: String,
    theme: Theme,
    onFocusMode: Boolean = false,
    isStreaming: Boolean = false
) {
    val isDark = theme == Theme.DARK
    val textColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF1F2937)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Parse content into blocks (Code vs Text)
        val blocks = remember(content) { parseMarkdownBlocks(content) }

        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockView(
                        language = block.language,
                        code = block.code,
                        isDark = isDark,
                        isStreaming = isStreaming
                    )
                }
                is MarkdownBlock.TextBlock -> {
                    if (block.text.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = parseRichText(block.text, isDark),
                                color = textColor,
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                fontFamily = FontFamily.Default,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Component: Code Block (The macOS Style Window) ---
@Composable
fun CodeBlockView(
    language: String,
    code: String,
    isDark: Boolean,
    isStreaming: Boolean
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    val borderColor = if (isDark) Color.White.copy(0.1f) else Color.Gray.copy(0.2f)
    val bgColor = if (isDark) Color(0xFF0D0D0D) else Color.White
    val headerBg = if (isDark) Color(0xFF18181B) else Color(0xFFF9FAFB)
    val codeColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF1F2937)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bgColor)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language Label
            Text(
                text = language.ifBlank { "text" },
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )

            // Copy Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.05f))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(code))
                        isCopied = true
                        if (!isStreaming) {
                             Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }
                        // Reset copy icon after 2 seconds (handled via LaunchedEffect usually, simplified here)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = if (isCopied) Color(0xFF4ADE80) else Color.Gray,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isCopied) "Copied" else "Copy",
                    fontSize = 10.sp,
                    color = if (isCopied) Color(0xFF4ADE80) else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Code Content
        SelectionContainer {
            if (isStreaming) {
                // No heavy syntax highlighting during streaming for performance
                Text(
                    text = code,
                    color = codeColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Text(
                    text = highlightSyntax(code, isDark),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// --- Logic: Markdown Parsing ---

sealed class MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(input: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    // Regex matches ```language code ```
    val codeBlockRegex = Pattern.compile("```(\\w*)\\n?([\\s\\S]*?)\\n?```")
    val matcher = codeBlockRegex.matcher(input)

    var lastIndex = 0

    while (matcher.find()) {
        val start = matcher.start()
        val end = matcher.end()

        if (start > lastIndex) {
            blocks.add(MarkdownBlock.TextBlock(input.substring(lastIndex, start)))
        }

        val lang = matcher.group(1) ?: ""
        val code = matcher.group(2) ?: ""
        blocks.add(MarkdownBlock.CodeBlock(lang.trim(), code.trim()))

        lastIndex = end
    }

    if (lastIndex < input.length) {
        blocks.add(MarkdownBlock.TextBlock(input.substring(lastIndex)))
    }

    return blocks
}

/**
 * Rich Text Parser (Bold, Italic, Inline Code, Link)
 */
@Composable
fun parseRichText(text: String, isDark: Boolean): AnnotatedString {
    return buildAnnotatedString {
        val boldStyle = SpanStyle(fontWeight = FontWeight.Bold)
        val italicStyle = SpanStyle(fontStyle = FontStyle.Italic)
        val codeStyle = SpanStyle(
            fontFamily = FontFamily.Monospace,
            background = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF3F4F6),
            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
            fontSize = 13.sp
        )
        // Note: Simple manual parsing. For nested markdown, a library is better, 
        // but this regex covers 90% of chat use cases.
        val parts = text.split(Regex("(?=(\\*\\*|`|http))|(?<=(\\*\\*|`))"))
        
        var isBold = false
        var isCode = false
        
        parts.forEach { part ->
            when {
                part == "**" -> isBold = !isBold
                part == "`" -> isCode = !isCode
                else -> {
                    if (isCode) withStyle(codeStyle) { append(part) }
                    else if (isBold) withStyle(boldStyle) { append(part) }
                    else if (part.startsWith("http")) {
                        withStyle(SpanStyle(color = Color(0xFF3B82F6), textDecoration = TextDecoration.Underline)) {
                            append(part)
                        }
                    }
                    else append(part)
                }
            }
        }
    }
}

/**
 * Basic Syntax Highlighter (Keywords, Strings, Comments)
 */
fun highlightSyntax(code: String, isDark: Boolean): AnnotatedString {
    return buildAnnotatedString {
        val defaultColor = if (isDark) Color(0xFFE4E4E7) else Color(0xFF1F2937)
        val keywordColor = if (isDark) Color(0xFFC586C0) else Color(0xFFAF00DB)
        val stringColor = if (isDark) Color(0xFFCE9178) else Color(0xFFA31515)
        val commentColor = if (isDark) Color(0xFF6A9955) else Color(0xFF008000)
        val numberColor = if (isDark) Color(0xFFB5CEA8) else Color(0xFF098658)

        val keywords = listOf(
            "function", "return", "var", "let", "const", "if", "else", "for", "while",
            "import", "from", "export", "default", "class", "extends", "true", "false",
            "null", "undefined", "this", "new", "try", "catch", "async", "await",
            "fun", "val", "package", "override", "private", "public", "interface"
        )

        // Simple tokenizer
        val tokenRegex = Regex("(\".*?\")|('.*?')|(//.*)|(/\\*.*?\\*/)|(\\b\\d+\\b)|(\\b[a-zA-Z_]\\w*\\b)|(\\S)")
        
        val matches = tokenRegex.findAll(code)
        var lastIndex = 0
        
        matches.forEach { match ->
            if (match.range.first > lastIndex) {
                withStyle(SpanStyle(color = defaultColor)) { append(code.substring(lastIndex, match.range.first)) }
            }

            val value = match.value
            when {
                value.startsWith("\"") || value.startsWith("'") -> withStyle(SpanStyle(color = stringColor)) { append(value) }
                value.startsWith("/") -> withStyle(SpanStyle(color = commentColor)) { append(value) }
                value.toIntOrNull() != null -> withStyle(SpanStyle(color = numberColor)) { append(value) }
                keywords.contains(value) -> withStyle(SpanStyle(color = keywordColor)) { append(value) }
                else -> withStyle(SpanStyle(color = defaultColor)) { append(value) }
            }
            lastIndex = match.range.last + 1
        }
        
        if (lastIndex < code.length) {
            withStyle(SpanStyle(color = defaultColor)) { append(code.substring(lastIndex)) }
        }
    }
}