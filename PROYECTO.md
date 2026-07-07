# Proyecto: bastion
> Cliente SSH multi-pestaña tipo Termius para Android (vault + terminal en pestañas, sin shell local)

## Datos clave
- **Código:** `/home/lerna/proyectos/bastion` (repo git independiente)
- **Stack:** Kotlin + Jetpack Compose + Material 3, Apache MINA SSHD 2.18.0, xterm.js 6.0.0 en WebView, Room + Android Keystore
- **Paquete:** com.bastion.app
- **Estado:** MVP en construcción (Iter 1)
- **Build:** Docker aislado con JDK17 + Android SDK (sin instalar en servidor base)
- **APK:** `~/apk-share/bastion-debug.apk`

## Cómo cargar este proyecto
- Spec: `HIM-001.spec.md`
- Contexto: `AGENTS.md`
- Memoria: `memoria/*.md`
- Tareas: `tareas/` (si aplica)
- Logs: `logs/` (si aplica)

## Hitos
- [ ] Iter 1: vault + conexión SSH mono-pestaña (password)
- [ ] Iter 2: multi-pestaña + auth publickey + resize
- [ ] Iter 3: agent forwarding + known-hosts + polish
- [ ] APK funcional en `~/apk-share/`
