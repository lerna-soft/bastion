package com.bastion.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bastion.app.data.AuthType
import com.bastion.app.data.Host
import com.bastion.app.data.VaultRepository
import com.bastion.app.logging.RemoteLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HostEditScreen(
    hostId: Long?,
    repository: VaultRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var serverName by remember { mutableStateOf("") }
    var hostname by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var privateKey by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var authType by remember { mutableStateOf(AuthType.PASSWORD) }
    // HIM-019 — jump host (ProxyJump): id de otro host del vault, o null = conexión directa.
    var jumpHostId by remember { mutableStateOf<Long?>(null) }
    var jumpMenuOpen by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showPassphrase by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var hostnameError by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf(false) }
    var portError by remember { mutableStateOf(false) }

    val isEdit = hostId != null
    // Hosts del vault disponibles como jump host (para el selector). Se excluye a sí mismo y
    // cualquier candidato que crearía un ciclo (ver jumpHostCandidates()).
    val allHosts by repository.getAllHosts().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val log = remember { RemoteLogger.logger("HostEdit") }

    if (isEdit && hostId != null) {
        LaunchedEffect(hostId) {
            isLoading = true
            val existing = withContext(Dispatchers.IO) {
                repository.getHostWithSecret(hostId)
            }
            if (existing != null) {
                serverName = existing.host.name
                hostname = existing.host.hostname
                port = existing.host.port.toString()
                username = existing.host.username
                authType = existing.host.authType
                jumpHostId = existing.host.jumpHostId
                password = existing.password ?: ""
                privateKey = existing.privateKeyPem ?: ""
                passphrase = existing.privateKeyPassphrase ?: ""
            }
            isLoading = false
        }
    }

    fun validate(): Boolean {
        val result = HostValidator.validate(hostname, username, port)
        hostnameError = result.hostnameError
        usernameError = result.usernameError
        portError = result.portError
        return result.isValid
    }

    fun save(): Long? {
        if (!validate()) return null
        val portNum = port.toIntOrNull() ?: 22
        val host = Host(
            id = hostId ?: 0L,
            name = serverName.ifBlank { hostname },
            hostname = hostname.trim(),
            port = portNum,
            username = username.trim(),
            authType = authType,
            jumpHostId = jumpHostId
        )
        var savedId: Long? = null
        scope.launch {
            isLoading = true
            try {
                savedId = withContext(Dispatchers.IO) {
                    repository.saveHost(host, password, privateKey, passphrase)
                }
                log.i("saved host id=$savedId")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Server saved: ${hostname.trim()}", Toast.LENGTH_SHORT).show()
                }
                onNavigateBack()
            } catch (e: Exception) {
                log.e("save failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isLoading = false
            }
        }
        return savedId
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1417))
    ) {
        Box(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F1417))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color(0xFFE2E2E2)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isEdit) "Edit Server" else "Add Server",
                    color = Color(0xFFE2E2E2),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {
                    Toast.makeText(context, "Fill in host details and save", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = Color(0xFF8E9192)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF75D1FF),
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.height(24.dp))

            StitchTextField(
                label = "SERVER NAME",
                value = serverName,
                onValueChange = { serverName = it },
                placeholder = "e.g. Production Web"
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StitchTextField(
                    label = "HOSTNAME",
                    value = hostname,
                    onValueChange = { hostname = it; hostnameError = false },
                    placeholder = "web-01.example.com",
                    isError = hostnameError,
                    errorMessage = "Required",
                    modifier = Modifier.weight(1f)
                )
                StitchTextField(
                    label = "PORT",
                    value = port,
                    onValueChange = { port = it; portError = false },
                    placeholder = "22",
                    keyboardType = KeyboardType.Number,
                    isError = portError,
                    errorMessage = "1-65535",
                    modifier = Modifier.width(80.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            StitchTextField(
                label = "USERNAME",
                value = username,
                onValueChange = { username = it; usernameError = false },
                placeholder = "root",
                isError = usernameError,
                errorMessage = "Required",
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF8E9192),
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuthToggleButton(
                    label = "PASSWORD",
                    isSelected = authType == AuthType.PASSWORD,
                    onClick = { authType = AuthType.PASSWORD },
                    modifier = Modifier.weight(1f)
                )
                AuthToggleButton(
                    label = "PRIVATE KEY",
                    isSelected = authType == AuthType.PUBLIC_KEY,
                    onClick = { authType = AuthType.PUBLIC_KEY },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (authType == AuthType.PASSWORD) {
                StitchTextField(
                    label = "PASSWORD",
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "••••••••",
                    isPassword = true,
                    showPassword = showPassword,
                    onTogglePassword = { showPassword = !showPassword }
                )
            } else {
                StitchTextField(
                    label = "PRIVATE KEY",
                    value = privateKey,
                    onValueChange = { privateKey = it },
                    placeholder = "ld_ed25519_production"
                )
                Spacer(Modifier.height(16.dp))
                StitchTextField(
                    label = "OPTIONAL PASSPHRASE",
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    placeholder = "••••••••",
                    isPassword = true,
                    showPassword = showPassphrase,
                    onTogglePassword = { showPassphrase = !showPassphrase }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── HIM-019: Jump host (ProxyJump) ────────────────────────────
            val candidates = remember(allHosts, hostId) {
                com.bastion.app.data.JumpHostChain.candidates(allHosts, hostId)
            }
            val selectedJump = allHosts.firstOrNull { it.id == jumpHostId }
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "JUMP HOST (OPTIONAL)",
                    color = Color(0xFF8E9192),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B2227), RoundedCornerShape(8.dp))
                            .clickable { jumpMenuOpen = true }
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AltRoute,
                            contentDescription = null,
                            tint = Color(0xFF8E9192),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = selectedJump?.let { "${it.name} (${it.hostname})" }
                                ?: "Direct connection (no jump)",
                            color = if (selectedJump != null) Color(0xFFE2E2E2) else Color(0xFF8E9192),
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select jump host",
                            tint = Color(0xFF8E9192)
                        )
                    }
                    DropdownMenu(
                        expanded = jumpMenuOpen,
                        onDismissRequest = { jumpMenuOpen = false },
                        modifier = Modifier.background(Color(0xFF1B2227))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Direct connection (no jump)", color = Color(0xFFE2E2E2)) },
                            onClick = { jumpHostId = null; jumpMenuOpen = false }
                        )
                        candidates.forEach { h ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${h.name}  ·  ${h.username}@${h.hostname}:${h.port}",
                                        color = Color(0xFFE2E2E2)
                                    )
                                },
                                onClick = { jumpHostId = h.id; jumpMenuOpen = false }
                            )
                        }
                        if (candidates.isEmpty()) {
                            DropdownMenuItem(
                                enabled = false,
                                text = { Text("No other servers saved", color = Color(0xFF8E9192)) },
                                onClick = {}
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Route the connection through another saved server (bastion). " +
                        "Chains automatically if that server also has a jump host.",
                    color = Color(0xFF8E9192),
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { save() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF75D1FF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isLoading) "Saving..." else "Connect",
                    color = Color(0xFF003548),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { save() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFE2E2E2)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isLoading) "Saving..." else "SAVE CONFIGURATION",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StitchTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String = ""
) {
    Column(modifier = modifier) {
        Text(
            label,
            color = if (isError) Color(0xFFFFB4AB) else Color(0xFF8E9192),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    placeholder,
                    color = Color(0xFF444748),
                    fontSize = 14.sp
                )
            },
            isError = isError,
            supportingText = if (isError) {
                { Text(errorMessage, color = Color(0xFFFFB4AB), fontSize = 11.sp) }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = if (isPassword && onTogglePassword != null) {
                {
                    IconButton(onClick = { onTogglePassword() }) {
                        Icon(
                            if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFF8E9192)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !showPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF75D1FF),
                unfocusedBorderColor = if (isError) Color(0xFFFFB4AB) else Color(0xFF1E2020),
                focusedContainerColor = Color(0xFF1E2020),
                unfocusedContainerColor = Color(0xFF1E2020),
                focusedTextColor = Color(0xFFE2E2E2),
                unfocusedTextColor = Color(0xFFE2E2E2),
                cursorColor = Color(0xFF75D1FF),
                errorBorderColor = Color(0xFFFFB4AB),
                errorContainerColor = Color(0xFF1E2020)
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun AuthToggleButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF1E2020) else Color(0xFF0F1417),
            contentColor = if (isSelected) Color(0xFFE2E2E2) else Color(0xFF8E9192)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
