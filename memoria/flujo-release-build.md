---
name: flujo-release-build
description: Pasos exactos de release.sh y build-apk.sh, y cuándo usar cada uno
metadata:
  type: project
---

**Release completo (`./release.sh [patch|minor|major]`)** — cumple [[reglas-rhd-bst]] al 100%:
1. Lee versión actual de `app/build.gradle.kts`.
2. Bump semver según tipo; `versionCode` +1 automático.
3. Actualiza `build.gradle.kts`, `git commit -m "chore: bump version to vX.Y.Z (code N)"`.
4. Build APK en Docker (`bastion-builder`, JDK17 + Android SDK montados).
5. Verifica que exista `app/build/outputs/apk/debug/app-debug.apk` (aborta si no).
6. Copia a `~/apk-share/bastion-v{VERSION}.apk` y `~/apk-share/bastion-debug.apk`.
7. `git tag -a vX.Y.Z` + push tag + push master.
8. `gh release create` en `lerna-admin/bastion` — solo notas, sin subir APK (RHD-BST-003).
9. Genera/actualiza `~/apk-share/latest.json` (versionName, versionCode, downloadUrl, changelog, etc.).
10. Mata proceso en :8765 y reinicia `serve.py` en background.

**Build rápido (`./build-apk.sh`)** — NO bumpea versión, NO tagea, NO crea release GitHub:
1. Verifica JDK/SDK locales.
2. `docker build` + `docker run` → `./gradlew assembleDebug`.
3. Copia APK a `bastion-debug.apk` y a nombre con timestamp.
4. Regenera `latest.json` con changelog genérico.
5. Reinicia `serve.py`.

**Why:** `build-apk.sh` existe para iterar rápido en desarrollo sin generar una versión "oficial" cada vez. Pero si se ejecuta directamente como si fuera un release, viola RHD-BST-001/006 (ver [[inconsistencias-pendientes]] #4).

**How to apply:** para cualquier entrega que el usuario vaya a instalar/probar como versión nueva, usar SIEMPRE `release.sh`. Reservar `build-apk.sh` solo para verificar que compila durante desarrollo activo, dejando claro al usuario que ese APK no quedó versionado/tageado/liberado formalmente.

Comando Docker manual documentado en `AGENTS.md` (monta `-v ~/apk-share:/out`) está desactualizado — el Dockerfile real no escribe en `/out`; ignorar esa variante y usar los scripts.
