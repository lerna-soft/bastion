package com.bastion.app.terminal

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bastion.app.ssh.SshSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TerminalTab(
    session: SshSession,
    modifier: Modifier,
    onTitleChange: ((String) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val webViewRef = remember { mutableMapOf<String, Any>() }
    var bridgeJob: Job? = remember { null }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#1e1e1e"))
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.domStorageEnabled = false
                settings.allowContentAccess = false
                settings.allowFileAccessFromFileURLs = true
                settings.allowUniversalAccessFromFileURLs = false
                settings.setSupportZoom(false)

                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()

                val bridge = TerminalBridge(this)
                bridge.initializeJs()
                bridge.setOnResizeCallback { cols, rows ->
                    session.resize(cols, rows)
                }

                loadUrl("file:///android_asset/terminal/index.html")

                scope.launch {
                    for (data in bridge.onData) {
                        session.write(data.toByteArray())
                    }
                }

                scope.launch {
                    session.output.collect { bytes ->
                        bridge.writeToTerminal(bytes)
                    }
                }
            }
        },
        modifier = modifier
    )
}
