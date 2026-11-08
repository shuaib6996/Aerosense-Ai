package com.aerosense.touchless.ai;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
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
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.content.Context;
import android.media.AudioManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AeroAccessibilityService extends AccessibilityService {

    public static AeroAccessibilityService instance;
    private WindowManager windowManager;
    private FrameLayout cursorContainer;
    private View cursorInnerDot;
    private WindowManager.LayoutParams cursorParams;
    private FrameLayout cursorContainer2;
    private View cursorInnerDot2;
    private WindowManager.LayoutParams cursorParams2;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int screenWidth = 1080;
    private int screenHeight = 2400;
    private int usableHeight = 2400;
    private int navBarHeight = 0;
    private float smoothX = 540f;
    private float smoothY = 1200f;
    private float smoothX2 = 540f;
    private float smoothY2 = 1200f;
    private long lastGestureDispatchTime = 0;
    private volatile boolean isCursorEnabled = false;
    private volatile boolean isTwoHandModeEnabled = false;
    private final AtomicBoolean isCursor1UpdatePending = new AtomicBoolean(false);
    private final AtomicBoolean isCursor2UpdatePending = new AtomicBoolean(false);
    private volatile float latestNormX1 = 0.5f;
    private volatile float latestNormY1 = 0.5f;
    private volatile boolean latestIsPinching1 = false;
    private volatile float latestNormX2 = 0.5f;
    private volatile float latestNormY2 = 0.5f;
    private volatile boolean latestIsPinching2 = false;
    private boolean hasHandHistory1 = false;
    private boolean hasHandHistory2 = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        updateScreenDimensions();
        smoothX = screenWidth / 2f;
        smoothY = screenHeight / 2f;
        hasHandHistory1 = false;
        hasHandHistory2 = false;
        createSystemAirCursorOverlay();
    }

    private void updateScreenDimensions() {
        if (windowManager != null) {
            DisplayMetrics dm = new DisplayMetrics();
            windowManager.getDefaultDisplay().getRealMetrics(dm);
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;

            DisplayMetrics appDm = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(appDm);
            usableHeight = appDm.heightPixels;
            navBarHeight = Math.max(0, screenHeight - usableHeight);
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

                // Cursor 2: Secondary Hand (Amber / Gold Accent for Two-Hand Typing)
                cursorContainer2 = new FrameLayout(this);
                GradientDrawable ring2 = new GradientDrawable();
                ring2.setShape(GradientDrawable.OVAL);
                ring2.setColor(Color.parseColor("#38F59E0B"));
                ring2.setStroke(5, Color.parseColor("#FBBF24"));
                cursorContainer2.setBackground(ring2);

                cursorInnerDot2 = new View(this);
                GradientDrawable dot2 = new GradientDrawable();
                dot2.setShape(GradientDrawable.OVAL);
                dot2.setColor(Color.parseColor("#F59E0B"));
                cursorInnerDot2.setBackground(dot2);

                FrameLayout.LayoutParams dotParams2 = new FrameLayout.LayoutParams(18, 18, Gravity.CENTER);
                cursorContainer2.addView(cursorInnerDot2, dotParams2);

                cursorParams2 = new WindowManager.LayoutParams(
                        70,
                        70,
                        overlayType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );
                cursorParams2.gravity = Gravity.TOP | Gravity.START;
                cursorParams2.x = screenWidth / 2;
                cursorParams2.y = screenHeight / 2;
                cursorContainer2.setVisibility(View.GONE);
                windowManager.addView(cursorContainer2, cursorParams2);
            } catch (Exception ignored) {}
        });
    }

    private final Runnable cursor1UpdateRunnable = new Runnable() {
        @Override
        public void run() {
            isCursor1UpdatePending.set(false);
            if (cursorContainer == null || cursorParams == null || windowManager == null || !isCursorEnabled) return;
            try {
                if (cursorContainer.getVisibility() != View.VISIBLE) {
                    cursorContainer.setVisibility(View.VISIBLE);
                }
                updateScreenDimensions();
                float targetX = latestNormX1 * screenWidth;
                float targetY = latestNormY1 * screenHeight;

                if (!hasHandHistory1) {
                    smoothX = targetX;
                    smoothY = targetY;
                    hasHandHistory1 = true;
                } else {
                    smoothX += (targetX - smoothX) * 0.45f;
                    smoothY += (targetY - smoothY) * 0.45f;
                }

                cursorParams.x = Math.max(0, Math.min(screenWidth - 70, (int) smoothX - 35));
                cursorParams.y = Math.max(0, Math.min(screenHeight - 70, (int) smoothY - 35));

                GradientDrawable ring = (GradientDrawable) cursorContainer.getBackground();
                if (latestIsPinching1) {
                    ring.setColor(Color.parseColor("#AA10B981"));
                    ring.setStroke(7, Color.parseColor("#10B981"));
                } else {
                    ring.setColor(Color.parseColor("#3822C55E"));
                    ring.setStroke(5, Color.parseColor("#22D3EE"));
                }
                windowManager.updateViewLayout(cursorContainer, cursorParams);
            } catch (Exception ignored) {}
        }
    };

    private final Runnable cursor2UpdateRunnable = new Runnable() {
        @Override
        public void run() {
            isCursor2UpdatePending.set(false);
            if (cursorContainer2 == null || cursorParams2 == null || windowManager == null || !isCursorEnabled || !isTwoHandModeEnabled) return;
            try {
                if (cursorContainer2.getVisibility() != View.VISIBLE) {
                    cursorContainer2.setVisibility(View.VISIBLE);
                }
                updateScreenDimensions();
                float targetX = latestNormX2 * screenWidth;
                float targetY = latestNormY2 * screenHeight;

                if (!hasHandHistory2) {
                    smoothX2 = targetX;
                    smoothY2 = targetY;
                    hasHandHistory2 = true;
                } else {
                    smoothX2 += (targetX - smoothX2) * 0.45f;
                    smoothY2 += (targetY - smoothY2) * 0.45f;
                }

                cursorParams2.x = Math.max(0, Math.min(screenWidth - 70, (int) smoothX2 - 35));
                cursorParams2.y = Math.max(0, Math.min(screenHeight - 70, (int) smoothY2 - 35));

                GradientDrawable ring = (GradientDrawable) cursorContainer2.getBackground();
                if (latestIsPinching2) {
                    ring.setColor(Color.parseColor("#AAF59E0B"));
                    ring.setStroke(7, Color.parseColor("#F59E0B"));
                } else {
                    ring.setColor(Color.parseColor("#38F59E0B"));
                    ring.setStroke(5, Color.parseColor("#FBBF24"));
                }
                windowManager.updateViewLayout(cursorContainer2, cursorParams2);
            } catch (Exception ignored) {}
        }
    };

    public void setCursorVisibility(boolean visible) {
        this.isCursorEnabled = visible;
        if (!visible) {
            hasHandHistory1 = false;
            hasHandHistory2 = false;
            mainHandler.removeCallbacks(cursor1UpdateRunnable);
            mainHandler.removeCallbacks(cursor2UpdateRunnable);
            isCursor1UpdatePending.set(false);
            isCursor2UpdatePending.set(false);
        }
        mainHandler.post(() -> {
            if (cursorContainer != null) {
                cursorContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
            if (cursorContainer2 != null) {
                cursorContainer2.setVisibility(visible && isTwoHandModeEnabled ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void setTwoHandMode(boolean enabled) {
        this.isTwoHandModeEnabled = enabled;
        if (!enabled) {
            hasHandHistory2 = false;
            mainHandler.removeCallbacks(cursor2UpdateRunnable);
            isCursor2UpdatePending.set(false);
        }
        mainHandler.post(() -> {
            if (cursorContainer2 != null) {
                cursorContainer2.setVisibility(enabled && isCursorEnabled ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void updateCursorPosition(float normX, float normY, boolean isPinching) {
        if (!isCursorEnabled) return;
        latestNormX1 = normX;
        latestNormY1 = normY;
        latestIsPinching1 = isPinching;
        if (isCursor1UpdatePending.compareAndSet(false, true)) {
            mainHandler.post(cursor1UpdateRunnable);
        }
    }

    public void updateCursorPosition2(float normX, float normY, boolean isPinching) {
        if (!isCursorEnabled || !isTwoHandModeEnabled) return;
        latestNormX2 = normX;
        latestNormY2 = normY;
        latestIsPinching2 = isPinching;
        if (isCursor2UpdatePending.compareAndSet(false, true)) {
            mainHandler.post(cursor2UpdateRunnable);
        }
    }

    public void performAirTap(float normX, float normY) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        long now = System.currentTimeMillis();
        if (now - lastGestureDispatchTime < 250) return;
        lastGestureDispatchTime = now;

        updateScreenDimensions();
        float density = getResources().getDisplayMetrics().density;
        float tapX = Math.max(10f, Math.min(screenWidth - 10f, normX * screenWidth));

        // Map normY across the interactive screen:
        // Top limit: below status bar (~15dp)
        // Bottom limit: inside bottom navigation bar / footer icons (~32dp above screen bottom),
        // preventing taps from overshooting into the Android system gesture bar / navigation bar deadzone!
        float minTapY = 15f * density;
        float maxTapY = screenHeight - (32f * density);
        float tapY = minTapY + (normY * (maxTapY - minTapY));

        Path clickPath = new Path();
        clickPath.moveTo(tapX, tapY);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, 60));
        dispatchGesture(builder.build(), null, null);
    }

    public static float swipeStartRatio = 0.68f;
    public static float swipeEndRatio = 0.32f;
    public static int swipeDuration = 170;

    public void setSwipePhysics(int level) {
        if (level == 1) {
            // Calm Snap (YouTube Shorts / Reels / Gallery): 18% travel in 160ms — calm, gentle 1-item flip
            swipeStartRatio = 0.59f;
            swipeEndRatio = 0.41f;
            swipeDuration = 160;
        } else if (level == 2) {
            // Balanced Feed (Social Media): 26% travel in 165ms
            swipeStartRatio = 0.63f;
            swipeEndRatio = 0.37f;
            swipeDuration = 165;
        } else {
            // Quick Scroll (Web / Lists): 36% travel in 170ms
            swipeStartRatio = 0.68f;
            swipeEndRatio = 0.32f;
            swipeDuration = 170;
        }
    }

    public void performAirSwipe(String direction) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        long now = System.currentTimeMillis();
        if (now - lastGestureDispatchTime < 350) return;
        lastGestureDispatchTime = now;

        updateScreenDimensions();
        float centerX = screenWidth * 0.5f;
        float startY = screenHeight * swipeStartRatio;
        float endY = screenHeight * swipeEndRatio;

        Path swipePath = new Path();
        if ("UP".equalsIgnoreCase(direction) || "NEXT".equalsIgnoreCase(direction)) {
            swipePath.moveTo(centerX, startY);
            swipePath.lineTo(centerX, endY);
        } else {
            swipePath.moveTo(centerX, endY);
            swipePath.lineTo(centerX, startY);
        }

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, swipeDuration));
        dispatchGesture(builder.build(), null, null);
    }

    public void performLikeGesture() {
        mainHandler.post(() -> {
            try {
                long now = System.currentTimeMillis();
                if (now - lastGestureDispatchTime < 350) return;
                lastGestureDispatchTime = now;

                boolean clicked = false;
                AccessibilityNodeInfo root = null;
                try {
                    root = getRootInActiveWindow();
                } catch (Exception ignored) {}

                String pkg = "";
                if (root != null && root.getPackageName() != null) {
                    pkg = root.getPackageName().toString().toLowerCase();
                }

                // 1. YouTube specific handler: Directly target YouTube video / short Like button
                if (root != null && (pkg.contains("youtube") || pkg.contains("chrome") || pkg.contains("browser"))) {
                    clicked = findAndClickYouTubeLikeButton(root);
                }

                // 2. Generic app handler (Twitter/X, Facebook, Reddit, etc.) - skip on Instagram
                if (!clicked && root != null && !pkg.contains("instagram")) {
                    clicked = findAndClickLikeButton(root);
                }

                if (root != null) {
                    try { root.recycle(); } catch (Exception ignored) {}
                }

                // 3. Fallback: For Instagram (Reels/Posts/Stories) or TikTok, perform universal double-tap at center
                if (!clicked) {
                    performDoubleTapCenter();
                }
            } catch (Exception ignored) {}
        });
    }

    private boolean findAndClickYouTubeLikeButton(AccessibilityNodeInfo root) {
        if (root == null) return false;
        try {
            updateScreenDimensions();

            // Check known YouTube Like button view IDs directly using fast native lookup
            String[] ytIds = new String[]{
                "com.google.android.youtube:id/segmented_like_button",
                "com.google.android.youtube:id/like_button",
                "com.google.android.youtube:id/reel_like_button",
                "com.google.android.youtube:id/slim_like_button",
                "com.google.android.youtube:id/like_toggle_button",
                "com.google.android.youtube:id/like_button_container"
            };

            for (String ytId : ytIds) {
                try {
                    List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(ytId);
                    if (nodes != null && !nodes.isEmpty()) {
                        for (AccessibilityNodeInfo n : nodes) {
                            if (isLegitimateLikeButtonNode(n)) {
                                boolean success = clickNodeOrCoordinates(n);
                                n.recycle();
                                if (success) return true;
                            } else {
                                n.recycle();
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Search for exact YouTube Like button in accessibility hierarchy
            AccessibilityNodeInfo targetNode = searchYouTubeLikeNode(root, 0);
            if (targetNode != null) {
                boolean success = clickNodeOrCoordinates(targetNode);
                targetNode.recycle();
                return success;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private AccessibilityNodeInfo searchYouTubeLikeNode(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 30) return null;
        try {
            CharSequence descSeq = node.getContentDescription();
            CharSequence textSeq = node.getText();
            String desc = descSeq != null ? descSeq.toString().toLowerCase() : "";
            String text = textSeq != null ? textSeq.toString().toLowerCase() : "";
            String id = node.getViewIdResourceName() != null ? node.getViewIdResourceName().toLowerCase() : "";

            if (isLegitimateLikeButtonNode(node)) {
                boolean matchesLike = false;

                // Match explicit YouTube like button descriptions
                if (desc.startsWith("like this video") || desc.startsWith("like this short") ||
                    desc.contains("like this video along with") || desc.contains("like this short along with") ||
                    desc.equals("like") || desc.equals("thumbs up") || desc.equals("like button") ||
                    (desc.startsWith("like ") && !desc.contains("comment") && !desc.contains("reply")) ||
                    (desc.contains("पसंद करें") && !desc.contains("नापसंद"))) {
                    matchesLike = true;
                }

                // Match view ID
                if (id.contains("like_button") || id.contains("segmented_like") ||
                    id.contains("reel_like") || id.contains("slim_like") || id.contains("like_toggle")) {
                    matchesLike = true;
                }

                // Match clean button text
                if (text.equals("like") || text.equals("thumbs up") || text.equals("👍")) {
                    matchesLike = true;
                }

                if (matchesLike) {
                    return AccessibilityNodeInfo.obtain(node);
                }
            }

            int childCount = node.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    AccessibilityNodeInfo found = searchYouTubeLikeNode(child, depth + 1);
                    child.recycle();
                    if (found != null) {
                        return found;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isLegitimateLikeButtonNode(AccessibilityNodeInfo node) {
        if (node == null) return false;
        try {
            CharSequence descSeq = node.getContentDescription();
            CharSequence textSeq = node.getText();
            String desc = descSeq != null ? descSeq.toString().toLowerCase() : "";
            String text = textSeq != null ? textSeq.toString().toLowerCase() : "";
            String id = node.getViewIdResourceName() != null ? node.getViewIdResourceName().toLowerCase() : "";

            // 1. Strictly reject dislikes, unlikes, and removed likes
            if (desc.contains("dislike") || text.contains("dislike") || id.contains("dislike")) return false;
            if (desc.contains("unlike") || text.contains("unlike") || id.contains("unlike")) return false;
            if (desc.contains("remove like") || desc.contains("liked")) return false;

            // 2. Strictly reject descriptions, expanders, metadata, comments, titles, channel headers
            if (id.contains("expand") || id.contains("collapse") || id.contains("description") || 
                id.contains("desc") || id.contains("metadata") || id.contains("comment") || 
                id.contains("title") || id.contains("header") || id.contains("channel") ||
                id.contains("summary") || id.contains("player_learn_more")) {
                return false;
            }

            if (desc.contains("more") || desc.contains("expand") || desc.contains("description") || 
                desc.contains("collapse") || desc.contains("view") || desc.contains("subscribe") ||
                desc.contains("share") || desc.contains("remix") || desc.contains("download")) {
                return false;
            }

            if (text.contains("more") || text.contains("view") || text.contains("subscriber") || text.length() > 20) {
                return false;
            }

            // 3. Must be visible on screen
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.width() <= 0 || bounds.height() <= 0) return false;
            if (bounds.bottom <= 0 || bounds.top >= screenHeight) return false;

            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean clickNodeOrCoordinates(AccessibilityNodeInfo node) {
        if (node == null) return false;
        try {
            // 1. If the node itself is clickable, execute accessibility click
            if (node.isClickable()) {
                boolean ok = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (ok) return true;
            }

            // 2. Check immediate parent if clickable (e.g. segmented button wrapper)
            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null) {
                String pId = parent.getViewIdResourceName() != null ? parent.getViewIdResourceName().toLowerCase() : "";
                if (!pId.contains("description") && !pId.contains("expand") && !pId.contains("metadata") && !pId.contains("scroll")) {
                    if (parent.isClickable()) {
                        boolean ok = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        parent.recycle();
                        if (ok) return true;
                    } else {
                        parent.recycle();
                    }
                } else {
                    parent.recycle();
                }
            }

            // 3. Guaranteed coordinate fallback: Tap exact center of button on screen
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (bounds.width() > 0 && bounds.height() > 0) {
                updateScreenDimensions();
                float cx = bounds.exactCenterX();
                float cy = bounds.exactCenterY();
                if (cx > 0 && cx < screenWidth && cy > 0 && cy < screenHeight) {
                    performTapAt(cx, cy, null);
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean findAndClickLikeButton(AccessibilityNodeInfo node) {
        if (node == null) return false;
        try {
            CharSequence descSeq = node.getContentDescription();
            CharSequence textSeq = node.getText();
            String desc = descSeq != null ? descSeq.toString().toLowerCase() : "";
            String text = textSeq != null ? textSeq.toString().toLowerCase() : "";
            String id = node.getViewIdResourceName() != null ? node.getViewIdResourceName().toLowerCase() : "";

            boolean isNegative = desc.contains("unlike") || text.contains("unlike") || id.contains("unlike") ||
                                 desc.contains("dislike") || text.contains("dislike") || id.contains("dislike") ||
                                 desc.contains("liked") || text.contains("liked") ||
                                 desc.contains("remove like") || text.contains("remove like");

            boolean isContentOrExpander = id.contains("expand") || id.contains("collapse") || 
                                          id.contains("description") || id.contains("desc") || 
                                          id.contains("metadata") || id.contains("title") || 
                                          id.contains("comment") || id.contains("header") ||
                                          desc.contains("more") || text.contains("more") || 
                                          text.length() > 20;

            if (!isNegative && !isContentOrExpander) {
                boolean hasLike = (desc.startsWith("like") || desc.equals("like") || desc.contains("like this") || desc.contains("thumbs up")) ||
                                  (text.equals("like") || text.equals("thumbs up") || text.equals("👍")) ||
                                  (id.contains("like") && !id.contains("dislike"));

                if (hasLike) {
                    return clickNodeOrCoordinates(node);
                }
            }

            int count = node.getChildCount();
            for (int i = 0; i < count; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    boolean found = findAndClickLikeButton(child);
                    child.recycle();
                    if (found) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public void performDoubleTapCenter() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        updateScreenDimensions();
        float centerX = screenWidth * 0.5f;
        float centerY = screenHeight * 0.5f;

        performTapAt(centerX, centerY, () -> {
            mainHandler.postDelayed(() -> {
                performTapAt(centerX, centerY, null);
            }, 90);
        });
    }

    private void performTapAt(float x, float y, Runnable onComplete) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;
        Path clickPath = new Path();
        clickPath.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, 50));
        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                if (onComplete != null) onComplete.run();
            }
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                if (onComplete != null) onComplete.run();
            }
        }, null);
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

    public void adjustSystemVolume(String direction) {
        mainHandler.post(() -> {
            try {
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    int flag = "UP".equalsIgnoreCase(direction) ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
                    // Adjust by exactly 1 single step and display system volume slider UI
                    am.adjustSuggestedStreamVolume(flag, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND);
                }
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
                } else if ("VOL_UP".equalsIgnoreCase(action)) {
                    adjustSystemVolume("UP");
                } else if ("VOL_DOWN".equalsIgnoreCase(action)) {
                    adjustSystemVolume("DOWN");
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
        if (windowManager != null) {
            if (cursorContainer != null) {
                try { windowManager.removeView(cursorContainer); } catch (Exception ignored) {}
            }
            if (cursorContainer2 != null) {
                try { windowManager.removeView(cursorContainer2); } catch (Exception ignored) {}
            }
        }
    }
}
