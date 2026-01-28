package com.cipher.studio.presentation.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cipher.studio.domain.model.Theme
import java.util.UUID

/**
 * CIPHER STUDIO: ULTIMATE MARKDOWN RENDERER (WEBVIEW ENGINE)
 *
 * PURPOSE: To render massive Markdown content (1000+ lines) without crashing Android UI.
 * STRATEGY: Uses Android's native WebView to process HTML/CSS/JS efficiently.
 * FEATURES: Syntax Highlighting, Tables, Images, Auto-Dark Mode.
 */

// --- COMPATIBILITY WRAPPER ---
// This ensures your MainScreen code doesn't break. We keep the same signature.
@Composable
fun MarkdownRenderer(
    content: String,
    theme: Theme,
    isStreaming: Boolean = false, // Kept for compatibility, though WebView handles stream updates naturally
    modifier: Modifier = Modifier
) {
    // Generate a unique ID to ensure State consistency
    val renderId = remember(content) { UUID.randomUUID().toString() }

    val isDark = theme == Theme.DARK
    // Convert Theme Colors to Hex for CSS
    val backgroundColor = if (isDark) "#1E1E1E" else "#FFFFFF"
    val textColor = if (isDark) "#E0E0E0" else "#212121"
    val codeBgColor = if (isDark) "#2D2D2D" else "#F5F5F5"
    
    // Generate the Full HTML Document with CSS and JS
    val htmlContent = remember(content, theme) {
        generateHtml(content, backgroundColor, textColor, codeBgColor)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color(0xFF1E1E1E) else Color.White)
    ) {
        try {
            CipherWebView(htmlContent, isDark)
        } catch (e: Exception) {
            // Safety Fallback: If WebView fails (very rare), show error text instead of crashing app
            Text(
                text = "Rendering Error: ${e.localizedMessage}",
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
            Log.e("CipherRender", "WebView Crash: ${e.stackTraceToString()}")
        }
    }
}

// --- THE WEBVIEW COMPONENT ---
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CipherWebView(htmlContent: String, isDark: Boolean) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            // Dynamic Height: Min 50dp, Max 3000dp (Scrolling handled inside WebView if larger)
            .heightIn(min = 50.dp, max = 3000.dp), 
        factory = { context ->
            WebView(context).apply {
                // Settings for Performance and Features
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = false
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                    cacheMode = WebSettings.LOAD_NO_CACHE // Ensure fresh render
                }
                
                // Transparent Background to blend with Compose
                setBackgroundColor(Color.Transparent.toArgb())

                // Chrome Client for Debugging
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                        // Forward JS logs to Android Logcat
                        Log.d("CipherWebView", "JS: ${message?.message()}")
                        return true
                    }
                }
                
                // View Client for Page Events
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Trigger PrismJS Highlight when page is ready
                        view?.evaluateJavascript("Prism.highlightAll();", null)
                    }
                }
            }
        },
        update = { webView ->
            // Load the HTML content securely
            webView.loadDataWithBaseURL("https://cipher.studio/", htmlContent, "text/html", "UTF-8", null)
        }
    )
}

// --- HTML GENERATOR (THE BRAIN) ---
// Injects Marked.js (Parser) and Prism.js (Highlighter) from CDN.
// No logic reduced: It handles Code, Tables, Quotes, Bold, Italic, Images.

fun generateHtml(markdown: String, bgColor: String, textColor: String, codeBgColor: String): String {
    // Escape special characters to prevent JS breakage
    val safeMarkdown = markdown
        .replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("$", "\\$")
        .replace("/", "\\/")
        // Handle newlines carefully for JS string injection
        .replace("\n", "\\n")
        .replace("\r", "")

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            
            <!-- 1. PRISM CSS (Syntax Highlighting Theme) -->
            <link href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-tomorrow.min.css" rel="stylesheet" />
            
            <style>
                body {
                    background-color: $bgColor;
                    color: $textColor;
                    font-family: 'Roboto', sans-serif;
                    font-size: 15px;
                    line-height: 1.6;
                    margin: 0;
                    padding: 12px;
                    word-wrap: break-word; /* Prevents horizontal scroll crash */
                }
                
                /* Code Blocks */
                pre {
                    background: $codeBgColor !important;
                    border-radius: 8px;
                    padding: 12px;
                    overflow-x: auto; /* Internal scrolling for code */
                    border: 1px solid #444;
                    margin-top: 8px;
                    margin-bottom: 8px;
                }
                code {
                    font-family: 'Fira Code', 'Consolas', monospace;
                    font-size: 13px;
                }
                
                /* Inline Code */
                p code, li code {
                    background: rgba(255, 255, 255, 0.1);
                    padding: 2px 4px;
                    border-radius: 4px;
                    color: #FF7B72;
                }

                /* Images */
                img {
                    max-width: 100%;
                    border-radius: 8px;
                    margin: 8px 0;
                    display: block;
                }

                /* Tables */
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 16px 0;
                    font-size: 13px;
                }
                th, td {
                    border: 1px solid #555;
                    padding: 8px;
                    text-align: left;
                }
                th {
                    background-color: rgba(255, 255, 255, 0.05);
                    font-weight: bold;
                }

                /* Blockquotes */
                blockquote {
                    border-left: 4px solid #00E676;
                    margin: 12px 0;
                    padding-left: 12px;
                    color: #888;
                    font-style: italic;
                }

                /* Lists */
                ul, ol {
                    padding-left: 20px;
                    margin: 8px 0;
                }
                li {
                    margin-bottom: 4px;
                }
                
                /* Headers */
                h1, h2, h3 { margin-top: 20px; margin-bottom: 10px; }
                h1 { font-size: 1.4em; border-bottom: 1px solid #444; padding-bottom: 5px; }
                h2 { font-size: 1.2em; }

                /* Links */
                a { color: #448AFF; text-decoration: none; }

                #content { opacity: 0; transition: opacity 0.3s ease; }
                .loaded { opacity: 1 !important; }
            </style>
            
            <!-- 2. MARKED JS (Markdown Parser) -->
            <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
        </head>
        <body>
            <div id="content"></div>

            <!-- 3. PRISM JS (Language Support) -->
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-kotlin.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-java.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-python.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-bash.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-json.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-sql.min.js"></script>
            
            <script>
                const rawMarkdown = `$safeMarkdown`;
                const contentDiv = document.getElementById('content');
                
                try {
                    // Configure Marked to use Prism for highlighting
                    marked.setOptions({
                        breaks: true, // Enable line breaks
                        gfm: true     // Enable Github Flavored Markdown
                    });

                    // Parse content
                    contentDiv.innerHTML = marked.parse(rawMarkdown);
                    
                    // Trigger Highlight
                    setTimeout(() => {
                        Prism.highlightAll();
                        contentDiv.classList.add('loaded'); // Fade in
                    }, 50);
                    
                } catch (e) {
                    contentDiv.innerHTML = "<h3 style='color:red'>Render Error</h3><p>" + e.message + "</p>";
                    console.error("Markdown Error:", e);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}