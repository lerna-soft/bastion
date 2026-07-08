# Terminax — Stitch Design System (SSH Terminal Manager)

> **Proyecto Stitch:** `2946918035035581471` — "SSH Terminal Manager"
> **Fecha extracción:** 2026-07-08
> **Fuente:** Google Stitch MCP (`stitch.googleapis.com/mcp`)
> **API Key:** ver `~/.bastion-secrets.env` (`STITCH_API_KEY`) — NUNCA hardcodear aquí, ver `AGENTS.md` sección "Secretos"

---

## Pantallas del diseño

| # | Pantalla | Screen ID | Resolución | Screenshot | HTML |
|---|----------|-----------|-----------|------------|------|
| 1 | **Terminal Activa** | `8340cef99f634e888a55fbacd13905d5` | 2560×2048 | ✅ | ✅ |
| 2 | **Dashboard de Conexiones** | `85b987da657947e1a61351d8aec94894` | 2560×2048 | ✅ | ✅ |
| 3 | **Terminax Terminal Dashboard** | `2208a2a609314aba9f150b9b1925b71e` | 1280×1024 | ❌ (solo HTML) | ✅ |
| 4 | **Configuración del Sistema** | `0e584c56060b442f85df03f29ee6d6e3` | 2560×2092 | ✅ | ✅ |
| 5 | **Gestión de Llaves SSH** | `073a0754ad594a868ff8e665654b18c8` | 2560×2048 | ✅ | ✅ |
| 6 | **SSH Terminal Logo** | `ef6d5367814d4d01a8ded48f1d3fda2c` | 200×200 | ✅ | SVG |

Archivos guardados en:
- `design/screenshots/` — PNG (añadir `=w780` para alta resolución)
- `design/html/` — HTML con Tailwind CSS
- `design/screens/` — JSON metadata de cada screen

---

## Design System — "Terminal Core"

### Modo: DARK (fidelidad)

### Colores

| Token | Hex | Uso |
|-------|-----|-----|
| `surface` / `surface-dim` | `#0c160a` | Fondo principal negro obsidiana |
| `surface-bright` | `#313c2e` | Superficies elevadas |
| `surface-container-lowest` | `#071106` | Footer, barra inferior |
| `surface-container-low` | `#141e12` | Inputs, tablas |
| `surface-container` | `#182216` | Sidebar, headers de sección |
| `surface-container-high` | `#222d20` | Elementos activos hover |
| `surface-container-highest` | `#2d382a` | Botones secundarios, scrollbar |
| `surface-variant` | `#2d382a` | Variante de superficie |
| `on-surface` | `#dae6d2` | Texto principal claro |
| `on-surface-variant` | `#b9ccb2` | Texto secundario / muted |
| `inverse-surface` | `#dae6d2` | Superficie inversa |
| `inverse-on-surface` | `#283326` | Texto sobre inversa |
| `outline` | `#84967e` | Bordes, íconos muted |
| `outline-variant` | `#3b4b37` | Bordes sutiles, separadores |
| `surface-tint` | `#00e639` | Tono de superficie |
| **`primary`** | **`#ebffe2`** | Texto en botones primarios |
| `on-primary` | `#003907` | Texto sobre primary |
| **`primary-container`** | **`#00ff41`** | **Neon Green** — accent principal, botones primarios, indicadores activos |
| `on-primary-container` | `#007117` | Texto sobre primary-container |
| `inverse-primary` | `#006e16` | Primary en modo inverso |
| `primary-fixed` | `#72ff70` | Active states, highlights |
| `primary-fixed-dim` | `#00e639` | Texto activo, labels importantes |
| `on-primary-fixed` | `#002203` | Texto sobre primary-fixed |
| `on-primary-fixed-variant` | `#00530e` | Variante |
| **`secondary`** | **`#adc7ff`** | Electric Blue — links, interactive elements |
| `on-secondary` | `#002e68` | Texto sobre secondary |
| **`secondary-container`** | **`#4a8eff`** | Botones secundarios, bordered |
| `on-secondary-container` | `#00285b` | Texto sobre secondary-container |
| `secondary-fixed` | `#d8e2ff` | Secondary highlight |
| `secondary-fixed-dim` | `#adc7ff` | Secondary muted |
| `on-secondary-fixed` | `#001a41` | Texto |
| `on-secondary-fixed-variant` | `#004493` | Texto |
| `tertiary` | `#fff8f4` | Blanco cálido |
| `on-tertiary` | `#442b10` | Texto sobre tertiary |
| `tertiary-container` | `#ffd5ae` | Warning/connecting states |
| `on-tertiary-container` | `#7a5b3c` | Texto sobre warning |
| **`error`** | **`#ffb4ab`** | Alert Red — errores, acciones destructivas |
| `on-error` | `#690005` | Texto sobre error |
| `error-container` | `#93000a` | Contenedor de error |
| `on-error-container` | `#ffdad6` | Texto sobre error container |
| `background` | `#0c160a` | Fondo general |
| `on-background` | `#dae6d2` | Texto sobre fondo |

### Tipografía (dual-font)

| Style | Font | Size | Weight | Line H | Letter Spacing | Uso |
|-------|------|------|--------|--------|---------------|-----|
| `display-lg` | Inter | 32px | 700 | 1.2 | -0.02em | Títulos grandes, logo |
| `headline-md` | Inter | 20px | 600 | 1.4 | — | Títulos de sección, command palette |
| `ui-body` | Inter | 14px | 400 | 1.5 | — | Texto de UI, navegación |
| `ui-label-bold` | Inter | 12px | 600 | 1.2 | — | Labels, sidebar items, headers tabla, botones |
| `terminal-main` | JetBrains Mono | 14px | 400 | 1.6 | — | Output de terminal, nombres de servidor |
| `terminal-sm` | JetBrains Mono | 12px | 400 | 1.4 | — | Metadata, IPs, timestamps |

### Espaciado

| Token | Valor | Uso |
|-------|-------|-----|
| `unit` | 4px | Baseline grid |
| `sidebar-width` | 260px | Ancho sidebar fijo |
| `terminal-padding` | 1rem | Padding interno de terminal |
| `gutter` | 12px | Gap entre elementos inline |
| `margin-sm` | 8px | Padding pequeño |
| `margin-md` | 16px | Padding estándar |
| `margin-lg` | 24px | Padding grande |

### Border Radius

| Token | Valor | Uso |
|-------|-------|-----|
| `DEFAULT` | 0.125rem (2px) | Base |
| `lg` | 0.25rem (4px) | Cards, paneles, botones |
| `xl` | 0.5rem (8px) | Modales, command palette |
| `full` | 0.75rem (12px) | Badges, pills |

---

## Layout Structure (Terminal Activa)

```
┌──────────────────────────────────────────────────────────┐
│ SIDEBAR (260px)                │  TOP NAV BAR (h:48px)   │
│ ┌────────────────┐             │  Sessions │ Clusters │   │
│ │ Logo + Terminax │             │  [Search...]    [Connect] │
│ │ v1.2.4-stable   │             ├──────────────────────────┤
│ ├────────────────┤             │ SESSION TABS BAR        │
│ │ [New Connection]│             │ ┌──────┬──────┬──────┐+ │
│ ├────────────────┤             │ │prod* │db-m  │redis │  │
│ │ Active Clusters│             ├──────────────────────────┤
│ │ ▶ Terminal (ON)│             │                          │
│ │   Connections   │             │    TERMINAL VIEWPORT     │
│ │   SSH Keys      │             │    (flex-1, scroll)      │
│ │   Settings      │             │                          │
│ ├────────────────┤             │                          │
│ │ Help │ Logs     │             ├──────────────────────────┤
│ │ [avatar] admin  │             │ SYSTEM STATS (w:288px)  │
│ └────────────────┘             │ CPU: 12.4% ████         │
│                                │ RAM: 4.2/16 GB ██████   │
│                                │ Disk: 412/512 GB ████████│
│                                │ Network: ↓1.2M ↑45K     │
│                                │ Connection Logs          │
├────────────────────────────────┴──────────────────────────┤
│ FOOTER (h:32px) — © Terminax | Docs | Privacy | Status   │
│              ● Connected | UTF-8 | Ln 14, Col 12          │
└──────────────────────────────────────────────────────────┘
```

### Componentes Identificados

#### 1. Sidebar (260px fijo)
- **Logo + Brand:** Imagen 32×32 + "Terminax" (headline-md, primary-fixed-dim) + subtítulo "v1.2.4-stable"
- **[New Connection]:** Botón primary-container full-width con icono `add`
- **Active Clusters:** Label en outline `text-[10px] uppercase tracking-widest`
- **Nav items:** Icon (18px) + label (ui-body), hover con bg-surface-container-highest
  - Active: border-l-2 secondary-container + bg-surface-container-high + text-primary-fixed-dim
  - Inactive: text-on-surface-variant
  - Indicador: w-2 h-2 rounded-full + shadow para active
- **Footer nav:** Help, Logs, avatar + username

#### 2. Top Nav Bar (h:48px)
- **Tabs:** Sessions (active, border-b-2 primary-fixed-dim), Clusters, History
- **Search:** Input con icono search, bg-surface-container-lowest, border outline-variant
- **Actions:** Notifications bell, comment icon, **[Connect]** button secondary-container

#### 3. Session Tabs Bar
- **Active tab:** bg-surface-container-high, border-t-2 primary-container, text-primary-fixed-dim
- **Inactive tabs:** text-on-surface-variant, hover:bg-surface-container
- **Tab content:** Icon terminal (16px) + server name (ui-label-bold) + close button
- **Add tab:** `+` icon, text-on-surface-variant, hover:text-on-surface

#### 4. Terminal Viewport
- Padding: terminal-padding (1rem)
- Font: terminal-main (JetBrains Mono 14px)
- Color: text-primary-fixed-dim (verde tenue)
- Background: con radial-gradient sutil

#### 5. System Stats Panel (w:288px)
- **Header:** "System Stats" (headline-md) + server name (10px outline uppercase)
- **Metrics:** CPU, RAM, Disk → barra de progreso (h:8px) en bg-surface-container-highest
  - Fill: primary-container con glow shadow
- **Network:** Downlink (↓ primary-container), Uplink (↑ on-secondary-container)
- **Connection Logs:** Timestamps (outline) + eventos (on-surface-variant), font-terminal-sm

#### 6. Footer (h:32px)
- bg-surface-container-lowest, border-t outline-variant
- Left: © + connected info
- Right: links docs/privacy/status + status indicator ● + UTF-8 + cursor position

#### 7. Command Palette (Overlay glassmorphism)
- Backdrop: black/40 + blur-sm
- Panel: glass-panel (rgba(22, 27, 34, 0.85) + blur(12px)), border outline, rounded-xl
- Input: headline-md, icon terminal (primary-fixed-dim)
- Recent commands: hover:bg-surface-container-highest
- Shortcuts: ⌘1, ⌘,, ⌘G

---

## Component States

### Buttons
| Tipo | Estilo |
|------|--------|
| **Primary** | bg-primary-container + text-on-primary-fixed → hover:brightness-110 → active:scale-95 |
| **Secondary** | bg-secondary-container/20 + border secondary-container + text-secondary-fixed-dim → hover:bg-secondary-container/30 |
| **Ghost** | text-on-surface-variant + border outline-variant → hover:text-on-surface + hover:bg-surface-container-highest |
| **Destructive** | bg-error + text-on-error (no presente en diseño, pero definido en DESIGN.md) |

### Status Indicators (w-8 h-8 rounded-full)
| Estado | Color | Clase |
|--------|-------|-------|
| Online / Active | `#00ff41` (primary-container) | `bg-primary-container` + `shadow-[0_0_8px_#00ff41]` + `status-pulse` |
| Connecting | `#ffd5ae` (tertiary-container) | `bg-tertiary-container` + `status-pulse` |
| Offline | `#84967e` (outline) | `bg-outline` |
| Error | `#ffb4ab` (error) | `bg-error` |

### Input Fields
- bg-surface-container-low, border outline-variant, rounded-sm
- Focus: border-secondary-container
- Placeholder: opacity-30

### Tables
- Header: bg-surface-container, text-ui-label-bold uppercase, text-on-surface-variant
- Row hover: bg-surface-container-highest/30
- Border: outline-variant/30

### Chips/Tags
- Text: 10px bold uppercase
- Example: `Prod` → bg-primary-container/10 + text-primary-fixed-dim + border-primary-container/20
- Example: `Dev` → bg-secondary-container/10 + text-secondary-fixed-dim + border-secondary-container/20
- Example: `Staging` → bg-tertiary-container/10 + text-tertiary-fixed-dim + border-tertiary-container/20

---

## Archivos de Referencia

```
design/
├── STITCH_DESIGN.md          ← Este archivo
├── design-system-complete.json ← Datos completos del design system
├── screenshots/              ← PNG del diseño visual
│   ├── Terminal_Activa.png
│   ├── Dashboard_de_Conexiones.png
│   ├── Gestion_de_Llaves_SSH.png
│   ├── Configuracion_del_Sistema.png
│   ├── SSH_Terminal_Logo.png
│   └── SSH_Terminal_Logo.svg
├── html/                     ← HTML + Tailwind del diseño
│   ├── Terminal_Activa.html          ← Pantalla principal de terminal
│   ├── Dashboard_de_Conexiones.html  ← Lista de conexiones
│   ├── Terminax_Terminal_Dashboard.html ← Dashboard (mismo que conexiones)
│   ├── Configuracion_del_Sistema.html  ← Settings
│   └── Gestion_de_Llaves_SSH.html     ← SSH Keys
└── screens/                  ← JSON metadata de cada screen
    ├── Terminal_Activa.json
    ├── Dashboard_de_Conexiones.json
    ├── Terminax_Terminal_Dashboard.json
    ├── Configuracion_del_Sistema.json
    ├── Gestion_de_Llaves_SSH.json
    └── SSH_Terminal_Logo.json
```
