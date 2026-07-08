package com.bastion.app.ssh

import android.util.Log
import com.bastion.app.logging.RemoteLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.sshd.client.channel.ChannelShell
import org.apache.sshd.client.session.ClientSession
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.security.KeyPair

enum class SessionState {
    IDLE, CONNECTING, AUTHENTICATING, SHELL_ACTIVE, CLOSING, CLOSED, ERROR
}

data class ConnectionError(
    val phase: String,
    val message: String,
    val exceptionText: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val fullText: String get() = buildString {
        appendLine("=== ERROR [$phase] ===")
        appendLine(message)
        appendLine()
        appendLine("--- Stack trace ---")
        appendLine(exceptionText)
        appendLine("--- End ---")
    }
}

data class AuthConfig(
    val hostname: String,
    val port: Int,
    val username: String,
    val password: String? = null,
    val keyPair: KeyPair? = null
)

class SshSession(
    private val cols: Int = 80,
    private val rows: Int = 24
) {
    private val log = RemoteLogger.logger("SshSession")
    private var clientSession: ClientSession? = null
    private var channel: ChannelShell? = null
    private var stdinStream: OutputStream? = null
    private var stdoutStream: InputStream? = null
    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(SessionState.IDLE)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val config: MutableStateFlow<AuthConfig?> = MutableStateFlow(null)

    private val _error = MutableStateFlow<ConnectionError?>(null)
    val error: StateFlow<ConnectionError?> = _error.asStateFlow()

    private val _output = MutableSharedFlow<ByteArray>(extraBufferCapacity = 1024)
    val output: SharedFlow<ByteArray> = _output.asSharedFlow()

    private fun setState(new: SessionState) {
        val old = _state.value
        if (old != new) {
            log.state(old, new)
            _state.value = new
        }
    }

    suspend fun connect(cfg: AuthConfig): Result<Unit> = withContext(Dispatchers.IO) {
        log.i("connect start ${cfg.username}@${cfg.hostname}:${cfg.port}")
        RemoteLogger.setSessionContext("${cfg.username}@${cfg.hostname}:${cfg.port}", cfg.hostname)
        setState(SessionState.CONNECTING)
        config.value = cfg
        try {
            val client = SshClientManager.getClient().getOrElse { e ->
                val err = ConnectionError(
                    phase = "client_init",
                    message = e.message ?: e.javaClass.simpleName,
                    exceptionText = Log.getStackTraceString(e)
                )
                log.e("client_init failed: ${e.message}", e)
                _error.value = err
                setState(SessionState.ERROR)
                return@withContext Result.failure(e)
            }
            val session = client.connect(
                cfg.username,
                cfg.hostname,
                cfg.port
            ).verify(10_000).session

            clientSession = session
            setState(SessionState.AUTHENTICATING)
            log.i("transport connected, authenticating")

            cfg.password?.let { session.addPasswordIdentity(it) }
            cfg.keyPair?.let { session.addPublicKeyIdentity(it) }

            session.auth().verify(15_000)
            setState(SessionState.SHELL_ACTIVE)
            _error.value = null
            log.i("auth OK, shell active")
            RemoteLogger.setSessionContext(
                "${cfg.username}@${cfg.hostname}:${cfg.port}", cfg.hostname
            )
            Result.success(Unit)
        } catch (e: Exception) {
            val err = ConnectionError(
                phase = "connect",
                message = e.message ?: e.javaClass.simpleName,
                exceptionText = Log.getStackTraceString(e)
            )
            log.e("connect failed: ${e.message}", e)
            _error.value = err
            setState(SessionState.ERROR)
            cleanup()
            Result.failure(e)
        }
    }

    suspend fun openShell(): Result<Unit> = withContext(Dispatchers.IO) {
        val session = clientSession ?: return@withContext Result.failure(Exception("Not connected"))
        log.i("openShell start")
        try {
            val ch = session.createShellChannel()
            ch.setPtyColumns(cols)
            ch.setPtyLines(rows)
            ch.setPtyType("xterm-256color")

            val pipeStdout = PipedOutputStream()
            val readStdout = PipedInputStream(pipeStdout)
            val writeStdin = PipedOutputStream()
            val readStdin = PipedInputStream(writeStdin)

            ch.setIn(readStdin)
            ch.setOut(pipeStdout)
            ch.open().verify(10_000)

            channel = ch
            stdinStream = writeStdin
            stdoutStream = readStdout

            readJob = scope.launch {
                try {
                    val buf = ByteArray(8192)
                    while (true) {
                        val len = readStdout.read(buf)
                        if (len <= 0) {
                            log.i("stdout EOF")
                            break
                        }
                        _output.emit(buf.copyOf(len))
                    }
                } catch (e: Exception) {
                    log.e("stdout read error: ${e.message}", e)
                    val err = ConnectionError(
                        phase = "read_stdout",
                        message = e.message ?: e.javaClass.simpleName,
                        exceptionText = Log.getStackTraceString(e)
                    )
                    _error.value = err
                    setState(SessionState.ERROR)
                }
            }

            setState(SessionState.SHELL_ACTIVE)
            _error.value = null
            log.i("shell channel open OK")
            Result.success(Unit)
        } catch (e: Exception) {
            log.e("openShell failed: ${e.message}", e)
            val err = ConnectionError(
                phase = "open_shell",
                message = e.message ?: e.javaClass.simpleName,
                exceptionText = Log.getStackTraceString(e)
            )
            _error.value = err
            setState(SessionState.ERROR)
            cleanup()
            Result.failure(e)
        }
    }

    fun write(data: ByteArray) {
        if (_state.value != SessionState.SHELL_ACTIVE) {
            log.w("write skipped, state=${_state.value}")
            return
        }
        try {
            stdinStream?.write(data)
            stdinStream?.flush()
        } catch (e: Exception) {
            log.e("write error: ${e.message}", e)
            val err = ConnectionError(
                phase = "write",
                message = e.message ?: e.javaClass.simpleName,
                exceptionText = Log.getStackTraceString(e)
            )
            _error.value = err
            setState(SessionState.ERROR)
        }
    }

    fun setError(phase: String, message: String, exception: Exception) {
        log.e("setError[$phase]: $message", exception)
        val err = ConnectionError(
            phase = phase,
            message = message,
            exceptionText = Log.getStackTraceString(exception)
        )
        _error.value = err
        setState(SessionState.ERROR)
    }

    fun resize(newCols: Int, newRows: Int) {
        try {
            channel?.setPtyColumns(newCols)
            channel?.setPtyLines(newRows)
        } catch (_: Exception) { }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        log.i("close requested")
        readJob?.cancel()
        readJob = null
        cleanup()
    }

    private fun cleanup() {
        log.i("cleanup")
        try { stdinStream?.close() } catch (_: Exception) { }
        try { stdoutStream?.close() } catch (_: Exception) { }
        try { channel?.close(true) } catch (_: Exception) { }
        try { clientSession?.close(true) } catch (_: Exception) { }
        stdinStream = null
        stdoutStream = null
        channel = null
        clientSession = null
        setState(SessionState.CLOSED)
    }
}

