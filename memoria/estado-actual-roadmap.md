---
name: estado-actual-roadmap
description: Estado real de bastion (v1.1.28) y roadmap, corrige el PROYECTO.md desactualizado
metadata:
  type: project
---

Versión real en código: `versionCode = 40`, `versionName = "1.1.28"` (`platforms/android/build.gradle.kts`,
desde HIM-016 ya no es `app/build.gradle.kts`), release publicado en `lerna-soft/bastion` 2026-07-09.

Historial reciente (desde v1.1.9, ver commits para detalle completo v1.1.1-1.1.9):
v1.1.9(HIM-006) → ... → v1.1.16(HIM-013) → v1.1.22(HIM-014/015) → v1.1.23(HIM-016 arranca:
reestructura por plataforma `core/`+`platforms/android/`+`platforms/desktop/` pendiente) →
v1.1.24(fix nombre APK tras HIM-016, `android-release.apk`) → v1.1.25(HIM-017: terminal usa
ancho real vía `channel.sendWindowChange()`; HIM-018: distribución vía GitHub — repo público,
transferido de `lerna-admin` a `lerna-soft`, password de keystore rotada preservando la misma
firma, releases con APK real adjunto, `UpdateChecker` consulta GitHub API en vez del servidor
local — ver [[reglas-rhd-bst]] RHD-BST-003 obsoleta) → v1.1.26(fix: `RemoteLogger.crash()`
perdía el stack trace por enqueue async que nunca corría antes de que el proceso muriera; ahora
envío inline) → v1.1.27: selección/copiar texto en la terminal — xterm.js
solo selecciona con eventos de RATÓN, así que un toggle "modo selección" reenvía los touch events
como mouse events sintéticos al WebView y agrega una barra Copiar/Todo/Cerrar con puente al
portapapeles Android; se **retiraron** los botones Esc/Tab/Ctrl/Alt/flechas (pedido explícito del
usuario, prioriza selección sobre teclas especiales); nueva sección "Updates" en Settings con
botón manual de chequeo (reusa `BastionApp.updateState/checkForUpdate/downloadUpdate`). Verificado
en dispositivo real por el usuario: detectó y descargó la actualización vía GitHub sin problema. →
**v1.1.28** (2026-07-09, actual): la app forzaba la actualización — `AlertDialog(onDismissRequest
= {})` sin forma de cerrarlo para Available/Downloading/Ready, y `downloadUpdate()` lanzaba el
instalador solo apenas terminaba de descargar. El usuario lo reportó como "horrible cuando pasa".
Fix: tarjeta flotante no-modal con X (`UpdateBanner` en `MainActivity.kt`), sin auto-instalar
(el usuario dispara "Install now" cuando quiere). Cerrar el aviso persiste
`AppSettings.skippedUpdateVersion` (Room migration 2→3) para no re-nagear la misma versión, pero
"Check for Updates" manual en Settings ignora el skip. Se agregó "Version History" en Settings
(lista de releases de GitHub, solo informativa) — el usuario pidió explícitamente poder "regresar
a una versión anterior", pero **Android bloquea instalar un versionCode menor al instalado a
nivel de PackageManager**; ninguna app normal puede saltarse eso sin permisos de sistema/root
(ni pidiendo el rol de "instalador por defecto" — ese permiso solo dispara el instalador, no
habilita el downgrade). El usuario aceptó la opción recomendada: historial informativo sin
rollback real, en vez de un flujo de desinstalar+backup+reinstalar (que sí sería técnicamente
viable pero mucho más grande/riesgoso — no se implementó, queda como posible follow-up si se
vuelve a pedir).

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
