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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
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
import com.bastion.app.data.HostWithSecret
import com.bastion.app.data.VaultRepository
import com.bastion.app.logging.RemoteLogger
import com.bastion.app.ssh.AuthConfig
import com.bastion.app.ssh.SshSession
import com.bastion.app.ssh.loadKeyPairFromPem
import com.bastion.app.terminal.SystemStatsPanel
import com.bastion.app.terminal.TerminalTab
import com.bastion.app.terminal.cleanupTerminalSession
import com.bastion.app.ui.theme.ColorMode
import com.bastion.app.ui.theme.StitchBackground
import com.bastion.app.ui.theme.StitchOnPrimaryFixed
import com.bastion.app.ui.theme.StitchOnSurface
import com.bastion.app.ui.theme.StitchOnSurfaceVariant
import com.bastion.app.ui.theme.StitchOutline
import com.bastion.app.ui.theme.StitchOutlineVariant
import com.bastion.app.ui.theme.StitchPrimaryContainer
import com.bastion.app.ui.theme.StitchPrimaryFixedDim
import com.bastion.app.ui.theme.StitchSecondary
import com.bastion.app.ui.theme.StitchSecondaryContainer
import com.bastion.app.ui.theme.StitchSurfaceContainer
import com.bastion.app.ui.theme.StitchSurfaceContainerHigh
import com.bastion.app.ui.theme.StitchSurfaceContainerHighest
import com.bastion.app.ui.theme.StitchSurfaceContainerLow
import com.bastion.app.ui.theme.StitchSurfaceContainerLowest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NavSection(val label: String, val icon: ImageVector) {
    SERVERS("Connections", Icons.Default.Dns),
    SSH_KEYS("SSH Keys", Icons.Default.VpnKey),
    SESSIONS("Terminal", Icons.Default.Devices),
    SETTINGS("Settings", Icons.Default.Settings)
}

private data class TerminalSession(
    val id: Int,
    val title: String,
    val session: SshSession,
    val hostId: Long,
    val hostname: String = "",
    val port: Int = 22,
    val username: String = "",
    val authTypeLabel: String = ""
)

@Composable
fun AppLayout(
    repository: VaultRepository,
    colorMode: ColorMode = ColorMode.DARK,
    onColorModeChange: (ColorMode) -> Unit = {},
    onNavigateToAddHost: () -> Unit,
    onNavigateToEditHost: (Long) -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(NavSection.SERVERS) }
    val terminalSessions = remember { mutableStateListOf<TerminalSession>() }
    var nextSessionId by remember { mutableStateOf(1) }
    val pagerState = rememberPagerState(pageCount = { terminalSessions.size })
    val webViewCache = remember { mutableMapOf<SshSession, WebView>() }
    val scope = rememberCoroutineScope()
    var fontSize by remember { mutableStateOf(14) }

    LaunchedEffect(Unit) {
        val settings = repository.getSettings()
        fontSize = settings.fontSize.toInt()
    }

    fun openTerminalSession(hostWithSecret: HostWithSecret) {
        val tabId = nextSessionId++
        val hostInfo = hostWithSecret.host
        val displayName = hostInfo.name
        RemoteLogger.i("AppLayout", "open terminal #$tabId $displayName")
        val session = SshSession()
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
                }
            )
        )
        selectedSection = NavSection.SESSIONS
        scope.launch {
            pagerState.animateScrollToPage(terminalSessions.size - 1)
        }
        scope.launch {
            startConnection(session, hostWithSecret)
        }
    }

    fun closeTerminalSession(index: Int) {
        if (index < 0 || index >= terminalSessions.size) return
        val ts = terminalSessions[index]
        RemoteLogger.i("AppLayout", "close terminal #${ts.id} ${ts.title}")
        cleanupTerminalSession(ts.session)
        webViewCache.remove(ts.session)?.destroy()
        CoroutineScope(Dispatchers.IO).launch { ts.session.close() }
        terminalSessions.removeAt(index)
    }

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showStats by remember { mutableStateOf(false) }
    val anySessionActive = terminalSessions.any {
        val s = it.session.state.value
        s == com.bastion.app.ssh.SessionState.SHELL_ACTIVE
    }

    Column(modifier = modifier.fillMaxSize().background(StitchBackground)) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Sidebar(
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it; searchQuery = "" },
                onNewInstance = onNavigateToAddHost
            )

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                AppHeader(
                    section = selectedSection,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onConnect = {
                        if (terminalSessions.isNotEmpty()) {
                            selectedSection = NavSection.SESSIONS
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage) }
                        }
                    },
                    terminalSessionsCount = terminalSessions.size
                )

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
                                            showStats = showStats,
                                            onToggleStats = { showStats = !showStats },
                                            fontSize = fontSize,
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
                                            onClose = { showStats = false }
                                        )
                                    }
                                }
                            }
                        }
                        NavSection.SSH_KEYS -> {
                            SSHKeysContent(
                                repository = repository,
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                modifier = Modifier.fillMaxSize()
                            )
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
            .background(StitchSurfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (collapsed) 8.dp else 16.dp, vertical = 16.dp),
            horizontalAlignment = if (collapsed) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(StitchSurfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        color = StitchPrimaryFixedDim,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!collapsed) {
                    Column {
                        Text(
                            text = "Bastion",
                            color = StitchPrimaryFixedDim,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Infrastructure Management",
                            color = StitchOnSurfaceVariant.copy(alpha = 0.6f),
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
                    .background(StitchPrimaryContainer)
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
                        tint = StitchOnPrimaryFixed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "New Connection",
                        color = StitchOnPrimaryFixed,
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
                    .background(StitchPrimaryContainer)
                    .clickable(onClick = onNewInstance),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Connection",
                    tint = StitchOnPrimaryFixed,
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
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lerna-admin/bastion/wiki"))
                        )
                    }
                )
                SidebarLinkItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    label = "Support",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lerna-admin/bastion/issues"))
                        )
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = StitchOutlineVariant
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
                        .background(StitchSurfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = StitchOnSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Not signed in",
                        color = StitchOnSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Sign in to manage profile",
                        color = StitchOnSurfaceVariant.copy(alpha = 0.3f),
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
    val bgColor = if (isSelected) StitchSurfaceContainerHigh else Color.Transparent
    val contentColor = if (isSelected) StitchPrimaryFixedDim else StitchOnSurfaceVariant

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
                            .background(StitchSecondaryContainer, RoundedCornerShape(1.dp))
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
            tint = StitchOnSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = StitchOnSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun AppHeader(
    section: NavSection,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onConnect: () -> Unit,
    terminalSessionsCount: Int
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(StitchSurfaceContainer)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StitchSurfaceContainerLow)
                        .border(1.dp, StitchOutlineVariant, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = StitchOnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            cursorBrush = SolidColor(StitchPrimaryContainer),
                            textStyle = TextStyle(
                                color = StitchOnSurface,
                                fontSize = 13.sp
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Global search...",
                                        color = StitchOnSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                IconButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lerna-admin/bastion"))
                        )
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = StitchOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StitchSecondaryContainer.copy(alpha = 0.2f))
                        .border(1.dp, StitchSecondaryContainer, RoundedCornerShape(4.dp))
                        .clickable(onClick = onConnect)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Connect",
                        color = StitchSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
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
            .background(StitchSurfaceContainerLowest)
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
                color = StitchPrimaryFixedDim,
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = "|",
                color = StitchOutlineVariant,
                fontSize = 11.sp
            )
            Text(
                text = sessionTitle,
                color = StitchOnSurfaceVariant,
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
                        .background(if (isConnected) StitchPrimaryContainer else StitchOutline)
                )
                Text(
                    text = if (isConnected) "Connected" else "Disconnected",
                    color = if (isConnected) StitchPrimaryFixedDim else StitchOnSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            Text(
                text = "UTF-8",
                color = StitchOnSurfaceVariant,
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
    showStats: Boolean,
    onToggleStats: () -> Unit,
    fontSize: Int = 14,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SessionTabBar(
            sessions = sessions,
            activeSessionIndex = pagerState.currentPage,
            onTabClick = { index ->
                kotlinx.coroutines.MainScope().launch { pagerState.animateScrollToPage(index) }
            },
            onTabClose = onCloseSession,
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
    onToggleStats: () -> Unit,
    showStats: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StitchSurfaceContainerLowest)
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
                                if (isActive) StitchSurfaceContainerHigh else Color.Transparent
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Devices,
                            contentDescription = null,
                            tint = if (isActive) StitchPrimaryFixedDim else StitchOnSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = session.title,
                            color = if (isActive) StitchPrimaryFixedDim else StitchOnSurfaceVariant,
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
                                tint = StitchOnSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (isActive) StitchPrimaryContainer else Color.Transparent
                            )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (showStats) StitchSurfaceContainerHigh else Color.Transparent
                )
                .clickable(onClick = onToggleStats),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "S",
                color = if (showStats) StitchPrimaryFixedDim else StitchOnSurfaceVariant,
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
                tint = StitchOutline,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No active sessions",
                color = StitchOnSurfaceVariant,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Connect to a server to start a terminal session",
                color = StitchOnSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBrowseServers) {
                Text(
                    "Browse servers",
                    color = StitchPrimaryContainer,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private suspend fun startConnection(session: SshSession, host: HostWithSecret) {
    val log = RemoteLogger.logger("Connection")
    log.i("start ${host.host.username}@${host.host.hostname}:${host.host.port}")
    withContext(Dispatchers.IO) {
        try {
            val keyPair = if (host.host.authType == com.bastion.app.data.AuthType.PUBLIC_KEY) {
                host.privateKeyPem?.let { pem ->
                    try {
                        loadKeyPairFromPem(pem, host.privateKeyPassphrase)
                    } catch (e: Exception) {
                        log.e("key load failed: ${e.message}", e)
                        session.setError(
                            phase = "key_load",
                            message = "Error loading private key: ${e.message}",
                            exception = e
                        )
                        return@withContext
                    }
                }
            } else {
                null
            }

            val config = AuthConfig(
                hostname = host.host.hostname,
                port = host.host.port,
                username = host.host.username,
                password = host.password,
                keyPair = keyPair
            )

            session.connect(config).onSuccess {
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
