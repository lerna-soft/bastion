package com.bastion.app.ssh

import org.apache.sshd.client.SshClient
import org.apache.sshd.common.NamedResource
import org.apache.sshd.common.signature.BuiltinSignatures

object SshClientManager {
    private var client: SshClient? = null

    @Synchronized
    fun getClient(): SshClient {
        val existing = client
        if (existing != null && existing.isOpen) return existing

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

        newClient.start()
        client = newClient
        return newClient
    }

    @Synchronized
    fun shutdown() {
        client?.close()
        client = null
    }
}
