---
name: him-019-jump-hosts-proxyjump
description: HIM-019 — Jump hosts / ProxyJump (conexión A→B→C encadenada por túneles SSH) en bastion
metadata:
  type: project
---

# HIM-019 — Jump hosts (ProxyJump / bastión encadenado)

Feature: conectar a un servidor destino tunelizando a través de uno o más saltos SSH
(dispositivo → A → B → C). Muy a tono con el nombre de la app.

## Modelo de datos (elegido por el usuario: referenciar hosts del vault)
- `Host.jumpHostId: Long?` (nullable) — id de OTRO host del vault que actúa como salto.
  null = conexión directa. La cadena se arma sola siguiendo `jumpHostId` de cada salto.
- Migración Room **3→4**: `ALTER TABLE hosts ADD COLUMN jumpHostId INTEGER` (`AppDatabase.MIGRATION_3_4`,
  version = 4). `saveHost()` ya lo persiste porque guarda el `Host` completo.
- `VaultRepository.resolveConnectionChain(targetId)`: sigue `jumpHostId` con secretos cargados,
  devuelve `[primerSalto … destino]` (destino último); protegido contra ciclos (set `visited`).

## Capa SSH (core) — el cómo técnico
`SshSession.connect(cfg, jumps: List<AuthConfig> = emptyList())`. Con saltos:
1. conecta+autentica el salto 1 (dirección real);
2. abre un **local port-forwarding** (`createLocalPortForwardingTracker(127.0.0.1:0, siguienteHop)`)
   sobre esa `ClientSession` — canal `direct-tcpip` cifrado;
3. conecta el siguiente salto al puerto local reenviado (`tracker.boundAddress`), y así en cadena;
4. el destino final se conecta al puerto reenviado por el último salto → ahí se abre el shell.

API MINA SSHD 2.18 verificada contra el jar (javap):
`ClientSession.createLocalPortForwardingTracker(SshdSocketAddress local, SshdSocketAddress remote)`
→ `ExplicitPortForwardingTracker.getBoundAddress()` (host/port). `SshdSocketAddress(String,int)`.
Imports: `org.apache.sshd.client.session.forward.ExplicitPortForwardingTracker`,
`org.apache.sshd.common.util.net.SshdSocketAddress`.

Sesiones de salto y trackers se guardan en `jumpSessions`/`forwardTrackers` y se cierran en
orden inverso (del más cercano al destino hacia el primer salto) en `cleanup()`. El heartbeat de
30s (`SshClientManager`) aplica a todas las sesiones → mantiene vivos los túneles intermedios.

## Lógica pura testeable
`com.bastion.app.data.JumpHostChain` (objeto sin Android/IO):
- `resolveChainIds(all, targetId)` — orden [primerSalto…destino], corta en ciclos/ids colgantes.
- `candidates(all, selfId)` — hosts válidos como salto (excluye self y ciclos A→B→A).
Lo usan tanto el selector (UI) como `VaultRepository.resolveConnectionChain` (que además carga
secretos con `HostDao.getAllHostsList()`), así no divergen. Tests en
`platforms/android/src/test/.../JumpHostChainTest.kt` (9 casos: directo, cadena lineal, id
colgante, ciclo sin colgar, target inexistente, candidatos self/ciclo/independiente).

## UI (HostEditScreen)
Selector "JUMP HOST (OPTIONAL)" (dropdown, icono `AltRoute`) antes del botón Connect:
"Direct connection (no jump)" + hosts del vault (vía `JumpHostChain.candidates`).
`AppLayout.openTerminalSession` resuelve la cadena y la pasa a `startConnection(session, chain)`,
que construye un `AuthConfig` por host (cargando su llave si aplica) y llama
`session.connect(target, jumps)`.

## Estado
Implementado, **compila** y **tests en verde** (39 tests, 0 fallos; los 9 de JumpHostChain
incluidos). **APK v1.1.35 (versionCode 47)** armado y firmado vía `build-apk.sh` en
`~/apk-share/bastion-debug.apk` (2026-07-09). **Pendiente:** (1) release a GitHub vía `release.sh`
(push + `gh release create` — acción hacia afuera, requiere confirmación del usuario; el
auto-update del device consulta GitHub API desde HIM-018) — mientras tanto se puede sideload el
APK local; (2) prueba en dispositivo real con una cadena real de 2+ saltos.
`StatsCollector` conecta directo (sin saltos) — aceptable para MVP.

Nota: la versión real en código venía en v1.1.34 (code 46) — la ficha `estado-actual-roadmap.md`
decía v1.1.28, estaba atrás. Con este build queda en v1.1.35.

Relacionado: [[arquitectura-decisiones]] (ADR-003 MINA SSHD), [[estado-actual-roadmap]] (port
forwarding estaba en "planeado"; esto cubre el caso jump host).
