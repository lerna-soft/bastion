package com.bastion.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bastion.app.data.Host
import com.bastion.app.data.HostWithSecret
import com.bastion.app.util.safe
import com.bastion.app.data.VaultRepository
import com.bastion.app.logging.RemoteLogger
import com.bastion.app.ssh.AuthConfig
import com.bastion.app.ssh.SshSession
import com.bastion.app.ssh.loadKeyPairFromPem
import com.bastion.app.terminal.SystemStatsPanel
import com.bastion.app.terminal.TerminalSession
import com.bastion.app.terminal.TerminalTab
import com.bastion.app.terminal.cleanupTerminalSession
import com.bastion.app.ui.theme.ColorMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NavSection(val label: String, val icon: ImageVector) {
    SERVERS("Connections", Icons.Default.Dns),
    SSH_KEYS("SSH Keys", Icons.Default.VpnKey),
    SESSIONS("Terminal", Icons.Default.Devices),
    LOGS("Logs", Icons.Default.Description),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun AppLayout(
    app: com.bastion.app.BastionApp,
    repository: VaultRepository,
    colorMode: ColorMode = ColorMode.DARK,
    onColorModeChange: (ColorMode) -> Unit = {},
    onNavigateToAddHost: () -> Unit,
    onNavigateToEditHost: (Long) -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(NavSection.SERVERS) }
    // HIM-013: hoisted a BastionApp — sobreviven a la navegación interna (Agregar/Editar servidor).
    val terminalSessions = app.terminalSessions
    val pagerState = rememberPagerState(pageCount = { terminalSessions.size })
    val webViewCache = app.terminalWebViewCache
    val scope = rememberCoroutineScope()
    var fontSize by remember { mutableStateOf(14) }

    LaunchedEffect(Unit) {
        val settings = repository.getSettings()
        fontSize = settings.fontSize.toInt()
    }

    fun openTerminalSession(hostWithSecret: HostWithSecret) {
        val tabId = app.nextTerminalId()
        val hostInfo = hostWithSecret.host
        val displayName = hostInfo.name
        RemoteLogger.i("AppLayout", "open terminal #$tabId $displayName")
        val session = SshSession()
        RemoteLogger.i("AppLayout", "terminales abiertas ahora: ${terminalSessions.size + 1} (WebViews en memoria)")
        terminalSessions.add(
            TerminalSession(
                id = tabId,
                title = displayName,
                session = session,
                hostId = hostInfo.id,
                hostname = hostInfo.hostname,
                port = hostInfo.port,
                username = hostInfo.username,
                authTypeLabel = when (hostInfo.authType) {
                    com.bastion.app.data.AuthType.PASSWORD -> "Password"
                    com.bastion.app.data.AuthType.PUBLIC_KEY -> "Public Key"
                    com.bastion.app.data.AuthType.AGENT_FORWARD -> "Agent"
                },
                theme = colorMode.name
            )
        )
        selectedSection = NavSection.SESSIONS
        scope.launch { safe("AppLayout") { pagerState.animateScrollToPage(terminalSessions.size - 1) } }
        scope.launch {
            safe("AppLayout") {
                // HIM-019: resuelve la cadena de saltos (jump hosts) del vault. Directo → [target].
                val chain = withContext(Dispatchers.IO) {
                    repository.resolveConnectionChain(hostInfo.id)
                }
                startConnection(session, if (chain.isEmpty()) listOf(hostWithSecret) else chain)
            }
        }
    }

    fun closeTerminalSession(index: Int) {
        if (index < 0 || index >= terminalSessions.size) return
        val ts = terminalSessions[index]
        RemoteLogger.i("AppLayout", "close terminal #${ts.id} ${ts.title}")
        safe("AppLayout") { cleanupTerminalSession(ts.session) }
        safe("AppLayout") { webViewCache.remove(ts.session)?.destroy() }
        CoroutineScope(Dispatchers.IO).launch { safe("AppLayout") { ts.session.close() } }
        terminalSessions.removeAt(index)
    }

    fun updateTerminalTheme(id: Int, mode: ColorMode) {
        val idx = terminalSessions.indexOfFirst { it.id == id }
        if (idx < 0) return
        val ts = terminalSessions[idx]
        terminalSessions[idx] = ts.copy(theme = mode.name)
    }

    val context = LocalContext.current
    var showStats by remember { mutableStateOf(false) }
    var showHostPicker by remember { mutableStateOf(false) }
    var showCrashNotice by remember { mutableStateOf(RemoteLogger.hasUnseenIncident()) }
    val allHosts by repository.getAllHosts().collectAsState(initial = emptyList())
    val anySessionActive = terminalSessions.any {
        val s = it.session.state.value
        s == com.bastion.app.ssh.SessionState.SHELL_ACTIVE
    }

    // Foreground service (HIM-011): mantener el proceso vivo mientras haya terminales abiertas,
    // para que Android no lo mate en segundo plano y las sesiones sobrevivan al cambiar de app.
    LaunchedEffect(terminalSessions.size) {
        if (terminalSessions.isEmpty()) {
            com.bastion.app.service.SessionKeepAliveService.stop(context)
        } else {
            com.bastion.app.service.SessionKeepAliveService.start(context, terminalSessions.size)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Sidebar(
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it },
                onNewInstance = onNavigateToAddHost
            )

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedSection) {
                        NavSection.SERVERS -> {
                            VaultTabContent(
                                repository = repository,
                                onAddHost = onNavigateToAddHost,
                                onEditHost = onNavigateToEditHost,
                                onConnect = { host -> openTerminalSession(host) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        NavSection.SESSIONS -> {
                            if (terminalSessions.isEmpty()) {
                                EmptyTerminalPlaceholder(
                                    onBrowseServers = { selectedSection = NavSection.SERVERS }
                                )
                            } else {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        TerminalPagerContent(
                                            sessions = terminalSessions,
                                            pagerState = pagerState,
                                            webViewCache = webViewCache,
                                            onCloseSession = { closeTerminalSession(it) },
                                            onNewTab = { showHostPicker = true },
                                            showStats = showStats,
                                            onToggleStats = { showStats = !showStats },
                                            fontSize = fontSize,
                                            onThemeChange = { id, mode -> updateTerminalTheme(id, mode) },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    if (showStats) {
                                        val currentSession = terminalSessions.getOrNull(pagerState.currentPage)
                                        SystemStatsPanel(
                                            hostname = currentSession?.hostname ?: "",
                                            isConnected = currentSession?.let { s ->
                                                s.session.state.value == com.bastion.app.ssh.SessionState.SHELL_ACTIVE
                                            } ?: false,
                                            session = currentSession?.session,
                                            authConfig = currentSession?.session?.config?.value,
                                            onClose = { showStats = false }
                                        )
                                    }
                                }
                            }
                        }
                        NavSection.SSH_KEYS -> {
                            SSHKeysContent(
                                repository = repository,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        NavSection.LOGS -> {
                            LogsScreen(modifier = Modifier.fillMaxSize())
                        }
                        NavSection.SETTINGS -> {
                            SettingsContent(
                                repository = repository,
                                colorMode = colorMode,
                                onColorModeChange = onColorModeChange,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (terminalSessions.isNotEmpty()) {
                    SessionFooter(
                        sessionTitle = terminalSessions.getOrNull(pagerState.currentPage)?.title ?: "",
                        isConnected = anySessionActive
                    )
                }
            }
        }
    }

    if (showCrashNotice) {
        AlertDialog(
            onDismissRequest = { RemoteLogger.markIncidentsSeen(); showCrashNotice = false },
            title = { Text("La app se cerró la última vez") },
            text = {
                Text(
                    "Se registró un cierre inesperado (crash o cierre del sistema). Puedes ver el motivo y el detalle en la sección Logs.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    RemoteLogger.markIncidentsSeen()
                    showCrashNotice = false
                    selectedSection = NavSection.LOGS
                }) { Text("Ver detalles") }
            },
            dismissButton = {
                TextButton(onClick = { RemoteLogger.markIncidentsSeen(); showCrashNotice = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showHostPicker) {
        NewTabHostPicker(
            hosts = allHosts,
            onDismiss = { showHostPicker = false },
            onPick = { host ->
                showHostPicker = false
                scope.launch {
                    safe("AppLayout") {
                        repository.getHostWithSecret(host.id)?.let { openTerminalSession(it) }
                    }
                }
            }
        )
    }
}

@Composable
private fun NewTabHostPicker(
    hosts: List<Host>,
    onDismiss: () -> Unit,
    onPick: (Host) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva pestaña") },
        text = {
            if (hosts.isEmpty()) {
                Text(
                    "No hay servidores guardados. Agrega uno desde la sección Servers.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    hosts.forEach { host ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(host) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    host.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${host.username}@${host.hostname}:${host.port}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun LogsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }
    val crash = remember(refreshTick) { RemoteLogger.readCrashLog() }
    val lastExit = remember(refreshTick) { RemoteLogger.readLastExit() }
    val recent = remember(refreshTick) { RemoteLogger.recentLogs() }
    val fmt = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US) }

    fun fullText(): String {
        val sb = StringBuilder()
        if (lastExit != null) sb.append(lastExit).append("\n\n")
        if (crash != null) sb.append("=== LAST CRASH ===\n").append(crash).append("\n\n")
        sb.append("=== RECENT (${recent.size}) ===\n")
        recent.forEach { sb.append("${fmt.format(java.util.Date(it.timeMillis))} ${it.level}/${it.tag}: ${it.msg}\n") }
        return sb.toString()
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            "Logs & Diagnóstico",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { refreshTick++ }) { Text("Refrescar") }
            TextButton(onClick = {
                safe("LogsScreen") {
                    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("bastion-logs", fullText()))
                }
            }) { Text("Copiar") }
            TextButton(onClick = {
                safe("LogsScreen") {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, fullText())
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(send, "Compartir logs")
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }) { Text("Compartir") }
            TextButton(onClick = { RemoteLogger.clearCrashLog(); refreshTick++ }) { Text("Borrar") }
        }
        Spacer(Modifier.height(8.dp))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(
                "Último cierre del sistema",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            if (lastExit == null) {
                Text(
                    "Sin cierres anormales registrados (o Android < 11).",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            } else {
                Text(
                    lastExit,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Último crash (excepción Java)",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            if (crash == null) {
                Text(
                    "Sin crashes de excepción registrados.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            } else {
                Text(
                    crash,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Actividad reciente (${recent.size})",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            if (recent.isEmpty()) {
                Text(
                    "Sin actividad registrada.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            } else {
                recent.forEach { e ->
                    val color = when (e.level) {
                        "ERROR", "CRASH" -> MaterialTheme.colorScheme.error
                        "WARN" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        "${fmt.format(java.util.Date(e.timeMillis))} ${e.level}/${e.tag}: ${e.msg}",
                        color = color,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun Sidebar(
    selectedSection: NavSection,
    onSectionSelected: (NavSection) -> Unit,
    onNewInstance: () -> Unit
) {
    val context = LocalContext.current
    var collapsed by remember { mutableStateOf(false) }
    val sidebarWidth by animateDpAsState(
        targetValue = if (collapsed) 64.dp else 260.dp,
        animationSpec = tween(300),
        label = "sidebarWidth"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (collapsed) 0f else 1f,
        animationSpec = tween(300),
        label = "contentAlpha"
    )

    Column(
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (collapsed) 8.dp else 16.dp, vertical = 16.dp),
            horizontalAlignment = if (collapsed) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Hamburger: colapsa/expande el sidebar.
                IconButton(
                    onClick = { collapsed = !collapsed },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = if (collapsed) "Expandir menú" else "Colapsar menú",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (!collapsed) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "Bastion",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Infrastructure Management",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        if (!collapsed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onNewInstance)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "New Connection",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onNewInstance),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Connection",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(if (collapsed) 12.dp else 8.dp))

        NavSection.entries.forEach { section ->
            SidebarNavItem(
                icon = section.icon,
                label = section.label,
                isSelected = section == selectedSection,
                collapsed = collapsed,
                onClick = { onSectionSelected(section) }
            )
        }

        Spacer(Modifier.weight(1f))

        if (!collapsed) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SidebarLinkItem(
                    icon = Icons.Default.Description,
                    label = "Documentation",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lerna-soft/bastion/wiki"))
                        )
                    }
                )
                SidebarLinkItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    label = "Support",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lerna-soft/bastion/issues"))
                        )
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Not signed in",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Sign in to manage profile",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    collapsed: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .then(
                if (collapsed) Modifier.padding(vertical = 12.dp)
                else Modifier.padding(start = 0.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
            ),
        contentAlignment = if (collapsed) Alignment.Center else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = if (collapsed) Modifier else Modifier.fillMaxWidth()
        ) {
            if (!collapsed) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(1.dp))
                    )
                } else {
                    Spacer(Modifier.width(2.dp))
                }
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            if (!collapsed) {
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun SidebarLinkItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SessionFooter(
    sessionTitle: String,
    isConnected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Bastion SSH",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = "|",
                color = MaterialTheme.colorScheme.outlineVariant,
                fontSize = 11.sp
            )
            Text(
                text = sessionTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 200.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outline)
                )
                Text(
                    text = if (isConnected) "Connected" else "Disconnected",
                    color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            Text(
                text = "UTF-8",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun TerminalPagerContent(
    sessions: List<TerminalSession>,
    pagerState: PagerState,
    webViewCache: MutableMap<SshSession, WebView>,
    onCloseSession: (Int) -> Unit,
    onNewTab: () -> Unit,
    showStats: Boolean,
    onToggleStats: () -> Unit,
    fontSize: Int = 14,
    onThemeChange: (Int, ColorMode) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SessionTabBar(
            sessions = sessions,
            activeSessionIndex = pagerState.currentPage,
            onTabClick = { index ->
                // safe: una excepción aquí (carrera con cierre de pestañas) sería main-thread y
                // tumbaría la app (HIM-012 F2).
                kotlinx.coroutines.MainScope().launch { safe("SessionTabBar") { pagerState.animateScrollToPage(index) } }
            },
            onTabClose = onCloseSession,
            onNewTab = onNewTab,
            onToggleStats = onToggleStats,
            showStats = showStats
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            val ts = sessions[page]
                TerminalTab(
                session = ts.session,
                webViewCache = webViewCache,
                hostname = ts.hostname,
                port = ts.port,
                username = ts.username,
                authTypeLabel = ts.authTypeLabel,
                title = ts.title,
                showStats = showStats,
                onToggleStats = onToggleStats,
                fontSize = fontSize,
                terminalColorMode = try { ColorMode.valueOf(ts.theme) } catch (_: Exception) { ColorMode.DARK },
                onThemeChange = { onThemeChange(ts.id, it) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SessionTabBar(
    sessions: List<TerminalSession>,
    activeSessionIndex: Int,
    onTabClick: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    onNewTab: () -> Unit,
    onToggleStats: () -> Unit,
    showStats: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(start = 0.dp, end = 4.dp, top = 0.dp, bottom = 0.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            itemsIndexed(sessions, key = { _, s -> s.id }) { index, session ->
                val isActive = index == activeSessionIndex

                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabClick(index) }
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                if (isActive) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Devices,
                            contentDescription = null,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = session.title,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 120.dp)
                        )
                        IconButton(
                            onClick = { onTabClose(index) },
                            modifier = Modifier.size(14.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                    )
                }
            }
        }

        // "+" — open a new terminal tab (picks a host from the vault).
        Box(
            modifier = Modifier
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onNewTab),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "New tab",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (showStats) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent
                )
                .clickable(onClick = onToggleStats),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                color = if (showStats) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun EmptyTerminalPlaceholder(onBrowseServers: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Devices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No active sessions",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Connect to a server to start a terminal session",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBrowseServers) {
                Text(
                    "Browse servers",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Conecta [session] a través de la [chain] resuelta (jump hosts). El último elemento es el destino
 * final; los anteriores son los saltos en orden (primer salto = alcanzable directo desde el móvil).
 * Una cadena de un solo elemento = conexión directa (comportamiento previo a HIM-019).
 */
private suspend fun startConnection(session: SshSession, chain: List<HostWithSecret>) {
    val log = RemoteLogger.logger("Connection")
    if (chain.isEmpty()) return
    val target = chain.last()
    val jumpHosts = chain.dropLast(1)
    val route = chain.joinToString(" → ") { "${it.host.username}@${it.host.hostname}:${it.host.port}" }
    log.i("start [$route]")
    withContext(Dispatchers.IO) {
        try {
            // Construye el AuthConfig de un host, cargando su llave privada si aplica.
            // Devuelve null y marca error en la sesión si la llave no carga.
            fun buildConfig(h: HostWithSecret): AuthConfig? {
                val keyPair = if (h.host.authType == com.bastion.app.data.AuthType.PUBLIC_KEY) {
                    h.privateKeyPem?.let { pem ->
                        try {
                            loadKeyPairFromPem(pem, h.privateKeyPassphrase)
                        } catch (e: Exception) {
                            log.e("key load failed (${h.host.hostname}): ${e.message}", e)
                            session.setError(
                                phase = "key_load",
                                message = "Error loading private key for ${h.host.hostname}: ${e.message}",
                                exception = e
                            )
                            return null
                        }
                    }
                } else {
                    null
                }
                return AuthConfig(
                    hostname = h.host.hostname,
                    port = h.host.port,
                    username = h.host.username,
                    password = h.password,
                    keyPair = keyPair
                )
            }

            val targetConfig = buildConfig(target) ?: return@withContext
            val jumpConfigs = ArrayList<AuthConfig>(jumpHosts.size)
            for (h in jumpHosts) {
                jumpConfigs.add(buildConfig(h) ?: return@withContext)
            }

            session.connect(targetConfig, jumpConfigs).onSuccess {
                log.i("connected, opening shell")
                session.openShell()
            }.onFailure { e ->
                log.e("connect failed: ${e.message}", e)
            }
        } catch (e: Exception) {
            log.e("startup error: ${e.message}", e)
            session.setError(
                phase = "startup",
                message = e.message ?: e.javaClass.simpleName,
                exception = e
            )
        }
    }
}
