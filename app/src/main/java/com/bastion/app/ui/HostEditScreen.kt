package com.bastion.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bastion.app.data.AuthType
import com.bastion.app.data.Host
import com.bastion.app.data.VaultRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostEditScreen(
    hostId: Long?,
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEdit = hostId != null && hostId > 0

    var name by remember { mutableStateOf("") }
    var hostname by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("") }
    var authType by remember { mutableStateOf(AuthType.PASSWORD) }
    var password by remember { mutableStateOf("") }
    var privateKeyPem by remember { mutableStateOf("") }
    var privateKeyPassphrase by remember { mutableStateOf("") }
    var useAgentForwarding by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Load existing host data
    androidx.compose.runtime.LaunchedEffect(hostId) {
        if (isEdit) {
            val hostWithSecret = repository.getHostWithSecret(hostId!!)
            hostWithSecret?.let { hws ->
                name = hws.host.name
                hostname = hws.host.hostname
                port = hws.host.port.toString()
                username = hws.host.username
                authType = hws.host.authType
                useAgentForwarding = hws.host.useAgentForwarding
                password = hws.password ?: ""
                privateKeyPem = hws.privateKeyPem ?: ""
                privateKeyPassphrase = hws.privateKeyPassphrase ?: ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Editar host" else "Nuevo host") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                placeholder = { Text("web1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = hostname,
                onValueChange = { hostname = it },
                label = { Text("Host / IP") },
                placeholder = { Text("10.0.0.5") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text("Puerto") },
                    placeholder = { Text("22") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.padding(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario") },
                    placeholder = { Text("root") },
                    singleLine = true,
                    modifier = Modifier.weight(1.5f)
                )
            }

            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = authTypeLabel(authType),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Autenticación") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    AuthType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(authTypeLabel(type)) },
                            onClick = {
                                authType = type
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (authType) {
                AuthType.PASSWORD -> {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AuthType.PUBLIC_KEY -> {
                    OutlinedTextField(
                        value = privateKeyPem,
                        onValueChange = { privateKeyPem = it },
                        label = { Text("Clave privada (PEM)") },
                        placeholder = { Text("-----BEGIN OPENSSH PRIVATE KEY-----...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = privateKeyPassphrase,
                        onValueChange = { privateKeyPassphrase = it },
                        label = { Text("Passphrase (opcional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AuthType.AGENT_FORWARD -> {
                    Text(
                        "Agent forwarding reenviará las claves cargadas en la app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (authType == AuthType.AGENT_FORWARD) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(
                        checked = useAgentForwarding,
                        onCheckedChange = { useAgentForwarding = it }
                    )
                    Text("Usar agent forwarding")
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val host = Host(
                        id = hostId ?: 0,
                        name = name,
                        hostname = hostname,
                        port = port.toIntOrNull() ?: 22,
                        username = username,
                        authType = authType,
                        useAgentForwarding = useAgentForwarding
                    )
                    scope.launch {
                        repository.saveHost(
                            host = host,
                            password = password.ifBlank { null },
                            privateKeyPem = privateKeyPem.ifBlank { null },
                            privateKeyPassphrase = privateKeyPassphrase.ifBlank { null }
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && hostname.isNotBlank() && username.isNotBlank()
            ) {
                Text(if (isEdit) "Guardar cambios" else "Crear host")
            }
        }
    }
}

private fun authTypeLabel(type: AuthType): String = when (type) {
    AuthType.PASSWORD -> "Contraseña"
    AuthType.PUBLIC_KEY -> "Clave pública (PEM)"
    AuthType.AGENT_FORWARD -> "Agent forwarding"
}
