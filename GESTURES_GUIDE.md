# AeroSense AI — Air Gestures & Controls Guide 🖐️📖

AeroSense AI uses high-performance computer vision powered by MediaPipe to detect and track 21 hand landmarks in real-time at 60 FPS using standard front-facing smartphone cameras.

---

## 🖐️ Core Air Gestures Cheat Sheet

### 1. 👆 Index + Thumb Air Pinch (Air Tap / Click)
- **How to perform**: Bring your Index fingertip (Landmark #8) and Thumb fingertip (Landmark #4) together within the sensitivity threshold.
- **Default Action**: Virtual click on any interactive button, icon, link, or media play/pause.
- **Air Pointer**: In Vision mode, moving your index fingertip moves the glowing cyan Air Cursor across the screen. Pinching executes an instant click at the cursor coordinates.

---

### 2. ↕️ Index Point & Fold Reel Scroll (Quick Scroll)
- **Open Palm (✋)**: Standby posture with green palm silhouette indicator.
- **Index Point (☝️)**: Point your single index finger up (other fingers folded) — immediately arms the scroll trigger.
- **Fold to Scroll Down / Next Reel (👇)**: Fold your index finger down into a loose fist. Instantly triggers a smooth swipe to the **Next Reel / Next Video** on Instagram, YouTube Shorts, or TikTok.
- **Repeat Hold for Continuous Scroll**: Keeping your finger folded auto-scrolls continuously with an optimized **Repeat Hold Pause of 0.3s** (customizable from 0.1s to 0.8s in Settings).
- **Two Scroll Distances**:
  - `Quick Scroll` (Default): Fast, responsive reel switching.
  - `Calm Snap`: Gentle snap for photo galleries and articles.

---

### 3. 👍 Thumbs-Up Like Gesture
- **How to perform**: Simply show your Thumbs-Up 👍 sign facing the camera and hold steady for 1.5s (no need to open hand first).
- **Default Action**: Triggers an instant like / heart reaction on YouTube, Instagram Reels, Facebook, and short video feeds.
- **Hold Duration**: 1.5s default (customizable from 0.0s to 5.0s in Settings).
- **Haptic / Tone**: Plays an interactive audio tone upon successful trigger.

---

### 4. 🖐️ Radial Hold Menu
- **How to perform**: Present an open palm facing the camera and hold steadily.
- **Default Action**: Opens the quick radial floating action menu.
- **Available Actions**:
  - ⬅️ **Global Back**
  - 🏠 **Global Home**
  - 📑 **Recents Apps**
  - 🔔 **Notification Panel**
  - ⚙️ **Settings Quick Jump**

---

### 5. ✊ Closed Fist (Privacy Stealth Mode)
- **How to perform**: Fold all five fingers tightly into a closed fist.
- **Default Action**: **Privacy Stealth Lock**.
  - Instantly blacks out the screen.
  - Pauses media playback for complete privacy.
  - To wake the screen, present an **Open Palm (✋)** or tap anywhere.

---

## 🪟 Resizable Floating Hand HUD

AeroSense AI features a lightweight floating transparent window that sits seamlessly on top of third-party apps:
- **Size Range**: 50px (Mini) to 88px (Max Limit). Default recommended size is **52px**.
- **Real-time Feedback**: Live visualization of detected 21 hand landmarks, active gesture names, and confidence meters.
- **Minimalist Footprint**: Non-intrusive rounded squircle design with frosted glass effect.
- **Adjusting HUD Size**: Use the slider in **Settings → Indicator → Popup HUD Size**, or tap the `HUD: 52px` header pill.

---

## 🎛️ Customizing Gestures in Settings

Navigate to the **Settings** tab (`#view-settings`) to fine-tune your touchless experience across three dedicated panels:

### 1. Gestures Panel (`#panel-gestures`)
- **Performance**: Battery Saver (1) to Faster Response (5).
- **Sensitivity**: Low (1) to Ultra (5). Default: 3.
- **Scroll Distance (Speed)**: Calm Snap (1), Balanced Feed (2), or Quick Scroll (3). Default: Quick Scroll (3).
- **Repeat Hold Pause**: 0.1s (Fast) to 0.8s (Slow). Default: 0.3s.
- **Pointer Speed**: Slow (1) to Fast (5). Default: 3.
- **Popup Hand Size**: Compact (0.6x) to Large (1.6x). Default: 1.0x.
- **Like Gesture Hold Delay**: 1.0s to 2.0s. Default: 1.5s.
- **Like Gesture Sensitivity**: Stable (1) to Sensitive (5). Default: 3.
- **Double Tap Speed**: Fast (150ms) to Slow (450ms). Default: 250ms.
- **Volume Hold Delay**: Instant (0.1s) to Slow (0.5s). Default: 0.2s.
- **Hand Sign Activation Delay**: 0.0s to 2.0s. Default: 1.0s.
- **Hand Sign Sensitivity**: Stable (1) to Sensitive (5). Default: 3.

### 2. Indicator Panel (`#panel-indicator`)
- **Popup HUD Size**: 50px to 88px. Default: 52px.
- **Camera Live Preview**: Toggle preview feed on/off.
- **Indicator Frequency**: Never (1), On Gesture (2), Always (4).
- **Indicator Opacity**: Transparent (15%) to Solid (100%). Default: 60%.
- **Indicator Size**: Small (80px) to Big (200px). Default: 120px.
- **Horizontal & Vertical Position**: Place indicator anywhere on screen.

### 3. Controls Panel (`#panel-controls`)
- **Interface Style**: Island, Compact, or Full.
- **Interface Width & Height**: Sliders with live preview.
- **Positioning**: Center, Left, Right, Top, or Bottom.
