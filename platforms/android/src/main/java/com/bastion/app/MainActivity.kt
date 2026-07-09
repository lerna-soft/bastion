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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
                Box(modifier = Modifier.fillMaxSize()) {
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

                    // Notificación de actualización — NUNCA un diálogo bloqueante. Es una tarjeta
                    // flotante y descartable: el usuario puede seguir usando la app debajo, y decide
                    // a discreción si/cuándo instalar. El botón "Dismiss"/"Later" solo oculta el
                    // aviso (marca la versión como descartada, ver BastionApp.dismissUpdate), nunca
                    // lanza el instalador por su cuenta.
                    val updateState by app.updateState.collectAsState()

                    when (val state = updateState) {
                        is UpdateState.Available -> {
                            UpdateBanner(
                                title = "Update available",
                                onClose = { app.dismissUpdate(state.info.versionName) }
                            ) {
                                Text(
                                    "Version ${state.info.versionName} is available (current: v${BuildConfig.VERSION_NAME}).",
                                    fontSize = 13.sp,
                                    color = StitchOnSurfaceVariant
                                )
                                if (state.info.changelog.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        state.info.changelog.take(200),
                                        fontSize = 12.sp,
                                        color = StitchOnSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                TextButton(onClick = { app.downloadUpdate(state.info) }) {
                                    Text("Download", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        is UpdateState.Downloading -> {
                            val progress = state.progress
                            val totalMb = state.info.fileSize / (1024f * 1024f)
                            val downloadedMb = (state.info.fileSize * progress / 100f) / (1024f * 1024f)
                            UpdateBanner(
                                title = "Downloading v${state.info.versionName}…",
                                onClose = { app.dismissUpdate(state.info.versionName) }
                            ) {
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
                        }
                        is UpdateState.Ready -> {
                            UpdateBanner(
                                title = "Update ready to install",
                                onClose = { app.dismissUpdate(state.info.versionName) }
                            ) {
                                Text(
                                    "v${state.info.versionName} downloaded. Install whenever you're ready.",
                                    fontSize = 13.sp,
                                    color = StitchOnSurfaceVariant
                                )
                                Spacer(Modifier.height(10.dp))
                                TextButton(onClick = {
                                    UpdateChecker.installApk(this@MainActivity, state.file)
                                }) {
                                    Text("Install now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta flotante no-modal para avisos de actualización. A diferencia de un AlertDialog, no
 * bloquea el resto de la pantalla ni obliga a elegir una opción — solo un botón de cerrar (X)
 * arriba a la derecha, siempre visible, que descarta el aviso sin instalar nada.
 */
@androidx.compose.runtime.Composable
private fun UpdateBanner(
    title: String,
    onClose: () -> Unit,
    content: @androidx.compose.runtime.Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceContainerHighest),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    IconButton(onClick = onClose, modifier = Modifier.height(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.height(18.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                content()
            }
        }
    }
}
