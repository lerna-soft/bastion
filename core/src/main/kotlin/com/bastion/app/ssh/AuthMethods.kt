package com.bastion.app.ssh

import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.client.keyverifier.ServerKeyVerifier
import org.apache.sshd.common.NamedResource
import org.apache.sshd.common.config.keys.FilePasswordProvider
import org.apache.sshd.common.session.SessionContext
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import java.io.File
import java.io.StringReader
import java.net.SocketAddress
import java.security.KeyPair
import java.security.PublicKey

class BastionKeyVerifier : ServerKeyVerifier {
    override fun verifyServerKey(
        session: ClientSession,
        remoteAddress: SocketAddress,
        serverKey: PublicKey
    ): Boolean = true
}

fun loadKeyPairFromPem(pemData: String, passphrase: String?): KeyPair? {
    return tryBouncyCastle(pemData, passphrase)
        ?: trySshdWithTempFile(pemData, passphrase)
}

private fun tryBouncyCastle(pemData: String, passphrase: String?): KeyPair? {
    return try {
        val reader = StringReader(pemData.trim())
        val parser = PEMParser(reader)
        when (val obj = parser.readObject()) {
            is PEMKeyPair -> JcaPEMKeyConverter().getKeyPair(obj)
            is org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo -> {
                if (passphrase != null) {
                    val decryptor = JceOpenSSLPKCS8DecryptorProviderBuilder()
                        .build(passphrase.toCharArray())
                    val info = obj.decryptPrivateKeyInfo(decryptor)
                    val privateKey = JcaPEMKeyConverter().getPrivateKey(info)
                    val next = parser.readObject()
                    if (next is SubjectPublicKeyInfo) {
                        KeyPair(JcaPEMKeyConverter().getPublicKey(next), privateKey)
                    } else null
                } else null
            }
            else -> null
        }
    } catch (_: Exception) { null }
}

private fun trySshdWithTempFile(pemData: String, passphrase: String?): KeyPair? {
    return try {
        val tempFile = File.createTempFile("bastion-key-", ".pem")
        tempFile.deleteOnExit()
        tempFile.writeText(pemData.trim())
        val provider = FilePasswordProvider { _: SessionContext?, _: NamedResource?, _: Int ->
            passphrase
        }
        val parser = org.apache.sshd.common.config.keys.loader.openssh
            .OpenSSHKeyPairResourceParser.INSTANCE
        val keys = parser.loadKeyPairs(null, tempFile.toPath(), provider)
        tempFile.delete()
        keys.firstOrNull()
    } catch (_: Exception) { null }
}
