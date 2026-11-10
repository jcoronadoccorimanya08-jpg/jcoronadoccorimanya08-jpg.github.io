plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.jcoronadoccorimanya08.rachas"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.jcoronadoccorimanya08.rachas"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "3.1"
    }

    signingConfigs {
        create("release") {
            // La llave de firma NO va en el repositorio: se pasa por variables de entorno al compilar.
            storeFile = System.getenv("RACHAS_KEYSTORE")?.let { file(it) }
            storePassword = System.getenv("RACHAS_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("RACHAS_KEY_ALIAS") ?: "rachas"
            keyPassword = System.getenv("RACHAS_KEY_PASSWORD") ?: System.getenv("RACHAS_KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Pruebas con Robolectric: dibujan los widgets con datos de ejemplo (sin teléfono) y guardan las imágenes.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation("androidx.browser:browser:1.8.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
}
