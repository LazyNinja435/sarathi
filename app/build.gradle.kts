import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val sarathiVersion = Properties().apply {
    rootProject.file("sarathi-version.properties").inputStream().use { load(it) }
}
val sarathiVersionCode = sarathiVersion.getProperty("SARATHI_VERSION_CODE").toInt()
val sarathiVersionName = sarathiVersion.getProperty("SARATHI_VERSION_NAME")

val releaseKeystorePath = System.getenv("SARATHI_KEYSTORE_PATH").orEmpty()
val releaseStorePassword = System.getenv("SARATHI_KEYSTORE_PASSWORD").orEmpty()
val releaseKeyAlias = System.getenv("SARATHI_KEY_ALIAS").orEmpty()
val releaseKeyPassword = System.getenv("SARATHI_KEY_PASSWORD").orEmpty()
val hasReleaseSigning = listOf(releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it.isNotBlank() } &&
    releaseKeystorePath.isNotBlank() &&
    File(releaseKeystorePath).isFile

android {
    namespace = "com.sarathi.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sarathi.app"
        minSdk = 26
        targetSdk = 35
        versionCode = sarathiVersionCode
        versionName = sarathiVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.mediapipe.tasks.genai)
    implementation(libs.litertlm.android)
    implementation(libs.guava)
    implementation(libs.google.material)
    implementation(libs.androidx.security.crypto)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
