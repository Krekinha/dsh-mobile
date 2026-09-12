# In-App Updater & UI Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement in-app update checking and installation via GitHub Releases, reposition and resize the floating settings button to the bottom-left with a server icon, and generate launcher icons from `@logo.png`.

**Architecture:** A standalone `UpdateManager` queries the GitHub Releases API, performs streaming downloads with real-time byte progress into the app cache, and launches the Android PackageInstaller via `FileProvider`. `VersionHelper` handles semantic version parsing and comparison. `MainActivity` connects the dialog view bindings to `UpdateManager`, while the floating server button is repositioned in `activity_main.xml`.

**Tech Stack:** Kotlin 1.9.22, Android SDK 26-34, Material Components, ViewBinding, `HttpURLConnection`, `FileProvider`, Python/PIL (for icon generation), GitHub Actions CI.

**Spec:** `docs/superpowers/specs/2026-09-12-in-app-updater-and-ui-refinement-design.md`

## Global Constraints

- Android `minSdk = 26`, `compileSdk = 34`, `targetSdk = 34`.
- Use AndroidX `FileProvider` (`androidx.core.content.FileProvider`).
- No heavyweight third-party HTTP or reactive libraries (keep APK lightweight and build times fast).
- Zero placeholders (`TBD`, `TODO`, `implement later` are prohibited).
- Commits must be made per task.

---

### Task 1: Generate App Launcher Icons from `@logo.png`

**Files:**
- Create:
  - `app/src/main/res/mipmap-mdpi/ic_launcher.png` (48x48)
  - `app/src/main/res/mipmap-hdpi/ic_launcher.png` (72x72)
  - `app/src/main/res/mipmap-xhdpi/ic_launcher.png` (96x96)
  - `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` (144x144)
  - `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` (192x192)
  - `app/src/main/res/mipmap-mdpi/ic_launcher_round.png`
  - `app/src/main/res/mipmap-hdpi/ic_launcher_round.png`
  - `app/src/main/res/mipmap-xhdpi/ic_launcher_round.png`
  - `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png`
  - `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png`
- Modify: `app/src/main/AndroidManifest.xml:7-14`

**Interfaces:**
- Consumes: `@logo.png` (512x512 PNG at repository root).
- Produces: Mipmap drawables referenced by `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"`.

- [ ] **Step 1: Write python script to generate standard and circular launcher icons**

Execute script using Python 3 and PIL to generate all density folders:
```bash
python3 - << 'EOF'
import os
from PIL import Image, ImageDraw

src_path = "logo.png"
base_dir = "app/src/main/res"

densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

img = Image.open(src_path).convert("RGBA")

for folder, size in densities.items():
    out_dir = os.path.join(base_dir, folder)
    os.makedirs(out_dir, exist_ok=True)
    
    # Square icon with rounded corners or resized
    resized = img.resize((size, size), Image.Resampling.LANCZOS)
    resized.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")
    
    # Circular mask for round icon
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    
    round_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    round_img.paste(resized, (0, 0), mask)
    round_img.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")

print("Generated all launcher icons successfully.")
EOF
```

- [ ] **Step 2: Verify generated icon files exist and have correct dimensions**

Run: `file app/src/main/res/mipmap-*/ic_launcher*.png`
Expected: Output showing PNG images for each density (48x48 up to 192x192).

- [ ] **Step 3: Update `AndroidManifest.xml` to declare application icons**

Edit `app/src/main/AndroidManifest.xml` under `<application`:
```xml
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.DshApp"
        android:usesCleartextTraffic="true"
        android:networkSecurityConfig="@xml/network_security_config">
```

- [ ] **Step 4: Commit Task 1**

```bash
git add app/src/main/res/mipmap* app/src/main/AndroidManifest.xml
git commit -m "feat: generate launcher icons from logo.png and configure manifest"
```

---

### Task 2: Implement and Unit-Test `VersionHelper`

**Files:**
- Create:
  - `app/src/main/java/com/dsh/app/VersionHelper.kt`
  - `app/src/test/java/com/dsh/app/VersionHelperTest.kt`

**Interfaces:**
- Produces:
  - `VersionHelper.isNewer(remoteVersion: String, currentVersion: String): Boolean`
  - `VersionHelper.clean(version: String): String`

- [ ] **Step 1: Write the unit test file `VersionHelperTest.kt`**

Create `app/src/test/java/com/dsh/app/VersionHelperTest.kt`:
```kotlin
package com.dsh.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionHelperTest {

    @Test
    fun clean_removesPrefixesAndWhitespace() {
        assertEquals("1.0.0", VersionHelper.clean("v1.0.0"))
        assertEquals("1.0.0", VersionHelper.clean("V1.0.0"))
        assertEquals("1.0.0", VersionHelper.clean("  1.0.0  "))
        assertEquals("2.3.4", VersionHelper.clean("v2.3.4-debug"))
    }

    @Test
    fun isNewer_returnsTrue_whenRemoteHasHigherMajor() {
        assertTrue(VersionHelper.isNewer("2.0.0", "1.0.0"))
        assertTrue(VersionHelper.isNewer("v2.0.0", "v1.9.9"))
    }

    @Test
    fun isNewer_returnsTrue_whenRemoteHasHigherMinor() {
        assertTrue(VersionHelper.isNewer("1.1.0", "1.0.0"))
        assertTrue(VersionHelper.isNewer("v1.2.0", "v1.1.9"))
    }

    @Test
    fun isNewer_returnsTrue_whenRemoteHasHigherPatch() {
        assertTrue(VersionHelper.isNewer("1.0.1", "1.0.0"))
        assertTrue(VersionHelper.isNewer("v1.0.2", "1.0.1"))
    }

    @Test
    fun isNewer_returnsFalse_whenEqualOrLower() {
        assertFalse(VersionHelper.isNewer("1.0.0", "1.0.0"))
        assertFalse(VersionHelper.isNewer("v1.0.0", "1.0.0"))
        assertFalse(VersionHelper.isNewer("0.9.9", "1.0.0"))
        assertFalse(VersionHelper.isNewer("1.0.0", "1.0.1"))
        assertFalse(VersionHelper.isNewer("1.0.0", "1.1.0"))
    }

    @Test
    fun isNewer_handlesDifferentLengthComponents() {
        assertTrue(VersionHelper.isNewer("1.0.1", "1.0"))
        assertFalse(VersionHelper.isNewer("1.0", "1.0.1"))
        assertFalse(VersionHelper.isNewer("1.0.0", "1.0"))
    }

    @Test
    fun isNewer_handlesEmptyOrInvalidStringsSafely() {
        assertFalse(VersionHelper.isNewer("", "1.0.0"))
        assertFalse(VersionHelper.isNewer("invalid", "1.0.0"))
    }
}
```

- [ ] **Step 2: Implement `VersionHelper.kt`**

Create `app/src/main/java/com/dsh/app/VersionHelper.kt`:
```kotlin
package com.dsh.app

object VersionHelper {

    fun clean(version: String): String {
        return version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .split("-")[0]
            .trim()
    }

    fun isNewer(remoteVersion: String, currentVersion: String): Boolean {
        val cleanRemote = clean(remoteVersion)
        val cleanCurrent = clean(currentVersion)

        if (cleanRemote.isEmpty() || cleanCurrent.isEmpty()) return false

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        if (remoteParts.isEmpty() || currentParts.isEmpty()) return false

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
```

- [ ] **Step 3: Verify with Python unit test runner**

Run quick standalone verification of the exact logic:
```bash
python3 -c '
def clean(v):
    return v.strip().lstrip("vV").split("-")[0].strip()

def is_newer(remote, current):
    cr, cc = clean(remote), clean(current)
    if not cr or not cc: return False
    try:
        rp = [int(x) for x in cr.split(".")]
        cp = [int(x) for x in cc.split(".")]
    except ValueError:
        return False
    maxlen = max(len(rp), len(cp))
    rp += [0] * (maxlen - len(rp))
    cp += [0] * (maxlen - len(cp))
    return rp > cp

assert is_newer("1.0.1", "1.0.0") is True
assert is_newer("v1.0.2", "1.0.1") is True
assert is_newer("1.0.0", "1.0.0") is False
assert is_newer("0.9.9", "1.0.0") is False
assert is_newer("2.0.0", "1.9.9") is True
print("VersionHelper algorithm verified.")
'
```

- [ ] **Step 4: Commit Task 2**

```bash
git add app/src/main/java/com/dsh/app/VersionHelper.kt app/src/test/java/com/dsh/app/VersionHelperTest.kt
git commit -m "feat: implement VersionHelper with semantic version comparison"
```

---

### Task 3: Setup FileProvider and Install Permissions for Android 8.0+

**Files:**
- Create: `app/src/main/res/xml/file_paths.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces:
  - Authority `${applicationId}.fileprovider` mapping `context.cacheDir/updates/` to content URI.
  - Manifest permission `android.permission.REQUEST_INSTALL_PACKAGES`.

- [ ] **Step 1: Create `app/src/main/res/xml/file_paths.xml`**

Create `app/src/main/res/xml/file_paths.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="updates" path="updates/" />
</paths>
```

- [ ] **Step 2: Add permission and FileProvider to `AndroidManifest.xml`**

Edit `app/src/main/AndroidManifest.xml`:
Add permission under `<manifest>`:
```xml
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```
And add provider inside `<application>`:
```xml
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
```

- [ ] **Step 3: Commit Task 3**

```bash
git add app/src/main/res/xml/file_paths.xml app/src/main/AndroidManifest.xml
git commit -m "feat: add FileProvider configuration and REQUEST_INSTALL_PACKAGES permission"
```

---

### Task 4: Reposition Floating Server Button and Add Server Vector Icon

**Files:**
- Create: `app/src/main/res/drawable/ic_server.xml`
- Modify:
  - `app/src/main/res/drawable/bg_settings_fab.xml`
  - `app/src/main/res/layout/activity_main.xml:90-103`

**Interfaces:**
- Consumes: `@drawable/ic_server` and updated `@drawable/bg_settings_fab`.
- Produces: `btnSettings` anchored at `bottom|start`, 32dp x 32dp, above the DSH bottom bar / gear icon.

- [ ] **Step 1: Create server vector drawable `ic_server.xml`**

Create `app/src/main/res/drawable/ic_server.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/text_primary"
        android:pathData="M4,1h16c1.1,0 2,0.9 2,2v4c0,1.1 -0.9,2 -2,2H4C2.9,9 2,8.1 2,7V3c0,-1.1 0.9,-2 2,-2zM4,5h16V3H4v2zM6,4.5c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1 -1,0.45 -1,1 0.45,1 1,1zM4,15h16c1.1,0 2,0.9 2,2v4c0,1.1 -0.9,2 -2,2H4c-1.1,0 -2,-0.9 -2,-2v-4c0,-1.1 0.9,-2 2,-2zM4,19h16v-2H4v2zM6,18.5c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1 -1,0.45 -1,1 0.45,1 1,1z" />
</vector>
```

- [ ] **Step 2: Update `bg_settings_fab.xml` for 32dp dimensions**

Edit `app/src/main/res/drawable/bg_settings_fab.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="#40FFFFFF">
    <item>
        <shape android:shape="oval">
            <solid android:color="@color/fab_background" />
            <stroke
                android:width="1dp"
                android:color="@color/fab_stroke" />
            <size
                android:width="32dp"
                android:height="32dp" />
        </shape>
    </item>
</ripple>
```

- [ ] **Step 3: Update `btnSettings` in `activity_main.xml`**

Edit `app/src/main/res/layout/activity_main.xml`:
Change `btnSettings` to:
```xml
    <!-- Translucent server settings button at bottom-left -->
    <ImageButton
        android:id="@+id/btnSettings"
        android:layout_width="32dp"
        android:layout_height="32dp"
        android:layout_gravity="bottom|start"
        android:layout_marginStart="10dp"
        android:layout_marginBottom="76dp"
        android:background="@drawable/bg_settings_fab"
        android:contentDescription="@string/desc_settings"
        android:elevation="4dp"
        android:padding="6dp"
        android:scaleType="centerInside"
        android:src="@drawable/ic_server" />
```

- [ ] **Step 4: Commit Task 4**

```bash
git add app/src/main/res/drawable/ic_server.xml app/src/main/res/drawable/bg_settings_fab.xml app/src/main/res/layout/activity_main.xml
git commit -m "feat: reposition and resize floating settings button with server icon"
```

---

### Task 5: Implement `UpdateManager.kt`

**Files:**
- Create: `app/src/main/java/com/dsh/app/UpdateManager.kt`

**Interfaces:**
- Consumes: `VersionHelper`, GitHub Releases API, Android `FileProvider`.
- Produces:
  - `data class ReleaseInfo(val tagName: String, val downloadUrl: String, val assetSize: Long, val body: String)`
  - `sealed class UpdateState`
  - `UpdateManager.checkForUpdates(currentVersion: String, callback: (UpdateState) -> Unit)`
  - `UpdateManager.downloadAndInstall(release: ReleaseInfo, progressCallback: (bytesRead: Long, totalBytes: Long, percent: Int) -> Unit, completionCallback: (Result<File>) -> Unit)`
  - `UpdateManager.installApk(context: Context, apkFile: File)`

- [ ] **Step 1: Write `UpdateManager.kt`**

Create `app/src/main/java/com/dsh/app/UpdateManager.kt`:
```kotlin
package com.dsh.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

data class ReleaseInfo(
    val tagName: String,
    val downloadUrl: String,
    val assetSize: Long,
    val body: String
)

sealed class UpdateState {
    object Checking : UpdateState()
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class UpdateManager(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val repoUrl = "https://api.github.com/repos/Krekinha/dsh-mobile/releases/latest"

    fun checkForUpdates(currentVersion: String, onResult: (UpdateState) -> Unit) {
        onResult(UpdateState.Checking)
        executor.execute {
            try {
                val url = URL(repoUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("User-Agent", "DSH-Mobile-App")
                }

                if (conn.responseCode != 200) {
                    onResult(UpdateState.Error("GitHub respondeu com código ${conn.responseCode}"))
                    return@execute
                }

                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)

                val tagName = json.optString("tag_name", "")
                val body = json.optString("body", "")
                val assets = json.optJSONArray("assets")

                var downloadUrl: String? = null
                var assetSize: Long = 0

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            downloadUrl = asset.optString("browser_download_url")
                            assetSize = asset.optLong("size", 0)
                            break
                        }
                    }
                }

                if (downloadUrl == null) {
                    onResult(UpdateState.Error("Nenhum APK encontrado na release do GitHub."))
                    return@execute
                }

                val release = ReleaseInfo(tagName, downloadUrl, assetSize, body)
                if (VersionHelper.isNewer(tagName, currentVersion)) {
                    onResult(UpdateState.UpdateAvailable(release))
                } else {
                    onResult(UpdateState.UpToDate)
                }

            } catch (e: Exception) {
                onResult(UpdateState.Error("Erro ao verificar atualização: ${e.message}"))
            }
        }
    }

    fun downloadApk(
        release: ReleaseInfo,
        onProgress: (bytesRead: Long, totalBytes: Long, percent: Int) -> Unit,
        onComplete: (Result<File>) -> Unit
    ) {
        executor.execute {
            try {
                val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
                val apkFile = File(updatesDir, "dsh-update.apk")
                if (apkFile.exists()) apkFile.delete()

                var currentUrl = release.downloadUrl
                var conn: HttpURLConnection

                // Follow redirects (GitHub Releases redirect to AWS S3)
                while (true) {
                    val url = URL(currentUrl)
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 15000
                        instanceFollowRedirects = false
                        setRequestProperty("User-Agent", "DSH-Mobile-App")
                    }

                    when (conn.responseCode) {
                        HttpURLConnection.HTTP_MOVED_PERM,
                        HttpURLConnection.HTTP_MOVED_TEMP,
                        307, 308 -> {
                            val newUrl = conn.getHeaderField("Location")
                            conn.disconnect()
                            currentUrl = newUrl
                        }
                        HttpURLConnection.HTTP_OK -> break
                        else -> {
                            onComplete(Result.failure(Exception("HTTP ${conn.responseCode} ao baixar APK")))
                            return@execute
                        }
                    }
                }

                val totalBytes = if (conn.contentLengthLong > 0) conn.contentLengthLong else release.assetSize
                var bytesReadTotal: Long = 0

                conn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesReadTotal += read
                            val percent = if (totalBytes > 0) {
                                ((bytesReadTotal * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else 0
                            onProgress(bytesReadTotal, totalBytes, percent)
                        }
                        output.flush()
                    }
                }

                onComplete(Result.success(apkFile))

            } catch (e: Exception) {
                onComplete(Result.failure(e))
            }
        }
    }

    fun installApk(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val authority = "${context.packageName}.fileprovider"
        val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
```

- [ ] **Step 2: Commit Task 5**

```bash
git add app/src/main/java/com/dsh/app/UpdateManager.kt
git commit -m "feat: implement UpdateManager with GitHub check, streaming download, and installer dispatch"
```

---

### Task 6: Update `dialog_settings.xml` with In-App Updater Section

**Files:**
- Modify: `app/src/main/res/layout/dialog_settings.xml`

**Interfaces:**
- Produces View IDs:
  - `txtVersionInfo` (TextView)
  - `btnCheckUpdates` (MaterialButton)
  - `layoutUpdateProgress` (LinearLayout)
  - `txtUpdateStatus` (TextView)
  - `progressBarUpdate` (ProgressBar)
  - `txtProgressDetail` (TextView)
  - `btnActionUpdate` (MaterialButton)

- [ ] **Step 1: Update `dialog_settings.xml`**

Edit `app/src/main/res/layout/dialog_settings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@color/surface"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/settings_title"
        android:textColor="@color/text_primary"
        android:textSize="18sp"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="4dp"
        android:text="Digite o endereço do servidor dsh na sua rede local:"
        android:textColor="@color/text_secondary"
        android:textSize="13sp" />

    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/inputLayoutUrl"
        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        app:boxStrokeColor="@color/primary"
        app:hintTextColor="@color/primary"
        app:placeholderText="http://192.168.0.113:3080/">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/editServerUrl"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="textUri"
            android:singleLine="true"
            android:textColor="@color/text_primary" />
    </com.google.android.material.textfield.TextInputLayout>

    <TextView
        android:id="@+id/btnResetDefault"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:clickable="true"
        android:focusable="true"
        android:padding="4dp"
        android:text="@string/settings_reset"
        android:textColor="@color/accent"
        android:textSize="13sp" />

    <!-- Divider separating server URL from updater -->
    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:layout_marginTop="20dp"
        android:layout_marginBottom="16dp"
        android:background="@color/surface_variant" />

    <!-- Update section header and current version info -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Atualizações do App"
            android:textColor="@color/text_primary"
            android:textSize="15sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/txtVersionInfo"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="v1.0.1"
            android:textColor="@color/text_secondary"
            android:textSize="12sp" />
    </LinearLayout>

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnCheckUpdates"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:text="Verificar Atualizações"
        android:textColor="@color/text_primary"
        app:cornerRadius="10dp"
        app:icon="@drawable/ic_refresh"
        app:iconGravity="textStart"
        app:iconTint="@color/text_primary"
        app:strokeColor="@color/surface_variant" />

    <!-- Live status and progress container -->
    <LinearLayout
        android:id="@+id/layoutUpdateProgress"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:orientation="vertical"
        android:visibility="gone">

        <TextView
            android:id="@+id/txtUpdateStatus"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Verificando atualizações..."
            android:textColor="@color/text_secondary"
            android:textSize="13sp" />

        <ProgressBar
            android:id="@+id/progressBarUpdate"
            style="@style/Widget.AppCompat.ProgressBar.Horizontal"
            android:layout_width="match_parent"
            android:layout_height="8dp"
            android:layout_marginTop="8dp"
            android:indeterminate="true"
            android:progressDrawable="@drawable/bg_settings_fab" />

        <TextView
            android:id="@+id/txtProgressDetail"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:text=""
            android:textColor="@color/text_secondary"
            android:textSize="11sp"
            android:visibility="gone" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnActionUpdate"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="10dp"
            android:text="Baixar e Instalar"
            android:textColor="@color/text_primary"
            android:visibility="gone"
            app:backgroundTint="@color/primary"
            app:cornerRadius="10dp" />
    </LinearLayout>

</LinearLayout>
```

- [ ] **Step 2: Commit Task 6**

```bash
git add app/src/main/res/layout/dialog_settings.xml
git commit -m "feat: add update checker and progress layout to dialog_settings.xml"
```

---

### Task 7: Integrate `UpdateManager` into `MainActivity.kt` and Bump App Version

**Files:**
- Modify:
  - `app/src/main/java/com/dsh/app/MainActivity.kt`
  - `app/build.gradle.kts`
  - `.github/workflows/build-apk.yml`

**Interfaces:**
- Connects `dialogView` update widgets to `UpdateManager`.
- Bumps version to `versionCode = 2`, `versionName = "1.0.1"`.

- [ ] **Step 1: Update `app/build.gradle.kts` with version 1.0.1**

Edit `app/build.gradle.kts`:
```kotlin
        versionCode = 2
        versionName = "1.0.1"
```

- [ ] **Step 2: Connect `UpdateManager` in `MainActivity.kt`**

Edit `app/src/main/java/com/dsh/app/MainActivity.kt` to initialize `UpdateManager` and bind dialog update components:
```kotlin
    private lateinit var updateManager: UpdateManager
```
Inside `onCreate`:
```kotlin
    updateManager = UpdateManager(this)
```
Inside `showSettingsDialog`:
```kotlin
        val txtVersionInfo = dialogView.findViewById<TextView>(R.id.txtVersionInfo)
        val btnCheckUpdates = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCheckUpdates)
        val layoutUpdateProgress = dialogView.findViewById<View>(R.id.layoutUpdateProgress)
        val txtUpdateStatus = dialogView.findViewById<TextView>(R.id.txtUpdateStatus)
        val progressBarUpdate = dialogView.findViewById<ProgressBar>(R.id.progressBarUpdate)
        val txtProgressDetail = dialogView.findViewById<TextView>(R.id.txtProgressDetail)
        val btnActionUpdate = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnActionUpdate)

        val currentVersion = BuildConfig.VERSION_NAME
        txtVersionInfo.text = "v$currentVersion"

        var latestRelease: ReleaseInfo? = null
        var downloadedApkFile: File? = null

        btnCheckUpdates.setOnClickListener {
            layoutUpdateProgress.visibility = View.VISIBLE
            progressBarUpdate.isIndeterminate = true
            txtProgressDetail.visibility = View.GONE
            btnActionUpdate.visibility = View.GONE
            txtUpdateStatus.text = "Buscando atualizações no GitHub..."

            updateManager.checkForUpdates(currentVersion) { state ->
                runOnUiThread {
                    when (state) {
                        is UpdateState.Checking -> {
                            txtUpdateStatus.text = "Buscando atualizações no GitHub..."
                        }
                        is UpdateState.UpdateAvailable -> {
                            latestRelease = state.release
                            val sizeMb = String.format(java.util.Locale.US, "%.1f", state.release.assetSize / (1024.0 * 1024.0))
                            txtUpdateStatus.text = "Nova versão ${state.release.tagName} disponível ($sizeMb MB)!"
                            progressBarUpdate.isIndeterminate = false
                            progressBarUpdate.progress = 0
                            btnActionUpdate.visibility = View.VISIBLE
                            btnActionUpdate.text = "Baixar e Instalar"
                        }
                        is UpdateState.UpToDate -> {
                            txtUpdateStatus.text = "Você já está na versão mais recente (v$currentVersion)."
                            progressBarUpdate.visibility = View.GONE
                            btnActionUpdate.visibility = View.GONE
                        }
                        is UpdateState.Error -> {
                            txtUpdateStatus.text = state.message
                            progressBarUpdate.visibility = View.GONE
                            btnActionUpdate.visibility = View.GONE
                        }
                    }
                }
            }
        }

        btnActionUpdate.setOnClickListener {
            val file = downloadedApkFile
            if (file != null && file.exists()) {
                updateManager.installApk(file)
                return@setOnClickListener
            }

            val release = latestRelease ?: return@setOnClickListener
            btnActionUpdate.isEnabled = false
            progressBarUpdate.visibility = View.VISIBLE
            progressBarUpdate.isIndeterminate = false
            txtProgressDetail.visibility = View.VISIBLE
            txtUpdateStatus.text = "Baixando ${release.tagName}..."

            updateManager.downloadApk(
                release = release,
                onProgress = { bytesRead, totalBytes, percent ->
                    runOnUiThread {
                        progressBarUpdate.progress = percent
                        val readMb = String.format(java.util.Locale.US, "%.1f", bytesRead / (1024.0 * 1024.0))
                        val totalMb = String.format(java.util.Locale.US, "%.1f", totalBytes / (1024.0 * 1024.0))
                        txtProgressDetail.text = "$readMb MB / $totalMb MB ($percent%)"
                    }
                },
                onComplete = { result ->
                    runOnUiThread {
                        btnActionUpdate.isEnabled = true
                        result.fold(
                            onSuccess = { apkFile ->
                                downloadedApkFile = apkFile
                                txtUpdateStatus.text = "Download concluído! Pronto para instalar."
                                btnActionUpdate.text = "Instalar APK"
                                updateManager.installApk(apkFile)
                            },
                            onFailure = { error ->
                                txtUpdateStatus.text = "Erro no download: ${error.message}"
                            }
                        )
                    }
                }
            )
        }
```

- [ ] **Step 3: Update GitHub Actions workflow to run tests and tag releases**

Edit `.github/workflows/build-apk.yml` to include unit test execution:
```yaml
      - name: Run Unit Tests
        run: gradle testDebugUnitTest

      - name: Build Debug APK
        run: gradle assembleDebug
```

- [ ] **Step 4: Commit Task 7**

```bash
git add app/build.gradle.kts app/src/main/java/com/dsh/app/MainActivity.kt .github/workflows/build-apk.yml
git commit -m "feat: integrate UpdateManager into MainActivity settings dialog and bump version to 1.0.1"
```

---

### Task 8: End-to-End Verification & CI Push

**Files:**
- Verified in git tree.

- [ ] **Step 1: Check git diff and status**

Run: `git status && git diff --cached`
Expected: Working tree clean or only untracked artifacts.

- [ ] **Step 2: Push changes to GitHub `main` branch**

Run: `git push origin main`
Expected: Push succeeds and triggers GitHub Actions workflow.

- [ ] **Step 3: Monitor GitHub Actions run**

Run: `gh run list --limit 1` and `gh run watch $(gh run list --limit 1 --json databaseId --jq '.[0].databaseId')`
Expected: `completed / success` for workflow `Build Android APK`.

- [ ] **Step 4: Verify updated release asset**

Run: `gh release view latest --json tagName,assets`
Expected: Tag `latest` with newly updated `app-debug.apk`.
