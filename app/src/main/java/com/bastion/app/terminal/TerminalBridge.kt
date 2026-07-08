package com.bastion.app.terminal

import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.channels.Channel

class TerminalBridge(private val webView: WebView) {

    companion object {
        private const val TAG = "TerminalBridge"
    }

    private val _onData = Channel<String>(Channel.BUFFERED)
    val onData: Channel<String> = _onData

    private var _onResizeCallback: ((cols: Int, rows: Int) -> Unit)? = null

    fun setOnResizeCallback(callback: (cols: Int, rows: Int) -> Unit) {
        _onResizeCallback = callback
    }

    @JavascriptInterface
    fun onData(data: String) {
        _onData.trySend(data)
    }

    @JavascriptInterface
    fun onResize(cols: Int, rows: Int) {
        _onResizeCallback?.invoke(cols, rows)
    }

    fun writeToTerminal(data: ByteArray) {
        val b64 = Base64.encodeToString(data, Base64.NO_WRAP)
        webView.post {
            webView.evaluateJavascript("writeToTerminal('$b64')", null)
        }
    }

    fun setSize(cols: Int, rows: Int) {
        webView.post {
            webView.evaluateJavascript("setSize($cols, $rows)", null)
        }
    }

    fun setColorTheme(name: String) {
        webView.post {
            webView.evaluateJavascript("setColorTheme('$name')", null)
        }
    }

    fun sendText(text: String) {
        val escaped = text.replace("\\", "\\\\").replace("'", "\\'")
        webView.post {
            webView.evaluateJavascript("term.paste('$escaped')", null)
        }
    }

    fun sendKey(key: String) {
        val js = when (key) {
            "ESC" -> "term.keyboard.sendKey('\\x1b')"
            "TAB" -> "term.keyboard.sendKey('\\t')"
            "CTRL" -> "term.keyboard.sendKey('\\x03')"
            "ALT" -> "term.keyboard.sendKey('\\x1b')"
            "UP" -> "term.keyboard.sendKey('\\x1b[A')"
            "DOWN" -> "term.keyboard.sendKey('\\x1b[B')"
            "LEFT" -> "term.keyboard.sendKey('\\x1b[D')"
            "RIGHT" -> "term.keyboard.sendKey('\\x1b[C')"
            else -> ""
        }
        if (js.isNotEmpty()) {
            webView.post {
                webView.evaluateJavascript(js, null)
            }
        }
    }

    fun initializeJs() {
        webView.addJavascriptInterface(this, "BastionBridge")
    }
}
