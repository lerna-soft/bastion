plugins {
    kotlin("jvm") version "2.0.21"
}

// HIM-016 (ADR-D1): módulo Kotlin/JVM plano, no Kotlin Multiplatform formal todavía.
// Android (:app) y Desktop (:desktopApp) son ambos JVM real — no hace falta expect/actual
// hasta que exista un target no-JVM (iOS, bloqueado por Apache MINA SSHD, ver ADR-D4).
kotlin {
    jvmToolchain(17)
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Apache MINA SSHD — JVM puro, funciona idéntico en Android y Desktop.
    api("org.apache.sshd:sshd-common:2.18.0")
    api("org.apache.sshd:sshd-core:2.18.0")

    // BouncyCastle + eddsa para soporte de llaves modernas.
    api("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    api("net.i2p.crypto:eddsa:0.3.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
