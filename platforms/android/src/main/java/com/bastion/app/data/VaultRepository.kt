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
    private val settingsDao = db.appSettingsDao()
    private val sshKeyDao = db.sshKeyDao()
    private val apiKeyDao = db.apiKeyDao()

    // ── Hosts ──────────────────────────────────────────────

    fun getAllHosts(): Flow<List<Host>> = dao.getAllHosts()

    suspend fun getHost(id: Long): Host? = dao.getHostById(id)

    suspend fun getHostWithSecret(id: Long): HostWithSecret? {
        val host = dao.getHostById(id) ?: return null
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
        val hostId = if (isNew) {
            dao.insert(hostToSave)
        } else {
            dao.update(hostToSave)
            hostToSave.id
        }
        when (host.authType) {
            AuthType.PASSWORD -> password?.let { secrets.savePassword(hostId, it) }
            AuthType.PUBLIC_KEY -> secrets.savePrivateKey(hostId, privateKeyPem ?: "", privateKeyPassphrase)
            AuthType.AGENT_FORWARD -> {}
        }
        return hostId
    }

    /**
     * HIM-019 — Resuelve la cadena de saltos para conectar a [targetId].
     * Sigue `jumpHostId` de cada host (target → jump → jump…) y devuelve la cadena
     * ordenada del PRIMER salto (alcanzable directo desde el dispositivo) al DESTINO final,
     * con sus secretos ya cargados. Conexión directa → lista de 1 elemento.
     * Protegido contra ciclos (visited): si un jumpHostId apunta a un host ya visto, corta.
     */
    suspend fun resolveConnectionChain(targetId: Long): List<HostWithSecret> {
        val all = dao.getAllHostsList()
        val orderedIds = JumpHostChain.resolveChainIds(all, targetId)
        if (orderedIds.size > 1) {
            log.i("resolveConnectionChain: cadena de ${orderedIds.size} saltos para host $targetId")
        }
        // Carga los secretos de cada host en el orden resuelto (primer salto → destino).
        return orderedIds.mapNotNull { getHostWithSecret(it) }
    }

    suspend fun deleteHost(id: Long) {
        dao.deleteById(id)
        secrets.deleteSecret(id)
    }

    // ── App Settings ───────────────────────────────────────

    suspend fun getSettings(): AppSettings {
        return settingsDao.getSettings() ?: AppSettings()
    }

    suspend fun saveSettings(settings: AppSettings) {
        settingsDao.saveSettings(settings)
    }

    // ── SSH Keys ───────────────────────────────────────────

    fun getAllSshKeys(): Flow<List<SshKey>> = sshKeyDao.getAllKeys()

    suspend fun addSshKey(name: String, type: String, fingerprint: String, servers: String): Long {
        val key = SshKey(
            name = name,
            type = type,
            fingerprint = fingerprint,
            servers = servers,
            created = System.currentTimeMillis(),
            lastUsed = System.currentTimeMillis(),
            isActive = true
        )
        return sshKeyDao.insert(key)
    }

    suspend fun renameSshKey(id: Long, name: String) {
        sshKeyDao.rename(id, name)
    }

    suspend fun deleteSshKey(id: Long) {
        sshKeyDao.deleteById(id)
    }

    // ── API Keys ───────────────────────────────────────────

    fun getAllApiKeys(): Flow<List<ApiKey>> = apiKeyDao.getAllKeys()

    suspend fun addApiKey(label: String, keyValue: String): Long {
        return apiKeyDao.insert(ApiKey(label = label, keyValue = keyValue))
    }

    suspend fun deleteApiKey(id: Long) {
        apiKeyDao.deleteById(id)
    }
}
