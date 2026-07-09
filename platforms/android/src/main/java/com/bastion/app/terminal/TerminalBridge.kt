package com.bastion.app.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.channels.Channel

class TerminalBridge(private val webView: WebView) {

    companion object {
        private const val TAG = "TerminalBridge"
    }

    /**
     * Modo selección: mientras esté activo, un arrastre con el dedo conduce la selección propia de
     * xterm.js (vía eventos de ratón sintéticos en el WebView) en lugar de mover el scroll, y el
     * listener táctil nativo NO abre el teclado. Lo lee [createWebView] en su OnTouchListener.
     */
    @Volatile
    var selectionMode: Boolean = false
        private set

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

    /** Activa/desactiva el modo selección (arrastre = seleccionar, sin teclado). */
    fun setSelectionMode(enabled: Boolean) {
        selectionMode = enabled
        webView.post {
            webView.evaluateJavascript("setSelectionMode($enabled)", null)
        }
    }

    /** Copia al portapapeles el texto actualmente seleccionado en la terminal. */
    fun copySelection() {
        webView.post {
            webView.evaluateJavascript("copySelection()", null)
        }
    }

    /** Copia al portapapeles todo el contenido del buffer de la terminal. */
    fun copyAll() {
        webView.post {
            webView.evaluateJavascript("copyAll()", null)
        }
    }

    /** Llamado desde JS con el texto seleccionado (o todo el buffer) para copiar al portapapeles. */
    @JavascriptInterface
    fun copyToClipboard(text: String) {
        if (text.isEmpty()) return
        webView.post {
            val cm = webView.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return@post
            cm.setPrimaryClip(ClipData.newPlainText("Bastion Terminal", text))
        }
    }

    fun initializeJs() {
        webView.addJavascriptInterface(this, "BastionBridge")
    }
}
