package com.bastion.app.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.channel.PtyMode
import org.apache.sshd.common.util.buffer.BufferUtils
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

enum class SessionState {
    IDLE,
    CONNECTING,
    AUTHENTICATING,
    SHELL_ACTIVE,
    CLOSING,
    CLOSED,
    ERROR
}

data class AuthConfig(
    val hostname: String,
    val port: Int,
    val username: String,
    val password: String? = null,
    val privateKeyProvider: BastionKeyIdentityProvider? = null
)

class SshSession(
    private val cols: Int = 80,
    private val rows: Int = 24
) {
    private var session: ClientSession? = null
    private var shellStream: OutputStream? = null
    private var authConfig: AuthConfig? = null

    private val _state = MutableStateFlow(SessionState.IDLE)
    val state: StateFlow<SessionState> = _state

    suspend fun connect(config: AuthConfig): Result<Unit> = withContext(Dispatchers.IO) {
        _state.value = SessionState.CONNECTING
        authConfig = config

        try {
            val client = SshClientManager.getClient()

            val sshSession = client.connect(
                config.username,
                config.hostname,
                config.port
            ).verify(10_000).session

            session = sshSession

            _state.value = SessionState.AUTHENTICATING

            if (config.password != null) {
                sshSession.addPasswordIdentity(config.password)
            }

            if (config.privateKeyProvider != null) {
                sshSession.addPublicKeyIdentity(config.privateKeyProvider)
            }

            sshSession.auth().verify(15_000)

            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = SessionState.ERROR
            closeInternal()
            Result.failure(e)
        }
    }

    suspend fun openShell(): Result<Unit> = withContext(Dispatchers.IO) {
        val sshSession = session ?: return@withContext Result.failure(Exception("Not connected"))

        try {
            val channel = sshSession.createShellChannel()
            channel.setPtyColumns(cols)
            channel.setPtyRows(rows)
            channel.setPtyType("xterm-256color")
            channel.setPtyModes(mapOf(PtyMode.ECHO to 0))

            channel.open().verify(10_000)

            shellStream = channel.getInvertedIn()
            _state.value = SessionState.SHELL_ACTIVE

            // Store channel for reading
            channelRef = channel
            Result.success(Unit)
        } catch (e: Exception) {
            _state.value = SessionState.ERROR
            closeInternal()
            Result.failure(e)
        }
    }

    private var channelRef: org.apache.sshd.client.channel.ClientChannel? = null

    val output: Flow<ByteArray> = callbackFlow {
        val ch = channelRef ?: run {
            close()
            return@callbackFlow
        }

        val stdout = ch.getInvertedOut()

        val buffer = ByteArray(8192)
        while (isActive) {
            try {
                val len = stdout.read(buffer)
                if (len <= 0) break
                val data = buffer.copyOf(len)
                trySend(data)
            } catch (e: Exception) {
                break
            }
        }

        awaitClose { }
    }

    fun write(data: ByteArray) {
        try {
            shellStream?.write(data)
            shellStream?.flush()
        } catch (e: Exception) {
            _state.value = SessionState.ERROR
        }
    }

    fun resize(newCols: Int, newRows: Int) {
        try {
            channelRef?.sendWindowChange(newCols, newRows)
        } catch (_: Exception) { }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        closeInternal()
    }

    private fun closeInternal() {
        try {
            shellStream?.close()
        } catch (_: Exception) { }
        try {
            channelRef?.close(true)
        } catch (_: Exception) { }
        try {
            session?.close(true)
        } catch (_: Exception) { }
        shellStream = null
        channelRef = null
        session = null
        _state.value = SessionState.CLOSED
    }
}
