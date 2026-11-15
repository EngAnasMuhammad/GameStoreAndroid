# 🎮 GameTrade Android

[![Android CI](https://github.com/EngAnasMuhammad/GameStoreAndroid/actions/workflows/ci.yml/badge.svg)](https://github.com/EngAnasMuhammad/GameStoreAndroid/actions/workflows/ci.yml)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Material 3](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![Ktor Client](https://img.shields.io/badge/Ktor%20Client-3.0.1-orange.svg?logo=ktor)](https://ktor.io)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A modern, reactive digital game storefront and key distribution Android application built with **100% Jetpack Compose (Material 3)** and **Kotlin**. Powered by **Ktor Client**, **Android Credential Manager**, and **DataStore Preferences**, GameTrade provides an end-to-end shopping experience featuring atomic cart key reservations, instant checkout, order history with one-tap key copying, and an administrative dashboard for catalog and key inventory management.

---

## 📱 Screenshots

|                 Storefront Grid                  |               Game Details Sheet               |          Cart & Key Reservation          |                 Orders & Keys                 |
|:------------------------------------------------:|:----------------------------------------------:|:----------------------------------------:|:---------------------------------------------:|
| ![Storefront](docs/screenshots/store_screen.png) | ![Details](docs/screenshots/details_sheet.png) | ![Cart](docs/screenshots/cart_sheet.png) | ![Orders](docs/screenshots/orders_screen.png) |

---

## ✨ Features

- **Adaptive 2-Column Storefront**:
  - Poster-style game cards with translucent lower panels and vignette gradients.
  - Asynchronous cover art loading with Coil: 350ms crossfade transitions, non-blocking shimmer pulse placeholders, and graceful error fallbacks.
  - Real-time stock tracking with bold "Sold Out" badges and dimmed states.

- **15-Minute Atomic Cart Key Locking**:
  - Adds games to an authenticated cart with temporary server-side reservation locks, eliminating checkout race conditions on limited inventory.
  - Countdown and reservation indicators displayed directly inside the Cart sheet.

- **Seamless Checkout & Key Reveal**:
  - One-tap purchase confirmation.
  - Interactive receipt dialog revealing unmasked activation keys with instant clipboard copying.

- **Order History & Archive**:
  - Complete list of previous purchases with formatted local timestamps and total amounts.
  - Persistent access to purchased game serial keys with copy feedback.

- **Next-Gen Authentication**:
  - One-tap Google Sign-In powered by **Android Credential Manager** & **Google ID** (`androidx.credentials`).
  - Traditional Email & Password authentication dialog with real-time field validation.
  - Secure JWT session and role management persisted in Jetpack DataStore.

- **Admin Management Suite**:
  - Role-gated dashboard for administrators (`ADMIN` role).
  - **Add New Game**: Client-side image compression (`ImageCompressor`) using bitmap downsampling (`inSampleSize`), EXIF rotation correction, and deferred uploading to prevent orphaned files on the server.
  - **Batch Key Upload**: Multi-line serial key upload to replenish game stock instantly.

---

## 🏗️ Architecture & Tech Stack

The application follows **Clean Architecture** and **Unidirectional Data Flow (UDF)** principles:

```
UI Layer (Compose Material 3) ──► ViewModel (StateFlow / UDF) ──► Repository / API (Ktor Client)
                                                                 │
                                                                 └──► Local Storage (DataStore Preferences)
```

- **UI & Design**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with [Material 3](https://m3.material.io/) design system, dynamic color, edge-to-edge support, and responsive bottom sheets.
- **Networking**: [Ktor Client 3.x](https://ktor.io/) configured with:
  - OkHttp Engine
  - `ContentNegotiation` with `kotlinx.serialization`
  - `HttpTimeout` (15s request/socket, 10s connect) to guard against connection leaks
  - Safe conditional logging (`LogLevel.BODY` in debug, `LogLevel.NONE` in release)
- **State Management**: `StateFlow` and `asStateFlow()` exposing immutable UI state to composables.
- **Local Persistence**: [Jetpack DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) for persistent, non-blocking JWT and role storage.
- **Image Pipeline**: [Coil Compose](https://coil-kt.github.io/coil/) with target-size bitmap downsampling and hardware bitmap rendering.
- **Security & DevSecOps**:
  - `android:allowBackup="false"` to prevent token extraction via ADB or cloud transfers.
  - `network_security_config.xml` enforcing HTTPS in production while whitelisting local development endpoints.
  - Automated R8 code shrinking and resource optimization with tailored keep rules.

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1) or Meerkat (2024.3.1)+
- **JDK**: Version 17
- **Android SDK**: Compile SDK 35+, Minimum SDK 26 (Android 8.0 Oreo)
- **Companion Backend**: A running instance of the Game Trade Ktor backend.

### 1. Clone the Repository
```bash
git clone https://github.com/EngAnasMuhammad/GameStoreAndroid.git
cd GameStoreAndroid
```

### 2. Configure `local.properties`
Create or update `local.properties` in the project root:

```properties
## SDK Location
sdk.dir=C\:\\Users\\username\\AppData\\Local\\Android\\Sdk

## Local Backend Endpoint
# For Android Studio Emulator:
BASE_URL=http://10.0.2.2:8080

# For Physical Device over Wi-Fi / LAN:
# BASE_URL=http://192.168.1.100:8080

## Google OAuth Web Client ID (from Google Cloud Console)
GOOGLE_WEB_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

> [!NOTE]
> `local.properties` is strictly ignored by Git to prevent exposing personal IP addresses or credentials.

### 3. Build & Run
Open the project in Android Studio and select your target device/emulator, or run via terminal:
```bash
# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest
```

---

## 📦 Release Signing & CI/CD

The project includes production-ready [GitHub Actions](.github/workflows/) workflows that automatically build, sign, and publish updates.

### 1. Automated Workflows
- **`ci.yml`**: Runs on every push and pull request to `main`. Validates code formatting, lint checks, unit tests, and compiles the debug APK.
- **`release.yml`**: Automatically triggers when a version tag (e.g., `v1.1.0`) is pushed or dispatched manually. It signs the release build with your keystore, builds the standalone APK, and publishes a new **GitHub Release** with the APK attached and auto-generated release notes.

### 2. Setting Up GitHub Secrets
To enable automated release signing, configure the following secrets in your GitHub repository (**Settings > Secrets and variables > Actions**):

| Secret Name | Description | Example |
|---|---|---|
| `KEYSTORE_BASE64` | Base64-encoded `.jks` release keystore file | `cat release.jks \| base64 -w 0` |
| `KEYSTORE_PASSWORD` | Password for the release keystore | `myKeystorePassword` |
| `KEY_ALIAS` | Key alias in the release keystore | `gamestore-key` |
| `KEY_PASSWORD` | Password for the specific key alias | `myKeyPassword` |
| `GOOGLE_WEB_CLIENT_ID` | Production Google OAuth Web Client ID | `*.apps.googleusercontent.com` |
| `BASE_URL` | Production API URL (HTTPS) | `https://api.gamestore.example.com` |

### 3. Local Release Signing (Optional)
To sign release builds locally on your workstation, create a `keystore.properties` file in the root directory (already git-ignored):
```properties
KEYSTORE_FILE=/path/to/release.jks
KEYSTORE_PASSWORD=your_keystore_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```
Then run:
```bash
./gradlew assembleRelease
```
The signed APK will be output to `app/build/outputs/apk/release/app-release.apk`.

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
