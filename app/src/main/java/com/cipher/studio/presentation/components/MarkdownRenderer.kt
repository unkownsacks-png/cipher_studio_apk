package com.cipher.studio.presentation.components

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
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
 * FIXED VERSION: Correct Imports and Layout Params to prevent Build Errors.
 */

@Composable
fun MarkdownRenderer(
    content: String,
    theme: Theme,
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Generate HTML only when content changes
    val htmlContent = remember(content, theme) {
        val isDark = theme == Theme.DARK
        val bgColor = if (isDark) "#1E1E1E" else "#FFFFFF"
        val txtColor = if (isDark) "#E0E0E0" else "#212121"
        val codeBg = if (isDark) "#2D2D2D" else "#F5F5F5"
        generateHtml(content, bgColor, txtColor, codeBg)
    }

    val isDark = theme == Theme.DARK

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color(0xFF1E1E1E) else Color.White)
    ) {
        try {
            CipherWebView(htmlContent)
        } catch (e: Exception) {
            Text(
                text = "Render Error: ${e.localizedMessage}",
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CipherWebView(htmlContent: String) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp, max = 5000.dp), // Height cap to prevent infinite layout loop
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = false
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                }
                
                setBackgroundColor(Color.Transparent.toArgb())

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                        return true // Suppress console logs
                    }
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript("Prism.highlightAll();", null)
                    }
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://cipher.studio/", htmlContent, "text/html", "UTF-8", null)
        }
    )
}

fun generateHtml(markdown: String, bgColor: String, textColor: String, codeBgColor: String): String {
    // Escape standard characters for JS string injection
    val safeMarkdown = markdown
        .replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("$", "\\$")
        .replace("/", "\\/")
        .replace("\n", "\\n")
        .replace("\r", "")

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
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
                    word-wrap: break-word;
                }
                pre {
                    background: $codeBgColor !important;
                    border-radius: 8px;
                    padding: 12px;
                    overflow-x: auto;
                    border: 1px solid #444;
                    margin: 8px 0;
                }
                code { font-family: 'Fira Code', monospace; font-size: 13px; }
                img { max-width: 100%; border-radius: 8px; margin: 8px 0; display: block; }
                table { border-collapse: collapse; width: 100%; margin: 16px 0; font-size: 13px; }
                th, td { border: 1px solid #555; padding: 8px; text-align: left; }
                blockquote { border-left: 4px solid #00E676; margin: 12px 0; padding-left: 12px; color: #888; font-style: italic; }
            </style>
            <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
        </head>
        <body>
            <div id="content"></div>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-kotlin.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-java.min.js"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-python.min.js"></script>
            <script>
                const rawMarkdown = `$safeMarkdown`;
                const contentDiv = document.getElementById('content');
                try {
                    marked.setOptions({ breaks: true, gfm: true });
                    contentDiv.innerHTML = marked.parse(rawMarkdown);
                    setTimeout(() => Prism.highlightAll(), 50);
                } catch (e) {
                    contentDiv.innerHTML = "<p style='color:red'>Render Error</p>";
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}