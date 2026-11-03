# AeroSense AI — Privacy Policy & Terms of Service 🔒🛡️

**Effective Date:** October 21, 2026  
**Last Updated:** November 2, 2026  
**Application Name:** AeroSense AI  
**Developer / Publisher:** AeroSense AI Labs  
**Package / Application ID:** `com.aerosense.touchless.ai`  

---

## 1. Introduction

AeroSense AI Labs ("we", "us", or "our") respects the privacy of our users ("user", "you"). This Privacy Policy explains how our mobile application and Progressive Web App (**AeroSense AI**) collect, use, and protect your information when using touchless camera gesture recognition, voice commands, and accessibility services.

---

## 2. 100% On-Device Camera Processing (Google Play Policy Compliance)

- **Purpose of Camera Access**: AeroSense AI requests camera permission (`android.permission.CAMERA`) strictly for detecting 21 real-time hand landmark coordinates to interpret touchless air gestures (e.g., pinch, swipe, like, fold).
- **No Video Recording or Image Transmission**: Camera video frames are processed ephemerally and client-side on your local device CPU/GPU utilizing Google MediaPipe.
- **Zero Cloud Storage**: **No video frames, camera pictures, or biometric facial/hand data are ever recorded, saved to disk, or transmitted to any external server, cloud service, or third party.**
- **Camera Standby Mode**: When gesture tracking is toggled off, the camera stream immediately terminates and hardware sensors are released.

---

## 3. Android Accessibility Service Disclosure

AeroSense AI uses the Android Accessibility Service API (`android.accessibilityservice.AccessibilityService`) strictly to:
- Perform touchless navigation gestures on behalf of the user, such as scrolling reels/feeds, performing back navigation, opening the home screen, or virtual tap simulation.
- **Data Protection**: The Accessibility Service does not collect, log, read, monitor, or transmit any user screen content, keystrokes, personal communications, or sensitive information.

---

## 4. Local Storage & Privacy

AeroSense AI uses local browser storage (`localStorage` / `CacheStorage`) strictly for:
- Caching offline application assets (`index.html`, `admin.html`, `manifest.json`, `sw.js`).
- Storing user preferences (e.g., custom gesture sensitivities, HUD size, scroll speeds).

No third-party trackers, invasive advertising SDKs, or unauthorized profiling cookies are embedded in the application.

---

## 5. Security & Safety

- **Safety Warning**: Users are cautioned not to use touchless gestures or distracted interaction while driving a motor vehicle, operating heavy machinery, or engaging in activities where full physical attention is required.
- **Children's Privacy**: AeroSense AI does not knowingly collect personally identifiable information from children under the age of 13.

---

## 6. Contact Information

For inquiries regarding this Privacy Policy or your data rights, contact:  
**AeroSense AI Labs Legal Team**  
- Email: privacy@aerosense.ai  
- Website: https://aerosense.ai
