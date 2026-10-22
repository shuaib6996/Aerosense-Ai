# AeroSense AI™ — Complete Android APK Build Guide 📱🛠️

This document explains how to build, test, and release the **AeroSense AI** native Android APK using either the Command Line (CLI) or Android Studio.

---

## 📋 Prerequisites

1. **Java Development Kit (JDK)**: JDK 17 or higher (Installed at `C:\Program Files\Microsoft\jdk-17...`).
2. **Android SDK**: API Level 34 (Android 14) with Build-Tools 34.0.0 (Installed at `C:\Users\<user>\AppData\Local\Android\Sdk`).
3. **Node.js** (Optional, for Capacitor/PWA features).

---

## ⚡ Method 1: Build via Command Line (CLI)

### 1. Build Debug APK (For Testing)

Open PowerShell or Command Prompt, navigate to the `android` folder, and execute:

```powershell
cd "C:\Aero sense ai\android"
.\gradlew.bat assembleDebug
```

Once compilation completes successfully, your APK will be available at:
```
android\app\build\outputs\apk\debug\app-debug.apk
```

#### Install on a connected Android phone via ADB:
```powershell
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
```

---

### 2. Build Release APK (For Play Store / Distribution)

```powershell
cd "C:\Aero sense ai\android"
.\gradlew.bat assembleRelease
```

The unsigned or signed release APK will be generated at:
```
android\app\build\outputs\apk\release\app-release-unsigned.apk
```

---

## 🎨 Method 2: Build via Android Studio (GUI)

1. Launch **Android Studio**.
2. Click **Open** and select the folder:
   ```
   C:\Aero sense ai\android
   ```
3. Allow Gradle to sync the project.
4. From the top navigation bar, choose:
   - **Build** ➔ **Build Bundle(s) / APK(s)** ➔ **Build APK(s)**
5. A pop-up notification will appear at the bottom right corner when finished. Click **locate** to open the folder containing the `.apk` file.

---

## 🔑 Generating a Signed Release Keystore for Google Play Store

Before publishing to Google Play Store, generate a release signing key:

```powershell
keytool -genkey -v -keystore aerosense-release.keystore -alias aerosense -keyalg RSA -keysize 2048 -validity 10000
```

Add your signing credentials to `android/app/build.gradle`:

```groovy
android {
    ...
    signingConfigs {
        release {
            storeFile file("aerosense-release.keystore")
            storePassword "YOUR_STORE_PASSWORD"
            keyAlias "aerosense"
            keyPassword "YOUR_KEY_PASSWORD"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

---

## 🛠️ Common Troubleshooting

### 1. `SDK location not found`
Ensure `android/local.properties` exists with:
```properties
sdk.dir=C\:\\Users\\shuai\\AppData\\Local\\Android\\Sdk
```

### 2. `Configuration contains AndroidX dependencies but android.useAndroidX is not enabled`
Ensure `android/gradle.properties` contains:
```properties
android.useAndroidX=true
android.enableJetifier=true
```

### 3. Camera or Audio Permission Denied inside WebView
`MainActivity.java` automatically intercepts and auto-grants WebRTC camera/audio requests via `WebChromeClient.onPermissionRequest`. Ensure the device has granted runtime permissions to the app in Android Settings.
