# Bastion Roadmap

> Ver `AGENTS.md` para el estado de continuidad completo (arquitectura, reglas, specs
> HIM). Este archivo es un resumen de alto nivel.

## Completed (Android, v1.1.27)
- [x] SSH connection (password + key auth)
- [x] Terminal WebView with xterm.js — usa el ancho real disponible (PTY window-change, HIM-017)
- [x] Terminal: modo selección + copiar texto (arrastre táctil → mouse sintético a xterm.js, v1.1.27);
      reemplazó los botones rápidos Esc/Tab/Ctrl/flechas (retirados por pedido explícito)
- [x] Vault for credential storage (encrypted)
- [x] Multi-tab support real (sin "reemplazo" de pestañas, HIM-008)
- [x] Pestañas sobreviven navegación interna (HIM-013)
- [x] Sesiones SSH sobreviven cambio de app (keepalive + foreground service, HIM-011)
- [x] Pinch zoom in terminal
- [x] Theme switching (6 themes)
- [x] Remote logging to test server + ring buffer + pantalla de Logs in-app (HIM-009)
- [x] Crash detection (Java + nativo/OOM/ANR) y captura a prueba de balas, envío inline
      sin pérdida de stack trace (v1.1.26)
- [x] App nunca se cierra por un error (resiliencia, HIM-009)
- [x] Auto-update system — distribución vía GitHub Releases (repo público `lerna-soft/bastion`,
      APK real como asset, chequeo contra `api.github.com`, HIM-018) + botón manual de
      chequeo en Settings → Updates (v1.1.27)
- [x] Build tipo `release` (no `debug`) — sin aviso de "app para desarrolladores"
- [x] About screen
- [x] UI no-funcional oculta/corregida (Settings Security/Notifications/API Keys, HIM-015)
- [x] GitHub Pages con índice de descargas por plataforma (`docs/index.html`, HIM-018)

## In Progress
- [ ] **Multiplataforma (HIM-016)** — `:core` extraído y Android sin regresión ✅;
      `platforms/desktop/` (Compose Desktop, Windows/Linux/Mac) pendiente de crear —
      ver `AGENTS.md` sección "Current State" y `HIM-016.spec.md` para el detalle
- [ ] Connection history

## Planned (v1.1+ / post-multiplataforma)
- [ ] Windows CI (GitHub Actions `windows-latest`) — spec sin asignar todavía
      (el número HIM-018 ya se usó para distribución vía GitHub, no confundir)
- [ ] Vault persistente + keychain nativo en Desktop — spec sin asignar todavía
      (el número HIM-017 ya se usó para el fix de ancho de terminal, no confundir)
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
