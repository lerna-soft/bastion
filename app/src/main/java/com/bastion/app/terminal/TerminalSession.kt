package com.bastion.app.terminal

import com.bastion.app.ssh.SshSession
import com.bastion.app.ui.theme.ColorMode

/**
 * Un tab de terminal abierto. Vive en BastionApp.terminalSessions (HIM-013) para sobrevivir a la
 * navegación interna del NavHost (agregar/editar servidor, about) — antes vivía en un remember{}
 * de AppLayout y se perdía al salir de la ruta MAIN.
 */
data class TerminalSession(
    val id: Int,
    val title: String,
    val session: SshSession,
    val hostId: Long,
    val hostname: String = "",
    val port: Int = 22,
    val username: String = "",
    val authTypeLabel: String = "",
    val theme: String = ColorMode.DARK.name
)
