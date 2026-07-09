plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

android {
    namespace = "com.bastion.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bastion.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 37
        versionName = "1.1.25"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // La password NUNCA va hardcodeada en el repo (fue rotada tras una exposición
            // previa en texto plano). Se lee de BASTION_KEYSTORE_PASSWORD — exportada en el
            // shell del servidor de build, nunca commiteada. Ver release.sh/build-apk.sh.
            val ksPassword = System.getenv("BASTION_KEYSTORE_PASSWORD")
                ?: error("BASTION_KEYSTORE_PASSWORD no está definida. Exportala antes de compilar release.")
            storeFile = file("${rootProject.projectDir}/bastion-release.keystore")
            storePassword = ksPassword
            keyAlias = "bastion"
            keyPassword = ksPassword
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Build de distribución: Android marca debuggable=true en cualquier build tipo `debug`
        // sin importar la firma, y eso dispara avisos extra ("app para desarrolladores") al
        // instalar fuera de Play Store. release{} es debuggable=false por defecto, firmado con
        // el mismo keystore. isMinifyEnabled=false a propósito: SSHD/BouncyCastle usan mucha
        // reflexión y no está probado el shrink con estas libs — mismo riesgo que debug.
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            pickFirsts += "/META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // HIM-016: lógica SSH movida a :core (Kotlin/JVM plano, compartido con :desktopApp).
    // sshd/bouncycastle/eddsa/coroutines-core llegan transitivos vía las deps `api` de :core.
    implementation(project(":core"))

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WebKit (WebView support)
    implementation("androidx.webkit:webkit:1.12.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Security - EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Core
    implementation("androidx.core:core-ktx:1.13.1")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("org.json:json:20240303")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
