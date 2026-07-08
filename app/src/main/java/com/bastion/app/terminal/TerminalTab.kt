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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bastion.app.logging.RemoteLogger
import com.bastion.app.ssh.AuthConfig
import com.bastion.app.ssh.ConnectionError
import com.bastion.app.ssh.SessionState
import com.bastion.app.ssh.SshSession
import com.bastion.app.ui.theme.BastionTheme
import com.bastion.app.ui.theme.ColorMode
import com.bastion.app.ui.theme.MonokaiBackground
import com.bastion.app.ui.theme.NeutralDarkBackground
import com.bastion.app.ui.theme.OledDarkBackground
import com.bastion.app.ui.theme.StitchBackground
import com.bastion.app.ui.theme.StitchLightBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private data class ThemeOption(val id: String, val label: String, val color: Color)

private data class TerminalThemeOption(
    val mode: ColorMode,
    val label: String,
    val preview: Color
)

private val terminalThemes = listOf(
    TerminalThemeOption(ColorMode.DARK, "Neutral", NeutralDarkBackground),
    TerminalThemeOption(ColorMode.STITCH_GREEN, "Stitch", StitchBackground),
    TerminalThemeOption(ColorMode.LIGHT, "Light", StitchLightBackground),
    TerminalThemeOption(ColorMode.MONOKAI, "Monokai", MonokaiBackground),
    TerminalThemeOption(ColorMode.OLED_DARK, "OLED", OledDarkBackground),
    TerminalThemeOption(ColorMode.SYSTEM, "System", NeutralDarkBackground)
)

private fun jsThemeFor(mode: ColorMode): String = when (mode) {
    ColorMode.STITCH_GREEN -> "stitch"
    ColorMode.LIGHT -> "light"
    ColorMode.MONOKAI -> "monokai"
    else -> "default"
}

private val terminalScopes = mutableMapOf<SshSession, CoroutineScope>()

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TerminalTab(
    session: SshSession,
    webViewCache: MutableMap<SshSession, WebView>,
    hostname: String = "",
    port: Int = 22,
    username: String = "",
    authTypeLabel: String = "",
    title: String = "",
    showStats: Boolean = false,
    onToggleStats: () -> Unit = {},
    fontSize: Int = 14,
    terminalColorMode: ColorMode = ColorMode.DARK,
    onThemeChange: (ColorMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by session.state.collectAsState()
    val error by session.error.collectAsState()
    val context = LocalContext.current
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCommandPalette by remember { mutableStateOf(false) }

    val bridge = remember { mutableStateOf<TerminalBridge?>(null) }

    val webView = remember {
        webViewCache.getOrPut(session) {
            createWebView(context, session, bridge)
        }
    }

    val scope = remember {
        terminalScopes.getOrPut(session) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }
    }

    DisposableEffect(session) {
        val br = bridge.value
        if (br != null && webViewCache.containsKey(session)) {
            scope.launch {
                try {
                    for (data in br.onData) {
                        if (!isActive) break
                        try { session.write(data.toByteArray()) }
                        catch (e: Exception) { RemoteLogger.w("TerminalTab", "write error: ${e.message}") }
                    }
                } catch (_: kotlinx.coroutines.CancellationException) {
                } catch (e: Exception) {
                    RemoteLogger.w("TerminalTab", "input loop: ${e.message}")
                }
            }
            scope.launch {
                try {
                    session.output.collect { bytes ->
                        if (!isActive) return@collect
                        try { br.writeToTerminal(bytes) }
                        catch (e: Exception) { RemoteLogger.w("TerminalTab", "output error: ${e.message}") }
                    }
                } catch (_: kotlinx.coroutines.CancellationException) {
                } catch (e: Exception) {
                    RemoteLogger.w("TerminalTab", "output loop: ${e.message}")
                }
            }
        }
        onDispose { }
    }

    LaunchedEffect(bridge.value, fontSize) {
        bridge.value?.setFontSize(fontSize)
    }

    BastionTheme(colorMode = terminalColorMode, applyStatusBar = false) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ServerInfoBar(
                title = title,
                hostname = hostname,
                port = port,
                username = username,
                authType = authTypeLabel,
                state = state
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
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
        }

        if (showCommandPalette) {
            CommandPalette(
                onDismiss = { showCommandPalette = false },
                onCommand = { cmd ->
                    bridge.value?.sendText(cmd)
                    showCommandPalette = false
                }
            )
        }
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentMode = terminalColorMode,
            onSelect = { mode ->
                bridge.value?.setColorTheme(jsThemeFor(mode))
                onThemeChange(mode)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
    }
}

fun cleanupTerminalSession(session: SshSession) {
    terminalScopes.remove(session)?.cancel("session closed")
}

@Composable
private fun ServerInfoBar(
    title: String,
    hostname: String,
    port: Int,
    username: String,
    authType: String,
    state: SessionState
) {
    val isConnected = state == SessionState.SHELL_ACTIVE
    val indicatorColor = when {
        isConnected -> MaterialTheme.colorScheme.primaryContainer
        state == SessionState.ERROR -> MaterialTheme.colorScheme.error
        state == SessionState.CONNECTING || state == SessionState.AUTHENTICATING -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.outline
    }
    val statusText = when (state) {
        SessionState.SHELL_ACTIVE -> "Connected"
        SessionState.CONNECTING -> "Connecting..."
        SessionState.AUTHENTICATING -> "Authenticating..."
        SessionState.ERROR -> "Error"
        SessionState.CLOSING -> "Closing..."
        SessionState.CLOSED -> "Disconnected"
        else -> "Unknown"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(indicatorColor)
                .then(
                    if (isConnected) Modifier
                    else Modifier
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.ifBlank { hostname },
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = statusText,
            color = indicatorColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun SystemStatsPanel(
    hostname: String,
    isConnected: Boolean,
    session: SshSession?,
    authConfig: AuthConfig? = null,
    onClose: () -> Unit
) {
    val panelWidth by animateDpAsState(
        targetValue = 288.dp,
        animationSpec = tween(300),
        label = "statsWidth"
    )

    val collector = remember(isConnected, session, authConfig) {
        if (isConnected && authConfig != null) StatsCollector(authConfig) else null
    }

    val stats = collector?.stats?.collectAsState()?.value ?: SystemStats()
    val logs = collector?.logs?.collectAsState()?.value ?: emptyList()

    LaunchedEffect(collector) {
        collector?.start()
    }
    DisposableEffect(collector) {
        onDispose { collector?.stop() }
    }

    Column(
        modifier = Modifier
            .width(panelWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "System Stats",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = hostname.ifBlank { "—" },
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isConnected && session != null) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!stats.collected) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                            Text(
                                text = "Collecting metrics...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    MetricBar(
                        label = "CPU Load",
                        value = "${String.format("%.1f", stats.cpuUsage * 100)}% (${stats.cpuCores} cores)",
                        progress = stats.cpuUsage,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    MetricBar(
                        label = "Memory",
                        value = "${stats.memUsedMb} / ${stats.memTotalMb} MB",
                        progress = stats.memProgress,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    MetricBar(
                        label = "Disk ${stats.diskPath}",
                        value = "${String.format("%.1f", stats.diskUsedGb)} / ${String.format("%.1f", stats.diskTotalGb)} GB",
                        progress = stats.diskProgress,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    )

                    Spacer(Modifier.height(8.dp))
                    HorizontalDividerStitch(color = MaterialTheme.colorScheme.outlineVariant)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatText("Load avg (1/5/15)", "${String.format("%.2f", stats.loadAvg1)} / ${String.format("%.2f", stats.loadAvg5)} / ${String.format("%.2f", stats.loadAvg15)}")
                        StatText("Uptime since", stats.uptime.ifBlank { "—" })
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDividerStitch(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = "Connection Logs",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        logs.takeLast(8).forEach { entry ->
                            LogEntry(entry.time, entry.message)
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No active connection",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun StatText(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun MetricBar(
    label: String,
    value: String,
    progress: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun HorizontalDividerStitch(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

@Composable
private fun LogEntry(time: String, event: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = time,
            color = MaterialTheme.colorScheme.outline,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = event,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun CommandPalette(
    onDismiss: () -> Unit,
    onCommand: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .padding(top = 120.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable(enabled = false, onClick = {})
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Type a command or search...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }

            HorizontalDividerStitch(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Quick Actions",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                CommandItem("Connect to production-db-replica", "\u2318 1", onClick = {
                    onCommand("ssh deploy@production-db-replica")
                })
                CommandItem("Open Settings", "\u2318 ,", onClick = {
                    onCommand("settings")
                })
                CommandItem("Generate SSH Key", "\u2318 G", onClick = {
                    onCommand("ssh-keygen")
                })
            }
        }
    }
}

@Composable
private fun CommandItem(
    label: String,
    shortcut: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Text(
            text = shortcut,
            color = MaterialTheme.colorScheme.outline,
            fontSize = 10.sp
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
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SpecialKeyButton("Esc", onEsc)
        SpecialKeyButton("Tab", onTab)
        SpecialKeyButton("Ctrl", onCtrl)
        SpecialKeyButton("Alt", onAlt)
        Spacer(Modifier.weight(1f))
        SpecialKeyButton("\u25B2", onUp)
        SpecialKeyButton("\u25BC", onDown)
        SpecialKeyButton("\u25C0", onLeft)
        SpecialKeyButton("\u25B6", onRight)
    }
}

@Composable
private fun SpecialKeyButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun createWebView(
    context: Context,
    session: SshSession,
    bridge: androidx.compose.runtime.MutableState<TerminalBridge?>
): WebView {
    val wv = WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.parseColor("#0c160a"))
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        settings.allowContentAccess = false
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
    }
    return wv
}

@Composable
private fun ThemeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = "Themes",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun ThemePickerDialog(
    currentMode: ColorMode,
    onSelect: (ColorMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terminal Theme", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column {
                terminalThemes.forEach { theme ->
                    val selected = theme.mode == currentMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onSelect(theme.mode) }
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                                .background(theme.preview)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = theme.label,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.primaryContainer) }
        }
    )
}

@Composable
private fun ConnectingOverlay(state: SessionState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primaryContainer,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = when (state) {
                    SessionState.CONNECTING -> "Connecting..."
                    SessionState.AUTHENTICATING -> "Authenticating..."
                    else -> "Starting..."
                },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun ErrorOverlay(error: ConnectionError?, context: Context) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Connection Error",
                color = MaterialTheme.colorScheme.error,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))

            if (error != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = error.message,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (error.exceptionText.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = error.exceptionText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Bastion Error", error.fullText)
                        clipboard.setPrimaryClip(clip)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy Error", fontSize = 13.sp)
                }
            } else {
                Text(
                    text = "Unknown error (no details)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Disconnected",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
