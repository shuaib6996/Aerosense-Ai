package com.aerosense.touchless.ai;

import android.Manifest;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Rational;
import android.view.Gravity;
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
    private String currentHudSize = "md";
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE && webView != null) {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript("if(window.refreshNativePermissionStatus) window.refreshNativePermissionStatus();", null);
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if ((isBackgroundEngineRunning || isFullMobileControlEnabled) && webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (isBackgroundEngineRunning || isFullMobileControlEnabled) {
            enterMiniHandFloatingWindow();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        exitSystemFloatingOverlay();
        if (webView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript("if(window.refreshNativePermissionStatus) window.refreshNativePermissionStatus();", null);
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
        if ("sm".equals(sizeKey)) return new int[]{90, 110};
        if ("lg".equals(sizeKey)) return new int[]{160, 195};
        return new int[]{118, 146};
    }

    private void enterSystemFloatingOverlay() {
        if (isFloatingOverlayAttached || webView == null || systemWindowManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return;

        try {
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
            floatingOverlayParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            floatingOverlayParams.x = 0;
            floatingOverlayParams.y = (int) (64 * density);

            systemWindowManager.addView(webView, floatingOverlayParams);
            webView.onResume();
            webView.resumeTimers();
            isFloatingOverlayAttached = true;
        } catch (Exception ignored) {}
    }

    private void exitSystemFloatingOverlay() {
        if (!isFloatingOverlayAttached || webView == null || systemWindowManager == null) return;
        try {
            systemWindowManager.removeView(webView);
            isFloatingOverlayAttached = false;
            notifyWebOverlayState(false);
            webView.setBackgroundColor(Color.parseColor("#050811"));
            setContentView(webView);
        } catch (Exception ignored) {}
    }

    public class AeroNativeBridge {

        @JavascriptInterface
        public void requestRuntimePermissions() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                    }, PERMISSION_REQ_CODE);
                }
            });
        }

        @JavascriptInterface
        public void startBackgroundService() {
            isBackgroundEngineRunning = true;
        }

        @JavascriptInterface
        public void stopBackgroundService() {
            isBackgroundEngineRunning = false;
            if (AeroAccessibilityService.instance != null) {
                AeroAccessibilityService.instance.setCursorVisibility(false);
            }
        }

        @JavascriptInterface
        public void setFullMobileControl(boolean enabled) {
            isFullMobileControlEnabled = enabled;
            isBackgroundEngineRunning = enabled || isBackgroundEngineRunning;
            if (!enabled && !isBackgroundEngineRunning && AeroAccessibilityService.instance != null) {
                AeroAccessibilityService.instance.setCursorVisibility(false);
            }
        }

        @JavascriptInterface
        public void setHandVisible(boolean visible) {
            runOnUiThread(() -> {
                if (!visible && AeroAccessibilityService.instance != null) {
                    AeroAccessibilityService.instance.setCursorVisibility(false);
                }
                if (isFloatingOverlayAttached && webView != null && floatingOverlayParams != null) {
                    try {
                        floatingOverlayParams.alpha = visible ? 1.0f : 0.0f;
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
                        isBackgroundEngineRunning = true;
                        enterMiniHandFloatingWindow();
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
                    Toast.makeText(MainActivity.this, "Enable 'AeroSense AI Hand Control' in Accessibility", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface
        public void openOverlaySettings() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception ignored) {}
                }
            });
        }

        @JavascriptInterface
        public void updateGlobalAirCursor(float normX, float normY, boolean isPinching) {
            if (AeroAccessibilityService.instance != null) {
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
            isBackgroundEngineRunning = true;
            runOnUiThread(() -> enterMiniHandFloatingWindow());
        }

        @JavascriptInterface
        public void openTargetAppWithOverlay(String packageName, String appName) {
            isBackgroundEngineRunning = true;
            runOnUiThread(() -> {
                enterMiniHandFloatingWindow();
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
        if (!hasFocus && (isBackgroundEngineRunning || isFullMobileControlEnabled) && webView != null) {
            webView.dispatchWindowVisibilityChanged(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceControlPatch != null) {
            voiceControlPatch.stopListening();
        }
    }
}
