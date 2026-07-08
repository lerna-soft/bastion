# Bastion Design System (from Google Stitch)

## Project
- **Name**: Bastion App Redesign
- **Project ID**: 2134170800474297456
- **Stitch URL**: https://stitch.withgoogle.com/project/2134170800474297456

## Theme
- **Color Mode**: DARK
- **Roundness**: ROUND_EIGHT (0.5rem)
- **Primary Font**: Inter
- **Headline Font**: Inter
- **Body Font**: Inter
- **Label Font**: JetBrains Mono

## Color Palette

### Surface Colors
| Token | Hex | Usage |
|-------|-----|-------|
| background | #121414 | Main app background |
| surface | #121414 | Base surface |
| surface_dim | #121414 | Dimmed surface |
| surface_bright | #37393a | Bright surface |
| surface_container_lowest | #0c0f0f | Lowest container |
| surface_container_low | #1a1c1c | Low container |
| surface_container | #1e2020 | Default container |
| surface_container_high | #282a2b | High container |
| surface_container_highest | #333535 | Highest container |
| surface_variant | #333535 | Variant surface |

### Text Colors
| Token | Hex | Usage |
|-------|-----|-------|
| on_background | #e2e2e2 | Text on background |
| on_surface | #e2e2e2 | Text on surface |
| on_surface_variant | #c4c7c7 | Secondary text |

### Primary Colors
| Token | Hex | Usage |
|-------|-----|-------|
| primary | #c8c6c5 | Primary elements |
| on_primary | #303030 | Text on primary |
| primary_container | #1e1e1e | Primary container |
| on_primary_container | #878585 | Text on primary container |

### Secondary Colors (Cyan Accent)
| Token | Hex | Usage |
|-------|-----|-------|
| secondary | #75d1ff | Accent color (cyan) |
| on_secondary | #003548 | Text on secondary |
| secondary_container | #009cce | Secondary container |
| on_secondary_container | #002e3f | Text on secondary container |

### Tertiary Colors
| Token | Hex | Usage |
|-------|-----|-------|
| tertiary | #c8c6c5 | Tertiary elements |
| on_tertiary | #303030 | Text on tertiary |
| tertiary_container | #1e1e1e | Tertiary container |

### Error Colors
| Token | Hex | Usage |
|-------|-----|-------|
| error | #ffb4ab | Error state |
| on_error | #690005 | Text on error |
| error_container | #93000a | Error container |
| on_error_container | #ffdad6 | Text on error container |

### Outline Colors
| Token | Hex | Usage |
|-------|-----|-------|
| outline | #8e9192 | Borders |
| outline_variant | #444748 | Subtle borders |

## Typography Scale

| Level | Font | Size | Weight | Line Height |
|-------|------|------|--------|-------------|
| display | Inter | 32px | 700 | 1.2 |
| headline-md | Inter | 24px | 600 | 1.3 |
| headline-sm | Inter | 20px | 600 | 1.4 |
| body-lg | Inter | 16px | 400 | 1.6 |
| body-sm | Inter | 14px | 400 | 1.5 |
| mono-lg | JetBrains Mono | 16px | 500 | 1.5 |
| mono-sm | JetBrains Mono | 13px | 400 | 1.4 |
| label-caps | Inter | 11px | 700 | 1 |

## Screens Generated

1. **App Icon** - Shield with "B" and terminal symbol
2. **Terminal Session** - Terminal emulator with tabs
3. **About Screen** - App info and features
4. **Edit Server** - Host configuration form

## Design Guidelines

### Elevation & Depth
- **Level 0 (Base):** #121414 - Main application background
- **Level 1 (Card/Sidebar):** #1E1E1E - Slightly raised
- **Level 2 (Modals/Popovers):** #2A2A2A with 1px subtle border

### Active State
When a terminal session is active, it receives a soft 40px blur glow of the Accent Cyan (5% opacity) to signify focus.

### Buttons
- **Primary:** Background Accent Cyan (#4FC3F7), Text #121414 (Bold)
- **Secondary:** Outline 1px (#FFFFFF at 10% opacity), Text #FFFFFF
- **Ghost:** No background, Text #94A3B8

### Inputs
- **Field:** Background #1E1E1E, Border 1px #2A2A2A
- **Focus State:** Border changes to Accent Cyan with 2px outer glow
