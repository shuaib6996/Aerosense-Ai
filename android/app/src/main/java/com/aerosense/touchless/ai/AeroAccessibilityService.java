package com.aerosense.touchless.ai;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

/**
 * AeroAccessibilityService allows AeroSense AI to:
 * 1. Show a glowing Air Cursor over Instagram, YouTube, WhatsApp, and the Android Home Screen.
 * 2. Dispatch real physical Air Taps (Pinch 👌) on any icon or button (e.g., Instagram Reels tab).
 * 3. Dispatch real physical Air Swipes (👆/👇) to scroll Instagram Reels, YouTube Shorts, and web pages.
 * 4. Execute System Home, Back, Recents, and Notification actions anywhere on the phone.
 */
public class AeroAccessibilityService extends AccessibilityService {

    public static AeroAccessibilityService instance;
    private WindowManager windowManager;
    private FrameLayout cursorContainer;
    private View cursorInnerDot;
    private WindowManager.LayoutParams cursorParams;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int screenWidth = 1080;
    private int screenHeight = 2400;
    private long lastGestureDispatchTime = 0;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        updateScreenDimensions();
        createSystemAirCursorOverlay();
    }

    private void updateScreenDimensions() {
        if (windowManager != null) {
            DisplayMetrics dm = new DisplayMetrics();
            windowManager.getDefaultDisplay().getRealMetrics(dm);
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;
        }
    }

    private void createSystemAirCursorOverlay() {
        mainHandler.post(() -> {
            if (windowManager == null || cursorContainer != null) return;
            try {
                cursorContainer = new FrameLayout(this);
                GradientDrawable ring = new GradientDrawable();
                ring.setShape(GradientDrawable.OVAL);
                ring.setColor(Color.parseColor("#4422D3EE"));
                ring.setStroke(6, Color.parseColor("#22D3EE"));
                cursorContainer.setBackground(ring);

                cursorInnerDot = new View(this);
                GradientDrawable dot = new GradientDrawable();
                dot.setShape(GradientDrawable.OVAL);
                dot.setColor(Color.WHITE);
                cursorInnerDot.setBackground(dot);

                FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(16, 16, Gravity.CENTER);
                cursorContainer.addView(cursorInnerDot, dotParams);

                int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
                        ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                        : WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

                cursorParams = new WindowManager.LayoutParams(
                        76,
                        76,
                        overlayType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );
                cursorParams.gravity = Gravity.TOP | Gravity.START;
                cursorParams.x = screenWidth / 2;
                cursorParams.y = screenHeight / 2;

                windowManager.addView(cursorContainer, cursorParams);
            } catch (Exception ignored) {}
        });
    }

    public void updateCursorPosition(float normX, float normY, boolean isPinching) {
        mainHandler.post(() -> {
            if (cursorContainer == null || cursorParams == null || windowManager == null) return;
            try {
                updateScreenDimensions();
                int targetX = (int) (normX * screenWidth) - 38;
                int targetY = (int) (normY * screenHeight) - 38;
                cursorParams.x = Math.max(0, Math.min(screenWidth - 76, targetX));
                cursorParams.y = Math.max(0, Math.min(screenHeight - 76, targetY));

                GradientDrawable ring = (GradientDrawable) cursorContainer.getBackground();
                if (isPinching) {
                    ring.setColor(Color.parseColor("#AA10B981"));
                    ring.setStroke(8, Color.parseColor("#10B981"));
                } else {
                    ring.setColor(Color.parseColor("#4422D3EE"));
                    ring.setStroke(6, Color.parseColor("#22D3EE"));
                }
                windowManager.updateViewLayout(cursorContainer, cursorParams);
            } catch (Exception ignored) {}
        });
    }

    public void performAirTap(float normX, float normY) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        long now = System.currentTimeMillis();
        if (now - lastGestureDispatchTime < 320) return;
        lastGestureDispatchTime = now;

        updateScreenDimensions();
        float tapX = Math.max(10f, Math.min(screenWidth - 10f, normX * screenWidth));
        float tapY = Math.max(10f, Math.min(screenHeight - 10f, normY * screenHeight));

        Path clickPath = new Path();
        clickPath.moveTo(tapX, tapY);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, 65));
        dispatchGesture(builder.build(), null, null);
    }

    public void performAirSwipe(String direction) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        long now = System.currentTimeMillis();
        if (now - lastGestureDispatchTime < 450) return;
        lastGestureDispatchTime = now;

        updateScreenDimensions();
        float centerX = screenWidth * 0.5f;
        float startY = screenHeight * 0.78f;
        float endY = screenHeight * 0.22f;

        Path swipePath = new Path();
        if ("UP".equalsIgnoreCase(direction) || "NEXT".equalsIgnoreCase(direction)) {
            // Scroll DOWN to NEXT Reel / Short (finger moves from bottom to top)
            swipePath.moveTo(centerX, startY);
            swipePath.lineTo(centerX, endY);
        } else {
            // Scroll UP to PREVIOUS Reel / Short (finger moves from top to bottom)
            swipePath.moveTo(centerX, endY);
            swipePath.lineTo(centerX, startY);
        }

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 260));
        dispatchGesture(builder.build(), null, null);
    }

    public void performGlobalPhoneAction(String action) {
        if ("HOME".equalsIgnoreCase(action)) {
            performGlobalAction(GLOBAL_ACTION_HOME);
        } else if ("BACK".equalsIgnoreCase(action)) {
            performGlobalAction(GLOBAL_ACTION_BACK);
        } else if ("RECENTS".equalsIgnoreCase(action)) {
            performGlobalAction(GLOBAL_ACTION_RECENTS);
        } else if ("NOTIFICATIONS".equalsIgnoreCase(action)) {
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (windowManager != null && cursorContainer != null) {
            try { windowManager.removeView(cursorContainer); } catch (Exception ignored) {}
        }
    }
}
