package com.bastion.app.ui

import android.content.Context
import android.widget.Toast
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
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
import com.bastion.app.terminal.TerminalTab
import com.bastion.app.ui.theme.StitchOnSurface
import com.bastion.app.ui.theme.StitchOnSurfaceVariant
import com.bastion.app.ui.theme.StitchOutlineVariant
import com.bastion.app.ui.theme.StitchPrimary
import com.bastion.app.ui.theme.StitchSecondary
import com.bastion.app.ui.theme.StitchSurfaceContainer
import com.bastion.app.ui.theme.StitchSurfaceContainerHigh
import com.bastion.app.ui.theme.StitchSurfaceContainerHighest
import com.bastion.app.ui.theme.StitchSurfaceContainerLowest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NavSection(val label: String, val icon: ImageVector) {
    SERVERS("Servers", Icons.Default.Dns),
    SSH_KEYS("SSH Keys", Icons.Default.VpnKey),
    SESSIONS("Sessions", Icons.Default.Devices),
    SETTINGS("Settings", Icons.Default.Settings)
}

private data class TerminalSession(
    val id: Int,
    val title: String,
    val session: SshSession,
    val hostId: Long
)

@Composable
fun AppLayout(
    repository: VaultRepository,
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

    fun openTerminalSession(hostWithSecret: HostWithSecret) {
        val tabId = nextSessionId++
        val hostname = hostWithSecret.host.name
        RemoteLogger.i("AppLayout", "open terminal #$tabId $hostname")
        val session = SshSession()
        terminalSessions.add(
            TerminalSession(tabId, hostname, session, hostWithSecret.host.id)
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
        webViewCache.remove(ts.session)?.destroy()
        CoroutineScope(Dispatchers.IO).launch { ts.session.close() }
        terminalSessions.removeAt(index)
    }

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    Row(modifier = modifier.fillMaxSize()) {
        Sidebar(
            selectedSection = selectedSection,
            onSectionSelected = { selectedSection = it; searchQuery = "" },
            onNewInstance = onNavigateToAddHost
        )

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (selectedSection != NavSection.SESSIONS) {
                AppHeader(
                    section = selectedSection,
                    showSearch = selectedSection == NavSection.SSH_KEYS,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onHelp = {
                        Toast.makeText(context, "Help & Documentation", Toast.LENGTH_SHORT).show()
                    },
                    onNotifications = {
                        Toast.makeText(context, "No new notifications", Toast.LENGTH_SHORT).show()
                    }
                )
            }

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
                            TerminalPagerContent(
                                sessions = terminalSessions,
                                pagerState = pagerState,
                                webViewCache = webViewCache,
                                onCloseSession = { closeTerminalSession(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    NavSection.SSH_KEYS -> {
                        SSHKeysContent(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    NavSection.SETTINGS -> {
                        SettingsContent(modifier = Modifier.fillMaxSize())
                    }
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
    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .drawWithContent {
                drawContent()
                drawLine(StitchOutlineVariant, Offset.Zero, Offset(0f, size.height), strokeWidth = 1.dp.toPx())
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = StitchOnSurface.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Bastion",
                        color = StitchPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Infrastructure Management",
                        color = StitchOnSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        NavSection.entries.forEach { section ->
            SidebarNavItem(
                icon = section.icon,
                label = section.label,
                isSelected = section == selectedSection,
                onClick = { onSectionSelected(section) }
            )
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StitchPrimary)
                .clickable(onClick = onNewInstance)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = Color(0xFF003548),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "New Instance",
                    color = Color(0xFF003548),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SidebarLinkItem(
                icon = Icons.Default.Description,
                label = "Documentation",
                onClick = { }
            )
            SidebarLinkItem(
                icon = Icons.AutoMirrored.Filled.Help,
                label = "Support",
                onClick = { }
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
                    tint = StitchOnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Admin User",
                    color = StitchOnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "System Superuser",
                    color = StitchOnSurfaceVariant,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = StitchPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) StitchSecondary.copy(alpha = 0.1f) else Color.Transparent
    val contentColor = if (isSelected) StitchSecondary else StitchOnSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(20.dp)
                    .background(StitchSecondary, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(7.dp))
        }
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
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
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StitchOnSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = StitchOnSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AppHeader(
    section: NavSection,
    showSearch: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onHelp: () -> Unit,
    onNotifications: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.background)
            .drawWithContent {
                drawContent()
                val y = size.height - 1.dp.toPx()
                drawLine(StitchOutlineVariant, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSearch) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, StitchOutlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null,
                            tint = StitchOnSurfaceVariant, modifier = Modifier.size(18.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            cursorBrush = SolidColor(StitchPrimary),
                            textStyle = TextStyle(color = StitchOnSurface, fontSize = 14.sp),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search keys, fingerprints, or servers...",
                                        color = StitchOnSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 14.sp)
                                }
                                innerTextField()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Text(
                    text = section.label,
                    color = StitchOnSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onHelp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = "Help",
                        tint = StitchOnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box {
                    IconButton(onClick = onNotifications) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = StitchOnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StitchSecondary)
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp, top = 4.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(StitchSurfaceContainerHighest)
                        .border(1.dp, StitchOutlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = StitchOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalPagerContent(
    sessions: List<TerminalSession>,
    pagerState: PagerState,
    webViewCache: MutableMap<SshSession, WebView>,
    onCloseSession: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        TerminalTabBar(
            sessions = sessions,
            activeSessionIndex = pagerState.currentPage,
            onTabClick = { index ->
                kotlinx.coroutines.MainScope().launch { pagerState.animateScrollToPage(index) }
            },
            onTabClose = onCloseSession
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            val session = sessions[page]
            TerminalTab(
                session = session.session,
                webViewCache = webViewCache,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun TerminalTabBar(
    sessions: List<TerminalSession>,
    activeSessionIndex: Int,
    onTabClick: (Int) -> Unit,
    onTabClose: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StitchSurfaceContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(sessions, key = { _, s -> s.id }) { index, session ->
                val isActive = index == activeSessionIndex
                val textColor = if (isActive) StitchPrimary else Color(0xFF777777)

                Row(
                    modifier = Modifier
                        .clickable { onTabClick(index) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Devices,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = session.title,
                        color = textColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(100.dp)
                    )
                    IconButton(
                        onClick = { onTabClose(index) },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF555555),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
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
                tint = Color(0xFF555555),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No active sessions",
                color = Color(0xFF888888),
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Connect to a server to start a terminal session",
                color = Color(0xFF666666),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBrowseServers) {
                Text(
                    "Browse servers",
                    color = StitchPrimary,
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
