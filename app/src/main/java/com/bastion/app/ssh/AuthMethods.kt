package com.bastion.app.ssh

import org.apache.sshd.client.auth.keyboard.UserInteraction
import org.apache.sshd.client.auth.password.PasswordIdentityProvider
import org.apache.sshd.client.keyverifier.ServerKeyVerifier
import org.apache.sshd.common.session.SessionContext
import java.net.SocketAddress
import java.security.KeyPair

class BastionPasswordProvider(private val password: String) : PasswordIdentityProvider {
    override fun getPassword(session: SessionContext?): String = password
}

class BastionKeyVerifier : ServerKeyVerifier {
    override fun verifyClientKey(
        session: SessionContext?,
        remoteAddress: SocketAddress?,
        serverKey: java.security.PublicKey?
    ): Boolean = true
}

class BastionKeyIdentityProvider(private val keyPair: KeyPair) :
    org.apache.sshd.client.auth.pubkey.KeyIdentityProvider {

    override fun loadKeys(session: SessionContext?): Iterable<KeyPair> {
        return listOf(keyPair)
    }
}

class BastionUserInteraction : UserInteraction {
    override fun isInteractionAllowed(session: SessionContext?): Boolean = true

    override fun getUpdatedPassword(session: SessionContext?, prompt: String?, lang: String?): String? = null

    override fun interactive(
        session: SessionContext?,
        name: String?,
        instruction: String?,
        lang: String?,
        prompts: MutableList<org.apache.sshd.common.auth.UserAuthMethod?(out Any)?>?
    ): List<String?>? = null
}
