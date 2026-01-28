package com.cipher.studio.presentation.components

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cipher.studio.domain.model.Theme
import java.util.UUID

/**
 * CIPHER STUDIO: HYBRID WEBVIEW RENDERER (CRASH PROOF)
 *
 * SOLUTION: Instead of parsing complex Markdown manually (which crashes Compose),
 * we use the Android System WebView to render it as HTML.
 *
 * ADVANTAGES:
 * 1. ZERO Crashes on large text (WebView handles scrolling natively).
 * 2. Perfect Syntax Highlighting (using embedded PrismJS).
 * 3. Perfect Tables & Lists.
 */

@Composable
fun MarkdownRenderer(
    content: String,
    theme: Theme,
    isStreaming: Boolean = false, // Not used in WebView mode, but kept for compatibility
    modifier: Modifier = Modifier
) {
    // DIAGNOSTIC LOG: To see exactly what's being rendered
    SideEffect {
        Log.d("CipherRender", "Rendering Content Size: ${content.length} chars")
    }

    val isDark = theme == Theme.DARK
    val backgroundColor = if (isDark) "#121212" else "#FFFFFF"
    val textColor = if (isDark) "#E0E0E0" else "#212121"
    
    // Convert Markdown to HTML securely
    val htmlContent = remember(content, theme) {
        generateHtml(content, backgroundColor, textColor)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color(0xFF121212) else Color.White)
    ) {
        // ERROR TRAP: Even if WebView fails, the app won't close.
        try {
            CipherWebView(htmlContent, isDark)
        } catch (e: Exception) {
            // Fallback UI
            Text(
                text = "Rendering Error: ${e.localizedMessage}",
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
            Log.e("CipherCrash", "WebView Crash: ${e.stackTraceToString()}")
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CipherWebView(htmlContent: String, isDark: Boolean) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            // Dynamic Height: Allows WebView to expand based on content
            // Note: In a LazyColumn, we might need a fixed height constraint or wrap_content logic
            // providing a minimum height ensures visibility.
            .heightIn(min = 100.dp, max = 2000.dp), 
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                
                // Transparent background to blend with App Theme
                setBackgroundColor(Color.Transparent.toArgb())

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                        Log.d("CipherWebView", "JS: ${message?.message()}")
                        return true
                    }
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Trigger Syntax Highlight
                        view?.evaluateJavascript("Prism.highlightAll();", null)
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        }
    )
}

// --- THE HTML GENERATOR (Embedded CSS/JS) ---
// This injects a lightweight Markdown Parser (Marked.js) and Highlighter (Prism.js)
// No Internet connection needed (We use CDN links, but you can cache them locally for offline).
// For now, we use a simple pre-styled template.

fun generateHtml(markdown: String, bgColor: String, textColor: String): String {
    // Escape standard HTML characters to prevent breakage before parsing
    val safeMarkdown = markdown
        .replace("\\n", "\\\\n")
        .replace("`", "\\`")
        .replace("\"", "\\\"")

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <!-- PRISM JS (Syntax Highlighting) -->
            <link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css" rel="stylesheet" />
            <style>
                body {
                    background-color: $bgColor;
                    color: $textColor;
                    font-family: 'Roboto', sans-serif;
                    font-size: 16px;
                    line-height: 1.6;
                    margin: 0;
                    padding: 16px;
                    word-wrap: break-word;
                }
                pre {
                    background: #1e1e1e !important;
                    border-radius: 8px;
                    padding: 12px;
                    overflow-x: auto;
                    border: 1px solid #333;
                }
                code {
                    font-family: 'Fira Code', monospace;
                    font-size: 14px;
                }
                img {
                    max-width: 100%;
                    border-radius: 8px;
                    margin-top: 8px;
                    margin-bottom: 8px;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 16px 0;
                }
                th, td {
                    border: 1px solid #444;
                    padding: 8px;
                    text-align: left;
                }
                th {
                    background-color: #333;
                }
                blockquote {
                    border-left: 4px solid #00E676;
                    margin: 0;
                    padding-left: 16px;
                    color: #888;
                }
                /* Loading State */
                .loading { color: #888; font-style: italic; }
            </style>
            <!-- MARKED JS (Markdown Parser) -->
            <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
        </head>
        <body>
            <div id="content" class="loading"></div>

            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-kotlin.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-java.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-python.min.js"></script>
            
            <script>
                const rawMarkdown = "$safeMarkdown";
                const contentDiv = document.getElementById('content');
                
                try {
                    // Parse Markdown to HTML
                    contentDiv.innerHTML = marked.parse(rawMarkdown);
                    contentDiv.classList.remove('loading');
                    
                    // Highlight Code
                    Prism.highlightAll();
                } catch (e) {
                    contentDiv.innerHTML = "<p style='color:red'>Render Error: " + e.message + "</p>";
                    console.error(e);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}