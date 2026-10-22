# AeroSense AI™ — Air Gesture & Voice Commands Guide 🖐️🎙️

AeroSense AI uses high-performance computer vision powered by MediaPipe to detect and track 21 hand landmarks in real-time at 60 FPS using only standard front-facing smartphone or laptop webcams.

---

## 🖐️ Hand Gestures Cheat Sheet

### 1. 👌 Index + Thumb Air Pinch
- **How to perform**: Bring your Index fingertip (Landmark #8) and Thumb fingertip (Landmark #4) together within the sensitivity threshold.
- **Default Action**: **Play / Pause** active stream, audio, or timer.
- **Air Pointer**: In Vision mode, moving your index fingertip moves the glowing cyan Air Cursor. Pinching performs an interactive virtual click on any on-screen button.

---

### 2. 👆 Air Swipe Up (Flick Upward)
- **How to perform**: Move your extended index finger rapidly upwards.
- **Default Action**:
  - **StreamDeck**: Advance to the **Next Video Reel**.
  - **SonicWave**: Jump to the **Next Music Track**.
  - **Chef Reader**: Advance to the **Next Recipe Step**.

---

### 3. 👇 Air Swipe Down (Flick Downward)
- **How to perform**: Move your extended index finger rapidly downwards.
- **Default Action**:
  - **StreamDeck**: Return to the **Previous Video Reel**.
  - **SonicWave**: Return to the **Previous Music Track**.
  - **Chef Reader**: Go back to the **Previous Recipe Step**.

---

### 4. ✌️ Peace Sign (Two Fingers V-Sign)
- **How to perform**: Extend your index and middle fingers while keeping ring and pinky fingers folded.
- **Default Action**: Trigger **Instant Like / Boost Burst ⚡** on the current stream.
- **Customizable**: Can be mapped in the **Auto Lab** tab to take an Air Snapshot or toggle Audio Mute.

---

### 5. ✊ Closed Fist Gesture
- **How to perform**: Fold all fingers tightly into a closed fist.
- **Default Action**: **Privacy Stealth Lock**.
  - Immediately blanks the screen to absolute black.
  - Pauses all media playback.
  - To wake the screen, present an **Open Palm ✋**, **Pinch 👌**, or tap anywhere.

---

### 6. 🤖 Smart Presence Guard (Auto-Pause)
- When enabled in the **Auto Lab** tab, AeroSense monitors hand visibility.
- If no hand is detected in camera view for **8 continuous seconds**, media automatically pauses to preserve battery and prevent missing content.

---

## 🎙️ Hands-Free Voice Commands

Click the **Voice** toggle in the top header bar to enable the speech recognition engine.

Supported spoken keywords:

| Spoken Phrase | Triggered Action |
| :--- | :--- |
| `"Next"` or `"Skip"` | Advance to next stream / song / step |
| `"Back"` or `"Previous"` | Return to previous item |
| `"Pause"` or `"Stop"` | Pause media playback |
| `"Play"` or `"Resume"` | Resume playback |
| `"Like"` or `"Love"` | Add to favorites with like burst |
| `"Louder"` or `"Volume Up"` | Increase volume by +15% |
| `"Mute"` | Toggle audio mute |

---

## ⚙️ Customizing Gestures in the Auto Lab

Navigate to the **Auto Lab** tab at the bottom to:
1. Re-map any gesture (Pinch, Peace, Fist) to your preferred action.
2. Adjust the pinch distance sensitivity slider (from `0.03` for tight pinches to `0.10` for loose detection).
3. Toggle the 8-second Smart Presence Guard on or off.
