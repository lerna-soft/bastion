---
name: reglas-rhd-bst
description: Reglas RHD-BST-001 a 012 del proyecto bastion, con duplicados detectados
metadata:
  type: project
---

Reglas vinculantes del proyecto bastion (definidas en `/home/lerna/AGENTS.md` §BASTION y repetidas en `HIM-002.spec.md`):

- **RHD-BST-001 / RHD-BST-006** (duplicadas, ver [[inconsistencias-pendientes]]): cada build/APK nuevo = nueva versión. Nunca compilar sin bump de `versionCode`+`versionName`.
- **RHD-BST-002**: pipeline de release completo = `./release.sh [patch|minor|major]` (7 pasos, ver [[flujo-release-build]]).
- **RHD-BST-003**: el APK se descarga SOLO desde `192.168.0.100:8765`. GitHub releases son solo tracking de versión, sin binario (repo privado).
- **RHD-BST-004**: `versionCode` = auto-increment; `versionName` = semver.
- **RHD-BST-005**: la app valida actualizaciones vía `http://192.168.0.100:8765/update`.
- **RHD-BST-007**: prohibido texto hardcodeado en UI — usar `BuildConfig` o estado dinámico.
- **RHD-BST-008**: botones "Save"/"Connect" en `HostEditScreen` deben tener lógica real, nunca `onClick = {}`.
- **RHD-BST-009**: pantallas de formulario deben validar campos requeridos.
- **RHD-BST-010**: prohibido bottom navigation dentro de `TerminalTab` — terminal es pantalla completa.
- **RHD-BST-011**: tests obligatorios antes de release (`./gradlew test` + cobertura).
- **RHD-BST-012**: fidelidad Stitch — cada elemento visual debe coincidir con el diseño de referencia.

Reglas sin ID formal pero vinculantes (`AGENTS.md` local del proyecto, sección "Critical Rules"):
- No shell local — prohibido `Runtime.exec`/`sh`, solo canales SSH (ADR-005).
- WebView de terminal sin acceso a Internet, solo assets locales + bridge JS.
- Secretos siempre cifrados vía Keystore → EncryptedSharedPreferences, nunca en Room en claro.
- Todo cambio debería actualizar `HIM-001.spec.md` — **en la práctica no se sigue** (ver [[inconsistencias-pendientes]]).
- Build en Docker aislado, sin instalar JDK/SDK en el servidor base.

**Why:** estas reglas se definieron para evitar builds sin versionar (el usuario no puede saber si tiene la última APK), evitar exponer el binario fuera del canal controlado, y mantener el terminal aislado de red por seguridad.

**How to apply:** antes de sugerir o ejecutar cualquier build de bastion, verificar cuál script se usará — `build-apk.sh` NO cumple RHD-BST-001/006 (no bumpea versión), solo `release.sh` cumple el flujo completo. Ver [[flujo-release-build]].
