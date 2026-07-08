# Bastion Roadmap

> Ver `AGENTS.md` para el estado de continuidad completo (arquitectura, reglas, specs
> HIM). Este archivo es un resumen de alto nivel.

## Completed (Android, v1.1.23)
- [x] SSH connection (password + key auth)
- [x] Terminal WebView with xterm.js — botones rápidos (Esc/Tab/Ctrl/flechas) funcionales
- [x] Vault for credential storage (encrypted)
- [x] Multi-tab support real (sin "reemplazo" de pestañas, HIM-008)
- [x] Pestañas sobreviven navegación interna (HIM-013)
- [x] Sesiones SSH sobreviven cambio de app (keepalive + foreground service, HIM-011)
- [x] Pinch zoom in terminal
- [x] Theme switching (6 themes)
- [x] Remote logging to test server + ring buffer + pantalla de Logs in-app (HIM-009)
- [x] Crash detection (Java + nativo/OOM/ANR) y captura a prueba de balas
- [x] App nunca se cierra por un error (resiliencia, HIM-009)
- [x] Auto-update system funcional (causa raíz del bug real resuelta en HIM-012)
- [x] Build tipo `release` (no `debug`) — sin aviso de "app para desarrolladores"
- [x] About screen
- [x] UI no-funcional oculta/corregida (Settings Security/Notifications/API Keys, HIM-015)

## In Progress
- [ ] **Multiplataforma (HIM-016)** — `:core` extraído y Android sin regresión ✅;
      `platforms/desktop/` (Compose Desktop, Windows/Linux/Mac) pendiente de crear —
      ver `AGENTS.md` sección "Current State" y `HIM-016.spec.md` para el detalle
- [ ] Connection history

## Planned (v1.1+ / post-multiplataforma)
- [ ] Windows CI (GitHub Actions `windows-latest`) — HIM-018
- [ ] Vault persistente + keychain nativo en Desktop — HIM-017
- [ ] User registration and authentication system
- [ ] Multi-user support with role-based access
- [ ] Server-side credential sync
- [ ] SSH key generation
- [ ] Port forwarding (local/remote)
- [ ] SFTP file browser
- [ ] Connection profiles (save/load)
- [ ] Batch commands across multiple servers
- [ ] Session recording and playback
- [ ] Two-factor authentication (real, no el placeholder oculto en HIM-015)
- [ ] Biometric unlock
- [ ] Export/import vault

## Future Considerations
- [ ] iOS — bloqueado técnicamente: Apache MINA SSHD no corre en Kotlin/Native, necesita
      librería SSH nativa Swift (ver ADR-D4 en `HIM-016.spec.md`)
- [ ] Team collaboration features
- [ ] Audit logging
- [ ] Compliance reports
