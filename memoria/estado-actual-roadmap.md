---
name: estado-actual-roadmap
description: Estado real de bastion (v1.1.9) y roadmap, corrige el PROYECTO.md desactualizado
metadata:
  type: project
---

Versión real en código: `versionCode = 21`, `versionName = "1.1.9"` (`app/build.gradle.kts`), commit `01a06c4` "v1.1.9 — HIM-006: tema app/terminal separados, tema por pestaña, StatsCollector oculto".

Historial: v1.1.1(13) → v1.1.2(14) → v1.1.3(15) → v1.1.4(16, HIM-002 MVP funcional: DB v2, SettingsScreen/SSHKeysScreen reales) → v1.1.5(17) → v1.1.6(18) → v1.1.7(HIM-004, rediseño Stitch verde obsidiana) → v1.1.8(HIM-005: Neutral Dark default, OLED, stats reales, fix status bar) → v1.1.9(HIM-006, actual).

Completado (ROADMAP.md): conexión SSH password+key, terminal WebView xterm.js, vault cifrado, multi-tab, pinch zoom, 6 temas, remote logging, crash detection/reporting, auto-update, About screen.

En progreso: RemoteLogger v2 (persistencia/buffering/crash logs), SSH keepalive config, historial de conexiones.

Planeado (v1.1+): registro/auth de usuarios, multi-usuario con roles, sync de credenciales server-side, generación de claves SSH, port forwarding, SFTP browser, perfiles de conexión, batch commands, grabación/playback de sesión, 2FA, unlock biométrico, export/import de vault.

Pendientes explícitos de HIM-006 (WIP, no desplegado en dispositivo):
- Falta build release formal (tag v1.1.9 vía `release.sh`, aunque el commit ya dice v1.1.9).
- `SettingsScreen` preview de Monokai deja tokens `Monokai*` fijos intencionalmente.
- WebView de terminal tiene `setBackgroundColor(#0c160a)` fijo (verde Stitch) detrás del xterm — flash inicial pendiente de resolver.
- Falta verificar en dispositivo real: tema Settings afecta shell, tema por pestaña no afecta el resto, stats no ensucian terminal.
- Posible mejora: extender tema por pestaña a `SystemStatsPanel` (hoy usa tema de app porque vive fuera de `TerminalTab`).

**Why:** `PROYECTO.md` decía "MVP en construcción (Iter 1)" — completamente desactualizado frente al código real; se corrigió en esta sesión (2026-07-08).

**How to apply:** usar este archivo como fuente de verdad del estado real en vez de `PROYECTO.md`/estados "borrador" de los specs HIM (ver [[inconsistencias-pendientes]]).
