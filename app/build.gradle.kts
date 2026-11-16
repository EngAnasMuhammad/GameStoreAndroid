import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.0.21"
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val googleWebClientId: String = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
    ?: System.getenv("GOOGLE_WEB_CLIENT_ID")
    ?: ""

val baseUrl: String = localProperties.getProperty("BASE_URL")
    ?: System.getenv("BASE_URL")
    ?: "http://10.0.2.2:8080"

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

val keystoreFilePath: String? = System.getenv("KEYSTORE_FILE")
    ?: keystoreProperties.getProperty("KEYSTORE_FILE")
val keystorePasswordVal: String? = System.getenv("KEYSTORE_PASSWORD")
    ?: keystoreProperties.getProperty("KEYSTORE_PASSWORD")
val keyAliasVal: String? = System.getenv("KEY_ALIAS")
    ?: keystoreProperties.getProperty("KEY_ALIAS")
val keyPasswordVal: String? = System.getenv("KEY_PASSWORD")
    ?: keystoreProperties.getProperty("KEY_PASSWORD")

val keystoreFileObj = keystoreFilePath?.let { path ->
    val direct = file(path)
    if (direct.exists()) direct else rootProject.file(path).takeIf { it.exists() }
}

val hasReleaseSigning = keystoreFileObj != null &&
    !keystorePasswordVal.isNullOrBlank() &&
    !keyAliasVal.isNullOrBlank() &&
    !keyPasswordVal.isNullOrBlank()

android {
    namespace = "com.anasdev.gamestoreandroid"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.anasdev.gamestoreandroid"
        minSdk = 26
        targetSdk = 37
        versionCode = 6
        versionName = "1.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = keystoreFileObj
                storePassword = keystorePasswordVal
                keyAlias = keyAliasVal
                keyPassword = keyPasswordVal
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

// Ktor Client Core + OkHttp Engine for Android
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

// JSON Serialization (The exact twin of your server plugin!)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

// Logging (So you can see every JSON request/response in Android Studio Logcat)
    implementation(libs.ktor.client.logging)

// ViewModel Compose & DataStore (For saving the JWT token on the phone)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)

    // Material Icons Extended & Coil
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)

    // Android Credential Manager & Google Identity
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // AndroidX Biometric
    implementation(libs.androidx.biometric)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
