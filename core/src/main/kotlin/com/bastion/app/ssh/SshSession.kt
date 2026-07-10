package com.bastion.app.ssh

import com.bastion.app.core.log.CoreLog
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
import org.apache.sshd.client.session.forward.ExplicitPortForwardingTracker
import org.apache.sshd.common.util.net.SshdSocketAddress
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
    private val log = CoreLog.logger("SshSession")
    private var clientSession: ClientSession? = null
    // HIM-019 — Jump hosts: sesiones intermedias + túneles local-forward que sostienen la
    // cadena A→B→C. Se cierran en orden inverso al destino en cleanup().
    private val jumpSessions = mutableListOf<ClientSession>()
    private val forwardTrackers = mutableListOf<ExplicitPortForwardingTracker>()
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

    /**
     * Conecta al destino [cfg]. Si [jumps] no está vacío, tunela la conexión a través de esa
     * cadena ordenada de saltos (el primer elemento es alcanzable directo desde el dispositivo,
     * cada siguiente se alcanza a través del anterior) — patrón ProxyJump / bastion.
     *
     * Implementación: se conecta+autentica el salto 1, se abre un reenvío de puerto local de esa
     * sesión hacia el salto 2 (`direct-tcpip` sobre el canal SSH ya cifrado), se conecta el salto 2
     * a ese puerto local, y así en cadena. El destino final se conecta al puerto reenviado por el
     * último salto — ahí vive el shell. Nada del tráfico intermedio sale sin cifrar del dispositivo.
     */
    suspend fun connect(
        cfg: AuthConfig,
        jumps: List<AuthConfig> = emptyList()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val route = (jumps + cfg).joinToString(" → ") { "${it.username}@${it.hostname}:${it.port}" }
        log.i("connect start [$route]")
        CoreLog.setContext("${cfg.username}@${cfg.hostname}:${cfg.port}", cfg.hostname)
        setState(SessionState.CONNECTING)
        config.value = cfg
        try {
            val client = SshClientManager.getClient().getOrElse { e ->
                val err = ConnectionError(
                    phase = "client_init",
                    message = e.message ?: e.javaClass.simpleName,
                    exceptionText = e.stackTraceToString()
                )
                log.e("client_init failed: ${e.message}", e)
                _error.value = err
                setState(SessionState.ERROR)
                return@withContext Result.failure(e)
            }

            // Recorre la cadena de saltos abriendo túneles locales encadenados.
            // nextHost/nextPort = dirección a la que se conecta el SIGUIENTE hop; arranca en el
            // primer salto (o directo al destino si no hay saltos).
            var nextHost = (jumps.firstOrNull() ?: cfg).hostname
            var nextPort = (jumps.firstOrNull() ?: cfg).port

            for ((index, hop) in jumps.withIndex()) {
                setState(SessionState.CONNECTING)
                log.i("jump ${index + 1}/${jumps.size}: ${hop.username}@$nextHost:$nextPort")
                val jumpSession = client.connect(hop.username, nextHost, nextPort)
                    .verify(10_000).session
                jumpSessions.add(jumpSession)
                setState(SessionState.AUTHENTICATING)
                authenticate(jumpSession, hop)
                log.i("jump ${index + 1} auth OK")

                // ¿A dónde tunelea este salto? Al siguiente salto, o al destino final si es el último.
                val target = jumps.getOrNull(index + 1) ?: cfg
                val tracker = jumpSession.createLocalPortForwardingTracker(
                    SshdSocketAddress("127.0.0.1", 0),
                    SshdSocketAddress(target.hostname, target.port)
                )
                forwardTrackers.add(tracker)
                val bound = tracker.boundAddress
                nextHost = bound.hostName
                nextPort = bound.port
                log.i("tunnel abierto vía salto ${index + 1} → ${target.hostname}:${target.port} (local ${nextHost}:${nextPort})")
            }

            setState(SessionState.CONNECTING)
            val session = client.connect(cfg.username, nextHost, nextPort)
                .verify(10_000).session
            clientSession = session
            setState(SessionState.AUTHENTICATING)
            log.i("transport connected to target${if (jumps.isNotEmpty()) " (vía ${jumps.size} salto/s)" else ""}, authenticating")
            authenticate(session, cfg)

            setState(SessionState.SHELL_ACTIVE)
            _error.value = null
            log.i("auth OK, shell active")
            CoreLog.setContext(
                "${cfg.username}@${cfg.hostname}:${cfg.port}", cfg.hostname
            )
            Result.success(Unit)
        } catch (e: Exception) {
            val err = ConnectionError(
                phase = "connect",
                message = e.message ?: e.javaClass.simpleName,
                exceptionText = e.stackTraceToString()
            )
            log.e("connect failed: ${e.message}", e)
            _error.value = err
            setState(SessionState.ERROR)
            cleanup()
            Result.failure(e)
        }
    }

    private fun authenticate(session: ClientSession, cfg: AuthConfig) {
        cfg.password?.let { session.addPasswordIdentity(it) }
        cfg.keyPair?.let { session.addPublicKeyIdentity(it) }
        session.auth().verify(15_000)
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
                    // EOF = el remoto cerró el shell (logout / conexión caída). Es un fin normal,
                    // no un error: pasamos a CLOSED para que la UI muestre "Disconnected" y no un
                    // stack trace. Si close() ya nos llevó a CLOSING/CLOSED, no lo pisamos.
                    if (_state.value == SessionState.SHELL_ACTIVE) {
                        setState(SessionState.CLOSED)
                    }
                } catch (e: Exception) {
                    log.e("stdout read error: ${e.message}", e)
                    val err = ConnectionError(
                        phase = "read_stdout",
                        message = e.message ?: e.javaClass.simpleName,
                        exceptionText = e.stackTraceToString()
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
                exceptionText = e.stackTraceToString()
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
        } catch (e: Throwable) {
            // Escribir sobre un pipe ya cerrado significa que la sesión murió (logout remoto /
            // red caída / canal cerrado). Es un fin de conexión, NO un error accionable: nunca
            // debe propagarse (tumbaría la app) y NO debemos mostrar un stack trace al usuario.
            // Pasamos a CLOSED → la UI muestra "Disconnected" en vez del overlay de error.
            log.w("write sobre sesión cerrada, marcando desconectada: ${e.message}")
            setState(SessionState.CLOSED)
        }
    }

    fun setError(phase: String, message: String, exception: Exception) {
        log.e("setError[$phase]: $message", exception)
        val err = ConnectionError(
            phase = phase,
            message = message,
            exceptionText = exception.stackTraceToString()
        )
        _error.value = err
        setState(SessionState.ERROR)
    }

    fun resize(newCols: Int, newRows: Int) {
        // HIM-017: setPtyColumns/setPtyLines son setters de configuración PRE-apertura del
        // canal — con el canal ya abierto no notifican nada al servidor remoto. El servidor
        // seguía creyendo que el terminal medía 80x24 (el default de apertura) para siempre,
        // por eso el shell remoto no usaba el ancho real disponible (ls/prompt/etc. formateaban
        // a 80 columnas). sendWindowChange() sí envía el mensaje SSH de cambio de ventana
        // (RFC 4254 §6.7) al canal ya abierto.
        val ch = channel ?: return
        try {
            ch.setPtyColumns(newCols)
            ch.setPtyLines(newRows)
            ch.sendWindowChange(newCols, newRows)
            log.i("window change sent: ${newCols}x${newRows}")
        } catch (e: Exception) {
            log.w("sendWindowChange failed: ${e.message}")
        }
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
        // HIM-019: cerrar los túneles y las sesiones de salto en orden inverso (del más
        // cercano al destino hacia el primer salto), después del destino.
        for (tracker in forwardTrackers.asReversed()) {
            try { tracker.close() } catch (_: Exception) { }
        }
        for (jump in jumpSessions.asReversed()) {
            try { jump.close(true) } catch (_: Exception) { }
        }
        forwardTrackers.clear()
        jumpSessions.clear()
        stdinStream = null
        stdoutStream = null
        channel = null
        clientSession = null
        setState(SessionState.CLOSED)
    }
}
