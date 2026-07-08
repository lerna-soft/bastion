package com.bastion.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bastion.app.data.SshKey
import com.bastion.app.data.VaultRepository
import com.bastion.app.ui.theme.StitchOnSurface
import com.bastion.app.ui.theme.StitchOnSurfaceVariant
import com.bastion.app.ui.theme.StitchOutlineVariant
import com.bastion.app.ui.theme.StitchOnPrimaryContainer
import com.bastion.app.ui.theme.StitchPrimary
import com.bastion.app.ui.theme.StitchPrimaryContainer
import com.bastion.app.ui.theme.StitchPrimaryFixedDim
import com.bastion.app.ui.theme.StitchSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SSHKeysContent(
    repository: VaultRepository,
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val keys by repository.getAllSshKeys().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<SshKey?>(null) }
    var editTarget by remember { mutableStateOf<SshKey?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filteredKeys = if (searchQuery.isBlank()) keys
    else keys.filter { it.name.contains(searchQuery, ignoreCase = true) ||
        it.fingerprint.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HeaderSection(
            keyCount = keys.size,
            activeCount = keys.count { it.isActive },
            onAddKey = { showAddDialog = true }
        )
        TableSection(
            keys = filteredKeys,
            onCopy = { key ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("SSH Fingerprint", key.fingerprint))
                Toast.makeText(context, "Fingerprint copied: ${key.name}", Toast.LENGTH_SHORT).show()
            },
            onEdit = { editTarget = it },
            onDelete = { deleteTarget = it }
        )
        Spacer(Modifier.height(24.dp))
        StatsRow(keyCount = keys.size, activeCount = keys.count { it.isActive })
    }

    if (showAddDialog) {
        AddKeyDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type ->
                scope.launch {
                    val fingerprint = withContext(Dispatchers.IO) {
                        "SHA256:${name.lowercase().replace(" ", "")}:${System.currentTimeMillis().toString().takeLast(8)}"
                    }
                    repository.addSshKey(name.trim(), type.trim(), fingerprint, "")
                    showAddDialog = false
                    Toast.makeText(context, "SSH key added: $name", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    editTarget?.let { key ->
        var newName by remember(key) { mutableStateOf(key.name) }
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Edit SSH Key") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it },
                    label = { Text("Key Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        scope.launch {
                            repository.renameSshKey(key.id, newName.trim())
                            editTarget = null
                            Toast.makeText(context, "Key renamed: ${key.name} \u2192 ${newName.trim()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Cancel") } }
        )
    }

    deleteTarget?.let { key ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete SSH Key") },
            text = { Text("Are you sure you want to delete \"${key.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deleteSshKey(key.id)
                        deleteTarget = null
                        Toast.makeText(context, "Key deleted: ${key.name}", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    return sdf.format(Date(millis))
}

private fun formatLastUsed(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}

@Composable
private fun HeaderSection(
    keyCount: Int,
    activeCount: Int,
    onAddKey: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "INFRASTRUCTURE",
                color = StitchOnSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = StitchOnSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "SSH KEYS",
                color = StitchPrimaryFixedDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "SSH Keys",
                    style = MaterialTheme.typography.headlineMedium,
                    color = StitchOnSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StitchSecondary.copy(alpha = 0.1f))
                        .border(1.dp, StitchSecondary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$activeCount Active",
                        color = StitchSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchPrimaryContainer)
                    .clickable(onClick = onAddKey)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = StitchOnPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Add SSH Key",
                        color = StitchOnPrimaryContainer,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AddKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Ed25519") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add SSH Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Key Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Algorithm (e.g. Ed25519, RSA 4096)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(name.trim(), type.trim())
            }) { Text("Add Key") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun TableSection(
    keys: List<SshKey>,
    onCopy: (SshKey) -> Unit,
    onEdit: (SshKey) -> Unit,
    onDelete: (SshKey) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, StitchOutlineVariant, RoundedCornerShape(12.dp))
    ) {
        TableHeader()
        HorizontalDivider(color = StitchOutlineVariant, thickness = 1.dp)
        LazyColumn {
            itemsIndexed(keys) { index, key ->
                KeyRow(key, onCopy, onEdit, onDelete)
                if (index < keys.lastIndex) {
                    HorizontalDivider(
                        color = StitchOutlineVariant.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Key Name", color = StitchOnSurfaceVariant, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1.8f))
        Text("Fingerprint", color = StitchOnSurfaceVariant, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1.5f))
        Text("Servers", color = StitchOnSurfaceVariant, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1.2f))
        Text("Created", color = StitchOnSurfaceVariant, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
        Text("Actions", color = StitchOnSurfaceVariant, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.weight(0.8f))
    }
}

@Composable
private fun KeyRow(
    key: SshKey,
    onCopy: (SshKey) -> Unit,
    onEdit: (SshKey) -> Unit,
    onDelete: (SshKey) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.8f)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.VpnKey, contentDescription = null, tint = StitchOnSurfaceVariant,
                    modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(key.name, color = StitchOnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(key.type, color = StitchOnSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Box(modifier = Modifier.weight(1.5f).clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(0.5.dp, StitchOutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(key.fingerprint, color = StitchOnSurfaceVariant, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }

        Row(modifier = Modifier.weight(1.2f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val serverList = key.servers.split(",").filter { it.isNotBlank() }
            serverList.take(2).forEach { server ->
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(0.5.dp, StitchOutlineVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(server.trim(), color = StitchOnSurfaceVariant, fontSize = 10.sp)
                }
            }
            if (serverList.size > 2) {
                Text("+${serverList.size - 2}", color = StitchOnSurfaceVariant, fontSize = 10.sp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(formatDate(key.created), color = StitchOnSurface, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (key.isActive) {
                    Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(StitchSecondary))
                    Text("Used ${formatLastUsed(key.lastUsed)}", color = StitchSecondary, fontSize = 11.sp)
                } else {
                    Text("Used ${formatLastUsed(key.lastUsed)}", color = StitchOnSurfaceVariant, fontSize = 11.sp)
                }
            }
        }

        Row(
            modifier = Modifier.weight(0.8f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Fingerprint",
                tint = StitchOnSurfaceVariant,
                modifier = Modifier.size(20.dp).clickable { onCopy(key) }.padding(4.dp))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = StitchOnSurfaceVariant,
                modifier = Modifier.size(20.dp).clickable { onEdit(key) }.padding(4.dp))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp).clickable { onDelete(key) }.padding(4.dp))
        }
    }
}

@Composable
private fun StatsRow(keyCount: Int, activeCount: Int) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            title = "TOTAL KEYS", value = "$keyCount Keys", valueColor = StitchSecondary,
            subtitle = "$activeCount active, ${keyCount - activeCount} inactive.",
            icon = Icons.Filled.VpnKey, modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "ACTIVE KEYS", value = "$activeCount Active", valueColor = StitchPrimaryFixedDim,
            subtitle = "Keys currently in use for SSH connections.",
            icon = Icons.Filled.VerifiedUser, modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "KEY USAGE LOG", value = "$keyCount Total", valueColor = StitchPrimaryFixedDim,
            subtitle = "Manage your SSH key infrastructure.",
            icon = Icons.Filled.History, modifier = Modifier.weight(1f),
            linkText = "View Audit History",
            onLinkClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lerna-admin/bastion/security"))
                )
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String, value: String, valueColor: Color, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    linkText: String? = null,
    onLinkClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, StitchOutlineVariant, RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Text(title, color = StitchOnSurfaceVariant, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Text(value, color = valueColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = StitchOnSurfaceVariant, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (linkText != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable(onClick = onLinkClick)) {
                Text(linkText, color = StitchPrimaryContainer, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    tint = StitchPrimaryContainer, modifier = Modifier.size(14.dp))
            }
        }
    }
}
