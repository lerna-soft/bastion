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

    data class LogEntry(
        val level: String,
        val tag: String,
        val msg: String,
        val stack: String? = null,
        val sessionId: String? = null,
        val host: String? = null
    )

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
        // Send any pending logs from previous crash
        if (filesDir != null) flushPendingFile(filesDir)
        Log.i(TAG, "RemoteLogger initialized → $serverUrl")
    }

    private fun pendingFile(dir: File): File = File(dir, "bastion_logs_pending.ndjson")

    @Synchronized
    private fun persistToFile(entry: LogEntry) {
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
            httpPost(json)
            f.delete()
            Log.i(TAG, "flushed pending file (${content.lines().size} entries)")
        } catch (e: Exception) {
            Log.w(TAG, "flushPendingFile failed: ${e.message}")
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

    private fun httpPost(json: String) {
        try {
            val url = URL(serverUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(conn.outputStream).use { it.write(json) }
            conn.responseCode
            conn.disconnect()
        } catch (_: Exception) { }
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
        enqueue(LogEntry("INFO", tag, msg, sessionId = _sessionId, host = _hostLabel))
    }

    fun w(tag: String, msg: String) {
        Log.w(tag, msg)
        enqueue(LogEntry("WARN", tag, msg, sessionId = _sessionId, host = _hostLabel))
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        Log.e(tag, msg, throwable)
        val entry = LogEntry("ERROR", tag, msg, throwable?.let { Log.getStackTraceString(it) }, sessionId = _sessionId, host = _hostLabel)
        persistToFile(entry)
        sendNow(entry)
    }

    fun crash(thread: Thread, throwable: Throwable) {
        val msg = "CRASH thread=${thread.name} ${throwable.message}"
        Log.e(TAG, msg, throwable)
        val entry = LogEntry("CRASH", TAG, msg, Log.getStackTraceString(throwable))
        enqueue(entry)
        forceFlush()
        // Also sync flush the pending file as last resort
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
