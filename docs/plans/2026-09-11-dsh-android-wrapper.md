# DSH Android Wrapper Implementation Plan

> **For Agent:** Implement this plan task-by-task, adhering to the spec in `docs/superpowers/specs/2026-09-11-dsh-android-wrapper-design.md`.

**Goal:** Build a native Android application wrapper for DSH (DeepSeek Harness) running on `http://192.168.0.102:3080/` with edge-to-edge clean design, file/image upload support, pull-to-refresh, direct back button minimizing, and a discreet settings dialog to update the server URL.

**Architecture:** Kotlin Android app using Android Gradle Plugin. MainActivity hosts a SwipeRefreshLayout wrapping a WebView with customized WebChromeClient (handling file choose intents via ActivityResultLauncher) and WebViewClient (handling network error state and refresh termination). AppPreferences persists the URL via SharedPreferences. UrlHelper validates and normalizes target URLs.

**Tech Stack:** Kotlin, Android Gradle Plugin 8.2+, AndroidX AppCompat, Material Components, SwipeRefreshLayout, JUnit 4.

---

### Task 1: Project Gradle Configuration & Wrapper

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `.gitignore`

**Step 1: Write `settings.gradle.kts`**
Configure plugin management and repositories (Google, MavenCentral).

**Step 2: Write root `build.gradle.kts`**
Declare Android Application and Kotlin plugins.

**Step 3: Write Gradle Wrapper files**
Configure Gradle 8.5 in `gradle/wrapper/gradle-wrapper.properties` and provide `gradlew` and `gradlew.bat` scripts.

**Step 4: Write `.gitignore`**
Ignore `.gradle`, `build/`, `local.properties`, `.idea/`, and keystores.

---

### Task 2: App Module Build Configuration

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`

**Step 1: Write `app/build.gradle.kts`**
- Configure `applicationId = "com.dsh.app"`
- Set `minSdk = 26`, `targetSdk = 34`, `compileSdk = 34`, `versionCode = 1`, `versionName = "1.0.0"`
- Enable `viewBinding = true`
- Dependencies: `androidx.appcompat`, `com.google.android.material`, `androidx.swiperefreshlayout`, `junit`.

**Step 2: Write `app/proguard-rules.pro`**
Keep rules for WebView JavascriptInterface and model classes if needed.

---

### Task 3: URL Helper & Unit Tests (TDD)

**Files:**
- Create: `app/src/test/java/com/dsh/app/UrlHelperTest.kt`
- Create: `app/src/main/java/com/dsh/app/UrlHelper.kt`

**Step 1: Write unit tests in `UrlHelperTest.kt`**
- Test normalizing URL (adding `http://` if missing, adding trailing slash, trimming whitespace).
- Test validating valid vs invalid IP/host URLs.

**Step 2: Implement `UrlHelper.kt`**
- Implement `normalize(raw: String): String` and `isValid(raw: String): Boolean`.

---

### Task 4: App Preferences & Persistence

**Files:**
- Create: `app/src/main/java/com/dsh/app/AppPreferences.kt`

**Step 1: Implement `AppPreferences.kt`**
- Manage `SharedPreferences` with default constant `DEFAULT_URL = "http://192.168.0.102:3080/"`.
- Expose `getServerUrl()`, `setServerUrl(url: String)`, `resetToDefault()`.

---

### Task 5: Network Security & Android Manifest

**Files:**
- Create: `app/src/main/res/xml/network_security_config.xml`
- Create: `app/src/main/AndroidManifest.xml`

**Step 1: Write `network_security_config.xml`**
Enable cleartext traffic permitted for local network and dev endpoints.

**Step 2: Write `AndroidManifest.xml`**
- Declare `INTERNET` and `ACCESS_NETWORK_STATE` permissions.
- Set `android:usesCleartextTraffic="true"` and `android:networkSecurityConfig="@xml/network_security_config"`.
- Set `MainActivity` as Launcher with soft input mode `adjustResize`.

---

### Task 6: UI Resources, Colors, Icons & Themes

**Files:**
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/ic_settings.xml`
- Create: `app/src/main/res/drawable/bg_settings_fab.xml`
- Create: `app/src/main/res/drawable/ic_refresh.xml`
- Create: `app/src/main/res/drawable/ic_wifi_off.xml`

**Step 1: Create color palette and themes**
Clean dark/light adaptive colors matching modern minimal dashboards.

**Step 2: Create vector drawables**
Vector icons for Settings (gear), Refresh, and Offline (wifi off).

---

### Task 7: Layouts & UI Structure

**Files:**
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/layout/dialog_settings.xml`
- Create: `app/src/main/res/layout/view_error_state.xml`

**Step 1: Implement `view_error_state.xml`**
Clean offline card with title, description, "Tentar Novamente", and "Trocar Endereço".

**Step 2: Implement `dialog_settings.xml`**
Material dialog with TextInputEditText for server address, "Restaurar Padrão", "Cancelar", and "Salvar".

**Step 3: Implement `activity_main.xml`**
CoordinatorLayout or FrameLayout with:
- `SwipeRefreshLayout` containing `WebView`
- Included `view_error_state` (initially `gone`)
- Floating discreet gear button (translucent, top-right).

---

### Task 8: WebView Clients (File Chooser & Error Handling)

**Files:**
- Create: `app/src/main/java/com/dsh/app/DshWebChromeClient.kt`
- Create: `app/src/main/java/com/dsh/app/DshWebViewClient.kt`

**Step 1: Implement `DshWebChromeClient.kt`**
- Override `onShowFileChooser(webView, filePathCallback, fileChooserParams)`.
- Pass file chooser requests to host activity launcher.
- Handle cancellation gracefully.

**Step 2: Implement `DshWebViewClient.kt`**
- Handle `onPageFinished` (stops refresh animation, hides error state).
- Handle `onReceivedError` (shows error state, stops refresh animation).

---

### Task 9: Main Activity & Edge-to-Edge Integration

**Files:**
- Create: `app/src/main/java/com/dsh/app/MainActivity.kt`

**Step 1: Implement `MainActivity.kt`**
- Edge-to-edge window setup (`WindowCompat.setDecorFitsSystemWindows(window, false)`).
- File chooser `ActivityResultLauncher<Intent>` integration.
- Back button callback (`onBackPressedDispatcher.addCallback`) calling `moveTaskToBack(true)`.
- Configure WebView settings (`javaScriptEnabled`, `domStorageEnabled`, `databaseEnabled`, `cacheMode`).
- Connect `SwipeRefreshLayout` reload.
- Connect Settings dialog with `AppPreferences` and `UrlHelper`.

---

### Task 10: Documentation & Build Instructions

**Files:**
- Create: `README.md`
- Update: `AGENTS.md`

**Step 1: Write `README.md`**
Detailed instructions on how to build the APK via command line or Android Studio, how to install via `adb install`, and how to change the host IP.

**Step 2: Update `AGENTS.md`**
Document the repository purpose, stack, and commands.
