---
name: datos-operativos
description: Rutas, puertos, URLs, IDs Stitch y comandos clave de bastion
metadata:
  type: reference
---

Rutas: proyecto en `/home/lerna/proyectos/bastion` (repo git independiente). Desde HIM-016 (2026-07-08) el código vive separado por plataforma: `core/` (compartido), `platforms/android/` (antes `app/`), `platforms/desktop/` (pendiente). JDK17 en `~/dev-tools/jdk-17.0.19+10`. Android SDK en `~/android-build-env/android-sdk` (platform android-36). APK Docker en `platforms/android/build/outputs/apk/release/app-release.apk` (buildType `release`, ver [[flujo-release-build]]). APK compartido en `~/apk-share/bastion-debug.apk` y `~/apk-share/bastion-v{VERSION}.apk`. `latest.json` en `~/apk-share/latest.json`. Detalle completo de arquitectura/reglas/estado en `AGENTS.md` del repo (viaja con git, a diferencia de esta memoria externa).

Servidor: `serve.py` en puerto **8765**, URL `http://192.168.0.100:8765/`. Endpoint update: `/update`. Endpoint logs: `POST /logs` (recepción) y `GET /logs` (visor). Endpoint APK: `GET /apk-share/bastion-v*.apk`.

GitHub: `https://github.com/lerna-admin/bastion` (privado) — releases solo con notas, sin subir APK (RHD-BST-003, ver [[reglas-rhd-bst]]).

Stitch (diseño, MCP):
- API Key: `AQ.Ab8RN6IAOY3R_brSs-qjuLZxfisqk-FefrRhzehu2jgaRhJLgg`
- Project ID activo ("SSH Terminal Manager" / Terminal Core): `2946918035035581471`
- Project ID antiguo/obsoleto ("Bastion App Redesign"): `2134170800474297456` (ver [[inconsistencias-pendientes]] #1)
- Bridge MCP local: `.opencode/stitch-bridge.mjs`, configurado en `opencode.json`.
- Screen IDs: Terminal Activa `8340cef99f634e888a55fbacd13905d5`, Dashboard Conexiones `85b987da657947e1a61351d8aec94894`, Terminax Dashboard `2208a2a609314aba9f150b9b1925b71e`, Configuración Sistema `0e584c56060b442f85df03f29ee6d6e3`, Gestión Llaves SSH `073a0754ad594a868ff8e665654b18c8`, Logo `ef6d5367814d4d01a8ded48f1d3fda2c`.

Dependencias clave: Apache MINA SSHD 2.18.0, xterm.js 6.0.0 core + addon-fit 0.11.0, `net.i2p.crypto:eddsa:0.3.0`, `org.bouncycastle:bcpkix-jdk18on:1.78.1`.

Comandos: release `./release.sh [patch|minor|major]`; build rápido `./build-apk.sh`; tests `./gradlew test` (obligatorio antes de release, RHD-BST-011); descargar assets terminal `scripts/download-xterm.sh`; reiniciar servidor manual `fuser -k 8765/tcp 2>/dev/null || true; python3 serve.py`.

**Why:** centralizar estos datos evita tener que releer todos los .md cada vez que se necesita un puerto, URL o ID de Stitch.

**How to apply:** consultar este archivo directo para cualquier tarea operativa (build, release, deploy de diseño) en vez de buscar en múltiples .md dispersos.
