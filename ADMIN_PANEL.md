# AeroSense AI — Master Admin Panel Architecture & Blueprint 🛰️📱

> **Author:** AeroSense AI Engineering & System Architecture  
> **Target Audience:** App Owner & Development Team  
> **Application:** AeroSense AI (Android & Touchless OS)  
> **Document Version:** 1.0.0 (Production Roadmap)

---

## 🎯 1. Executive Summary & Purpose

AeroSense AI is a camera-driven, touchless AI mobile OS relying on MediaPipe computer vision, Android Accessibility Services, and background overlay windows. Because gesture physics, phone hardware, and Android OS updates vary across thousands of smartphone models, the **App Owner needs a centralized Admin Panel** to:

1. **Tune Gestures Remotely without rebuilding or re-releasing APKs** (e.g., adjust scroll speed, hold delays, like sensitivity via Cloud Remote Config).
2. **Push Over-The-Air (OTA) Updates & In-App Announcements** (force update on critical bug fixes, show "What's New" prompts).
3. **Monitor Live User Telemetry & Device Compatibility** (which phone brands, Android versions, or screen resolutions experience camera freezes or permission drop-offs).
4. **Track Crash Vitals & Background Battery Kills** (identify when Android kills the background engine or when WebGL shaders stall).
5. **Direct User Engagement & Feedback Loop** (receive user bug reports, gesture accuracy ratings, and feature requests).

---

## 🏛️ 2. High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   AeroSense AI Mobile App                   │
│  (MediaPipe Vision + Accessibility Service + Web Engine)    │
└──────────────────────────────┬──────────────────────────────┘
                               │
            HTTPS / WebSocket  │  REST API / Cloud SDK
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 Cloud Backend & Database                     │
│       (Firebase Firestore / Supabase / Custom Node.js)       │
│                                                             │
│   ├── Remote Config Cache      ├── Telemetry & Vitals       │
│   ├── Version & OTA Manifest   ├── User Feedback Store      │
│   └── FCM Push Notification    └── Auth & RBAC Security     │
└──────────────────────────────▲──────────────────────────────┘
                               │
            Secure Admin Auth  │  Web Dashboard (Desktop / Mobile)
                               │
┌─────────────────────────────────────────────────────────────┐
│             AeroSense AI Super Admin Panel                  │
│    (admin.html / Modern Dashboard for the App Owner)        │
└─────────────────────────────────────────────────────────────┘
```

---

## 🧩 3. Full Structure & Core Modules of the Admin Panel

The Admin Panel is divided into **6 Essential Control Hubs**:

```
AeroSense AI Admin Center
├── 1. Live Telemetry & KPI Analytics Hub
├── 2. Remote Config & Live Gesture Tuning Engine (Zero-APK-Rebuild)
├── 3. OTA Updates, Versioning & Emergency Kill-Switch
├── 4. Crash Vitals, ANR & Camera Diagnostics Monitor
├── 5. Push Notifications & In-App Broadcast Center
└── 6. User Feedback, Gesture Ratings & Support Desk
```

---

### Module 1: Live Telemetry & KPI Analytics Hub 📊
*Objective: Give the app owner instant visibility into user growth, engagement, and gesture adoption.*

| Widget / Metric | Data Tracked | Value to App Owner |
| :--- | :--- | :--- |
| **Active Users (Real-Time)** | Concurrent active sessions, DAU, MAU, Retention Rate | Monitor active adoption after updates or marketing. |
| **Gesture Adoption Heatmap** | Daily counts of Air Tap, Quick Scroll, Thumbs-Up Like, Hold Menu | Shows which gestures are hits and which are ignored. |
| **Permission Health Matrix** | % of users with Camera, Accessibility, Overlay, and Battery Exemption active | Identifies where users drop off during onboarding. |
| **Device & OS Breakdown** | Device manufacturers (Samsung, Xiaomi, Vivo, OnePlus), Android 11–14 distribution | Prioritize testing on the most common devices. |
| **Session Length & Engine Uptime** | Average minutes the background camera engine remains active | Detects early if background killer is affecting users. |

---

### Module 2: Remote Config & Live Gesture Tuning Engine 🎛️
*Objective: Change gesture thresholds and physics instantly on all users' phones without building a new APK.*

This is the **single most powerful tool** for the App Owner. When users complain that "scroll is too fast" or "like takes too long", you simply change a slider in the Admin Panel and click **Publish Config**. The phone fetches the new values on launch.

#### Configurable Parameters in Admin Panel:
1. **Scroll Distance & Speed Multiplier**:
   - `scroll_speed_multiplier`: Range `1.0` to `3.0` (Default: `2.0`).
   - `scroll_hold_delay_seconds`: Range `0.1s` to `0.8s` (Default: `0.3s`).
2. **Thumbs-Up Like Trigger Duration**:
   - `like_hold_duration_seconds`: Range `1.0s` to `2.5s` (Default: `1.5s`).
3. **Air Tap & Pinch Sensitivity**:
   - `pinch_threshold_distance`: Range `0.04` to `0.09` (Default: `0.065`).
4. **Floating HUD Default Dimensions**:
   - `default_hud_size_dp`: `50` to `88` (Default: `52px`).
   - `default_hud_opacity`: `15%` to `100%` (Default: `60%`).
5. **Thermal Protection & FPS Cap**:
   - `max_camera_fps`: `24`, `30`, `60` FPS.
   - `model_complexity`: `0` (Lite) vs `1` (Full).

---

### Module 3: OTA Updates, Versioning & Emergency Kill-Switch 🚀
*Objective: Keep all users on the latest version and prevent broken app experiences.*

1. **Version Control Card**:
   - **Latest Version**: e.g., `v2.5.0` (Build `25`).
   - **Minimum Supported Version**: e.g., `v2.4.0`.
   - **APK Download URL**: Direct link to Play Store or Google Drive APK mirror.
2. **Update Prompts**:
   - **Soft Update**: Shows an attractive dismissible banner: *"AeroSense AI v2.5 is here with 2x smoother scrolling! Tap to update."*
   - **Force Update**: Mandatory full-screen modal blocking older versions if a breaking Android OS bug is patched.
3. **Dynamic Changelog Editor**:
   - Rich-text editor to write update bullet points in Hindi and English. App displays this modal on user's first launch after updating.
4. **Emergency Kill-Switch / Maintenance Mode**:
   - If an unexpected third-party library or Android security patch breaks tracking, the owner can flip a single toggle to activate **"Maintenance Mode"**, notifying users politely without crashing their phones.

---

### Module 4: Crash Vitals, ANR & Camera Diagnostics Monitor 🚨
*Objective: Detect and fix crashes before bad 1-star reviews hit the Play Store.*

1. **Camera Hardware Health**:
   - Track `NotReadableError` (Camera claimed by another app).
   - Track `OverconstrainedError` (Unsupported camera resolution).
   - Track `MediaStreamTrack.onended` frequency (OS cutting camera stream).
2. **Android Background Process Terminations (LMK)**:
   - Logs when the OS kills `AeroForegroundService` due to memory pressure or missing battery exemptions.
3. **Device-Specific Incompatibility Alerts**:
   - Automatically clusters errors by phone model (e.g. *"Xiaomi MIUI aggressively killing overlay on Redmi Note 12"*).
4. **Crash-Free User Rate Gauge**:
   - Visual gauge targeting `> 99.5%` Play Store standard.

---

### Module 5: Push Notifications & In-App Broadcast Center 📢
*Objective: Engage users, announce tips, and remind them to keep gestures active.*

1. **Broadcast Push Notifications (Firebase Cloud Messaging - FCM)**:
   - **Title**: e.g., *"💡 Pro Tip: Fold your index finger to auto-scroll Reels!"*
   - **Body**: Detailed instruction with deep-link to the in-app gesture guide.
   - **Targeting**:
     - *All Users*
     - *Users with Accessibility permission OFF* (prompting them to enable it)
     - *Inactive Users for > 3 days*
2. **Scheduled Notifications**:
   - Automatic greeting or weekend reminder.

---

### Module 6: User Feedback, Gesture Ratings & Support Desk 💬
*Objective: Hear directly from users on what feels natural and what needs improvement.*

1. **In-App Feedback Inbox**:
   - Displays user submissions from the "Help & Feedback" button.
   - Includes auto-attached diagnostic metadata: Android OS, Phone Brand, Camera FPS, Screen Resolution, Current Config.
2. **Gesture Accuracy Satisfaction Poll**:
   - Live 1–5 star rating by users for each gesture (Scroll satisfaction, Tap satisfaction, Like satisfaction).
3. **Feature Request Voting Board**:
   - App users can vote on upcoming features (e.g., *"Add TikTok Double-Swipe"*, *"Add Zoom Pinch Gesture"*).

---

## 🗄️ 4. Data Models & JSON API Specifications

Below are the exact JSON contracts that connect the Admin Panel to the mobile app:

### 1. `remote_config.json` (Fetched on App Launch)
```json
{
  "status": "success",
  "config_version": 104,
  "updated_at": "2026-11-03T18:00:00Z",
  "tuning": {
    "scroll_distance_default": 3,
    "scroll_hold_delay_seconds": 0.3,
    "like_hold_duration_seconds": 1.5,
    "pointer_speed_default": 3,
    "pinch_threshold_distance": 0.065,
    "model_complexity": 0,
    "camera_fps_target": 30
  },
  "hud": {
    "default_size_px": 52,
    "min_size_px": 50,
    "max_size_px": 88,
    "default_opacity": 60
  },
  "features": {
    "enable_voice_commands": true,
    "enable_hold_menu": true,
    "enable_quick_like": true,
    "enable_stealth_fist": true
  }
}
```

---

### 2. `app_version.json` (OTA Versioning & Maintenance)
```json
{
  "latest_version": "2.5.0",
  "latest_version_code": 25,
  "min_required_version_code": 22,
  "force_update": false,
  "maintenance_mode": false,
  "maintenance_message": "AeroSense AI servers are upgrading for 15 minutes. Touchless gestures will resume shortly.",
  "download_url": "https://play.google.com/store/apps/details?id=com.aerosense.touchless.ai",
  "changelog": [
    "🚀 2x Faster Quick Reel Scroll with 0.3s repeat pause",
    "👍 Instant Thumbs-Up Like on Reels & Shorts (1.5s)",
    "🎛️ Unified Bento Card Slider UI with instant reset",
    "🔋 24-Hour Keep-Alive Background Service & Battery Exemption"
  ]
}
```

---

### 3. `client_telemetry_event.json` (Sent from Mobile App to Admin)
```json
{
  "event_type": "gesture_triggered",
  "gesture_name": "SMART_SCROLL_DOWN",
  "device_info": {
    "manufacturer": "Samsung",
    "model": "Galaxy S23",
    "android_version": 14,
    "screen_resolution": "1080x2340",
    "battery_level": 78
  },
  "performance": {
    "camera_fps": 31.4,
    "mediapipe_latency_ms": 18.2,
    "engine_uptime_minutes": 42
  },
  "timestamp": "2026-11-03T18:05:12Z"
}
```

---

## 🛠️ 5. Implementation Roadmap for App Owner

```mermaid
flowchart TD
    A["Phase 1: Local Admin Console (Offline Mockup)"] --> B["Phase 2: Free Cloud Backend (Firebase / Supabase)"]
    B --> C["Phase 3: Live Remote Config & OTA Sync"]
    C --> D["Phase 4: FCM Push Notifications & Telemetry"]
    D --> E["Phase 5: Automated Scaling & Monetization"]

    style A fill:#1e293b,stroke:#06b6d4,stroke-width:2px,color:#fff
    style B fill:#1e293b,stroke:#3b82f6,stroke-width:2px,color:#fff
    style C fill:#1e293b,stroke:#10b981,stroke-width:2px,color:#fff
    style D fill:#1e293b,stroke:#f59e0b,stroke-width:2px,color:#fff
    style E fill:#1e293b,stroke:#8b5cf6,stroke-width:2px,color:#fff
```

### Phase 1: Master Web Admin Console (Local Development Mode)
- Main development file: [admin.html](file:///c:/Aero%20sense%20ai/admin.html) with modern dark squircle styling, interactive KPI cards, live Remote Config sliders, OTA version editor, FCM push broadcaster, and feedback inbox.
- **Firebase Hosting Status**: Temporarily disabled while local features and UI enhancements are being built.
- **Mobile App Clean Separation**: Admin tab successfully removed from mobile app bottom navigation, leaving a clean 3-tab consumer layout (`Home`, `Settings`, `Gestures`).

### Phase 2: Live Firebase Backend Setup (✅ COMPLETED & LIVE)
- **Firebase Project Name**: `AeroSense AI`
- **Project ID**: `aerosense-ai-touchless`
- **Project Number**: `876494618360`
- **Web App ID**: `1:876494618360:web:95b014d820d06560a6850d`
- **Web API Key**: `AIzaSyBi4jskQ85jlfltpnAf5I82zS-zTRMUyjE`
- **Android Package**: `com.aerosense.touchless.ai` (Registered with [google-services.json](file:///c:/Aero%20sense%20ai/android/app/google-services.json))
- **Firestore Database**: `(default)` deployed in region `nam5` with security rules.
- **Active Collections & Documents**:
  - `aerosense_config/remote_config`: Live gesture physics & HUD dimensions.
  - `aerosense_config/ota_version`: Live versioning & changelog manifest.

### Phase 3: Mobile App Auto-Sync (✅ COMPLETED & LIVE)
- In [index.html](file:///c:/Aero%20sense%20ai/index.html) and [assets/index.html](file:///c:/Aero%20sense%20ai/android/app/src/main/assets/index.html), on startup the non-blocking background functions `syncAeroRemoteConfig()` and `checkAeroOtaUpdate()` run 2.5s after launch.
- Automatically applies owner's remote changes to `aeroConfig` and shows in-app update prompts without re-releasing APKs.
- 100% offline-safe with local storage fallback.

---

## 🔐 6. Security & Role-Based Access Control (RBAC)

1. **Authentication**: Admin panel must be protected by Google OAuth or email + 2FA (Two-Factor Authentication).
2. **Role Hierarchy**:
   - **Super Admin (Owner - Shuaib Jaxon)**: Full access, can change Remote Config, publish OTA, send pushes, and view financials.
   - **Support / QA Engineer**: View crash logs, read user feedback, test beta gesture configs.
3. **Audit Trail**: Every change to Remote Config or OTA version is logged with timestamp and author name to prevent accidental misconfigurations.

---

## 📋 7. Summary of Benefits for App Owner

1. **Zero Downtime Updates**: Modify gesture sensitivities without making users download a 30MB APK update.
2. **Instant Emergency Response**: If an Android OS update causes camera lag, immediately throttle camera FPS or switch model complexity remotely.
3. **Data-Driven Feature Development**: Build what users actually use by analyzing the Gesture Adoption Heatmap.
4. **High Play Store Ratings**: Catch camera freezes and crash bugs before they turn into 1-star reviews.

