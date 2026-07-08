package com.bastion.app.data

import com.bastion.app.data.crypto.SecretsStore
import com.bastion.app.logging.RemoteLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class HostWithSecret(
    val host: Host,
    val password: String? = null,
    val privateKeyPem: String? = null,
    val privateKeyPassphrase: String? = null
)

class VaultRepository(
    private val db: AppDatabase,
    private val secrets: SecretsStore
) {
    private val log = RemoteLogger.logger("VaultRepo")
    private val dao = db.hostDao()

    fun getAllHosts(): Flow<List<Host>> = dao.getAllHosts()

    suspend fun getHost(id: Long): Host? = dao.getHostById(id)

    suspend fun getHostWithSecret(id: Long): HostWithSecret? {
        val host = dao.getHostById(id) ?: return null
        log.i("getHostWithSecret ${host.name}")
        val password = secrets.getPassword(id)
        val keyData = secrets.getPrivateKey(id)

        return HostWithSecret(
            host = host,
            password = password,
            privateKeyPem = keyData?.first,
            privateKeyPassphrase = keyData?.second
        )
    }

    suspend fun saveHost(
        host: Host,
        password: String? = null,
        privateKeyPem: String? = null,
        privateKeyPassphrase: String? = null
    ): Long {
        val now = System.currentTimeMillis()
        val hostToSave = host.copy(
            updatedAt = if (host.id == 0L) now else now,
            createdAt = if (host.id == 0L) now else host.createdAt
        )
        val isNew = hostToSave.id == 0L
        log.i("saveHost ${hostToSave.name} (${if (isNew) "new" else "update"})")

        val hostId = if (isNew) {
            dao.insert(hostToSave)
        } else {
            dao.update(hostToSave)
            hostToSave.id
        }

        when (host.authType) {
            AuthType.PASSWORD -> {
                password?.let { secrets.savePassword(hostId, it) }
            }
            AuthType.PUBLIC_KEY -> {
                secrets.savePrivateKey(hostId, privateKeyPem ?: "", privateKeyPassphrase)
            }
            AuthType.AGENT_FORWARD -> {}
        }

        log.i("saveHost done id=$hostId")
        return hostId
    }

    suspend fun deleteHost(id: Long) {
        log.i("deleteHost id=$id")
        dao.deleteById(id)
        secrets.deleteSecret(id)
    }
}
