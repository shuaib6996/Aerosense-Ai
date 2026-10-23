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

public class AeroAccessibilityService extends AccessibilityService {

    public static AeroAccessibilityService instance;
    private WindowManager windowManager;
    private FrameLayout cursorContainer;
    private View cursorInnerDot;
    private WindowManager.LayoutParams cursorParams;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int screenWidth = 1080;
    private int screenHeight = 2400;
    private float smoothX = 540f;
    private float smoothY = 1200f;
    private long lastGestureDispatchTime = 0;
    private volatile boolean isCursorEnabled = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        updateScreenDimensions();
        smoothX = screenWidth / 2f;
        smoothY = screenHeight / 2f;
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
                ring.setColor(Color.parseColor("#3822C55E"));
                ring.setStroke(5, Color.parseColor("#22D3EE"));
                cursorContainer.setBackground(ring);

                cursorInnerDot = new View(this);
                GradientDrawable dot = new GradientDrawable();
                dot.setShape(GradientDrawable.OVAL);
                dot.setColor(Color.parseColor("#22C55E"));
                cursorInnerDot.setBackground(dot);

                FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(18, 18, Gravity.CENTER);
                cursorContainer.addView(cursorInnerDot, dotParams);

                int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
                        ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                        : WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

                cursorParams = new WindowManager.LayoutParams(
                        70,
                        70,
                        overlayType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );
                cursorParams.gravity = Gravity.TOP | Gravity.START;
                cursorParams.x = screenWidth / 2;
                cursorParams.y = screenHeight / 2;
                cursorContainer.setVisibility(View.GONE);
                windowManager.addView(cursorContainer, cursorParams);
            } catch (Exception ignored) {}
        });
    }

    public void setCursorVisibility(boolean visible) {
        this.isCursorEnabled = visible;
        mainHandler.post(() -> {
            if (cursorContainer != null) {
                cursorContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void updateCursorPosition(float normX, float normY, boolean isPinching) {
        if (!isCursorEnabled) return;
        mainHandler.post(() -> {
            if (cursorContainer == null || cursorParams == null || windowManager == null || !isCursorEnabled) return;
            try {
                if (cursorContainer.getVisibility() != View.VISIBLE) {
                    cursorContainer.setVisibility(View.VISIBLE);
                }
                updateScreenDimensions();
                float targetX = normX * screenWidth;
                float targetY = normY * screenHeight;

                smoothX += (targetX - smoothX) * 0.45f;
                smoothY += (targetY - smoothY) * 0.45f;

                cursorParams.x = Math.max(0, Math.min(screenWidth - 70, (int) smoothX - 35));
                cursorParams.y = Math.max(0, Math.min(screenHeight - 70, (int) smoothY - 35));

                GradientDrawable ring = (GradientDrawable) cursorContainer.getBackground();
                if (isPinching) {
                    ring.setColor(Color.parseColor("#AA10B981"));
                    ring.setStroke(7, Color.parseColor("#10B981"));
                } else {
                    ring.setColor(Color.parseColor("#3822C55E"));
                    ring.setStroke(5, Color.parseColor("#22D3EE"));
                }
                windowManager.updateViewLayout(cursorContainer, cursorParams);
            } catch (Exception ignored) {}
        });
    }

    public void performAirTap(float normX, float normY) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        long now = System.currentTimeMillis();
        if (now - lastGestureDispatchTime < 300) return;
        lastGestureDispatchTime = now;

        updateScreenDimensions();
        float tapX = Math.max(5f, Math.min(screenWidth - 5f, normX * screenWidth));
        float tapY = Math.max(5f, Math.min(screenHeight - 5f, normY * screenHeight));

        Path clickPath = new Path();
        clickPath.moveTo(tapX, tapY);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, 60));
        dispatchGesture(builder.build(), null, null);
    }

    public void performAirSwipe(String direction) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        long now = System.currentTimeMillis();
        if (now - lastGestureDispatchTime < 420) return;
        lastGestureDispatchTime = now;

        updateScreenDimensions();
        float centerX = screenWidth * 0.5f;
        float startY = screenHeight * 0.78f;
        float endY = screenHeight * 0.22f;

        Path swipePath = new Path();
        if ("UP".equalsIgnoreCase(direction) || "NEXT".equalsIgnoreCase(direction)) {
            swipePath.moveTo(centerX, startY);
            swipePath.lineTo(centerX, endY);
        } else {
            swipePath.moveTo(centerX, endY);
            swipePath.lineTo(centerX, startY);
        }

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 240));
        dispatchGesture(builder.build(), null, null);
    }

    public void performBackGesture() {
        mainHandler.post(() -> {
            try {
                boolean executed = performGlobalAction(GLOBAL_ACTION_BACK);
                if (!executed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    updateScreenDimensions();
                    Path backSwipe = new Path();
                    backSwipe.moveTo(10f, screenHeight * 0.5f);
                    backSwipe.lineTo(screenWidth * 0.35f, screenHeight * 0.5f);
                    GestureDescription.Builder builder = new GestureDescription.Builder();
                    builder.addStroke(new GestureDescription.StrokeDescription(backSwipe, 0, 180));
                    dispatchGesture(builder.build(), null, null);
                }
            } catch (Exception ignored) {}
        });
    }

    public void performHomeGesture() {
        mainHandler.post(() -> {
            try {
                performGlobalAction(GLOBAL_ACTION_HOME);
            } catch (Exception ignored) {}
        });
    }

    public void performGlobalPhoneAction(String action) {
        mainHandler.post(() -> {
            try {
                if ("HOME".equalsIgnoreCase(action)) {
                    performHomeGesture();
                } else if ("BACK".equalsIgnoreCase(action)) {
                    performBackGesture();
                } else if ("RECENTS".equalsIgnoreCase(action)) {
                    performGlobalAction(GLOBAL_ACTION_RECENTS);
                } else if ("NOTIFICATIONS".equalsIgnoreCase(action)) {
                    performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
                }
            } catch (Exception ignored) {}
        });
    }

    public void injectAirTypedText(String typedText, boolean submitAfterTyping) {
        mainHandler.post(() -> {
            try {
                AeroAirKeyboardAccessibilityPatch.injectTextIntoFocusedAppInput(this, typedText, submitAfterTyping);
            } catch (Exception ignored) {}
        });
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
