plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import java.util.Properties

// Signing: release/keystore.properties (never committed; the build writes it from secrets)
val keystoreProperties = Properties().apply {
    rootProject.file("release/keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun signing(key: String) = keystoreProperties.getProperty(key)?.takeIf { it.isNotBlank() }

android {
    namespace = "io.github.fluxsniffermods.gaming"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.fluxsniffermods.fluxgaming"
        minSdk = 29
        targetSdk = 36
        versionCode = providers.gradleProperty("FLUX_VERSION_CODE").orNull?.toIntOrNull() ?: 1
        versionName = providers.gradleProperty("FLUX_VERSION_NAME").orNull ?: "1.0.0-flux.local"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // SteamGridDB's key for game art: from the build's secret, never the source (empty without it)
        val steamGridDbKey = (providers.environmentVariable("STEAMGRIDDB_API_KEY").orNull ?: "").trim()
            .takeIf { it.matches(Regex("[A-Za-z0-9]+")) } ?: ""
        buildConfigField("String", "STEAMGRIDDB_API_KEY", "\"$steamGridDbKey\"")
    }

    signingConfigs {
        create("release") {
            val store = signing("storeFile")
            if (store != null) {
                storeFile = rootProject.file("release/$store")
                storePassword = signing("storePassword")
                keyAlias = signing("keyAlias")
                keyPassword = signing("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signing("storeFile") != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core"))
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
