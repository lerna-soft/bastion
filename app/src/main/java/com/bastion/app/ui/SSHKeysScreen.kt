package com.bastion.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.bastion.app.ui.theme.StitchOnSurface
import com.bastion.app.ui.theme.StitchOnSurfaceVariant
import com.bastion.app.ui.theme.StitchOutlineVariant
import com.bastion.app.ui.theme.StitchPrimary
import com.bastion.app.ui.theme.StitchSecondary

private data class SshKeyEntry(
    val name: String,
    val type: String,
    val fingerprint: String,
    val servers: List<String>,
    val created: String,
    val lastUsed: String,
    val isActive: Boolean
)

@Composable
fun SSHKeysContent(
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<SshKeyEntry?>(null) }
    var editTarget by remember { mutableStateOf<SshKeyEntry?>(null) }
    var keys by remember { mutableStateOf(defaultSampleKeys()) }
    val context = LocalContext.current

    val filteredKeys = if (searchQuery.isBlank()) keys
    else keys.filter { it.name.contains(searchQuery, ignoreCase = true) ||
        it.fingerprint.contains(searchQuery, ignoreCase = true) ||
        it.servers.any { s -> s.contains(searchQuery, ignoreCase = true) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HeaderSection(
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
        StatsRow()
    }

    if (showAddDialog) {
        AddKeyDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type ->
                val newKey = SshKeyEntry(
                    name = name,
                    type = type,
                    fingerprint = "SHA256:new:key:...generated",
                    servers = emptyList(),
                    created = "Just now",
                    lastUsed = "Now",
                    isActive = true
                )
                keys = listOf(newKey) + keys
                showAddDialog = false
                Toast.makeText(context, "SSH key added: $name", Toast.LENGTH_SHORT).show()
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
                        keys = keys.map { if (it.name == key.name) it.copy(name = newName.trim()) else it }
                        editTarget = null
                        Toast.makeText(context, "Key renamed: ${key.name} \u2192 ${newName.trim()}", Toast.LENGTH_SHORT).show()
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
                    keys = keys.filter { it.name != key.name }
                    deleteTarget = null
                    Toast.makeText(context, "Key deleted: ${key.name}", Toast.LENGTH_SHORT).show()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

private fun defaultSampleKeys(): List<SshKeyEntry> = listOf(
    SshKeyEntry("prod-deploy-key", "RSA 4096", "SHA256:f1:2a:b3:d4...c5",
        listOf("us-east-web-01", "us-east-db-02"), "Oct 12, 2023", "2m ago", true),
    SshKeyEntry("backup-sync", "Ed25519", "SHA256:e9:2a:b3:d4...z0",
        listOf("backup-vault-01"), "Aug 24, 2023", "4d ago", false),
    SshKeyEntry("dev-macbook", "Ed25519", "SHA256:d5:1a:b3:d4...w2",
        listOf("staging-k8s", "dev-cluster-01"), "Jan 05, 2024", "12h ago", false),
)

@Composable
private fun HeaderSection(
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
                color = StitchPrimary,
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
                        text = "4 Active",
                        color = StitchSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchPrimary)
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
                        tint = Color(0xFF003548),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Add SSH Key",
                        color = Color(0xFF003548),
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
    keys: List<SshKeyEntry>,
    onCopy: (SshKeyEntry) -> Unit,
    onEdit: (SshKeyEntry) -> Unit,
    onDelete: (SshKeyEntry) -> Unit
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
    key: SshKeyEntry,
    onCopy: (SshKeyEntry) -> Unit,
    onEdit: (SshKeyEntry) -> Unit,
    onDelete: (SshKeyEntry) -> Unit
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
            key.servers.take(2).forEach { server ->
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(0.5.dp, StitchOutlineVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(server, color = StitchOnSurfaceVariant, fontSize = 10.sp)
                }
            }
            if (key.servers.size > 2) {
                Text("+${key.servers.size - 2}", color = StitchOnSurfaceVariant, fontSize = 10.sp)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(key.created, color = StitchOnSurface, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (key.isActive) {
                    Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(StitchSecondary))
                    Text("Used ${key.lastUsed}", color = StitchSecondary, fontSize = 11.sp)
                } else {
                    Text("Used ${key.lastUsed}", color = StitchOnSurfaceVariant, fontSize = 11.sp)
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
private fun StatsRow() {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            title = "SECURITY STATUS", value = "98%", valueColor = StitchSecondary,
            subtitle = "All keys are compliant with RSA 4096 or Ed25519 standards.",
            icon = Icons.Filled.VerifiedUser, modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "RECENT ROTATION", value = "2 Keys", valueColor = StitchPrimary,
            subtitle = "Successfully rotated in the last 30 days.",
            icon = Icons.Filled.Sync, modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "KEY USAGE LOG", value = "245 Events", valueColor = StitchPrimary,
            subtitle = "View detailed audit logs of all SSH authentication attempts.",
            icon = Icons.Filled.History, modifier = Modifier.weight(1f),
            linkText = "View Audit History",
            onLinkClick = {
                Toast.makeText(context, "Opening Audit History...", Toast.LENGTH_SHORT).show()
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
                Text(linkText, color = StitchPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    tint = StitchPrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}
