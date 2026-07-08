package com.bastion.app.ssh

import com.bastion.app.logging.RemoteLogger
import org.apache.sshd.client.SshClient
import org.apache.sshd.common.signature.BuiltinSignatures
import org.apache.sshd.core.CoreModuleProperties
import java.time.Duration

object SshClientManager {
    private var client: SshClient? = null

    @Synchronized
    fun getClient(): Result<SshClient> {
        val existing = client
        if (existing != null && existing.isOpen) return Result.success(existing)
        return try {
            val newClient = SshClient.setUpDefaultClient()
            newClient.signatureFactories = listOf(
                BuiltinSignatures.ed25519,
                BuiltinSignatures.ed25519_cert,
                BuiltinSignatures.nistp256,
                BuiltinSignatures.nistp384,
                BuiltinSignatures.nistp521,
                BuiltinSignatures.rsaSHA512,
                BuiltinSignatures.rsaSHA256,
                BuiltinSignatures.rsa
            )
            // Keepalive (HIM-011): heartbeat SSH cada 30s + TCP keepalive, para que la conexión no
            // muera por inactividad cuando la app pasa a segundo plano (NAT/servidor no la cortan).
            CoreModuleProperties.HEARTBEAT_INTERVAL.set(newClient, Duration.ofSeconds(30))
            CoreModuleProperties.SOCKET_KEEPALIVE.set(newClient, true)

            newClient.start()
            client = newClient
            RemoteLogger.i("SshClientManager", "SSH client initialized OK (heartbeat 30s)")
            Result.success(newClient)
        } catch (e: Throwable) {
            RemoteLogger.e("SshClientManager", "SSH client init failed", e)
            client?.close(true)
            client = null
            Result.failure(e)
        }
    }

    @Synchronized
    fun shutdown() {
        try {
            client?.close(true)
        } catch (_: Exception) { }
        client = null
    }
}
