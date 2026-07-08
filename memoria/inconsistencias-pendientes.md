---
name: inconsistencias-pendientes
description: Contradicciones detectadas entre docs de bastion y pendientes reales de HIM-006
metadata:
  type: project
---

Detectadas en revisión de 2026-07-08 (ver [[reglas-rhd-bst]], [[arquitectura-decisiones]]):

1. **Dos design systems contradictorios**: `DESIGN_SYSTEM.md` (viejo, tema cian `#75d1ff`, Stitch project `2134170800474297456`) vs `AGENTS.md`/`HIM-004`/`design/STITCH_DESIGN.md` (tema "Terminal Core" verde neón `#00ff41`, Stitch project `2946918035035581471`). Ninguno refleja el estado final: HIM-005 abandonó el verde neón como default ("al usuario no le gusta"), pasó a "Neutral Dark" con 6 temas seleccionables. **Ningún doc de diseño está actualizado al estado real.**
2. Regla "todo cambio debe actualizar HIM-001.spec.md" no se sigue — cada iteración crea spec nuevo (HIM-002..006) y HIM-001 quedó con su plan de iteraciones Iter1/2/3 obsoleto.
3. RHD-BST-001 y RHD-BST-006 son duplicados casi idénticos (error de numeración al agregar reglas en HIM-002 sin revisar las ya existentes).
4. `build-apk.sh` NO bumpea versión — contradice la regla "cada build = nueva versión" si se usa directo en vez de `release.sh` (ver [[flujo-release-build]]).
5. Comando Docker manual en `AGENTS.md` monta `-v ~/apk-share:/out`, pero ni el Dockerfile ni los scripts reales usan `/out` — está desactualizado, no usar tal cual.
6. `PROYECTO.md` decía "MVP en construcción (Iter 1)" con hitos sin marcar, pero el código real está en v1.1.9 con features muy posteriores — **corregido en esta sesión** (ver estado real en [[estado-actual-roadmap]]).
7. ~~Specs HIM-002 y HIM-005 decían `estado: borrador` pero ya fueron liberadas~~ — **corregido en esta sesión** (2026-07-08): ambos specs ahora dicen `estado: implementado — liberado en vX.X.X`. Solo HIM-006 sigue como WIP real (no desplegado en dispositivo).
8. No está confirmado si `TerminalSession.theme` ya persiste en Room (columna propuesta en HIM-006) o sigue solo en memoria como data class en `AppLayout`.
9. Known-hosts verification: HIM-001 la difiere a "post-MVP", pero ROADMAP.md actual no la menciona ni como pendiente ni como hecha — cabo suelto sin seguimiento.
10. `bastion-release.keystore` está en la raíz del repo sin documentar contraseña/alias en ningún .md — dato sensible fuera de control documental (no se inspeccionó su contenido).

**Why:** estas inconsistencias generan riesgo de que un agente futuro actúe sobre specs "borrador" que en realidad ya están en producción, o aplique un flujo de build que no versiona correctamente.

**How to apply:** antes de tomar el "estado" de un spec HIM-00N como verdad, contrastar contra `app/build.gradle.kts` y `git log`. Antes de decidir el tema visual activo, verificar en código (no en `DESIGN_SYSTEM.md` ni `STITCH_DESIGN.md`, ambos desactualizados).
