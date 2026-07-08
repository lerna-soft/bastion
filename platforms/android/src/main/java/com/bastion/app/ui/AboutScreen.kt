package com.bastion.app.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bastion.app.BuildConfig

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    "About",
                    color = Color(0xFFE2E2E2),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // App Icon - Shield from Stitch design
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF1E2020), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color(0xFF75D1FF),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // App Name
            Text(
                "Bastion",
                color = Color(0xFFE2E2E2),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            // Version
            Text(
                "v${BuildConfig.VERSION_NAME}",
                color = Color(0xFF8E9192),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(32.dp))

            // About Section
            AboutSection(
                icon = Icons.Default.Info,
                title = "About",
                description = "A powerful, secure SSH terminal client built for professionals. Manage servers with ease from anywhere."
            )

            Spacer(Modifier.height(20.dp))

            // Security Section
            AboutSection(
                icon = Icons.Default.Security,
                title = "Security",
                description = "All credentials and private keys are encrypted using Android Keystore and stored in a secure local vault."
            )

            Spacer(Modifier.height(20.dp))

            // Technology Section
            AboutSection(
                icon = Icons.Default.Star,
                title = "Technology",
                description = "Built with Kotlin + Jetpack Compose, MINA SSHD for secure connections, and xterm.js for high-performance terminal emulation."
            )

            Spacer(Modifier.height(20.dp))

            // Features Section
            FeaturesSection()

            Spacer(Modifier.height(32.dp))

            // Footer Links
            FooterLink(
                "Privacy Policy",
                "https://github.com/lerna-admin/bastion/wiki/Privacy"
            )
            FooterLink(
                "Terms of Service",
                "https://github.com/lerna-admin/bastion/wiki/Terms"
            )
            FooterLink(
                "Security Audit",
                "https://github.com/lerna-admin/bastion/security"
            )

            Spacer(Modifier.height(24.dp))

            // Copyright
            Text(
                "\u00A9 2026 Bastion Core Technologies Inc. All Rights Reserved.",
                color = Color(0xFF444748),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AboutSection(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF75D1FF),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                color = Color(0xFFE2E2E2),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            color = Color(0xFFC4C7C7),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 32.dp)
        )
    }
}

@Composable
private fun FeaturesSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFF75D1FF),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Features",
                color = Color(0xFFE2E2E2),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(12.dp))

        val features = listOf(
            "Multi-tab sessions",
            "Encrypted credential vault",
            "Pinch-to-zoom terminal",
            "Custom themes",
            "Automatic server sync"
        )

        features.forEach { feature ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "•",
                    color = Color(0xFF75D1FF),
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    feature,
                    color = Color(0xFFC4C7C7),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun FooterLink(text: String, url: String = "") {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFF1E2020)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (url.isNotBlank()) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    )
                }
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = Color(0xFFC4C7C7),
            fontSize = 14.sp
        )
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF8E9192),
            modifier = Modifier.size(20.dp)
        )
    }
}
