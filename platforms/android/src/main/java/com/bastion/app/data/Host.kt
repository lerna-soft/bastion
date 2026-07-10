package com.bastion.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AuthType {
    PASSWORD,
    PUBLIC_KEY,
    AGENT_FORWARD
}

@Entity(tableName = "hosts")
data class Host(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val hostname: String,
    val port: Int = 22,
    val username: String,
    val authType: AuthType = AuthType.PASSWORD,
    val useAgentForwarding: Boolean = false,
    // HIM-019 — Jump host / ProxyJump: id de OTRO host del vault a través del cual se
    // tunelea la conexión. null = conexión directa. La cadena se resuelve siguiendo
    // jumpHostId de cada salto (A→B→C) en VaultRepository.resolveConnectionChain().
    val jumpHostId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
