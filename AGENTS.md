# BASTION — Cliente SSH multiplataforma (Android + Desktop + iOS futuro)

<!-- CURRENT_DATE: 2026-07-08 -->
<!-- PROJECT_DIR: /home/lerna/proyectos/bastion/ (repo git independiente) -->
<!-- SCOPE: Documento de continuidad — léelo primero en cualquier servidor tras `git pull` -->

## Project Overview
Cliente SSH tipo Termius, multi-pestaña, sin shell local. **Android** es la plataforma
estable y publicada (v1.1.23). Desde HIM-016 el proyecto se está expandiendo a
**Windows/Linux/Mac** (Compose Desktop) y, a futuro, **iOS** (bloqueado técnicamente,
ver ADR-D4 abajo).

---

## Current State — 2026-07-08

### ✅ Android (`platforms/android/`) — ESTABLE, publicado
- **Versión instalada/publicada:** v1.1.23 (versionCode 35) — **no tocar sin razón**, es
  la versión que el usuario tiene funcionando en su teléfono ahora mismo.
- Multi-pestaña real, sesiones sobreviven navegación interna y cambio de app (keepalive
  SSH + foreground service + exención de batería), captura de crashes (Java + nativo/OOM/ANR
  vía `ApplicationExitInfo`), pantalla de Logs in-app, auto-update funcional, build tipo
  `release` (no `debug`, para que Android no marque la app como "para desarrolladores").
- Ver tabla de specs HIM abajo para el detalle de cada fix.

### 🚧 Multiplataforma (HIM-016) — EN PROGRESO, es el trabajo activo

Decisiones de arquitectura ya discutidas y confirmadas con el usuario (detalle completo
con ADRs en `HIM-016.spec.md`):
1. **Estructura por directorio de plataforma** (decisión explícita del usuario, no el
   layout Gradle-multimódulo "plano" que se usa por defecto):
   ```
   bastion/
   ├── core/                  ← Kotlin/JVM compartido (YA CREADO)
   ├── platforms/
   │   ├── android/           ← antes "app/", YA MOVIDO, sin regresión
   │   ├── desktop/           ← Windows/Linux/Mac — PENDIENTE, siguiente paso
   │   └── ios/               ← futuro, bloqueado (ver ADR-D4)
   └── settings.gradle.kts
   ```
2. **`:core` es Kotlin/JVM plano**, NO Kotlin Multiplatform formal todavía (sin
   `expect`/`actual`). Android y Desktop son ambos JVM real — Apache MINA SSHD corre
   idéntico en los dos sin necesitar abstracción de plataforma. Se formaliza a KMP
   cuando entre iOS.
3. **Terminal en Desktop:** reusar tal cual los assets de xterm.js
   (`platforms/android/src/main/assets/terminal/*`) vía un WebView de escritorio
   basado en JCEF (Chromium embebido). Trade-off aceptado: +150-200MB al instalador.
4. **Secretos en Desktop:** keychain nativo del SO (Linux: Secret Service/libsecret;
   Windows: DPAPI; Mac: Keychain) — mismo nivel que Android Keystore.
5. **Secuenciación:** Linux primero, end-to-end, en este mismo servidor. Windows queda
   para una spec aparte con GitHub Actions (`windows-latest`) — normalmente hace falta
   compilar en esa plataforma. Mac requiere macOS real + cuenta Apple Developer para
   notarizar (no bloquea el código, sí el empaquetado firmado).

**Progreso real de HIM-016:**
- [x] Reestructuración de directorios (`core/`, `platforms/android/`, `platforms/desktop/`
      placeholder) — commit `6b9ad9a`.
- [x] `:core` creado, `SshSession`/`SshClientManager`/`AuthMethods` movidos ahí.
      `CoreLog` (abstracción de logging sin Android) reemplaza `RemoteLogger` dentro de
      `:core`; `:app` sigue viendo el mismo comportamiento de logging (instala un sink
      que reenvía a `RemoteLogger`, cero regresión).
- [x] **Regresión Android verificada:** `:platforms:android:testReleaseUnitTest` +
      `:platforms:android:assembleRelease` compilan y pasan OK (build local, NO se
      publicó — v1.1.23 sigue siendo la versión distribuida).
- [ ] **PENDIENTE — siguiente paso:** crear `platforms/desktop/` (módulo Compose Desktop):
  - Pantalla mínima de conexión (host/puerto/usuario/password, **sin persistencia** en
    esta primera iteración — vault/keychain quedan para HIM-017).
  - Terminal JCEF + xterm.js reusado.
  - Conexión SSH real end-to-end usando `:core` (el mismo `SshSession` que Android).
  - Empaquetado: tarea Compose Desktop `nativeDistributions` → `.deb`/AppImage.
- [ ] HIM-018 (spec aparte, no creada aún): CI GitHub Actions `windows-latest` para
      generar el instalador de Windows.
- [ ] iOS: bloqueado — Apache MINA SSHD es JVM puro, **no corre en Kotlin/Native**. El
      día que se aborde, la capa SSH necesita una librería Swift nativa vía interop
      (algo tipo Citadel/NIOSSH), detrás de `expect`/`actual` en `:core` ya formalizado
      como KMP. No es "portar", es un componente nuevo.

### ⚠️ Restricción de este servidor
Este servidor (donde corre Claude Code al momento de escribir esto) **no tiene entorno
gráfico** (`$DISPLAY` vacío, sin Wayland, sin Xvfb instalado). Para el trabajo de
`platforms/desktop/`:
- Compilación y smoke-tests de arranque: sí se pueden hacer aquí (con Xvfb, a instalar).
- Verificación visual real (que el terminal se vea/funcione bien): requiere una máquina
  Linux con GUI — o instalar Xvfb en este servidor y aceptar que la verificación visual
  fina la hace el usuario en su propia máquina.

---

## Reglas críticas

### RHD-BST — reglas operativas de build/release (copiadas aquí desde el AGENTS.md
### global de `~/` para que viajen con el repo — la fuente original vive en
### `/home/lerna/AGENTS.md`, que es específico del servidor y NO se clona con git)

- **RHD-BST-001 / RHD-BST-006** (duplicadas, mismo significado): CADA NUEVO
  APK/BUILD = NUEVA VERSIÓN. No builds sin bump de `versionCode`+`versionName`.
- **RHD-BST-002:** pipeline de release completo = `./release.sh [patch|minor|major]`.
  Pasos: lee versión de `platforms/android/build.gradle.kts` → bump semver → commit →
  rebuild imagen Docker `bastion-builder` → `./gradlew assembleRelease` → copia APK a
  `~/apk-share/` → tag git + push → `gh release create` (solo notas, sin subir APK) →
  actualiza `~/apk-share/latest.json` → reinicia `serve.py` en :8765.
- **RHD-BST-003:** el APK se descarga SOLO desde `192.168.0.100:8765`. GitHub releases
  son solo para tracking de versión, sin binario (repo privado).
- **RHD-BST-004:** `versionCode` = auto-increment; `versionName` = semver.
- **RHD-BST-005:** la app valida actualizaciones vía `http://192.168.0.100:8765/update`.
- **RHD-BST-007:** prohibido texto hardcodeado en UI — usar `BuildConfig` o estado
  dinámico.
- **RHD-BST-008:** botones "Save"/"Connect" en `HostEditScreen` deben tener lógica
  real, nunca `onClick = {}`.
- **RHD-BST-009:** pantallas de formulario deben validar campos requeridos.
- **RHD-BST-010:** prohibido bottom navigation dentro de `TerminalTab` — terminal es
  pantalla completa.
- **RHD-BST-011:** tests obligatorios antes de release (`./gradlew test` + cobertura).
- **RHD-BST-012:** fidelidad Stitch — cada elemento visual debe coincidir con el
  diseño de referencia.

### Reglas de arquitectura (sin ID formal, vinculantes)
- **No shell local** — prohibido `Runtime.exec`/`sh` en ninguna plataforma. Solo
  canales SSH.
- **WebView de terminal sin acceso a Internet** — solo assets locales + bridge JS
  (aplica también al JCEF de Desktop cuando se implemente).
- **Secretos siempre cifrados** — Keystore/EncryptedSharedPreferences en Android;
  keychain nativo del SO en Desktop (ver ADR-D3 en HIM-016). Nunca en claro en DB.
- **Docker build** — no instalar JDK/SDK/Android SDK en el servidor base; todo build
  Android corre en el contenedor `bastion-builder`.
- **La app nunca debe cerrarse por un error** (requisito duro del usuario, HIM-009):
  todo `CoroutineScope` propio usa `CoroutineExceptionHandler`; los callbacks de UI que
  ejecutan lógica real se envuelven en el helper `safe {}`
  (`com.bastion.app.util.safe`, Android) — sigue el mismo criterio si se agrega lógica
  equivalente en Desktop.
- **Version management:** ANTES de hacer build, SIEMPRE verificar versión actual en
  `platforms/android/build.gradle.kts` e incrementar `versionCode` + `versionName`.
  Cada build debe ser una versión nueva que el usuario no tenga.
- **NO tocar el release publicado sin que el usuario lo pida explícitamente** — al
  trabajar en features nuevas (como HIM-016 multiplataforma), verificar/compilar en
  local está bien; correr `release.sh` (que sí publica) requiere intención explícita.

### ADRs de HIM-016 (multiplataforma) — resumen, detalle completo en `HIM-016.spec.md`
- **ADR-D1:** `:core` como módulo Kotlin/JVM plano (no KMP formal) mientras solo haya
  targets JVM (Android + Desktop).
- **ADR-D2:** terminal de Desktop reusa xterm.js vía JCEF embebido (no reescribir con
  librería nativa tipo JediTerm).
- **ADR-D3:** secretos en Desktop vía keychain nativo del SO.
- **ADR-D4:** iOS bloqueado — Apache MINA SSHD no corre en Kotlin/Native.
- **ADR-D5:** Linux primero end-to-end; Windows vía CI aparte; Mac requiere macOS real
  para notarizar.

---

## Estado de los specs HIM

| Spec | Feature | Estado | Liberado en |
|------|---------|--------|-------------|
| HIM-001 | MVP inicial (vault + SSH + terminal) | validado | v1.0.x |
| HIM-002 | Funcionalidad real + fidelidad Stitch + tests | completado | v1.1.4 |
| HIM-003 | Server Info Panel + sidebar colapsable | completado | v1.1.6/v1.1.7 |
| HIM-004 | Diseño Stitch "Terminal Core" | completado (superseded parcial por HIM-005/006) | v1.1.9 |
| HIM-005 | Correcciones UI (Neutral Dark default, etc.) | completado | v1.1.8 |
| HIM-006 | Temas separados App/Terminal + StatsCollector oculto | completado | v1.1.9 |
| HIM-007 | Diagnóstico/arreglo flujo auto-actualización (F1-F4) | completado | v1.1.10 |
| HIM-008 | Multi-pestaña real + botón nueva pestaña | completado | v1.1.11 |
| HIM-009 | Resiliencia (app nunca se cierra) + pantalla de Logs | completado | v1.1.12/v1.1.13 |
| HIM-010 | Mitigación OOM (largeHeap, onTrimMemory) | completado | v1.1.14 |
| HIM-011 | Mantener sesiones SSH vivas en 2do plano | completado | v1.1.16 |
| HIM-012 | **Causa raíz real del crash de Update now** (`String.format` con Int) | completado | v1.1.18 |
| HIM-013 | Pestañas sobreviven navegación interna (hoisted a BastionApp) | completado | v1.1.19 |
| HIM-014 | Chequeo de update se repite en `onResume` | completado | v1.1.22 |
| HIM-015 | Ocultar/corregir funcionalidades no conectadas (Settings, header) | completado | v1.1.23 |
| HIM-016 | **Multiplataforma — core extraction + desktop skeleton** | 🚧 en-progreso | — (no publicado, ver arriba) |

---

## Estructura del repo

```
bastion/
├── core/                          Kotlin/JVM compartido (SSH, próximamente más)
│   └── src/main/kotlin/com/bastion/app/
│       ├── ssh/                   SshSession, SshClientManager, AuthMethods
│       └── core/log/              CoreLog (logging sin dependencia Android)
├── platforms/
│   ├── android/                   App Android (antes "app/")
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/bastion/app/
│   │       ├── data/               Vault (Room) + SecretsStore
│   │       ├── terminal/           TerminalBridge, TerminalTab, StatsCollector
│   │       ├── ui/                 AppLayout, Settings, HostEdit, etc.
│   │       ├── update/             UpdateChecker
│   │       ├── logging/            RemoteLogger
│   │       ├── service/            SessionKeepAliveService
│   │       └── util/               Safe.kt
│   └── desktop/                   PENDIENTE de crear (siguiente paso de HIM-016)
├── design/                        Referencias de diseño Stitch (sin cambios)
├── Dockerfile                     Build Android aislado (JDK17+SDK montados)
├── release.sh / build-apk.sh      Scripts de build/release (rutas actualizadas a platforms/android/)
├── serve.py                       Servidor de APKs + logs remotos (puerto 8765)
├── HIM-*.spec.md                  Specs de la metodología HIM (ver tabla arriba)
└── settings.gradle.kts            include(":platforms:android"), include(":core")
```

## Stack por plataforma

| | Android | Desktop (Win/Linux/Mac) | iOS |
|---|---|---|---|
| UI | Jetpack Compose (androidx) | Compose Multiplatform Desktop (pendiente) | Compose Multiplatform iOS (futuro) |
| SSH | Apache MINA SSHD (vía `:core`) | Apache MINA SSHD (vía `:core`, mismo código) | ❌ bloqueado — necesita librería Swift nativa |
| Terminal | WebView + xterm.js | JCEF + xterm.js (mismos assets) | por definir |
| Secretos | Android Keystore/ESP | Keychain nativo del SO (pendiente) | Keychain iOS (futuro) |
| Estado | ✅ v1.1.23 publicado | 🚧 skeleton pendiente | ⛔ bloqueado |

## Development Commands

| Task | Command |
|------|---------|
| Build APK Android (Docker) | `./build-apk.sh` |
| Release completo (bump+build+tag+publish) | `./release.sh [patch\|minor\|major]` |
| Compilar solo (sin publicar) | `docker run --rm -v $(pwd):/src -v ~/dev-tools/jdk-17.0.19+10:/opt/jdk17 -v ~/android-build-env/android-sdk:/opt/android-sdk bastion-builder ./gradlew :core:compileKotlin :platforms:android:testReleaseUnitTest :platforms:android:assembleRelease` |
| Descargar assets xterm.js | `./scripts/download-xterm.sh` |
| APK publicado (descarga) | `http://192.168.0.100:8765/apk-share/bastion-v{VERSION}.apk` |

## SSH Implementation Details (vive en `:core` desde HIM-016)

### Session Lifecycle
```kotlin
// SshSession state machine: IDLE → CONNECTING → AUTHENTICATING → SHELL_ACTIVE → CLOSED
class SshSession {
  suspend fun connect(cfg: AuthConfig): Result<Unit>
  fun write(data: ByteArray)       // from xterm onData
  fun resize(cols: Int, rows: Int) // from xterm FitAddon
  suspend fun close()
  val output: SharedFlow<ByteArray> // to xterm.write(b64)
}
```

### Auth Methods (`core/.../ssh/AuthMethods.kt`)
- **Password:** `session.addPasswordIdentity(it)`
- **PublicKey:** PEM cargado vía BouncyCastle (`loadKeyPairFromPem`), con fallback a
  `OpenSSHKeyPairResourceParser` de MINA SSHD.

### Terminal Bridge (Android; Desktop reusa los mismos assets vía JCEF)
- WebView/JCEF carga `assets/terminal/index.html` con xterm.js bundle.
- `@JavascriptInterface fun onData(data: String)` — desde xterm → SSH stdin.
- `fun writeToTerminal(base64: String)` → `evaluateJavascript("writeToTerminal(b64)")`.
- `fun sendKey(key: String)` — botones rápidos (Esc/Tab/Ctrl/flechas), inyecta bytes
  directo al canal `onData` (NO usa ninguna API de xterm.js — esa era una API
  inexistente y causaba que los botones no hicieran nada, fix en v1.1.20).

### MINA SSHD Notes
- Registrar ed25519: `client.signatureFactories = listOf(BuiltinSignatures.ed25519, ...)`.
- Keepalive: `CoreModuleProperties.HEARTBEAT_INTERVAL` (30s) + `SOCKET_KEEPALIVE` (true).
- Deps: `net.i2p.crypto:eddsa:0.3.0`, `org.bouncycastle:bcpkix-jdk18on:1.78.1`.

## Architecture (Android)
```
flowchart LR
  UI[Compose UI] --> REPO[VaultRepository]
  REPO --> DB[(Room)]
  REPO --> KS[(Keystore/ESP)]
  UI --> SESS[SshSession :core] --> SSH[MINA SSHD]
  SESS <--> BR[TerminalBridge]
  BR <--> WV[WebView + xterm.js]
```

---

## Stitch Design System — "Terminal Core" (referencia de diseño original)

> Nota: el tema visual real de la app YA NO es el verde neón de esta referencia —
> HIM-005 cambió el default a "Neutral Dark" con 6 temas seleccionables. Esta sección
> queda como referencia histórica de assets/tokens de Stitch, no como el diseño activo.

- **Project ID:** `2946918035035581471` — "SSH Terminal Manager"
- **API Key:** `AQ.Ab8RN6IAOY3R_brSs-qjuLZxfisqk-FefrRhzehu2jgaRhJLgg`
- **MCP endpoint:** `https://stitch.googleapis.com/mcp`
- **Diseño completo:** `design/STITCH_DESIGN.md`

| # | Pantalla | Screen ID |
|---|----------|-----------|
| 1 | Terminal Activa | `8340cef99f634e888a55fbacd13905d5` |
| 2 | Dashboard de Conexiones | `85b987da657947e1a61351d8aec94894` |
| 3 | Terminax Terminal Dashboard | `2208a2a609314aba9f150b9b1925b71e` |
| 4 | Configuración del Sistema | `0e584c56060b442f85df03f29ee6d6e3` |
| 5 | Gestión de Llaves SSH | `073a0754ad594a868ff8e665654b18c8` |
| 6 | SSH Terminal Logo | `ef6d5367814d4d01a8ded48f1d3fda2c` |

---

## Cómo retomar el trabajo en otro servidor

1. `git clone`/`git pull` este repo.
2. Leer este `AGENTS.md` completo (ya lo estás haciendo).
3. Revisar `HIM-016.spec.md` para el detalle completo de las decisiones de
   arquitectura multiplataforma y los criterios de aceptación pendientes.
4. El siguiente paso concreto es: **crear `platforms/desktop/`** (módulo Compose
   Desktop) siguiendo el plan de implementación de `HIM-016.spec.md` sección "Plan de
   implementación", pasos 4-7 (los pasos 1-3 ya están hechos).
5. Prerrequisitos de build Android (sin cambios): JDK17 en
   `~/dev-tools/jdk-17.0.19+10`, Android SDK en `~/android-build-env/android-sdk`,
   Docker con imagen `bastion-builder` construida (`docker build -t bastion-builder .`).
6. Prerrequisitos nuevos para Desktop (a instalar en el servidor que se use): JDK17
   (mismo), y si no hay entorno gráfico, `Xvfb` para smoke-tests de arranque.
