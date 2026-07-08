package com.bastion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController
import com.bastion.app.logging.RemoteLogger
import com.bastion.app.update.UpdateChecker
import com.bastion.app.ui.BastionNavGraph
import com.bastion.app.ui.theme.BastionTheme
import com.bastion.app.ui.theme.ColorMode
import com.bastion.app.ui.theme.StitchOnSurfaceVariant
import com.bastion.app.ui.theme.StitchPrimaryContainer
import com.bastion.app.ui.theme.StitchSurfaceContainerHighest

class MainActivity : ComponentActivity() {
    private val log = RemoteLogger.logger("MainActivity")

    private fun requestIgnoreBatteryOptimizations() {
        try {
            val pm = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                @android.annotation.SuppressLint("BatteryLife")
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        } catch (e: Throwable) {
            log.w("battery-opt request falló: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        (application as BastionApp).checkForUpdateIfIdle()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log.i("onCreate")

        // En dispositivos agresivos (Samsung, etc.) el sistema mata la app en segundo plano y se
        // pierden las sesiones SSH. Pedir exención de optimización de batería (una sola vez).
        requestIgnoreBatteryOptimizations()

        val app = application as BastionApp

        setContent {
            var colorMode by remember { mutableStateOf(ColorMode.DARK) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                val settings = app.repository.getSettings()
                val saved = try { ColorMode.valueOf(settings.colorMode) } catch (_: Exception) { ColorMode.DARK }
                colorMode = saved
            }

            BastionTheme(colorMode = colorMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    val navController = rememberNavController()
                    BastionNavGraph(
                        navController = navController,
                        app = app,
                        repository = app.repository,
                        colorMode = colorMode,
                        onColorModeChange = {
                            colorMode = it
                            scope.launch {
                                val s = app.repository.getSettings()
                                app.repository.saveSettings(s.copy(colorMode = it.name))
                            }
                        }
                    )
                }

                val updateState by app.updateState.collectAsState()

                when (val state = updateState) {
                    is UpdateState.Available -> {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text("Update available") },
                            text = {
                                Text(
                                    "Version ${state.info.versionName} is available.\n\n" +
                                    "Changes: ${state.info.changelog}\n\n" +
                                    "Current: v${BuildConfig.VERSION_NAME}"
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { app.downloadUpdate(state.info) }) {
                                    Text("Update now", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { app.checkForUpdate() }) {
                                    Text("Skip")
                                }
                            }
                        )
                    }
                    is UpdateState.Downloading -> {
                        val progress = state.progress
                        val totalMb = state.info.fileSize / (1024f * 1024f)
                        val downloadedMb = (state.info.fileSize * progress / 100f) / (1024f * 1024f)
                        AlertDialog(
                            onDismissRequest = {},
                            title = {
                                Text(
                                    text = "Downloading update...",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            text = {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .background(StitchSurfaceContainerHighest, RoundedCornerShape(3.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(progress / 100f)
                                                .fillMaxHeight()
                                                .background(StitchPrimaryContainer, RoundedCornerShape(3.dp))
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            // progress es Int: %.0f con Int lanza IllegalFormatConversionException
                                            // en el main thread y tumbaba la app al abrir este diálogo (HIM-012).
                                            text = "$progress%",
                                            color = StitchPrimaryContainer,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${String.format("%.1f", downloadedMb)} / ${String.format("%.1f", totalMb)} MB",
                                            color = StitchOnSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            },
                            confirmButton = {}
                        )
                    }
                    is UpdateState.Ready -> {
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text("Update ready") },
                            text = { Text("The update has been downloaded. Install it when ready.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    UpdateChecker.installApk(this@MainActivity, state.file)
                                }) {
                                    Text("Install")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { app.checkForUpdate() }) {
                                    Text("Later")
                                }
                            }
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
