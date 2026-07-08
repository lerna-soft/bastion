package com.bastion.app.terminal

import com.bastion.app.ssh.AuthConfig
import com.bastion.app.ssh.SshSession
import com.bastion.app.ssh.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

data class SystemStats(
    val cpuUsage: Float = 0f,
    val cpuCores: Int = 0,
    val memTotalMb: Int = 0,
    val memUsedMb: Int = 0,
    val memProgress: Float = 0f,
    val diskTotalGb: Float = 0f,
    val diskUsedGb: Float = 0f,
    val diskProgress: Float = 0f,
    val diskPath: String = "/",
    val netDownKbps: Float = 0f,
    val netUpKbps: Float = 0f,
    val loadAvg1: Float = 0f,
    val loadAvg5: Float = 0f,
    val loadAvg15: Float = 0f,
    val uptime: String = "",
    val collected: Boolean = false
)

data class StatsLogEntry(
    val time: String,
    val message: String
)

class StatsCollector(
    private val authConfig: AuthConfig,
    private val intervalMs: Long = 5000L
) {
    // Handler propio: un fallo en la sesión oculta de stats jamás debe escalar (HIM-012 F3).
    private val scope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() +
            kotlinx.coroutines.CoroutineExceptionHandler { _, e ->
                com.bastion.app.logging.RemoteLogger.w("StatsCollector", "error no capturado: ${e.message}")
            }
    )
    private var collectJob: Job? = null
    private var statsSession: SshSession? = null

    private val STATS_BEGIN = "__BASTION_STATS_BEGIN__"
    private val STATS_END = "__BASTION_STATS_END__"

    private val _stats = MutableStateFlow(SystemStats())
    val stats: StateFlow<SystemStats> = _stats.asStateFlow()

    private val _logs = MutableStateFlow<List<StatsLogEntry>>(emptyList())
    val logs: StateFlow<List<StatsLogEntry>> = _logs.asStateFlow()

    private val _isCollecting = MutableStateFlow(false)
    val isCollecting: StateFlow<Boolean> = _isCollecting.asStateFlow()

    private val outputBuffer = StringBuilder()
    private var capturing = false

    fun start() {
        if (collectJob != null) return
        _isCollecting.value = true
        addLog("Stats collector started (hidden session)")

        collectJob = scope.launch {
            val session = SshSession()
            val connectResult = session.connect(authConfig)
            if (!connectResult.isSuccess) {
                addLog("Hidden session connect failed: ${connectResult.exceptionOrNull()?.message}")
                _isCollecting.value = false
                return@launch
            }
            session.openShell()
            statsSession = session
            addLog("Hidden session connected")

            val parseJob = launch {
                session.output.collect { bytes ->
                    val text = String(bytes, StandardCharsets.UTF_8)
                    processOutput(text)
                }
            }

            try {
                while (isActive) {
                    if (session.state.value == SessionState.SHELL_ACTIVE) {
                        sendStatsCommand(session)
                    }
                    delay(intervalMs)
                }
            } finally {
                parseJob.cancel()
            }
        }
    }

    fun stop() {
        collectJob?.cancel()
        collectJob = null
        val s = statsSession
        statsSession = null
        scope.launch {
            try { s?.close() } catch (_: Exception) { }
        }
        outputBuffer.clear()
        capturing = false
        _isCollecting.value = false
        addLog("Stats collector stopped")
    }

    private fun sendStatsCommand(session: SshSession) {
        val cmd = "echo $STATS_BEGIN; " +
            "cat /proc/stat | head -1; " +
            "grep 'cpu' /proc/stat | wc -l; " +
            "free -m | grep Mem:; " +
            "df -h / | tail -1; " +
            "grep 'cpu ' /proc/stat | head -1; " +
            "cat /proc/loadavg; " +
            "uptime -s; " +
            "echo $STATS_END\n"
        session.write(cmd.toByteArray(StandardCharsets.UTF_8))
    }

    private fun processOutput(text: String) {
        synchronized(outputBuffer) {
            outputBuffer.append(text)

            while (true) {
                val beginIdx = outputBuffer.indexOf(STATS_BEGIN)
                if (beginIdx == -1) {
                    if (capturing) {
                        capturing = false
                        outputBuffer.clear()
                    }
                    return
                }

                val endIdx = outputBuffer.indexOf(STATS_END, beginIdx)
                if (endIdx == -1) {
                    if (beginIdx > 0) {
                        outputBuffer.delete(0, beginIdx)
                    }
                    return
                }

                if (!capturing) {
                    capturing = true
                }

                val statsText = outputBuffer.substring(
                    beginIdx + STATS_BEGIN.length,
                    endIdx
                )
                parseStats(statsText)

                outputBuffer.delete(0, endIdx + STATS_END.length)
            }
        }
    }

    private fun parseStats(text: String) {
        try {
            val lines = text.trim().split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size < 6) return

            // Line 0: /proc/stat first line (cpu global)
            // cpu  3357 0 4313 1367591 12345 0 321 0 0 0
            val cpuLine1 = lines[0].split(Regex("\\s+"))
            // Line 1: cpu cores count
            val cores = lines[1].trim().toIntOrNull()?.minus(1) ?: 0

            // Line 2: free -m | grep Mem:
            // Mem:               15867        4232       10031          64        1603       11140
            val memLine = lines[2].split(Regex("\\s+"))
            val memTotal = memLine.getOrNull(1)?.toIntOrNull() ?: 0
            val memUsed = memLine.getOrNull(2)?.toIntOrNull() ?: 0
            val memProgress = if (memTotal > 0) memUsed.toFloat() / memTotal.toFloat() else 0f

            // Line 3: df -h / | tail -1
            // /dev/sda1        50G   12G   36G  25% /
            val diskLine = lines[3].split(Regex("\\s+"))
            val diskUsed = diskLine.getOrNull(2)?.let { parseSizeGb(it) } ?: 0f
            val diskTotal = diskLine.getOrNull(1)?.let { parseSizeGb(it) } ?: 0f
            val diskPath = diskLine.getOrNull(5) ?: "/"
            val diskProgress = if (diskTotal > 0) diskUsed / diskTotal else 0f

            // Line 4: second grep 'cpu ' /proc/stat (for delta calculation)
            val cpuLine2 = lines[4].split(Regex("\\s+"))

            // Line 5: /proc/loadavg
            // 0.12 0.14 0.10 2/345 6789
            val loadLine = lines[5].split(Regex("\\s+"))
            val load1 = loadLine.getOrNull(0)?.toFloatOrNull() ?: 0f
            val load5 = loadLine.getOrNull(1)?.toFloatOrNull() ?: 0f
            val load15 = loadLine.getOrNull(2)?.toFloatOrNull() ?: 0f

            // CPU usage from /proc/stat: cpu user nice system idle iowait irq softirq steal guest guest_nice
            // total = sum(all), idle = idle + iowait
            val cpuTotal1 = cpuLine1.drop(1).map { it.toLongOrNull() ?: 0L }.sum()
            val cpuIdle1 = (cpuLine1.getOrNull(4)?.toLongOrNull() ?: 0L) + (cpuLine1.getOrNull(5)?.toLongOrNull() ?: 0L)
            val cpuTotal2 = cpuLine2.drop(1).map { it.toLongOrNull() ?: 0L }.sum()
            val cpuIdle2 = (cpuLine2.getOrNull(4)?.toLongOrNull() ?: 0L) + (cpuLine2.getOrNull(5)?.toLongOrNull() ?: 0L)

            val totalDiff = cpuTotal2 - cpuTotal1
            val idleDiff = cpuIdle2 - cpuIdle1
            val cpuUsage = if (totalDiff > 0) ((totalDiff - idleDiff).toFloat() / totalDiff.toFloat()).coerceIn(0f, 1f) else 0f

            // Line 6 (optional): uptime -s
            // 2026-07-08 10:23:45
            val uptime = lines.getOrNull(6)?.trim() ?: ""

            _stats.value = SystemStats(
                cpuUsage = cpuUsage,
                cpuCores = cores,
                memTotalMb = memTotal,
                memUsedMb = memUsed,
                memProgress = memProgress,
                diskTotalGb = diskTotal,
                diskUsedGb = diskUsed,
                diskProgress = diskProgress,
                diskPath = diskPath,
                loadAvg1 = load1,
                loadAvg5 = load5,
                loadAvg15 = load15,
                uptime = uptime,
                collected = true
            )
        } catch (e: Exception) {
            addLog("Parse error: ${e.message}")
        }
    }

    private fun parseSizeGb(s: String): Float {
        val num = s.dropLast(1).toFloatOrNull() ?: return 0f
        return when (s.last()) {
            'T' -> num * 1024f
            'G' -> num
            'M' -> num / 1024f
            'K' -> num / (1024f * 1024f)
            else -> num
        }
    }

    private fun addLog(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        _logs.value = _logs.value + StatsLogEntry(time, message)
        if (_logs.value.size > 20) {
            _logs.value = _logs.value.takeLast(20)
        }
    }
}
