package com.bastion.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bastion.app.data.AuthType

@Composable
fun HostEditScreen(
    hostId: Long?,
    repository: com.bastion.app.data.VaultRepository,
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
    var showPassword by remember { mutableStateOf(false) }
    var showPassphrase by remember { mutableStateOf(false) }

    val isEdit = hostId != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1417))
    ) {
        // Header
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
                IconButton(onClick = { }) {
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

            // Shield Icon
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF75D1FF),
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Server Name
            StitchTextField(
                label = "SERVER NAME",
                value = serverName,
                onValueChange = { serverName = it },
                placeholder = "e.g. Production Web"
            )

            Spacer(Modifier.height(16.dp))

            // Hostname + Port Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StitchTextField(
                    label = "HOSTNAME",
                    value = hostname,
                    onValueChange = { hostname = it },
                    placeholder = "web-01.example.com",
                    modifier = Modifier.weight(1f)
                )
                StitchTextField(
                    label = "PORT",
                    value = port,
                    onValueChange = { port = it },
                    placeholder = "22",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.width(80.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Username
            StitchTextField(
                label = "USERNAME",
                value = username,
                onValueChange = { username = it },
                placeholder = "root",
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

            // Auth Type Toggle
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

            // Password or Private Key field
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

            Spacer(Modifier.height(32.dp))

            // Connect Button
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF75D1FF)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Connect",
                    color = Color(0xFF003548),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Save Button
            OutlinedButton(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFE2E2E2)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "SAVE CONFIGURATION",
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
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            label,
            color = Color(0xFF8E9192),
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
                unfocusedBorderColor = Color(0xFF1E2020),
                focusedContainerColor = Color(0xFF1E2020),
                unfocusedContainerColor = Color(0xFF1E2020),
                focusedTextColor = Color(0xFFE2E2E2),
                unfocusedTextColor = Color(0xFFE2E2E2),
                cursorColor = Color(0xFF75D1FF)
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
