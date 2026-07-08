# BASTION — Cliente SSH multi-pestaña para Android

<!-- CURRENT_DATE: 2026-07-07 -->
<!-- PROJECT_DIR: /home/lerna/proyectos/bastion/ -->
<!-- SCOPE: Contexto para construir Bastion APK Android (Kotlin, MiniSSHD, xterm.js) -->

## Project Overview
App Android nativa tipo Termius: vault de conexiones SSH + terminal multi-pestaña en WebView con xterm.js. Sin shell local del dispositivo.

## Stack
- **UI:** Kotlin + Jetpack Compose + Material 3 (sidebar 260dp + HorizontalPager para terminal)
- **SSH:** Apache MINA SSHD 2.18.0 (Java puro, password + publickey ed25519/rsa + agent forwarding)
- **Terminal:** xterm.js 6.0.0 (MIT) en WebView local (assets, sin red)
- **Vault:** Room + EncryptedSharedPreferences (Android Keystore)
- **Build:** Docker con JDK17 + SDK Android montados (sin instalar en host)
- **Min SDK:** API 26 (Android 8.0)
- **Target SDK:** API 36

## Key Files

| File | Purpose |
|------|---------|
| `HIM-001.spec.md` | Spec completo: user story, Gherkin, ADRs, modelo datos, secuencias |
| `PROYECTO.md` | Datos clave del proyecto |
| `app/build.gradle.kts` | Dependencias: MiniSSHD, Compose, Room, xterm.js assets |
| `app/src/main/.../data/` | Vault (Room: Host entity + DAO) + SecretsStore (EncryptedSharedPreferences) |
| `app/src/main/.../ssh/` | SshClientManager + SshSession (MINA SSHD wrapper) + AuthMethods |
| `app/src/main/.../terminal/` | TerminalBridge (JsInterface) + TerminalTab composable |
| `app/src/main/.../ui/` | AppLayout (sidebar+header+content), MainTabsScreen (VaultTabContent, composables compartidos), HostEditScreen, AboutScreen |
| `Dockerfile` + `build-apk.sh` | Build Docker aislado |

## SSH Implementation Details

### Session Lifecycle
```kotlin
// SshSession state machine: IDLE → CONNECTING → AUTHENTICATING → SHELL_ACTIVE → CLOSED
class SshSession {
  suspend fun connect(host: String, port: Int, username: String, auth: AuthMethod)
  fun write(data: ByteArray)       // from xterm onData
  fun resize(cols: Int, rows: Int) // from xterm FitAddon
  suspend fun close()
  val output: Flow<ByteArray>      // to xterm.write(b64)
}
```

### Auth Methods
- **Password:** `PasswordIdentityProvider` de MINA SSHD
- **PublicKey:** `KeyIdentityProvider` con clave PEM cargada via `BouncyCastleKeyPairResourceParser` + passphrase
- **AgentForwarding:** `AgentIdentity` + `SshClient.setAgentFactory`

### Terminal Bridge
- WebView carga `assets/terminal/index.html` con xterm.js bundle
- `@JavascriptInterface fun onData(data: String)` — desde xterm → SSH stdin
- `fun writeToTerminal(base64: String)` → `evaluateJavascript("term.write(b64)")` — SSH stdout → xterm
- `fun resize(cols: Int, rows: Int)` → xterm FitAddon resize callback → `channel.sendWindowChange`

### MINA SSHD Android Notes
- minifyEnabled = false (debug MVP) — evitar class-not-found
- Registrar ed25519: `client.signatureFactories = listOf(BuiltinSignatures.ed25519, ...)`
- Deps: `net.i2p.crypto:eddsa:0.3.0`, `org.bouncycastle:bcpkix-jdk18on:1.78.1`

## Development Commands

| Task | Command |
|------|---------|
| Build APK (Docker) | `./build-apk.sh` |
| Clean build | `docker build -t bastion-builder . && docker run --rm -v $(pwd):/src -v ~/android-build-env/android-sdk:/opt/android-sdk -v ~/dev-tools/jdk-17.0.19+10:/opt/jdk17 -v ~/apk-share:/out bastion-builder` |
| Debug APK location | `~/apk-share/bastion-debug.apk` |

## Critical Rules
- **No local shell** — prohibido Runtime.exec/sh. Solo canales SSH.
- **xterm + WebView** — assets locales, sin Internet en WebView.
- **Secretos cifrados** — siempre via Keystore → EncryptedSharedPreferences.
- **Spec-driven** — todo cambio debe actualizar `HIM-001.spec.md`.
- **Docker build** — no instalar JDK/SDK en el servidor base.
- **Version management** — ANTES de hacer build, SIEMPRE verificar versión actual en `app/build.gradle.kts` y incrementar `versionCode` + `versionName`. NUNCA asumir que el usuario no tiene la última versión. Cada build debe ser una versión nueva que el usuario no tenga.
- **GitHub releases** — Crear release en GitHub para cada versión publicada con changelog.

## Architecture
```
flowchart LR
  UI[Compose UI] --> VM[ViewModels] --> REPO[VaultRepository]
  REPO --> DB[(Room)]
  REPO --> KS[(Keystore/ESP)]
  UI --> SESS[SshSession]
  SESS --> SSH[MINA SSHD]
  SESS <--> BR[TerminalBridge]
  BR <--> WV[WebView + xterm.js]
```

## Iteration Plan
1. **Iter 1 (MVP):** Scaffold + Room/Secrets + SshSession(password) + xterm bridge + VaultScreen + TerminalScreen mono-pestaña → APK funcional
2. **Iter 2:** Multi-pestaña + auth publickey + resize + edit/delete hosts
3. **Iter 3:** Agent forwarding + known-hosts verification + dark theme + polish

## Stitch Redesign (v1.1.0)
- **Sidebar** 260dp fijo con navegación: Connections (dns), Terminal (devices), SSH Keys (key), Settings (settings)
- **Header** con título, sub-nav (Sessions/Clusters/History), search, notifications, add button
- **AppLayout.kt** reemplaza MainTabsScreen como entry point
- Nav.kt usa AppLayout como MAIN route
- Terminal tabs se manejan dentro de la sección TERMINAL con HorizontalPager
- VaultTabContent ahora sin cabecera interna (la provee AppHeader)
