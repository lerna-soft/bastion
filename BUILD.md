# Bastion APK — Build & Deploy

## Release Pipeline (RECOMENDADO)

```bash
./release.sh [patch|minor|major]
```

Esto automatiza todo: bump de versión → build APK → tag GitHub → release notes → update `latest.json` → restart server.

**Regla RHD-BST-001:** Cada nuevo APK debe ser una nueva versión. No builds sin bump.

## Build rápido (solo compilar, sin release)

## Requisitos

| Recurso | Ruta |
|---------|------|
| JDK 17 | `/home/lerna/dev-tools/jdk-17.0.19+10` |
| Android SDK | `/home/lerna/android-build-env/android-sdk` (platform android-36) |
| Docker | `bastion-builder` image (84MB) |

## Compilar APK

```bash
# Desde el directorio del proyecto:
./build-apk.sh
```

El script:
1. Construye la imagen Docker `bastion-builder` (Ubuntu 22.04 + curl + unzip)
2. Ejecuta el contenedor montando JDK, SDK y código fuente
3. Copia el APK firmado a `$HOME/apk-share/`
4. Genera nombre versionado (`bastion-v{version}-{timestamp}.apk`)
5. Crea `latest.json` para auto-update
6. Inicia `serve.py` (servidor HTTP) en puerto 8765

### Comando Docker manual

```bash
docker run --rm \
    -v /home/lerna/proyectos/bastion:/src \
    -v /home/lerna/dev-tools/jdk-17.0.19+10:/opt/jdk17 \
    -v /home/lerna/android-build-env/android-sdk:/opt/android-sdk \
    bastion-builder
```

## Servidor APK/Logs

`serve.py` corre en el host en **puerto 8765**:

| Ruta | Descripción |
|------|-------------|
| `GET /` | Index HTML con links a APKs |
| `GET /apk-share/bastion-debug.apk` | Último APK |
| `GET /apk-share/bastion-v*.apk` | APK específico por versión |
| `GET /update` | `latest.json` para auto-update en app |
| `POST /logs` | Recibir logs desde la app |
| `GET /logs` | Ver logs recibidos |

Iniciar manualmente:

```bash
fuser -k 8765/tcp 2>/dev/null || true
python3 /home/lerna/proyectos/bastion/serve.py
```

## Dockerfile

```dockerfile
FROM ubuntu:22.04
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl unzip && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/opt/jdk17
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
WORKDIR /src
CMD ["./gradlew", "assembleRelease"]
```

> Nota: se usa `assembleRelease` (no `assembleDebug`) para que el APK final tenga
> `debuggable=false` — un build `debug` siempre lleva ese flag sin importar la firma,
> y Android/Play Protect muestra avisos extra ("app para desarrolladores") al instalarlo
> fuera de Play Store. `release` está firmado con el mismo `bastion-release.keystore`.

## Output APK

```bash
# APK generado en:
/home/lerna/proyectos/bastion/app/build/outputs/apk/release/app-release.apk

# Copiado automáticamente a:
/home/lerna/apk-share/bastion-debug.apk
/home/lerna/apk-share/bastion-v{VERSION}-{TIMESTAMP}.apk
```

El APK se sirve desde `http://192.168.0.100:8765/apk-share/bastion-debug.apk`.
