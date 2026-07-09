---
name: flujo-release-build
description: Pasos exactos de release.sh y build-apk.sh, y cuándo usar cada uno
metadata:
  type: project
---

**Release completo (`./release.sh [patch|minor|major]`)** — cumple [[reglas-rhd-bst]] al 100%:
1. Lee versión actual de `platforms/android/build.gradle.kts` (desde HIM-016, antes `app/build.gradle.kts`).
2. Bump semver según tipo; `versionCode` +1 automático.
3. Actualiza `build.gradle.kts`, `git commit -m "chore: bump version to vX.Y.Z (code N)"`.
4. Rebuildea la imagen Docker `bastion-builder` (desde 2026-07-08, ver nota abajo) y compila con
   `./gradlew assembleRelease` (buildType `release`, JDK17 + Android SDK montados).
5. Verifica que exista `platforms/android/build/outputs/apk/release/app-release.apk` (aborta si no).
6. Copia a `~/apk-share/bastion-v{VERSION}.apk` y `~/apk-share/bastion-debug.apk` (el nombre del
   alias sigue diciendo "debug" por compatibilidad, pero el binario ya es `release`).
7. `git tag -a vX.Y.Z` + push tag + push master.
8. `gh release create` en `lerna-soft/bastion` — solo notas, sin subir APK (RHD-BST-003).
9. Genera/actualiza `~/apk-share/latest.json` (versionName, versionCode, downloadUrl, changelog, etc.).
10. Mata proceso en :8765 y reinicia `serve.py` en background.

**Build rápido (`./build-apk.sh`)** — NO bumpea versión, NO tagea, NO crea release GitHub:
1. Verifica JDK/SDK locales.
2. `docker build` + `docker run` → `./gradlew assembleRelease`.
3. Copia APK a `bastion-debug.apk` (alias) y a nombre con timestamp.
4. Regenera `latest.json` con changelog genérico.
5. Reinicia `serve.py`.

**Cambio 2026-07-08 (debuggable=false):** el `Dockerfile` pasó de `assembleDebug` a
`assembleRelease`. Antes, TODO build (incluso firmado con el keystore real) llevaba
`android:debuggable="true"` porque Android lo marca automático para el buildType `debug`,
sin importar la firma — eso disparaba avisos extra de Android/Play Protect ("app para
desarrolladores") al instalar fuera de Play Store. Se agregó un buildType `release`
(mismo keystore, `isMinifyEnabled=false` a propósito por reflexión de SSHD/BouncyCastle
no probada con shrink) y `release.sh` ahora rebuildea la imagen Docker en cada corrida
(antes no lo hacía, asumía la imagen ya construida — necesario porque cambió el `CMD`).

**Why:** `build-apk.sh` existe para iterar rápido en desarrollo sin generar una versión "oficial" cada vez. Pero si se ejecuta directamente como si fuera un release, viola RHD-BST-001/006 (ver [[inconsistencias-pendientes]] #4).

**How to apply:** para cualquier entrega que el usuario vaya a instalar/probar como versión nueva, usar SIEMPRE `release.sh`. Reservar `build-apk.sh` solo para verificar que compila durante desarrollo activo, dejando claro al usuario que ese APK no quedó versionado/tageado/liberado formalmente.

Comando Docker manual documentado en `AGENTS.md` (monta `-v ~/apk-share:/out`) está desactualizado — el Dockerfile real no escribe en `/out`; ignorar esa variante y usar los scripts.

**HIM-016 (2026-07-08):** el repo se reestructuró por plataforma (`core/`, `platforms/android/`,
`platforms/desktop/` pendiente). Esta memoria vive FUERA del repo (no viaja con `git pull` a otro
servidor) — el `AGENTS.md` del repo es ahora la fuente de verdad portable para arquitectura,
reglas y estado de specs; esta memoria queda como contexto operativo de este servidor específico.
