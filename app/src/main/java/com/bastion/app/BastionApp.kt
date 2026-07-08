package com.bastion.app

import android.app.Application
import android.os.Process
import com.bastion.app.data.AppDatabase
import com.bastion.app.data.VaultRepository
import com.bastion.app.data.crypto.SecretsStore
import com.bastion.app.logging.RemoteLogger
import com.bastion.app.update.UpdateChecker
import com.bastion.app.update.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data class Downloading(val progress: Int, val info: UpdateInfo) : UpdateState()
    data class Ready(val file: File, val info: UpdateInfo) : UpdateState()
    data class Error(val msg: String) : UpdateState()
}

class BastionApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var secretsStore: SecretsStore
        private set
    lateinit var repository: VaultRepository
        private set

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        RemoteLogger.init(filesDir = filesDir)
        RemoteLogger.i("BastionApp", "START v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) device=${android.os.Build.MODEL} sdk=${android.os.Build.VERSION.SDK_INT}")

        try {
            java.security.Security.removeProvider("BC")
        } catch (_: Exception) { }
        try {
            java.security.Security.insertProviderAt(
                org.bouncycastle.jce.provider.BouncyCastleProvider(), 1
            )
        } catch (_: Exception) { }

        org.apache.sshd.common.util.io.PathUtils.setUserHomeFolderResolver {
            java.nio.file.Paths.get(filesDir.absolutePath)
        }

        database = AppDatabase.getInstance(this)
        secretsStore = SecretsStore(this)
        repository = VaultRepository(database, secretsStore)

        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            RemoteLogger.crash(thread, throwable)
            try {
                val crashFile = File(filesDir, "bastion_crash.log")
                FileWriter(crashFile).use { w ->
                    w.write("=== BASTION CRASH ===\n")
                    w.write("Thread: ${thread.name}\n")
                    w.write("Time: ${System.currentTimeMillis()}\n")
                    w.write("Message: ${throwable.message}\n")
                    w.write("\n--- Stack trace ---\n")
                    throwable.printStackTrace(java.io.PrintWriter(w))
                    w.write("\n--- Cause ---\n")
                    var cause: Throwable? = throwable.cause
                    while (cause != null) {
                        w.write("Caused by: ${cause.message}\n")
                        cause.printStackTrace(java.io.PrintWriter(w))
                        cause = cause.cause
                    }
                    w.write("--- End ---\n")
                }
            } catch (_: Exception) { }

            oldHandler?.uncaughtException(thread, throwable)
            Process.killProcess(Process.myPid())
            System.exit(1)
        }

        checkForUpdate()
    }

    fun checkForUpdate() {
        _updateState.value = UpdateState.Checking
        appScope.launch {
            try {
                val info = UpdateChecker.checkForUpdate("http://192.168.0.100:8765")
                if (info != null) {
                    _updateState.value = UpdateState.Available(info)
                    RemoteLogger.i("BastionApp", "update available: v${info.versionName} (code ${info.versionCode})")
                } else {
                    _updateState.value = UpdateState.Idle
                    RemoteLogger.i("BastionApp", "no update available")
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "unknown")
            }
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        _updateState.value = UpdateState.Downloading(0, info)
        appScope.launch {
            try {
                val file = UpdateChecker.downloadApk(this@BastionApp, info) { pct ->
                    _updateState.value = UpdateState.Downloading(pct, info)
                }
                if (file != null) {
                    _updateState.value = UpdateState.Ready(file, info)
                    UpdateChecker.installApk(this@BastionApp, file)
                } else {
                    _updateState.value = UpdateState.Error("download failed")
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "download failed")
            }
        }
    }
}
