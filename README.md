# DSH Mobile — Android App Wrapper

[![Build Android APK](https://github.com/Krekinha/dsh-mobile/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Krekinha/dsh-mobile/actions/workflows/build-apk.yml)
[![Latest Release](https://img.shields.io/github/v/release/Krekinha/dsh-mobile?label=Release&color=blue)](https://github.com/Krekinha/dsh-mobile/releases/latest)
[![Android Min SDK](https://img.shields.io/badge/minSdk-26%20(Android%208.0)-brightgreen)](https://developer.android.com/tools/releases/platforms)
[![Android Target SDK](https://img.shields.io/badge/targetSdk-34%20(Android%2014)-blue)](https://developer.android.com/tools/releases/platforms)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple.svg)](https://kotlinlang.org/)

**DSH Mobile** is a lightweight, native Android application built with Kotlin that wraps the web interface of **DSH (DeepSeek Harness)**. It strips away browser address bars and navigation clutter to provide a clean, responsive, and immersive native app experience on mobile devices.

---

## ✨ Features

- **📱 Edge-to-Edge Immersive Experience:** Runs in a true edge-to-edge layout, utilizing the entire screen while respecting system bar insets (status bar, navigation bar, and display cutouts).
- **🔄 In-App Updater:** Check for new app updates directly within the settings dialog. Fetches releases via the GitHub API, displays real-time download progress (MB downloaded and percentage), and launches the native Android package installer.
- **🖥️ Floating Server Switcher:** A discreet 32×32dp translucent server button at the bottom-left allows you to switch host IPs and ports instantly without obstructing DSH UI elements.
- **💾 Persistent Configuration:** Saves your custom server URL in private storage (`SharedPreferences`) and provides a quick reset to the default address (`http://192.168.0.102:3080/`).
- **📎 Native File & Image Uploads:** Full Android file chooser integration (`WebChromeClient` with `onShowFileChooser`), allowing you to attach screenshots, photos, and files directly to DSH conversations.
- **⚡ Smooth Web Experience:** Direct WebView rendering without pull-to-refresh gesture interference, ensuring seamless vertical scrolling in long chats and code blocks.
- **📴 Offline & Error Recovery:** Displays a friendly error screen whenever the DSH server is unreachable, with quick actions to retry or reconfigure the server address.
- **🔙 Smart Back Navigation:** Pressing the device's back gesture or button minimizes/backgrounds the app cleanly (`moveTaskToBack`), preventing browser history traps.
- **🔓 Local Cleartext HTTP Traffic:** Configured with `usesCleartextTraffic="true"` and a custom `network_security_config.xml` to allow seamless local network connections (`http://192.168.x.x:3080/`) without requiring SSL certificates.
- **🎨 Custom App Icon:** Complete launcher icon suite generated across all density buckets (`mipmap-mdpi` through `xxxhdpi`) including circular adaptive icons.

---

## 🏗️ Project Architecture

```
dsh-mobile/
├── .github/
│   └── workflows/
│       └── build-apk.yml                # CI/CD: Automated build, test, and GitHub release
├── app/
│   ├── build.gradle.kts                 # Module configuration (SDKs, dependencies, packaging)
│   ├── proguard-rules.pro               # ProGuard / R8 rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml      # App permissions, activities, and FileProvider
│       │   ├── java/com/dsh/app/
│       │   │   ├── MainActivity.kt          # UI controller, WebView setup, updater wiring
│       │   │   ├── UpdateManager.kt         # GitHub release checker, streaming downloader, installer
│       │   │   ├── VersionHelper.kt         # Semantic version comparator (SemVer)
│       │   │   ├── AppPreferences.kt        # Persistent server URL storage
│       │   │   ├── UrlHelper.kt             # URL validation and normalization
│       │   │   ├── DshWebViewClient.kt      # Page load events and error state handling
│       │   │   └── DshWebChromeClient.kt    # Native file chooser integration
│       │   └── res/
│       │       ├── drawable/            # Vector assets (ic_server, ic_refresh, ic_wifi_off)
│       │       ├── layout/              # XML layouts (activity_main, dialog_settings, view_error_state)
│       │       ├── mipmap-*/            # Generated launcher icons (standard and round)
│       │       ├── values/              # Strings, colors, and themes
│       │       └── xml/
│       │           ├── file_paths.xml               # FileProvider cache paths for APK installer
│       │           └── network_security_config.xml  # Cleartext network security rules
│       └── test/java/com/dsh/app/
│           ├── UrlHelperTest.kt         # Unit tests for URL validation and normalization
│           └── VersionHelperTest.kt     # Unit tests for SemVer comparisons
├── build.gradle.kts                     # Root build script
├── settings.gradle.kts                  # Gradle project settings
├── logo.png                             # Source high-resolution app icon
└── README.md
```

### Core Components

| Component | Description |
|---|---|
| `MainActivity` | Coordinates the WebView lifecycle, handles insets, manages the settings dialog, and triggers the installer intent. |
| `UpdateManager` | Queries GitHub Releases API for new versions, streams APK downloads to cache with progress callbacks, and exposes installation methods. |
| `VersionHelper` | Cleans and compares semantic version strings (`vX.Y.Z`) to determine if an update is available. |
| `AppPreferences` | Manages persistent user preferences using `SharedPreferences`. |
| `UrlHelper` | Normalizes user input (adds scheme and trailing slash if missing) and validates HTTP/HTTPS URLs. |
| `DshWebChromeClient` | Intercepts HTML file inputs (`<input type="file">`) to open the native Android system file picker. |
| `DshWebViewClient` | Handles page navigation, external URL routing, and triggers error states when the server is unreachable. |

---

## 📥 Download & Installation

### Option 1: Direct Download (Latest Release)

You can download the pre-compiled APK directly from GitHub Releases:
1. Go to the [Latest Releases](https://github.com/Krekinha/dsh-mobile/releases/latest) page.
2. Download `dsh-mobile-v1.0.2.apk` (or the latest release APK).
3. Open the file on your Android device and allow installation from unknown sources if prompted.

### Option 2: In-App Updates

Once installed, future updates can be downloaded and installed directly from within the app:
1. Tap the **Server** icon in the bottom-left corner.
2. Tap **Verificar Atualizações** (Check for Updates).
3. If a newer version is available on GitHub Releases, tap **Baixar e Instalar** (Download & Install).

---

## 🛠️ Building from Source

### Prerequisites

- **Java Development Kit (JDK):** Version 17
- **Android SDK:** Compile SDK 34, Min SDK 26
- **Gradle:** 8.5+ (or use the included `./gradlew` wrapper)

### Option 1: Using Android Studio (Recommended)

1. Clone this repository:
   ```bash
   git clone https://github.com/Krekinha/dsh-mobile.git
   ```
2. Open **Android Studio** and select **Open**, then choose the cloned `dsh-mobile` directory.
3. Allow Gradle to synchronize dependencies.
4. Connect an Android phone with USB Debugging enabled (or start an Android Emulator).
5. Click **Run** (`Shift + F10`).

### Option 2: Using the Command Line (Gradle CLI)

1. **Run Unit Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

2. **Assemble the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

3. The generated APK will be located at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Install via ADB:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## ⚙️ Connecting to DeepSeek Harness

1. Ensure that your DSH server is running on your local machine and accessible over your Wi-Fi network:
   ```bash
   # Example: starting DSH web interface
   dsh web
   ```
2. Make sure your Android smartphone is connected to the **same Wi-Fi network**.
3. Open **DSH Mobile** on your phone.
4. Tap the **Server icon** (computer icon) at the bottom-left of the screen.
5. Enter your machine's local IP address and port (e.g., `http://192.168.0.105:3080/`).
6. Tap **Salvar e Conectar** (Save and Connect). The app will remember this URL and automatically reload.

---

## 🤖 CI/CD Automation

This repository includes a GitHub Actions workflow (`.github/workflows/build-apk.yml`) that automatically:
- Triggers on every push and pull request to `main` and `master`.
- Sets up JDK 17 and executes the unit test suite (`testDebugUnitTest`).
- Builds the debug APK (`assembleDebug`).
- Uploads the APK as a workflow artifact.
- Automatically publishes/updates the GitHub Release with the versioned APK (`dsh-mobile-v<version>.apk`).

---

## 📄 License

This project is open-source and intended for use with [DeepSeek Harness](https://github.com/Krekinha).
