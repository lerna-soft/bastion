package com.bastion.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val serverName: String = "BASTION-PRIME-01",
    val timezone: String = "EST (Eastern Standard Time)",
    val language: String = "English (United States)",
    val fontSize: Float = 14f,
    val twoFactorEnabled: Boolean = true,
    val sessionTimeout: String = "30 Minutes",
    val webhookUrl: String = "",
    val emailAlerts: Boolean = false,
    val colorMode: String = "DARK",
    // Versión que el usuario descartó explícitamente (botón "Dismiss"/"Later") — evita que la
    // misma versión vuelva a aparecer como banner en cada onResume. checkForUpdate() manual desde
    // Settings ignora este campo a propósito (acción explícita del usuario, siempre debe mostrar).
    val skippedUpdateVersion: String = ""
)
