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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bastion.app.BuildConfig
import com.bastion.app.data.AppSettings
import com.bastion.app.data.ApiKey
import com.bastion.app.data.VaultRepository
import com.bastion.app.ui.theme.ColorMode
import com.bastion.app.ui.theme.MonokaiBackground
import com.bastion.app.ui.theme.MonokaiOnSurface
import com.bastion.app.ui.theme.MonokaiPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// HIM-015: "notifications", "security" y "api-keys" ocultos — se persisten en AppSettings/DB
// pero ninguna lógica de la app los aplica todavía (sin envío real de webhook/email, sin 2FA/
// timeout de sesión aplicado, sin API propia que las keys puedan autenticar). Ocultos hasta que
// se implementen de verdad, para no dar una falsa sensación de que están activos.
private val settingsSections = listOf(
    "general" to "General",
    "appearance" to "Appearance",
    "about" to "About"
)

@Composable
fun SettingsContent(
    repository: VaultRepository,
    colorMode: ColorMode = ColorMode.DARK,
    onColorModeChange: (ColorMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var activeSection by remember { mutableStateOf("general") }
    var settings by remember { mutableStateOf(AppSettings()) }

    val apiKeys by repository.getAllApiKeys().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        settings = repository.getSettings()
    }

    fun saveSettings(new: AppSettings) {
        settings = new
        scope.launch {
            repository.saveSettings(new)
        }
    }

    Row(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        SettingsSidebar(activeSection = activeSection, onSectionSelected = { activeSection = it })

        Column(
            modifier = Modifier.weight(1f).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            when (activeSection) {
                "general" -> GeneralSection(
                    serverName = settings.serverName,
                    timezone = settings.timezone,
                    language = settings.language,
                    onServerNameChange = { saveSettings(settings.copy(serverName = it)) },
                    onTimezoneChange = { saveSettings(settings.copy(timezone = it)) },
                    onLanguageChange = { saveSettings(settings.copy(language = it)) }
                )
                "notifications" -> NotificationsSection(
                    webhookUrl = settings.webhookUrl,
                    emailAlerts = settings.emailAlerts,
                    onWebhookUrlChange = { saveSettings(settings.copy(webhookUrl = it)) },
                    onEmailAlertsChange = { saveSettings(settings.copy(emailAlerts = it)) }
                )
                "appearance" -> AppearanceSection(
                    colorMode = colorMode,
                    onColorModeChange = onColorModeChange,
                    fontSize = settings.fontSize,
                    onFontSizeChange = { saveSettings(settings.copy(fontSize = it)) }
                )
                "security" -> SecuritySection(
                    twoFactorEnabled = settings.twoFactorEnabled,
                    sessionTimeout = settings.sessionTimeout,
                    onTwoFactorChange = { saveSettings(settings.copy(twoFactorEnabled = it)) },
                    onSessionTimeoutChange = { saveSettings(settings.copy(sessionTimeout = it)) }
                )
                "api-keys" -> ApiKeysSection(
                    apiKeys = apiKeys,
                    repository = repository
                )
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
            val bg = if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f) else Color.Transparent
            val textColor = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
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
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
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
            Box(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(selected, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    Icon(
                        imageVector = if (expanded)
                            Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp) },
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
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
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
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
            ))
    }
}

@Composable
private fun GeneralSection(
    serverName: String,
    timezone: String,
    language: String,
    onServerNameChange: (String) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    SectionCard(title = "General Configuration", icon = Icons.Default.Settings) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextField(label = "Server Name", value = serverName, onValueChange = onServerNameChange,
                modifier = Modifier.weight(1f))
            SelectField(label = "Timezone", options = listOf(
                "UTC (Coordinated Universal Time)", "EST (Eastern Standard Time)",
                "PST (Pacific Standard Time)", "GMT (Greenwich Mean Time)"
            ), selected = timezone, onSelected = onTimezoneChange)
        }
        Spacer(Modifier.height(16.dp))
        SelectField(label = "Language", options = listOf(
            "English (United States)", "German (Deutsch)", "Japanese (日本語)", "Spanish (Español)"
        ), selected = language, onSelected = onLanguageChange)
    }
}

@Composable
private fun NotificationsSection(
    webhookUrl: String,
    emailAlerts: Boolean,
    onWebhookUrlChange: (String) -> Unit,
    onEmailAlertsChange: (Boolean) -> Unit
) {
    SectionCard(title = "Notifications", icon = Icons.Default.NotificationsActive) {
        TextField(label = "Slack Webhook URL", value = webhookUrl,
            onValueChange = onWebhookUrlChange, monospace = true, password = true)
        Spacer(Modifier.height(16.dp))
        ToggleRow(title = "Email Alerts",
            description = "Receive critical system health reports via email",
            checked = emailAlerts, onCheckedChange = onEmailAlertsChange)
    }
}

private val themeOptions = listOf(
    ColorMode.DARK to "Neutral" to Color(0xFF0F1115),
    ColorMode.STITCH_GREEN to "Stitch" to Color(0xFF0c160a),
    ColorMode.LIGHT to "Light" to Color(0xFFF8F9FA),
    ColorMode.MONOKAI to "Monokai" to Color(0xFF272822),
    ColorMode.OLED_DARK to "OLED" to Color(0xFF000000),
    ColorMode.SYSTEM to "System" to Color(0xFF1B2023),
)

@Composable
private fun AppearanceSection(
    colorMode: ColorMode,
    onColorModeChange: (ColorMode) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit
) {
    SectionCard(title = "Appearance", icon = Icons.Default.Palette) {
        SectionLabel("Color Mode")
        Spacer(Modifier.height(12.dp))
        val rows = themeOptions.chunked(3)
        rows.forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowOptions.forEach { (pair, _) ->
                    val (mode, label) = pair
                    val isSelected = colorMode == mode
                    val bgColor = when (mode) {
                        ColorMode.DARK -> MaterialTheme.colorScheme.surfaceDim
                        ColorMode.STITCH_GREEN -> MaterialTheme.colorScheme.surface
                        ColorMode.LIGHT -> Color.White
                        ColorMode.MONOKAI -> MonokaiBackground
                        ColorMode.OLED_DARK -> Color.Black
                        ColorMode.SYSTEM -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.outlineVariant
                    val textColor = when (mode) {
                        ColorMode.DARK -> MaterialTheme.colorScheme.onSurface
                        ColorMode.STITCH_GREEN -> MaterialTheme.colorScheme.onSurface
                        ColorMode.LIGHT -> Color(0xFF1C2023)
                        ColorMode.MONOKAI -> MonokaiOnSurface
                        ColorMode.OLED_DARK -> Color(0xFFE0E0E0)
                        ColorMode.SYSTEM -> MaterialTheme.colorScheme.onSurface
                    }
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { onColorModeChange(mode) }.padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(6.dp))
                                .background(bgColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp)))
                            Spacer(Modifier.height(8.dp))
                            Text(label,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("Font Size")
            Text("${fontSize.toInt()}px", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(8.dp))
        Slider(value = fontSize, onValueChange = onFontSizeChange,
            valueRange = 12f..20f, steps = 7,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primaryContainer,
                activeTrackColor = MaterialTheme.colorScheme.primaryContainer, inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Compact", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text("Default", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text("Large", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
        Spacer(Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(16.dp)) {
            Column {
                Text("Preview Terminal Text:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Text("[admin@bastion ~]$ ssh root@server-01\nAuthentication successful.",
                    color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun SecuritySection(
    twoFactorEnabled: Boolean,
    sessionTimeout: String,
    onTwoFactorChange: (Boolean) -> Unit,
    onSessionTimeoutChange: (String) -> Unit
) {
    SectionCard(title = "Security", icon = Icons.Default.Security) {
        ToggleRow(title = "Two-Factor Authentication",
            description = "Require a TOTP token for all admin logins",
            checked = twoFactorEnabled, onCheckedChange = onTwoFactorChange)
        Spacer(Modifier.height(16.dp))
        SelectField(label = "Session Timeout", options = listOf(
            "15 Minutes", "30 Minutes", "1 Hour", "4 Hours"
        ), selected = sessionTimeout, onSelected = onSessionTimeoutChange)
    }
}

@Composable
private fun ApiKeysSection(
    apiKeys: List<ApiKey>,
    repository: VaultRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCreateKey by remember { mutableStateOf(false) }

    SectionCard(title = "API Management", icon = Icons.Default.VpnKey) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Manage API Keys")
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { showCreateKey = true }) {
                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(16.dp))
                Text("Create New Key", color = MaterialTheme.colorScheme.primaryContainer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        apiKeys.forEachIndexed { index, key ->
            ApiKeyRow(key.label, key.keyValue,
                java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
                    .format(java.util.Date(key.created)),
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("API Key", key.keyValue))
                    Toast.makeText(context, "API key copied: ${key.label}", Toast.LENGTH_SHORT).show()
                },
                onRevoke = {
                    scope.launch {
                        repository.deleteApiKey(key.id)
                        Toast.makeText(context, "API key revoked: ${key.label}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            if (index < apiKeys.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
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
                        scope.launch {
                            val rawKey = "${keyLabel.lowercase().replace(" ", "-")}_${System.currentTimeMillis().toString().takeLast(8)}"
                            repository.addApiKey(keyLabel.trim(), rawKey)
                            showCreateKey = false
                            Toast.makeText(context, "API key created: ${keyLabel.trim()}", Toast.LENGTH_SHORT).show()
                        }
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
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f))
        Text(keyValue, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1.5f))
        Text(created, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy",
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).clickable(onClick = onCopy).padding(2.dp))
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
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Bastion Server Admin", style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Advanced high-fidelity server management for critical enterprise infrastructure.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 40.dp))
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SectionLabel("Version")
                    Text(BuildConfig.VERSION_NAME, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
                Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SectionLabel("Engine")
                    Text("v${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutButton("Documentation") {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lerna-admin/bastion/wiki"))
                    )
                }
                AboutButton("Support Portal") {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lerna-admin/bastion/issues"))
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("\u00A9 2026 Bastion Core Technologies Inc. All Rights Reserved.",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 10.sp, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun AboutButton(text: String, primary: Boolean = false, onClick: () -> Unit) {
    if (primary) {
        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(text, color = MaterialTheme.colorScheme.primaryContainer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp)) {
            Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
