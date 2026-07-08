package com.bastion.app.terminal

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bastion.app.logging.RemoteLogger
import com.bastion.app.ssh.ConnectionError
import com.bastion.app.ssh.SessionState
import com.bastion.app.ssh.SshSession
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private data class ThemeOption(val id: String, val label: String, val color: ComposeColor)

private val themes = listOf(
    ThemeOption("stitch", "Stitch", ComposeColor(0xFF121414)),
    ThemeOption("default", "Oscuro", ComposeColor(0xFF1E1E1E)),
    ThemeOption("light", "Claro", ComposeColor(0xFFFFFFFF)),
    ThemeOption("monokai", "Monokai", ComposeColor(0xFF272822)),
    ThemeOption("solarized-dark", "Solarized", ComposeColor(0xFF002B36)),
    ThemeOption("dracula", "Dracula", ComposeColor(0xFF282A36)),
    ThemeOption("nord", "Nord", ComposeColor(0xFF2E3440))
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TerminalTab(
    session: SshSession,
    webViewCache: MutableMap<SshSession, WebView>,
    modifier: Modifier = Modifier
) {
    val state by session.state.collectAsState()
    val error by session.error.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }

    val bridge = remember { mutableStateOf<TerminalBridge?>(null) }

    val webView = remember {
        webViewCache.getOrPut(session) {
            createWebView(context, session, scope, bridge)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Terminal area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable {
                    webView.requestFocus()
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT)
                }
        ) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )

            when (state) {
                SessionState.IDLE,
                SessionState.CONNECTING,
                SessionState.AUTHENTICATING -> {
                    ConnectingOverlay(state)
                }
                SessionState.ERROR -> {
                    ErrorOverlay(error = error, context = context)
                }
                SessionState.CLOSING,
                SessionState.CLOSED -> {
                    ClosedOverlay()
                }
                SessionState.SHELL_ACTIVE -> {
                    ThemeButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }
        }

        // Special Keys Bar - EXACT Stitch design
        if (state == SessionState.SHELL_ACTIVE) {
            SpecialKeysBar(
                onEsc = { bridge.value?.sendKey("ESC") },
                onTab = { bridge.value?.sendKey("TAB") },
                onCtrl = { bridge.value?.sendKey("CTRL") },
                onAlt = { bridge.value?.sendKey("ALT") },
                onUp = { bridge.value?.sendKey("UP") },
                onDown = { bridge.value?.sendKey("DOWN") },
                onLeft = { bridge.value?.sendKey("LEFT") },
                onRight = { bridge.value?.sendKey("RIGHT") }
            )
        }

        // Bottom Navigation - EXACT Stitch design
        BottomNavigationBar(
            onSessionsClick = { },
            onServersClick = { },
            onKeysClick = { },
            onSettingsClick = { }
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentTheme = null,
            onSelect = { themeId ->
                bridge.value?.setColorTheme(themeId)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
private fun SpecialKeysBar(
    onEsc: () -> Unit,
    onTab: () -> Unit,
    onCtrl: () -> Unit,
    onAlt: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeColor(0xFF1E2020))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SpecialKeyButton("ESC", onEsc)
        SpecialKeyButton("TAB", onTab)
        SpecialKeyButton("CTRL", onCtrl)
        SpecialKeyButton("ALT", onAlt)
        SpecialKeyButton("▲", onUp)
        SpecialKeyButton("▼", onDown)
        SpecialKeyButton("◀", onLeft)
        SpecialKeyButton("▶", onRight)
    }
}

@Composable
private fun SpecialKeyButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ComposeColor(0xFF282A2B),
            contentColor = ComposeColor(0xFFE2E2E2)
        ),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BottomNavigationBar(
    onSessionsClick: () -> Unit,
    onServersClick: () -> Unit,
    onKeysClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeColor(0xFF0F1417))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavItem(
            icon = "⚡",
            label = "Sessions",
            isSelected = true,
            onClick = onSessionsClick
        )
        BottomNavItem(
            icon = "🖥",
            label = "Servers",
            isSelected = false,
            onClick = onServersClick
        )
        BottomNavItem(
            icon = "🔑",
            label = "Keys",
            isSelected = false,
            onClick = onKeysClick
        )
        BottomNavItem(
            icon = "⚙",
            label = "Settings",
            isSelected = false,
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun BottomNavItem(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            icon,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = if (isSelected) ComposeColor(0xFF75D1FF) else ComposeColor(0xFF8E9192),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

private fun createWebView(
    context: Context,
    session: SshSession,
    scope: CoroutineScope,
    bridge: MutableState<TerminalBridge?>
): WebView {
    val wv = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.parseColor("#121414"))
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.domStorageEnabled = false
        settings.allowContentAccess = false
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = false
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        isFocusable = true
        isFocusableInTouchMode = true

        setOnTouchListener { v, event ->
            v.performClick()
            v.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
            false
        }

        webChromeClient = WebChromeClient()
        webViewClient = WebViewClient()

        val br = TerminalBridge(this)
        br.initializeJs()
        br.setOnResizeCallback { cols, rows ->
            session.resize(cols, rows)
        }
        bridge.value = br

        loadUrl("file:///android_asset/terminal/index.html")

        scope.launch {
            try {
                for (data in br.onData) {
                    try {
                        session.write(data.toByteArray())
                    } catch (e: Exception) {
                        RemoteLogger.e("TerminalTab", "write error", e)
                    }
                }
            } catch (e: Exception) {
                RemoteLogger.e("TerminalTab", "input loop ended", e)
            }
        }

        scope.launch {
            try {
                session.output.collect { bytes ->
                    try {
                        br.writeToTerminal(bytes)
                    } catch (e: Exception) {
                        RemoteLogger.e("TerminalTab", "output write error", e)
                    }
                }
            } catch (e: Exception) {
                RemoteLogger.e("TerminalTab", "output loop ended", e)
            }
        }
    }
    return wv
}

@Composable
private fun ThemeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(ComposeColor(0x66000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "Temas",
            tint = ComposeColor(0xFFCCCCCC),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ThemePickerDialog(
    currentTheme: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tema de terminal") },
        text = {
            Column {
                themes.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(theme.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(theme.color)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(text = theme.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ConnectingOverlay(state: SessionState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xCC0F1417)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ComposeColor(0xFF75D1FF))
            Spacer(Modifier.height(16.dp))
            Text(
                text = when (state) {
                    SessionState.CONNECTING -> "Conectando..."
                    SessionState.AUTHENTICATING -> "Autenticando..."
                    else -> "Iniciando..."
                },
                color = ComposeColor(0xFFE2E2E2),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ErrorOverlay(error: ConnectionError?, context: Context) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xCC0F1417)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = ComposeColor(0xFFFFB4AB),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Error de conexión",
                color = ComposeColor(0xFFFFB4AB),
                fontSize = 18.sp
            )
            Spacer(Modifier.height(8.dp))

            if (error != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .background(ComposeColor(0xFF1E2020), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Fase: ${error.phase}",
                        color = ComposeColor(0xFFFFB4AB),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = error.message,
                        color = ComposeColor(0xFFE2E2E2),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = error.exceptionText,
                        color = ComposeColor(0xFFC4C7C7),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 12.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Bastion Error", error.fullText)
                        clipboard.setPrimaryClip(clip)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFF282A2B)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = ComposeColor(0xFFE2E2E2),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Copiar error", color = ComposeColor(0xFFE2E2E2))
                }
            } else {
                Text(
                    text = "Error desconocido (sin detalles)",
                    color = ComposeColor(0xFFC4C7C7),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ClosedOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xCC0F1417)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Desconectado",
            color = ComposeColor(0xFF8E9192),
            fontSize = 16.sp
        )
    }
}
