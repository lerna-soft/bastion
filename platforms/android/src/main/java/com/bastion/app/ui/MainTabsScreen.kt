package com.bastion.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bastion.app.data.AuthType
import com.bastion.app.data.Host
import com.bastion.app.data.HostWithSecret
import com.bastion.app.data.VaultRepository
import com.bastion.app.logging.RemoteLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File



@Composable
internal fun VaultTabContent(
    repository: VaultRepository,
    onAddHost: () -> Unit,
    onEditHost: (Long) -> Unit,
    onConnect: (HostWithSecret) -> Unit,
    modifier: Modifier = Modifier
) {
    val hosts by repository.getAllHosts().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var crashText by remember { mutableStateOf<String?>(null) }

    // Leer (y consumir) el crash log una sola vez por entrada a la pantalla. Antes esto vivía en el
    // cuerpo del composable, así que se relanzaba en CADA recomposición; como lee-y-borra el archivo,
    // cualquier recomposición temprana podía consumir el stack trace antes de mostrarlo.
    LaunchedEffect(Unit) {
        crashText = withContext(Dispatchers.IO) {
            val f = File(context.filesDir, "bastion_crash.log")
            if (f.exists()) f.readText().also { f.delete() } else null
        }
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (crashText != null) {
                CrashBanner(
                    crashText = crashText!!,
                    onDismiss = { crashText = null },
                    context = context
                )
            }

            if (hosts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Devices,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF555555)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No hay servidores",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF888888)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Toca + para agregar tu primer servidor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hosts, key = { it.id }) { host ->
                        VaultHostCard(
                            host = host,
                            onClick = {
                                scope.launch {
                                    val hws = repository.getHostWithSecret(host.id)
                                    if (hws != null) onConnect(hws)
                                }
                            },
                            onEdit = { onEditHost(host.id) },
                            onDelete = {
                                scope.launch { repository.deleteHost(host.id) }
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddHost,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF4FC3F7)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Agregar host", tint = Color.Black)
        }
    }
}

@Composable
internal fun VaultHostCard(
    host: Host,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(hostColor(host.name)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = host.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${host.username}@${host.hostname}:${host.port}",
                    color = Color(0xFF999999),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = authIcon(host.authType),
                contentDescription = null,
                tint = Color(0xFF666666),
                modifier = Modifier.size(22.dp)
            )
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

internal val colorPalette = listOf(
    Color(0xFF1976D2),
    Color(0xFF388E3C),
    Color(0xFFD32F2F),
    Color(0xFFF57C00),
    Color(0xFF7B1FA2),
    Color(0xFF0097A7),
    Color(0xFFC2185B),
    Color(0xFF689F38)
)

internal fun hostColor(hostName: String): Color = colorPalette[hostName.hashCode() and 0x7FFFFFFF % colorPalette.size]

@Composable
internal fun CrashBanner(crashText: String, onDismiss: () -> Unit, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF330000))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF4444),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Crash detectado en sesión anterior",
                color = Color(0xFFFF6666),
                fontSize = 14.sp
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Text("X", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = crashText.take(500),
            color = Color(0xFFAAAAAA),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 8
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clip.setPrimaryClip(ClipData.newPlainText("Bastion Crash", crashText))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Copiar crash", color = Color.White, fontSize = 12.sp)
        }
    }
}

internal fun authIcon(type: AuthType): ImageVector = when (type) {
    AuthType.PASSWORD -> Icons.Default.Lock
    AuthType.PUBLIC_KEY -> Icons.Default.Key
    AuthType.AGENT_FORWARD -> Icons.Default.Shield
}
