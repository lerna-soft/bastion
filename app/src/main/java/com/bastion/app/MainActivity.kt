package com.bastion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import com.bastion.app.logging.RemoteLogger
import com.bastion.app.update.UpdateChecker
import com.bastion.app.ui.BastionNavGraph
import com.bastion.app.ui.theme.BastionTheme
import com.bastion.app.ui.theme.ColorMode

class MainActivity : ComponentActivity() {
    private val log = RemoteLogger.logger("MainActivity")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        log.i("onCreate")

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> log.i("lifecycle CREATE")
                Lifecycle.Event.ON_START -> log.i("lifecycle START")
                Lifecycle.Event.ON_RESUME -> log.i("lifecycle RESUME")
                Lifecycle.Event.ON_PAUSE -> log.i("lifecycle PAUSE")
                Lifecycle.Event.ON_STOP -> log.i("lifecycle STOP")
                Lifecycle.Event.ON_DESTROY -> log.i("lifecycle DESTROY")
                Lifecycle.Event.ON_ANY -> {}
            }
        })

        enableEdgeToEdge()

        val app = application as BastionApp

        setContent {
            var colorMode by remember { mutableStateOf(ColorMode.DARK) }
            BastionTheme(colorMode = colorMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    BastionNavGraph(
                        navController = navController,
                        repository = app.repository,
                        colorMode = colorMode,
                        onColorModeChange = { colorMode = it }
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
                        AlertDialog(
                            onDismissRequest = {},
                            title = { Text("Downloading update...") },
                            text = {
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier.fillMaxSize()
                                )
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
                                    val file = java.io.File(cacheDir, "updates")
                                        .listFiles()?.firstOrNull()
                                    if (file != null) UpdateChecker.installApk(this@MainActivity, file)
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
