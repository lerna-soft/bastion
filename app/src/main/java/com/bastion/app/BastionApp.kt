package com.bastion.app

import android.app.Application
import android.os.Process
import android.webkit.WebView
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.bastion.app.data.AppDatabase
import com.bastion.app.data.VaultRepository
import com.bastion.app.data.crypto.SecretsStore
import com.bastion.app.logging.RemoteLogger
import com.bastion.app.ssh.SshSession
import com.bastion.app.terminal.TerminalSession
import com.bastion.app.update.UpdateChecker
import com.bastion.app.update.UpdateInfo
import kotlinx.coroutines.CoroutineExceptionHandler
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

    // HIM-013: pestañas de terminal hoisted a nivel Application. Antes vivían en un remember{}
    // de AppLayout (ruta MAIN del NavHost) y se perdían al navegar a Agregar/Editar servidor o
    // Acerca de — Compose destruye ese remember al salir de la ruta. Aquí sobreviven mientras el
    // proceso esté vivo (no sobreviven si Android mata el proceso — eso es un HIM aparte).
    val terminalSessions: SnapshotStateList<TerminalSession> = mutableStateListOf()
    val terminalWebViewCache: MutableMap<SshSession, WebView> = mutableMapOf()
    private val nextTerminalIdCounter = java.util.concurrent.atomic.AtomicInteger(1)
    fun nextTerminalId(): Int = nextTerminalIdCounter.getAndIncrement()

    // Any error escaping an update coroutine is captured here (never crashes the app) and surfaced
    // as an error state the UI can show. appScope is used only for update operations.
    private val appExceptionHandler = CoroutineExceptionHandler { _, e ->
        RemoteLogger.e("BastionApp", "uncaught update-coroutine error: ${e.message}", e)
        _updateState.value = UpdateState.Error(e.message ?: "error inesperado")
    }
    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + appExceptionHandler)

    override fun onCreate() {
        super.onCreate()
        RemoteLogger.init(filesDir = filesDir)
        RemoteLogger.i("BastionApp", "START v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) device=${android.os.Build.MODEL} sdk=${android.os.Build.VERSION.SDK_INT}")

        // Por qué murió el proceso la última vez. Captura crashes NATIVOS, OOM y ANR — que el
        // handler de excepciones de la JVM NO puede ver. Sobrevive al reinicio (lo da el sistema).
        logLastExitReason()

        // Instalar el capturador de crashes LO ANTES POSIBLE — antes de inicializar Keystore, Room,
        // BouncyCastle, etc. — para que un fallo en esos pasos tempranos también quede con stack trace.
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Escribir el archivo PRIMERO (síncrono, sin red) para que nunca se pierda el stack trace,
            // incluso si el envío al servidor se cuelga o el proceso muere enseguida.
            try {
                val crashFile = File(filesDir, "bastion_crash.log")
                FileWriter(crashFile).use { w ->
                    w.write("=== BASTION CRASH ===\n")
                    w.write("Version: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
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

            // Luego intentar registrar/enviar (best-effort, puede tocar red).
            try { RemoteLogger.crash(thread, throwable) } catch (_: Exception) { }

            // Requisito HIM-009: la app nunca debe cerrarse. Un error en un hilo de fondo se
            // registra y se traga — la app sigue viva. Solo el hilo principal es irrecuperable:
            // ahí sí persistimos y dejamos que el sistema termine el crash (se mostrará al reabrir).
            val isMainThread = thread === android.os.Looper.getMainLooper().thread
            if (isMainThread) {
                oldHandler?.uncaughtException(thread, throwable)
                Process.killProcess(Process.myPid())
                System.exit(1)
            }
        }

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

        checkForUpdate()
    }

    /**
     * Consulta al sistema por qué murió el proceso la última vez (API 30+). Esto captura crashes
     * nativos (SIGSEGV), OOM (LOW_MEMORY / SIGNALED) y ANR, que NO pasan por el uncaught handler
     * de la JVM. Si el último cierre fue anormal, lo persiste en bastion_last_exit.log para que la
     * pantalla de Logs lo muestre, y lo registra en el servidor.
     */
    private fun logLastExitReason() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return
        try {
            val am = getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val exits = am.getHistoricalProcessExitReasons(packageName, 0, 10)
            if (exits.isEmpty()) return
            // Considerar SOLO el proceso principal. Los procesos aislados (renderer del WebView,
            // p.ej. "com.bastion.app:sandboxed_process0") los mata Android con REASON_OTHER
            // "isolated not needed" de forma normal — NO son crashes de la app (falso positivo).
            val last = exits.firstOrNull { it.processName == packageName } ?: return
            val reason = exitReasonName(last.reason)
            val summary = "last exit: $reason (status=${last.status}, importance=${last.importance}) — ${last.description}"
            RemoteLogger.i("ExitInfo", summary)

            // Solo alarmar por cierres realmente problemáticos (crash/OOM/ANR), no por cierres
            // normales del sistema (OTHER, USER_REQUESTED, PACKAGE actualizado, etc.).
            val crashLike = last.reason == android.app.ApplicationExitInfo.REASON_CRASH ||
                last.reason == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE ||
                last.reason == android.app.ApplicationExitInfo.REASON_ANR ||
                last.reason == android.app.ApplicationExitInfo.REASON_LOW_MEMORY ||
                last.reason == android.app.ApplicationExitInfo.REASON_SIGNALED ||
                last.reason == android.app.ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE
            if (crashLike) {
                RemoteLogger.e("ExitInfo", "cierre anormal: $reason — ${last.description}")
                try {
                    val sb = StringBuilder()
                    sb.append("=== ÚLTIMO CIERRE DEL SISTEMA ===\n")
                    sb.append("Motivo: $reason\n")
                    sb.append("Descripción: ${last.description}\n")
                    sb.append("status=${last.status} importance=${last.importance}\n")
                    sb.append("timestamp=${last.timestamp}\n")
                    // ANR y crash nativo traen traza del sistema (API 31+).
                    try {
                        last.traceInputStream?.bufferedReader()?.use { r ->
                            sb.append("\n--- Traza del sistema ---\n").append(r.readText())
                        }
                    } catch (_: Exception) { }
                    File(filesDir, "bastion_last_exit.log").writeText(sb.toString())
                } catch (_: Exception) { }
            }
        } catch (e: Exception) {
            RemoteLogger.w("ExitInfo", "getHistoricalProcessExitReasons falló: ${e.message}")
        }
    }

    private fun exitReasonName(reason: Int): String = when (reason) {
        android.app.ApplicationExitInfo.REASON_ANR -> "ANR (app no responde)"
        android.app.ApplicationExitInfo.REASON_CRASH -> "CRASH (excepción Java)"
        android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH NATIVO"
        android.app.ApplicationExitInfo.REASON_LOW_MEMORY -> "MEMORIA BAJA (OOM del sistema)"
        android.app.ApplicationExitInfo.REASON_SIGNALED -> "SEÑAL (killed, p.ej. OOM)"
        android.app.ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "USO EXCESIVO DE RECURSOS"
        android.app.ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCIA MURIÓ"
        android.app.ApplicationExitInfo.REASON_OTHER -> "OTRO"
        android.app.ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "FALLO DE INICIALIZACIÓN"
        android.app.ApplicationExitInfo.REASON_USER_REQUESTED -> "USUARIO (cierre normal)"
        android.app.ApplicationExitInfo.REASON_EXIT_SELF -> "SALIDA PROPIA"
        else -> "DESCONOCIDO ($reason)"
    }

    // Visibilidad de presión de memoria (M2): si el sistema aprieta la memoria antes de un cierre
    // por OOM, queda registrado — clave para confirmar/descartar OOM como causa de crash.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        RemoteLogger.w("Memory", "onTrimMemory level=${trimLevelName(level)} ($level)")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        RemoteLogger.w("Memory", "onLowMemory — el sistema está muy bajo de memoria")
    }

    private fun trimLevelName(level: Int): String = when (level) {
        TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
        TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
        TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
        TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
        TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
        TRIM_MEMORY_MODERATE -> "MODERATE"
        TRIM_MEMORY_COMPLETE -> "COMPLETE"
        else -> "OTHER"
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
