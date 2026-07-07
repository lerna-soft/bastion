package com.bastion.app.ssh

import org.apache.sshd.client.auth.password.PasswordIdentityProvider
import org.apache.sshd.client.keyverifier.ServerKeyVerifier
import org.apache.sshd.common.session.SessionContext
import java.net.SocketAddress
import java.security.KeyPair
import java.security.PublicKey

class BastionPasswordProvider(private val password: String) : PasswordIdentityProvider {
    override fun getPassword(session: SessionContext?): String = password
}

class BastionKeyVerifier : ServerKeyVerifier {
    override fun verifyClientKey(
        session: SessionContext?,
        remoteAddress: SocketAddress?,
        serverKey: PublicKey?
    ): Boolean = true
}

class BastionKeyIdentityProvider(private val keyPair: KeyPair) :
    org.apache.sshd.client.auth.pubkey.KeyIdentityProvider {

    override fun loadKeys(session: SessionContext?): Iterable<KeyPair> {
        return listOf(keyPair)
    }
}
