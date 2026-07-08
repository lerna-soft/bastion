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

    fun setFontSize(size: Int) {
        webView.post {
            webView.evaluateJavascript("setFontSize($size)", null)
        }
    }

    fun sendText(text: String) {
        val escaped = text.replace("\\", "\\\\").replace("'", "\\'")
        webView.post {
            webView.evaluateJavascript("term.paste('$escaped')", null)
        }
    }

    /**
     * Botones rápidos (Esc/Tab/Ctrl/Alt/flechas). Antes esto ejecutaba `term.keyboard.sendKey(...)`
     * en el WebView — una API que NO existe en xterm.js, así que el JS lanzaba un ReferenceError
     * que `evaluateJavascript(js, null)` traga en silencio: los botones no hacían nada.
     * Fix: enviar el byte de control directamente por el mismo canal que usa el tipeo real
     * (onData → escrito al stdin de la sesión SSH), sin depender de ninguna API de xterm.js.
     */
    fun sendKey(key: String) {
        val raw = when (key) {
            "ESC" -> "\u001B"
            "TAB" -> "\t"
            "CTRL" -> "\u0003" // Ctrl+C
            "ALT" -> "\u001B"
            "UP" -> "\u001B[A"
            "DOWN" -> "\u001B[B"
            "LEFT" -> "\u001B[D"
            "RIGHT" -> "\u001B[C"
            else -> return
        }
        onData(raw)
    }

    fun initializeJs() {
        webView.addJavascriptInterface(this, "BastionBridge")
    }
}
