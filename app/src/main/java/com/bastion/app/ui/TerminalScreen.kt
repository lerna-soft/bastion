package com.bastion.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bastion.app.data.Host
import com.bastion.app.data.HostWithSecret
import com.bastion.app.data.VaultRepository
import com.bastion.app.ssh.SshSession
import com.bastion.app.ssh.AuthConfig
import com.bastion.app.ssh.BastionKeyIdentityProvider
import com.bastion.app.terminal.TerminalTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import org.apache.sshd.common.keyprovider.KeyPairProvider

private data class TabData(
    val id: Int,
    val hostName: String,
    val hostId: Long,
    val session: SshSession
)

@Composable
fun TerminalScreen(
    repository: VaultRepository,
    initialHostId: Long?,
    modifier: Modifier = Modifier
) {
    val tabs = remember { mutableStateListOf<TabData>() }
    var activeTabIndex by remember { mutableStateOf(0) }
    var nextTabId by remember { mutableStateOf(0) }

    // Open initial host if provided
    androidx.compose.runtime.LaunchedEffect(initialHostId) {
        if (initialHostId != null && tabs.isEmpty()) {
            val hostWithSecret = repository.getHostWithSecret(initialHostId)
            if (hostWithSecret != null) {
                val tabId = nextTabId++
                val session = createSession(hostWithSecret)
                tabs.add(TabData(tabId, hostWithSecret.host.name, initialHostId, session))
                activeTabIndex = tabs.size - 1
            }
        }
    }

    Scaffold(
        topBar = {
            // Tab bar
            if (tabs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2D2D2D))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(tabs, key = { _, tab -> tab.id }) { index, tab ->
                            TabChip(
                                title = tab.hostName,
                                isActive = index == activeTabIndex,
                                onClick = { activeTabIndex = index },
                                onClose = {
                                    closeTab(tabs, tab, index) { newIdx -> activeTabIndex = newIdx }
                                }
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            // Show vault picker to add new tab
                        }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Nueva pestaña",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        if (tabs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Selecciona un host del vault para conectar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                tabs.forEachIndexed { index, tab ->
                    if (index == activeTabIndex) {
                        TerminalTab(
                            session = tab.session,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val bgColor = if (isActive) Color(0xFF1E1E1E) else Color(0xFF3D3D3D)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = Color.Gray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun closeTab(
    tabs: MutableList<TabData>,
    tab: TabData,
    index: Int,
    onActiveChange: (Int) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        tab.session.close()
    }
    tabs.removeAt(index)
    if (tabs.isEmpty()) {
        onActiveChange(0)
    } else if (index >= tabs.size) {
        onActiveChange(tabs.size - 1)
    } else {
        onActiveChange(index)
    }
}

private fun createSession(host: HostWithSecret): SshSession {
    val session = SshSession()
    CoroutineScope(Dispatchers.IO).launch {
        val config = AuthConfig(
            hostname = host.host.hostname,
            port = host.host.port,
            username = host.host.username,
            password = host.password
        )
        session.connect(config)
        session.openShell()
    }
    return session
}
