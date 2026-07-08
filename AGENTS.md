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

## Stitch Design System — "Terminal Core"

### Proyecto Stitch
- **Project ID:** `2946918035035581471`
- **Título:** "SSH Terminal Manager"
- **Device:** DESKTOP
- **API Key:** `AQ.Ab8RN6IAOY3R_brSs-qjuLZxfisqk-FefrRhzehu2jgaRhJLgg`
- **MCP endpoint:** `https://stitch.googleapis.com/mcp`
- **MCP Bridge local:** `.opencode/stitch-bridge.mjs`
- **Diseño completo:** `design/STITCH_DESIGN.md`
- **Screenshots:** `design/screenshots/*.png`
- **HTML referencias:** `design/html/*.html`
- **Metadata:** `design/screens/*.json`

### Pantallas en Stitch
| # | Pantalla | Screen ID | Resolución |
|---|----------|-----------|-----------|
| 1 | Terminal Activa | `8340cef99f634e888a55fbacd13905d5` | 2560×2048 |
| 2 | Dashboard de Conexiones | `85b987da657947e1a61351d8aec94894` | 2560×2048 |
| 3 | Terminax Terminal Dashboard | `2208a2a609314aba9f150b9b1925b71e` | 1280×1024 |
| 4 | Configuración del Sistema | `0e584c56060b442f85df03f29ee6d6e3` | 2560×2092 |
| 5 | Gestión de Llaves SSH | `073a0754ad594a868ff8e665654b18c8` | 2560×2048 |
| 6 | SSH Terminal Logo | `ef6d5367814d4d01a8ded48f1d3fda2c` | 200×200 |

### Colores clave
| Token | Hex | Uso |
|-------|-----|-----|
| `surface` / fondo | `#0c160a` | Fondo general (obsidiana oscuro) |
| `primary-container` | `#00ff41` | **Neon Green** — accent principal, botones, indicadores activos (glow) |
| `on-surface` | `#dae6d2` | Texto principal claro |
| `on-surface-variant` | `#b9ccb2` | Texto secundario / muted |
| `secondary-container` | `#4a8eff` | Electric Blue — botones secundarios, bordes focus |
| `outline` | `#84967e` | Bordes muted |
| `outline-variant` | `#3b4b37` | Bordes sutiles, separadores |
| `error` | `#ffb4ab` | Alert Red |
| `tertiary-container` | `#ffd5ae` | Warning/connecting states |

### Tipografía
| Style | Font | Size | Uso |
|-------|------|------|-----|
| `display-lg` | Inter 32px 700 | Logo, títulos grandes |
| `headline-md` | Inter 20px 600 | Títulos sección, command palette |
| `ui-body` | Inter 14px 400 | Navegación, texto UI |
| `ui-label-bold` | Inter 12px 600 | Labels, sidebar, botones, headers tabla, tags |
| `terminal-main` | JetBrains Mono 14px 400 | Output terminal, server names |
| `terminal-sm` | JetBrains Mono 12px 400 | Metadata, IPs, timestamps |

### Layout (Terminal Activa)
```
┌─────────────────────────────────────────────────────┐
│ SIDEBAR 260px     │ TOP NAV h:48 (Sessions│Clusters│
│ Logo + Terminax   │ [Search...]      [Connect]     │
│ [New Connection]  ├────────────────────────────────┤
│ ▶ Terminal (ON)   │ SESSION TABS (prod*│db-m│redis│+)
│   Connections     ├────────────────────────────────┤
│   SSH Keys        │                                │
│   Settings        │  TERMINAL VIEWPORT             │
│   Help / Logs     │  (flex-1, scroll)              │
│ [avatar] admin    │                        │ STATS │
└────────────────────┴───────────────────────────────┘
│ FOOTER h:32 — © Terminax | Docs | Privacy | Status │
│ ● Connected | UTF-8 | Ln 14, Col 12               │
└────────────────────────────────────────────────────┘
```

### Layout (Dashboard de Conexiones)
```
┌─────────────────────────────────────────────────────┐
│ SIDEBAR 260px     │ TOP NAV (Sessions│Clusters│Hist)│
│ Connections (ON)  │ [🔍 Search... ⌘K] [🔔][💬][Connect]
│ Terminal          ├────────────────────────────────┤
│ SSH Keys          │ Quick Connect ┌──────────────┐ │
│ Settings          │ ssh [user@host│...] -p 22    │ │
│                   │ [Launch Session]              │ │
│                   ├────────────────────────────────┤
│                   │ Saved Connections              │ │
│                   │ Status│Name│IP│Last│Tags│Actions│
│                   │ ● On │prod-01│192.168...│...│[Con]│
│                   │ ◐ Con│staging│10.0.0.42│...│[...]│
│                   │ ○ Off│dev-db│127.0.0.1│...│[Con]│
│                   │ Showing 4 of 28 | 2 active    │
└────────────────────────────────────────────────────┘
```

### Componentes a implementar en Compose
1. **Sidebar** — 260dp fijo, animación collapse, iconos Material + texto, active state con border-l-2
2. **TopNavBar** — h:48dp, tabs Sessions/Clusters/History, search input, notification icons, Connect button (secondary)
3. **SessionTabs** — HorizontalPager tabs con active indicator (border-t-2), server name, close, add button
4. **TerminalViewport** — WebView padding 1rem, fondo surface
5. **SystemStatsPanel** — w:288dp sidebar derecho, CPU/RAM/Disk progress bars con glow, network, logs
6. **Footer** — h:32dp, copyright, status indicator, links
7. **CommandPalette** — glassmorphism overlay, blur backdrop
8. **ConnectionTable** — tabla conexiones con status dot + pulse, server name, IP, tags, action button
9. **QuickConnectBar** — input ssh con prefix/suffix, Launch button primary con glow
10. **StatusIndicator** — w:8dp h:8dp rounded-full, estados: online (primary-container + glow pulse), connecting (tertiary-container), offline (outline)

### Estados de componentes
- **Primary button:** bg-primary-container, text-on-primary-fixed, hover:brightness-110, active:scale-95
- **Secondary button:** border secondary-container, text-secondary-fixed-dim, bg-secondary-container/20
- **Active sidebar item:** border-l-2 secondary-container, bg-surface-container-high, text-primary-fixed-dim
- **Hover sidebar item:** bg-surface-container-highest
- **Online indicator:** bg-primary-container + `shadow-[0_0_8px_#00ff41]` + pulse animation
- **Input focus:** border-secondary-container
- **Table row hover:** bg-surface-container-highest/30
- **Chips:** 10px bold uppercase, color-coded bg with border (Prod/Dev/Staging/AWS/Tools)

### Cómo consultar Stitch nuevamente
```bash
# Listar herramientas disponibles
curl -s -X POST "https://stitch.googleapis.com/mcp" \
  -H "Content-Type: application/json" \
  -H "X-Goog-Api-Key: AQ.Ab8RN6IAOY3R_brSs-qjuLZxfisqk-FefrRhzehu2jgaRhJLgg" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}'

# Obtener detalle de proyecto
curl -s -X POST "https://stitch.googleapis.com/mcp" \
  -H "Content-Type: application/json" \
  -H "X-Goog-Api-Key: AQ.Ab8RN6IAOY3R_brSs-qjuLZxfisqk-FefrRhzehu2jgaRhJLgg" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"get_project","arguments":{"name":"projects/2946918035035581471"}}}'

# Listar screens
curl -s -X POST "https://stitch.googleapis.com/mcp" \
  -H "Content-Type: application/json" \
  -H "X-Goog-Api-Key: AQ.Ab8RN6IAOY3R_brSs-qjuLZxfisqk-FefrRhzehu2jgaRhJLgg" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"list_screens","arguments":{"projectId":"2946918035035581471"}}}'

# Obtener screen individual
curl -s -X POST "https://stitch.googleapis.com/mcp" \
  -H "Content-Type: application/json" \
  -H "X-Goog-Api-Key: AQ.Ab8RN6IAOY3R_brSs-qjuLZxfisqk-FefrRhzehu2jgaRhJLgg" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"get_screen","arguments":{"projectId":"2946918035035581471","screenId":"<SCREEN_ID>"}}}'
```

## Stitch Redesign History (v1.1.0 — v1.1.6)
- **v1.1.0:** Sidebar 260dp + Header + AppLayout
- **v1.1.4:** DB Room v2, settings desde DB, SSH keys desde DB
- **v1.1.6:** ServerInfoPanel, sidebar colapsable, URLs funcionales, TerminalTabBar rediseñada
- **v1.1.7 (pendiente):** Reescritura TerminalTab + TerminalTabBar basada en diseño Stitch real
