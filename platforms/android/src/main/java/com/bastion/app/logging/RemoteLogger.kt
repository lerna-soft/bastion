package com.bastion.app.logging

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

object RemoteLogger {
    private const val TAG = "RemoteLogger"
    private var serverUrl = "http://192.168.0.100:8765/logs"
    private var dataDir: File? = null
    private var fileWriter: OutputStreamWriter? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val logQueue = MutableSharedFlow<LogEntry>(extraBufferCapacity = 512)
    private var flushJob: Job? = null
    private val buffer = mutableListOf<LogEntry>()
    private val fileBuffer = mutableListOf<LogEntry>()
    private val flushing = AtomicBoolean(false)
    private var _sessionId: String? = null
    private var _hostLabel: String? = null

    // In-memory ring buffer of recent entries, shown by the in-app Logs screen (HIM-009).
    private const val RECENT_MAX = 200
    private val recentBuffer = LogRingBuffer(RECENT_MAX)

    data class LogEntry(
        val level: String,
        val tag: String,
        val msg: String,
        val stack: String? = null,
        val sessionId: String? = null,
        val host: String? = null
    )

    data class RecentEntry(
        val timeMillis: Long,
        val level: String,
        val tag: String,
        val msg: String
    )

    /** Most-recent-first snapshot of the in-memory log ring buffer. */
    fun recentLogs(): List<RecentEntry> = recentBuffer.snapshotNewestFirst()

    private fun addRecent(level: String, tag: String, msg: String) {
        recentBuffer.add(RecentEntry(System.currentTimeMillis(), level, tag, msg))
    }

    // --- Incident access for the in-app Logs screen ---
    //
    // Two sources: bastion_crash.log (JVM uncaught exception, escrito por el uncaught handler) y
    // bastion_last_exit.log (motivo del sistema: crash nativo, OOM, ANR — escrito al arrancar desde
    // ApplicationExitInfo). Un "seen marker" único cubre ambos.

    private fun crashFile(): File? = dataDir?.let { File(it, "bastion_crash.log") }
    private fun lastExitFile(): File? = dataDir?.let { File(it, "bastion_last_exit.log") }
    private fun seenFile(): File? = dataDir?.let { File(it, "bastion_incident.seen") }

    private fun readFileOrNull(f: File?): String? {
        if (f == null) return null
        return try { if (f.exists() && f.length() > 0) f.readText(Charsets.UTF_8) else null }
        catch (_: Exception) { null }
    }

    /** Full text of the last JVM crash (bastion_crash.log), or null. */
    fun readCrashLog(): String? = readFileOrNull(crashFile())

    /** System-reported reason for the last process death (native crash / OOM / ANR), or null. */
    fun readLastExit(): String? = readFileOrNull(lastExitFile())

    private fun incidentSignature(): String {
        val c = crashFile()?.let { if (it.exists()) it.lastModified() else 0L } ?: 0L
        val e = lastExitFile()?.let { if (it.exists()) it.lastModified() else 0L } ?: 0L
        return "$c:$e"
    }

    fun clearCrashLog() {
        try { crashFile()?.delete(); lastExitFile()?.delete(); seenFile()?.delete(); crashUploadedMarker()?.delete() } catch (_: Exception) { }
        recentBuffer.clear()
    }

    /** True when there is a crash or abnormal-exit record the user has not acknowledged yet. */
    fun hasUnseenIncident(): Boolean {
        val hasCrash = crashFile()?.let { it.exists() && it.length() > 0L } ?: false
        val hasExit = lastExitFile()?.let { it.exists() && it.length() > 0L } ?: false
        if (!hasCrash && !hasExit) return false
        val seen = seenFile() ?: return true
        return try { !seen.exists() || seen.readText().trim() != incidentSignature() }
        catch (_: Exception) { true }
    }

    fun markIncidentsSeen() {
        try { seenFile()?.writeText(incidentSignature()) } catch (_: Exception) { }
    }

    fun init(url: String = serverUrl, filesDir: File? = null) {
        serverUrl = url
        dataDir = filesDir
        flushJob?.cancel()
        flushJob = scope.launch {
            logQueue.collect { entry ->
                val size = synchronized(buffer) { buffer.add(entry); buffer.size }
                if (size >= 10) flush()
            }
        }
        scope.launch {
            while (true) {
                delay(30_000)
                if (buffer.isNotEmpty()) flush()
            }
        }
        // Send any pending logs from previous crash, then re-upload the crash stack trace itself
        // if the inline send at crash time didn't reach the server. Both run on the IO scope so a
        // slow/unreachable server never blocks app startup.
        if (filesDir != null) {
            scope.launch {
                flushPendingFile(filesDir)
                uploadCrashLogIfUnsent()
            }
        }
        Log.i(TAG, "RemoteLogger initialized → $serverUrl")
    }

    private fun pendingFile(dir: File): File = File(dir, "bastion_logs_pending.ndjson")

    @Synchronized
    private fun persistToFile(entry: LogEntry) {
        // Solo WARN/ERROR/CRASH: los INFO no necesitan sobrevivir a un crash, y su replay al
        // reiniciar duplicaba entradas ya enviadas en vivo al servidor (HIM-012 F4).
        if (entry.level == "INFO") return
        val dir = dataDir ?: return
        try {
            val f = pendingFile(dir)
            val line = buildJsonObject(entry) + "\n"
            f.appendText(line, Charsets.UTF_8)
        } catch (_: Exception) { }
    }

    private fun flushPendingFile(dir: File) {
        val f = pendingFile(dir)
        if (!f.exists() || f.length() == 0L) return
        try {
            val content = f.readText(Charsets.UTF_8).trim()
            if (content.isEmpty()) return
            val json = "[${content.replace("\n", ",")}]"
            // Only delete once the server confirmed receipt — otherwise a brief outage on boot
            // would silently drop the very WARN/ERROR/CRASH entries this file exists to preserve.
            if (httpPost(json)) {
                f.delete()
                Log.i(TAG, "flushed pending file (${content.lines().size} entries)")
            } else {
                Log.w(TAG, "flushPendingFile: server unreachable, keeping ${content.lines().size} entries for next boot")
            }
        } catch (e: Exception) {
            Log.w(TAG, "flushPendingFile failed: ${e.message}")
        }
    }

    private fun crashUploadedMarker(): File? = dataDir?.let { File(it, "bastion_crash.uploaded") }

    /**
     * On boot, re-upload the full stack trace in bastion_crash.log to the server if it wasn't
     * delivered inline at crash time. The uncaught handler tries an inline httpPost, but that is
     * best-effort (2s timeout, may run with no network or be cut short by process death), so a
     * crash's stack trace could stay stranded on-device forever — only ever visible in the in-app
     * Logs screen. This closes that gap: the marker stores the crash file's lastModified, so each
     * distinct crash is uploaded exactly once, and only after the server confirms receipt.
     */
    private fun uploadCrashLogIfUnsent() {
        val cf = crashFile() ?: return
        if (!cf.exists() || cf.length() == 0L) return
        val stamp = cf.lastModified().toString()
        val marker = crashUploadedMarker()
        val alreadySent = try { marker?.exists() == true && marker.readText().trim() == stamp }
            catch (_: Exception) { false }
        if (alreadySent) return
        val text = readFileOrNull(cf) ?: return
        val entry = LogEntry("CRASH", "CrashReplay", "stack trace del crash anterior (re-envío en arranque)", text)
        if (httpPost(buildJsonArray(listOf(entry)))) {
            try { marker?.writeText(stamp) } catch (_: Exception) { }
            Log.i(TAG, "uploaded stranded crash log to server")
        } else {
            Log.w(TAG, "uploadCrashLogIfUnsent: server unreachable, will retry next boot")
        }
    }

    fun setSessionContext(sessionId: String, host: String?) {
        _sessionId = sessionId
        _hostLabel = host
    }

    fun clearSessionContext() {
        _sessionId = null
        _hostLabel = null
    }

    fun logger(tag: String): ComponentLogger = ComponentLogger(tag)

    class ComponentLogger(private val tag: String) {
        fun i(msg: String) = RemoteLogger.i(tag, msg)
        fun w(msg: String) = RemoteLogger.w(tag, msg)
        fun e(msg: String, throwable: Throwable? = null) = RemoteLogger.e(tag, msg, throwable)
        fun state(from: Any, to: Any) = RemoteLogger.i(tag, "state $from → $to")
    }

    /** @return true only if the server accepted the POST (2xx). Callers that delete local data
     * after sending (pending file, crash log) MUST gate the delete on this — a swallowed failure
     * used to drop entries whenever the server was briefly unreachable. */
    private fun httpPost(json: String): Boolean {
        return try {
            val url = URL(serverUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(conn.outputStream).use { it.write(json) }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) { false }
    }

    private fun flush() {
        if (flushing.getAndSet(true)) return
        try {
            val batch = synchronized(buffer) {
                val b = buffer.toList()
                buffer.clear()
                b
            }
            if (batch.isNotEmpty()) httpPost(buildJsonArray(batch))
        } catch (_: Exception) { } finally {
            flushing.set(false)
        }
    }

    fun forceFlush() {
        if (buffer.isNotEmpty()) flush()
    }

    /**
     * Deliver [entry] to the server without depending on the async SharedFlow buffer.
     * Fixes the race where an ERROR was emitted to [logQueue] but forceFlush() ran
     * before the collector had added it to [buffer], so the error never reached the server.
     * Sends any pending buffered entries together with [entry] in a single batch.
     */
    private fun sendNow(entry: LogEntry) {
        scope.launch {
            try {
                val pending = synchronized(buffer) {
                    val b = buffer.toList()
                    buffer.clear()
                    b
                }
                httpPost(buildJsonArray(pending + entry))
            } catch (_: Exception) { }
        }
    }

    private fun enqueue(entry: LogEntry) {
        persistToFile(entry)
        logQueue.tryEmit(entry)
    }

    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
        addRecent("INFO", tag, msg)
        enqueue(LogEntry("INFO", tag, msg, sessionId = _sessionId, host = _hostLabel))
    }

    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
        addRecent("WARN", tag, msg)
        enqueue(LogEntry("WARN", tag, msg, sessionId = _sessionId, host = _hostLabel))
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        Log.e(tag, msg, throwable)
        addRecent("ERROR", tag, if (throwable != null) "$msg — ${throwable.message}" else msg)
        val entry = LogEntry("ERROR", tag, msg, throwable?.let { Log.getStackTraceString(it) }, sessionId = _sessionId, host = _hostLabel)
        persistToFile(entry)
        sendNow(entry)
    }

    fun crash(thread: Thread, throwable: Throwable) {
        val msg = "CRASH thread=${thread.name} ${throwable.message}"
        Log.e(TAG, msg, throwable)
        addRecent("CRASH", TAG, msg)
        val entry = LogEntry("CRASH", TAG, msg, Log.getStackTraceString(throwable))
        persistToFile(entry)
        // Send inline, NOT via enqueue()+forceFlush(): the process may be killed right after this
        // call returns, and forceFlush() only reads whatever is already in [buffer] — the same race
        // sendNow() was written to avoid for e(). logQueue.tryEmit() needs its scope.launch collector
        // to run before the entry lands in [buffer], which isn't guaranteed in time, so every crash
        // was silently dropped from the server logs. Build the batch from [entry] directly instead.
        try {
            val pending = synchronized(buffer) {
                val b = buffer.toList()
                buffer.clear()
                b
            }
            httpPost(buildJsonArray(pending + entry))
        } catch (_: Exception) { }
        // Also sync flush the pending file as last resort (covers the case where httpPost above
        // failed transiently but a retry via the file-based path succeeds).
        dataDir?.let { dir ->
            try {
                val f = pendingFile(dir)
                if (f.exists() && f.length() > 0) {
                    val content = f.readText(Charsets.UTF_8).trim()
                    if (content.isNotEmpty()) {
                        // entries are newline-separated JSON objects; join with commas for a valid array
                        httpPost("[${content.replace("\n", ",")}]")
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun buildJsonObject(entry: LogEntry): String {
        val sb = StringBuilder("{")
        sb.append("\"level\":\"${esc(entry.level)}\",\"tag\":\"${esc(entry.tag)}\",\"msg\":\"${esc(entry.msg)}\"")
        if (entry.stack != null) sb.append(",\"stack\":\"${esc(entry.stack)}\"")
        if (entry.sessionId != null) sb.append(",\"sessionId\":\"${esc(entry.sessionId)}\"")
        if (entry.host != null) sb.append(",\"host\":\"${esc(entry.host)}\"")
        sb.append("}")
        return sb.toString()
    }

    private fun buildJsonArray(entries: List<LogEntry>): String {
        val sb = StringBuilder("[")
        for ((i, e) in entries.withIndex()) {
            if (i > 0) sb.append(",")
            sb.append(buildJsonObject(e))
        }
        sb.append("]")
        return sb.toString()
    }

    private fun esc(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
