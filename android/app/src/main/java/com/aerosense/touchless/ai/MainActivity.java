package com.aerosense.touchless.ai;

import android.Manifest;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends Activity {

    private WebView webView;
    private static final int PERMISSION_REQ_CODE = 1001;
    private boolean isBackgroundEngineRunning = false;
    private boolean isFullMobileControlEnabled = false;
    private boolean isFloatingOverlayAttached = false;
    private WindowManager systemWindowManager;
    private WindowManager.LayoutParams floatingOverlayParams;
    private String currentHudSize = "sm";
    private int customHudWidthDp = 68;
    private int customHudHeightDp = 84;
    private boolean isModalOpenInWeb = false;
    private final AeroVoiceControlPatch voiceControlPatch = new AeroVoiceControlPatch();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        systemWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#050811"));
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setDatabaseEnabled(true);

        webView.addJavascriptInterface(new AeroNativeBridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    runOnUiThread(() -> request.grant(request.getResources()));
                }
            }
        });

        webView.loadUrl("file:///android_asset/index.html");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                }, PERMISSION_REQ_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE && webView != null) {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript("if(window.onNativePermissionsResult) window.onNativePermissionsResult(); if(window.refreshNativePermissionStatus) window.refreshNativePermissionStatus();", null);
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isBackgroundEngineRunning && webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (isBackgroundEngineRunning) {
            enterMiniHandFloatingWindow();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        exitSystemFloatingOverlay();
        if (!isBackgroundEngineRunning && AeroAccessibilityService.instance != null) {
            AeroAccessibilityService.instance.setCursorVisibility(false);
        }
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
            webView.postDelayed(() -> {
                if (!isFloatingOverlayAttached && webView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(
                            "if(window.setNativeOverlayMode) window.setNativeOverlayMode(false);" +
                            "document.documentElement.classList.remove('native-overlay-mode');" +
                            "document.body.classList.remove('native-overlay-mode');" +
                            "document.body.style.backgroundColor='#050811';" +
                            "if(window.refreshNativePermissionStatus) window.refreshNativePermissionStatus();",
                            null
                    );
                }
            }, 120);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        exitSystemFloatingOverlay();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        notifyWebOverlayState(isInPictureInPictureMode);
        if (!isInPictureInPictureMode) {
            exitSystemFloatingOverlay();
        }
    }

    private void notifyWebOverlayState(boolean isOverlay) {
        if (webView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final String script = "if(window.setNativeOverlayMode) window.setNativeOverlayMode(" + isOverlay + ");";
            webView.post(() -> webView.evaluateJavascript(script, null));
        }
    }

    private void enterMiniHandFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            enterSystemFloatingOverlay();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Rational aspectRatio = new Rational(3, 4);
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .build();
                enterPictureInPictureMode(params);
            } catch (Exception ignored) {}
        }
    }

    private int[] getHudDimensionsDp(String sizeKey) {
        if (customHudWidthDp > 0 && customHudHeightDp > 0) {
            return new int[]{customHudWidthDp, customHudHeightDp};
        }
        if ("lg".equals(sizeKey)) return new int[]{88, 108};
        if ("md".equals(sizeKey)) return new int[]{78, 96};
        return new int[]{68, 84}; // default sleek & compact (max 88px)
    }

    private void enterSystemFloatingOverlay() {
        if (isFloatingOverlayAttached || webView == null || systemWindowManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return;

        try {
            // Synchronously notify web to enter transparent overlay mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(
                        "if(window.setNativeOverlayMode) window.setNativeOverlayMode(true);" +
                        "document.documentElement.classList.add('native-overlay-mode');" +
                        "document.body.classList.add('native-overlay-mode');",
                        null
                );
            }

            if (webView.getParent() instanceof ViewGroup) {
                ((ViewGroup) webView.getParent()).removeView(webView);
            }

            webView.setBackgroundColor(Color.TRANSPARENT);
            notifyWebOverlayState(true);

            int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

            float density = getResources().getDisplayMetrics().density;
            int[] dims = getHudDimensionsDp(currentHudSize);
            int widthPx = (int) (dims[0] * density);
            int heightPx = (int) (dims[1] * density);

            floatingOverlayParams = new WindowManager.LayoutParams(
                    widthPx,
                    heightPx,
                    overlayType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );
            DisplayMetrics dm = getResources().getDisplayMetrics();
            final int screenWidth = dm.widthPixels;
            final int screenHeight = dm.heightPixels;

            floatingOverlayParams.gravity = Gravity.TOP | Gravity.START;
            floatingOverlayParams.x = (screenWidth - widthPx) / 2;
            floatingOverlayParams.y = screenHeight - heightPx - (int) (64 * density);
            floatingOverlayParams.alpha = 0.0f; // Hidden until hand gesture is active

            webView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;
                private boolean isMoving = false;
                private long lastUpdateTime = 0;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (!isFloatingOverlayAttached || floatingOverlayParams == null || systemWindowManager == null) {
                        return false;
                    }
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = floatingOverlayParams.x;
                            initialY = floatingOverlayParams.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            isMoving = false;
                            lastUpdateTime = System.currentTimeMillis();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - initialTouchX;
                            float dy = event.getRawY() - initialTouchY;
                            if (!isMoving && (Math.abs(dx) > 3 || Math.abs(dy) > 3)) {
                                isMoving = true;
                            }
                            if (isMoving) {
                                int curW = floatingOverlayParams.width;
                                int curH = floatingOverlayParams.height;
                                int newX = (int) (initialX + dx);
                                int newY = (int) (initialY + dy);
                                floatingOverlayParams.x = Math.max(0, Math.min(screenWidth - curW, newX));
                                floatingOverlayParams.y = Math.max(0, Math.min(screenHeight - curH, newY));

                                long now = System.currentTimeMillis();
                                if (now - lastUpdateTime >= 16) {
                                    lastUpdateTime = now;
                                    try {
                                        systemWindowManager.updateViewLayout(webView, floatingOverlayParams);
                                    } catch (Exception ignored) {}
                                }
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (isMoving) {
                                try {
                                    systemWindowManager.updateViewLayout(webView, floatingOverlayParams);
                                } catch (Exception ignored) {}
                            }
                            isMoving = false;
                            return true;
                    }
                    return false;
                }
            });

            systemWindowManager.addView(webView, floatingOverlayParams);
            webView.onResume();
            webView.resumeTimers();
            isFloatingOverlayAttached = true;
        } catch (Exception ignored) {}
    }

    private void exitSystemFloatingOverlay() {
        if (systemWindowManager != null && webView != null) {
            try {
                if (isFloatingOverlayAttached) {
                    systemWindowManager.removeView(webView);
                }
            } catch (Exception ignored) {
                try {
                    systemWindowManager.removeViewImmediate(webView);
                } catch (Exception ignored2) {}
            }
        }
        isFloatingOverlayAttached = false;

        if (webView != null) {
            try {
                webView.setOnTouchListener(null);
                if (webView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) webView.getParent()).removeView(webView);
                }
                webView.setBackgroundColor(Color.parseColor("#050811"));
                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                webView.setLayoutParams(lp);
                setContentView(webView);
                webView.onResume();
                webView.resumeTimers();
            } catch (Exception ignored) {}

            notifyWebOverlayState(false);
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && webView != null) {
                    webView.evaluateJavascript(
                            "if(window.setNativeOverlayMode) window.setNativeOverlayMode(false);" +
                            "document.documentElement.classList.remove('native-overlay-mode');" +
                            "document.body.classList.remove('native-overlay-mode');" +
                            "document.body.style.backgroundColor='#050811';" +
                            "if(window.refreshNativePermissionStatus) window.refreshNativePermissionStatus();",
                            null
                    );
                }
            });
        }
    }

    public class AeroNativeBridge {

        @JavascriptInterface
        public void requestRuntimePermissions() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                        checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO
                        }, PERMISSION_REQ_CODE);
                    } else {
                        Toast.makeText(MainActivity.this, "Camera & Microphone already granted!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @JavascriptInterface
        public void startBackgroundService() {
            isBackgroundEngineRunning = true;
            if (AeroAccessibilityService.instance != null) {
                AeroAccessibilityService.instance.setCursorVisibility(true);
            }
        }

        @JavascriptInterface
        public void stopBackgroundService() {
            isBackgroundEngineRunning = false;
            runOnUiThread(() -> exitSystemFloatingOverlay());
            if (AeroAccessibilityService.instance != null) {
                AeroAccessibilityService.instance.setCursorVisibility(false);
            }
        }

        @JavascriptInterface
        public void exitFloatingOverlay() {
            isBackgroundEngineRunning = false;
            runOnUiThread(() -> exitSystemFloatingOverlay());
            if (AeroAccessibilityService.instance != null) {
                AeroAccessibilityService.instance.setCursorVisibility(false);
            }
        }

        @JavascriptInterface
        public void setModalState(boolean isOpen) {
            isModalOpenInWeb = isOpen;
        }

        @JavascriptInterface
        public void setFullMobileControl(boolean enabled) {
            isFullMobileControlEnabled = enabled;
            if (!enabled && !isBackgroundEngineRunning && AeroAccessibilityService.instance != null) {
                AeroAccessibilityService.instance.setCursorVisibility(false);
            }
        }

        @JavascriptInterface
        public void setHandVisible(boolean visible) {
            runOnUiThread(() -> {
                if (AeroAccessibilityService.instance != null) {
                    AeroAccessibilityService.instance.setCursorVisibility(visible && isBackgroundEngineRunning);
                }
                if (isFloatingOverlayAttached && webView != null && floatingOverlayParams != null && systemWindowManager != null) {
                    try {
                        floatingOverlayParams.alpha = visible ? 1.0f : 0.0f;
                        systemWindowManager.updateViewLayout(webView, floatingOverlayParams);
                    } catch (Exception ignored) {}
                }
            });
        }

        @JavascriptInterface
        public void setCustomHudDimensions(int widthDp, int heightDp) {
            final int w = Math.min(88, Math.max(50, widthDp));
            final int h = Math.min(108, Math.max(62, heightDp));
            customHudWidthDp = w;
            customHudHeightDp = h;
            runOnUiThread(() -> {
                if (isFloatingOverlayAttached && webView != null && floatingOverlayParams != null && systemWindowManager != null) {
                    try {
                        float density = getResources().getDisplayMetrics().density;
                        floatingOverlayParams.width = (int) (w * density);
                        floatingOverlayParams.height = (int) (h * density);
                        systemWindowManager.updateViewLayout(webView, floatingOverlayParams);
                    } catch (Exception ignored) {}
                }
            });
        }

        @JavascriptInterface
        public void resizeFloatingOverlay(String sizeKey) {
            currentHudSize = sizeKey;
            runOnUiThread(() -> {
                if (isFloatingOverlayAttached && webView != null && floatingOverlayParams != null) {
                    try {
                        float density = getResources().getDisplayMetrics().density;
                        int[] dims = getHudDimensionsDp(sizeKey);
                        floatingOverlayParams.width = (int) (dims[0] * density);
                        floatingOverlayParams.height = (int) (dims[1] * density);
                        systemWindowManager.updateViewLayout(webView, floatingOverlayParams);
                    } catch (Exception ignored) {}
                }
            });
        }

        @JavascriptInterface
        public void adjustSystemVolume(String direction) {
            try {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    int flag = "UP".equalsIgnoreCase(direction) ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER;
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, flag, AudioManager.FLAG_SHOW_UI);
                }
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void showSystemHoldMenu(boolean show) {
            runOnUiThread(() -> {
                if (isFloatingOverlayAttached && webView != null && floatingOverlayParams != null) {
                    try {
                        float density = getResources().getDisplayMetrics().density;
                        if (show) {
                            floatingOverlayParams.width = (int) (320 * density);
                            floatingOverlayParams.height = (int) (340 * density);
                            floatingOverlayParams.gravity = Gravity.CENTER;
                            floatingOverlayParams.y = 0;
                        } else {
                            int[] dims = getHudDimensionsDp(currentHudSize);
                            floatingOverlayParams.width = (int) (dims[0] * density);
                            floatingOverlayParams.height = (int) (dims[1] * density);
                            floatingOverlayParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                            floatingOverlayParams.y = (int) (64 * density);
                        }
                        systemWindowManager.updateViewLayout(webView, floatingOverlayParams);
                    } catch (Exception ignored) {}
                }
            });
        }

        @JavascriptInterface
        public void triggerBackGesture() {
            performSystemAction("BACK");
        }

        @JavascriptInterface
        public void triggerHomeGesture() {
            performSystemAction("HOME");
        }

        @JavascriptInterface
        public void performSystemAction(String action) {
            runOnUiThread(() -> {
                try {
                    if ("HOME".equalsIgnoreCase(action)) {
                        if (isBackgroundEngineRunning) {
                            enterMiniHandFloatingWindow();
                        }
                        if (AeroAccessibilityService.instance != null) {
                            AeroAccessibilityService.instance.performHomeGesture();
                        } else {
                            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                            homeIntent.addCategory(Intent.CATEGORY_HOME);
                            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(homeIntent);
                        }
                    } else if ("BACK".equalsIgnoreCase(action)) {
                        if (AeroAccessibilityService.instance != null) {
                            AeroAccessibilityService.instance.performBackGesture();
                        } else {
                            onBackPressed();
                        }
                    } else if (AeroAccessibilityService.instance != null) {
                        AeroAccessibilityService.instance.performGlobalPhoneAction(action);
                    } else if ("NOTIFICATIONS".equalsIgnoreCase(action)) {
                        Object service = getSystemService("statusbar");
                        Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                        Method expand = statusbarManager.getMethod("expandNotificationsPanel");
                        expand.invoke(service);
                    }
                } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface
        public String getInstalledDeviceApps() {
            JSONArray arr = new JSONArray();
            try {
                PackageManager pm = getPackageManager();
                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> resolvedList = pm.queryIntentActivities(mainIntent, 0);
                for (ResolveInfo info : resolvedList) {
                    if (info.activityInfo != null && info.activityInfo.packageName != null) {
                        String pkg = info.activityInfo.packageName;
                        if (pkg.equals(getPackageName())) continue;
                        String label = info.loadLabel(pm).toString();
                        JSONObject obj = new JSONObject();
                        obj.put("name", label);
                        obj.put("pkg", pkg);
                        arr.put(obj);
                    }
                }
            } catch (Exception ignored) {}
            return arr.toString();
        }

        @JavascriptInterface
        public String checkPermissionsJson() {
            JSONObject obj = new JSONObject();
            try {
                boolean accEnabled = AeroAccessibilityService.instance != null;
                boolean overlayEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(MainActivity.this);
                boolean camEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                        checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                boolean micEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                obj.put("accessibility", accEnabled);
                obj.put("overlay", overlayEnabled);
                obj.put("camera", camEnabled);
                obj.put("microphone", micEnabled);
            } catch (Exception ignored) {}
            return obj.toString();
        }

        @JavascriptInterface
        public void openAccessibilitySettings() {
            runOnUiThread(() -> {
                try {
                    Toast.makeText(MainActivity.this, "Enable 'AeroSense AI Hand Control' (Installed Apps)", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e1) {
                    try {
                        openAppSettings();
                    } catch (Exception ignored) {}
                }
            });
        }

        @JavascriptInterface
        public void openOverlaySettings() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e1) {
                        try {
                            Intent intent2 = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent2);
                        } catch (Exception e2) {
                            openAppSettings();
                        }
                    }
                }
            });
        }

        @JavascriptInterface
        public void openAppSettings() {
            runOnUiThread(() -> {
                try {
                    Toast.makeText(MainActivity.this, "Grant Permissions in App Settings", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface
        public void updateGlobalAirCursor(float normX, float normY, boolean isPinching) {
            if (isBackgroundEngineRunning && AeroAccessibilityService.instance != null) {
                AeroAccessibilityService.instance.updateCursorPosition(normX, normY, isPinching);
            }
        }

        @JavascriptInterface
        public void triggerGlobalAirTap(float normX, float normY) {
            if (AeroAccessibilityService.instance != null) {
                AeroAccessibilityService.instance.performAirTap(normX, normY);
            }
        }

        @JavascriptInterface
        public void triggerGlobalAirSwipe(String direction) {
            if (AeroAccessibilityService.instance != null) {
                AeroAccessibilityService.instance.performAirSwipe(direction);
            }
        }

        @JavascriptInterface
        public void updateSelectedApps(String jsonPackages) {}

        @JavascriptInterface
        public void injectAirTypedText(String typedText, boolean submitAfterTyping) {
            if (AeroAccessibilityService.instance != null) {
                AeroAccessibilityService.instance.injectAirTypedText(typedText, submitAfterTyping);
            }
        }

        @JavascriptInterface
        public void updateFloatingIndicatorBounds(float size, float horiz, float vert, float opacity) {}

        @JavascriptInterface
        public void startNativeVoiceRecognition(String languageTag) {
            voiceControlPatch.startListening(MainActivity.this, webView, languageTag);
        }

        @JavascriptInterface
        public void stopNativeVoiceRecognition() {
            voiceControlPatch.stopListening();
        }

        @JavascriptInterface
        public void enterFloatingOverlay() {
            if (isBackgroundEngineRunning) {
                runOnUiThread(() -> enterMiniHandFloatingWindow());
            }
        }

        @JavascriptInterface
        public void openTargetAppWithOverlay(String packageName, String appName) {
            runOnUiThread(() -> {
                if (isBackgroundEngineRunning) {
                    enterMiniHandFloatingWindow();
                }
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                }
            });
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            exitSystemFloatingOverlay();
        } else if (isBackgroundEngineRunning && webView != null) {
            webView.dispatchWindowVisibilityChanged(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (isModalOpenInWeb && webView != null) {
            isModalOpenInWeb = false;
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript("if(window.closeAllModals) window.closeAllModals();", null);
                }
            });
            return;
        }

        if (webView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript(
                    "(function(){ return (window.closeAllModals ? window.closeAllModals() : false); })();",
                    value -> {
                        if ("true".equals(value)) {
                            isModalOpenInWeb = false;
                            return;
                        }
                        runOnUiThread(() -> handleAppBackNavigation());
                    }
            );
            return;
        }

        handleAppBackNavigation();
    }

    private void handleAppBackNavigation() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        if (isBackgroundEngineRunning) {
            enterMiniHandFloatingWindow();
            moveTaskToBack(true);
            return;
        }
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        if (isFloatingOverlayAttached && systemWindowManager != null && webView != null) {
            try {
                systemWindowManager.removeView(webView);
            } catch (Exception ignored) {}
            isFloatingOverlayAttached = false;
        }
        super.onDestroy();
        if (voiceControlPatch != null) {
            voiceControlPatch.stopListening();
        }
    }
}
