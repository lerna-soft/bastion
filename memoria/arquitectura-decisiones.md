---
name: arquitectura-decisiones
description: Stack, ADRs y convenciones de código establecidas en bastion
metadata:
  type: project
---

Stack: Kotlin + Jetpack Compose + Material 3, Apache MINA SSHD 2.18.0, xterm.js 6.0.0 en WebView local (sin red), Room + Android Keystore/EncryptedSharedPreferences. Min SDK 26, Target SDK 36. Paquete `com.bastion.app`.

ADRs (`HIM-001.spec.md`):
- ADR-001: Compose+Material3 sobre Flutter/RN.
- ADR-002: xterm.js MIT sobre Termux terminal-emulator (GPLv3 descartado por contagio de licencia).
- ADR-003: Apache MINA SSHD sobre JSch (viejo) / sshj (agent forwarding inmaduro).
- ADR-004: secretos en Keystore/ESP, metadatos en Room, nunca secretos en claro en Room.
- ADR-005: sin shell local, único I/O es SSH.
- ADR-006: build en Docker aislado montando JDK/SDK existentes.

Máquina de estados `SshSession`: `IDLE → CONNECTING → AUTHENTICATING → SHELL_ACTIVE → CLOSING → CLOSED`, con ramas `AUTH_FAILED`/`CONNECT_FAILED`, expuesta vía `StateFlow<SessionState>`.

Estructura de código: `data/` (Vault Room+DAO, SecretsStore), `ssh/` (SshClientManager, SshSession, AuthMethods), `terminal/` (TerminalBridge JsInterface, TerminalTab), `ui/` (AppLayout, MainTabsScreen, HostEditScreen, AboutScreen, SettingsScreen, SSHKeysScreen).

Convención de tema: usar `MaterialTheme.colorScheme.*`, nunca constantes `Stitch*` hardcodeadas (corregido en HIM-006 tras detectar la violación en el shell). `BastionTheme(colorMode, applyStatusBar: Boolean = true)` permite tema anidado por pestaña sin pisar el status bar global.

Modelo de datos: `Host` (Room), `Secrets` (ESP, key=`host:<id>`), desde v1.1.4 (HIM-002) DB migrada a v2 con `app_settings`, `ssh_keys`, `api_keys`. HIM-006 propone columna `theme TEXT DEFAULT 'NEUTRAL_DARK'` — no confirmado si ya persiste en Room o solo vive en memoria (ver [[inconsistencias-pendientes]] #8).

**Why:** estas decisiones fijan límites duros (sin shell local, sin red en el WebView, secretos siempre cifrados) que no deben revertirse sin una nueva decisión explícita del usuario.

**How to apply:** cualquier propuesta de feature que requiera shell local, acceso a red desde el WebView de terminal, o guardar secretos en Room sin cifrar, debe marcarse como violación de ADR y consultarse antes de implementar.
