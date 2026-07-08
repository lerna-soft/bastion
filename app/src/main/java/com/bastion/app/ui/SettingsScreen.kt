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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bastion.app.ui.theme.StitchOnSurface
import com.bastion.app.ui.theme.StitchOnSurfaceVariant
import com.bastion.app.ui.theme.StitchOutlineVariant
import com.bastion.app.ui.theme.StitchPrimary
import com.bastion.app.ui.theme.StitchSecondary
import com.bastion.app.ui.theme.StitchSurfaceContainerLow

private val settingsSections = listOf(
    "general" to "General", "notifications" to "Notifications",
    "appearance" to "Appearance", "security" to "Security",
    "api-keys" to "API Keys", "about" to "About"
)

@Composable
fun SettingsContent(modifier: Modifier = Modifier) {
    var activeSection by remember { mutableStateOf("general") }

    Row(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        SettingsSidebar(activeSection = activeSection, onSectionSelected = { activeSection = it })

        Column(
            modifier = Modifier.weight(1f).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            when (activeSection) {
                "general" -> GeneralSection()
                "notifications" -> NotificationsSection()
                "appearance" -> AppearanceSection()
                "security" -> SecuritySection()
                "api-keys" -> ApiKeysSection()
                "about" -> AboutSection()
            }
        }
    }
}

@Composable
private fun SettingsSidebar(activeSection: String, onSectionSelected: (String) -> Unit) {
    val icons = mapOf(
        "general" to Icons.Default.Settings, "notifications" to Icons.Default.NotificationsActive,
        "appearance" to Icons.Default.Palette, "security" to Icons.Default.Security,
        "api-keys" to Icons.Default.VpnKey, "about" to Icons.Default.Info
    )

    Column(
        modifier = Modifier.width(220.dp).fillMaxHeight().padding(end = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        settingsSections.forEach { (id, label) ->
            val isActive = id == activeSection
            val bg = if (isActive) StitchSecondary.copy(alpha = 0.1f) else Color.Transparent
            val textColor = if (isActive) StitchSecondary else StitchOnSurfaceVariant
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg)
                    .clickable { onSectionSelected(id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icons[id] ?: Icons.Default.Settings, contentDescription = null,
                    tint = textColor, modifier = Modifier.size(20.dp))
                Text(label, color = textColor, fontSize = 12.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(StitchSurfaceContainerLow)
            .border(1.dp, StitchOutlineVariant, RoundedCornerShape(12.dp)).padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = StitchOnSurface,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label.uppercase(), color = StitchOnSurfaceVariant, fontSize = 11.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        SectionLabel(label)
        Spacer(Modifier.height(6.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StitchPrimary,
                    unfocusedBorderColor = StitchOutlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedTextColor = StitchOnSurface,
                    unfocusedTextColor = StitchOnSurface
                ),
                shape = RoundedCornerShape(8.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = StitchOnSurface) },
                        onClick = { onSelected(option); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun TextField(label: String, value: String, onValueChange: (String) -> Unit,
                      monospace: Boolean = false, password: Boolean = false,
                      modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SectionLabel(label)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = if (password) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                color = StitchOnSurface
            ),
            modifier = modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StitchPrimary,
                unfocusedBorderColor = StitchOutlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedTextColor = StitchOnSurface,
                unfocusedTextColor = StitchOnSurface
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun ToggleRow(title: String, description: String, checked: Boolean,
                      onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = StitchOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(description, color = StitchOnSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = StitchSecondary,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = StitchOutlineVariant
            ))
    }
}

@Composable
private fun GeneralSection() {
    var serverName by remember { mutableStateOf("BASTION-PRIME-01") }
    var timezone by remember { mutableStateOf("EST (Eastern Standard Time)") }
    var language by remember { mutableStateOf("English (United States)") }

    SectionCard(title = "General Configuration", icon = Icons.Default.Settings) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextField(label = "Server Name", value = serverName, onValueChange = { serverName = it },
                modifier = Modifier.weight(1f))
            SelectField(label = "Timezone", options = listOf(
                "UTC (Coordinated Universal Time)", "EST (Eastern Standard Time)",
                "PST (Pacific Standard Time)", "GMT (Greenwich Mean Time)"
            ), selected = timezone, onSelected = { timezone = it })
        }
        Spacer(Modifier.height(16.dp))
        SelectField(label = "Language", options = listOf(
            "English (United States)", "German (Deutsch)", "Japanese (日本語)", "Spanish (Español)"
        ), selected = language, onSelected = { language = it })
    }
}

@Composable
private fun NotificationsSection() {
    var webhookUrl by remember { mutableStateOf("https://hooks.slack.com/services/T00...") }
    var emailAlerts by remember { mutableStateOf(false) }

    SectionCard(title = "Notifications", icon = Icons.Default.NotificationsActive) {
        TextField(label = "Slack Webhook URL", value = webhookUrl,
            onValueChange = { webhookUrl = it }, monospace = true, password = true)
        Spacer(Modifier.height(16.dp))
        ToggleRow(title = "Email Alerts",
            description = "Receive critical system health reports via email",
            checked = emailAlerts, onCheckedChange = { emailAlerts = it })
    }
}

@Composable
private fun AppearanceSection() {
    var isDarkMode by remember { mutableStateOf(true) }
    var fontSize by remember { mutableStateOf(14f) }

    SectionCard(title = "Appearance", icon = Icons.Default.Palette) {
        SectionLabel("Color Mode")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .border(if (isDarkMode) 2.dp else 1.dp,
                        if (isDarkMode) StitchPrimary else StitchOutlineVariant,
                        RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { isDarkMode = true }.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceDim)
                        .border(1.dp, StitchOutlineVariant, RoundedCornerShape(6.dp)))
                    Spacer(Modifier.height(8.dp))
                    Text("Dark Mode", color = if (isDarkMode) StitchPrimary else StitchOnSurfaceVariant,
                        fontSize = 13.sp, fontWeight = if (isDarkMode) FontWeight.Bold else FontWeight.Normal)
                }
            }
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .border(if (!isDarkMode) 2.dp else 1.dp,
                        if (!isDarkMode) StitchPrimary else StitchOutlineVariant,
                        RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { isDarkMode = false }.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color.White).border(1.dp, StitchOutlineVariant, RoundedCornerShape(6.dp)))
                    Spacer(Modifier.height(8.dp))
                    Text("Light Mode", color = if (!isDarkMode) StitchPrimary else StitchOnSurfaceVariant,
                        fontSize = 13.sp, fontWeight = if (!isDarkMode) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("Font Size")
            Text("${fontSize.toInt()}px", color = StitchPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(8.dp))
        Slider(value = fontSize, onValueChange = { fontSize = it },
            valueRange = 12f..20f, steps = 7,
            colors = SliderDefaults.colors(thumbColor = StitchPrimary,
                activeTrackColor = StitchPrimary, inactiveTrackColor = StitchOutlineVariant))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Compact", color = StitchOnSurfaceVariant, fontSize = 10.sp)
            Text("Default", color = StitchOnSurfaceVariant, fontSize = 10.sp)
            Text("Large", color = StitchOnSurfaceVariant, fontSize = 10.sp)
        }
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, StitchOutlineVariant, RoundedCornerShape(8.dp)).padding(16.dp)) {
            Column {
                Text("Preview Terminal Text:", color = StitchOnSurfaceVariant, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Text("[admin@bastion ~]$ ssh root@server-01\nAuthentication successful.",
                    color = StitchSecondary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun SecuritySection() {
    var twoFactor by remember { mutableStateOf(true) }
    var sessionTimeout by remember { mutableStateOf("30 Minutes") }

    SectionCard(title = "Security", icon = Icons.Default.Security) {
        ToggleRow(title = "Two-Factor Authentication",
            description = "Require a TOTP token for all admin logins",
            checked = twoFactor, onCheckedChange = { twoFactor = it })
        Spacer(Modifier.height(16.dp))
        SelectField(label = "Session Timeout", options = listOf(
            "15 Minutes", "30 Minutes", "1 Hour", "4 Hours"
        ), selected = sessionTimeout, onSelected = { sessionTimeout = it })
    }
}

@Composable
private fun ApiKeysSection() {
    val context = LocalContext.current
    var showCreateKey by remember { mutableStateOf(false) }
    var apiKeys by remember { mutableStateOf(listOf(
        "Production-Main" to "bs_live_••••••••••••x8y2" to "Oct 12, 2023",
        "Staging-Webhook" to "bs_test_••••••••••••a4f1" to "Jan 05, 2024"
    )) }

    SectionCard(title = "API Management", icon = Icons.Default.VpnKey) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Manage API Keys")
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { showCreateKey = true }) {
                Icon(Icons.Default.Add, contentDescription = null, tint = StitchPrimary, modifier = Modifier.size(16.dp))
                Text("Create New Key", color = StitchPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        apiKeys.forEachIndexed { index, entry ->
            val (labelKeyPair, created) = entry
            val (label, keyValue) = labelKeyPair
            ApiKeyRow(label, keyValue, created,
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("API Key", keyValue))
                    Toast.makeText(context, "API key copied: $label", Toast.LENGTH_SHORT).show()
                },
                onRevoke = {
                    apiKeys = apiKeys.filterIndexed { i, _ -> i != index }
                    Toast.makeText(context, "API key revoked: $label", Toast.LENGTH_SHORT).show()
                }
            )
            if (index < apiKeys.lastIndex) {
                HorizontalDivider(color = StitchOutlineVariant, thickness = 1.dp)
            }
        }
    }

    if (showCreateKey) {
        var keyLabel by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateKey = false },
            title = { Text("Create New API Key") },
            text = {
                OutlinedTextField(value = keyLabel, onValueChange = { keyLabel = it },
                    label = { Text("Key Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (keyLabel.isNotBlank()) {
                        val newKey = "${keyLabel.lowercase().replace(" ", "-")}_••••••••••••xxxx"
                        apiKeys = apiKeys + ((keyLabel.trim() to newKey) to "Just now")
                        showCreateKey = false
                        Toast.makeText(context, "API key created: ${keyLabel.trim()}", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateKey = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ApiKeyRow(label: String, keyValue: String, created: String,
                      onCopy: () -> Unit, onRevoke: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = StitchOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f))
        Text(keyValue, color = StitchOnSurfaceVariant, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1.5f))
        Text(created, color = StitchOnSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy",
                tint = StitchPrimary, modifier = Modifier.size(20.dp).clickable(onClick = onCopy).padding(2.dp))
            Icon(Icons.Default.DeleteForever, contentDescription = "Revoke",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp).clickable(onClick = onRevoke).padding(2.dp))
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(StitchSurfaceContainerLow)
            .border(1.dp, StitchOutlineVariant, RoundedCornerShape(12.dp)).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, StitchOutlineVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Security, contentDescription = null, tint = StitchPrimary,
                    modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Bastion Server Admin", style = MaterialTheme.typography.headlineSmall,
                color = StitchOnSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Advanced high-fidelity server management for critical enterprise infrastructure.",
                color = StitchOnSurfaceVariant, fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 40.dp))
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SectionLabel("Version")
                    Text("2.4.0-stable", color = StitchSecondary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(StitchOutlineVariant))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SectionLabel("Engine")
                    Text("V8.2.1-Core", color = StitchOnSurface, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutButton("Documentation") {
                    Toast.makeText(context, "Opening Documentation...", Toast.LENGTH_SHORT).show()
                }
                AboutButton("Support Portal") {
                    Toast.makeText(context, "Opening Support Portal...", Toast.LENGTH_SHORT).show()
                }
                AboutButton("Check for Updates", primary = true) {
                    Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("\u00A9 2024 Bastion Core Technologies Inc. All Rights Reserved.",
                color = StitchOnSurfaceVariant.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun AboutButton(text: String, primary: Boolean = false, onClick: () -> Unit) {
    if (primary) {
        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, StitchPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(text, color = StitchPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .border(1.dp, StitchOutlineVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(text, color = StitchOnSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
