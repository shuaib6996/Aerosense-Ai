package com.aerosense.touchless.ai;

import android.Manifest;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen awake during touchless hand tracking
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        systemWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        webView = new WebView(this);
        setContentView(webView);

        // Request Android runtime permissions for Camera & Microphone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                }, PERMISSION_REQ_CODE);
            }
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setDatabaseEnabled(true);

        // Register Native Bridge for Background Service, Full Mobile Control & Floating HUD
        webView.addJavascriptInterface(new AeroNativeBridge(), "AndroidBridge");

        webView.setWebViewClient(new WebViewClient());

        // Automatically grant WebRTC Camera & Microphone access inside WebView
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    runOnUiThread(() -> request.grant(request.getResources()));
                }
            }
        });

        // Load index.html placed inside android/app/src/main/assets/index.html
        webView.loadUrl("file:///android_asset/index.html");
    }

    // Keep WebView & Camera tracking active even when user switches apps or goes to Home Screen
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
        // If returning to AeroSense AI app, restore WebView from floating overlay back into full Activity
        exitSystemFloatingOverlay();
    }

    private void enterMiniHandFloatingWindow() {
        // Prefer True System Overlay (FLAG_NOT_FOCUSABLE) so camera & hand gestures keep working while user touches/scrolls Instagram
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

    private void enterSystemFloatingOverlay() {
        if (isFloatingOverlayAttached || webView == null || systemWindowManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return;

        try {
            if (webView.getParent() instanceof ViewGroup) {
                ((ViewGroup) webView.getParent()).removeView(webView);
            }

            int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

            float density = getResources().getDisplayMetrics().density;
            int widthPx = (int) (136 * density);
            int heightPx = (int) (176 * density);

            floatingOverlayParams = new WindowManager.LayoutParams(
                    widthPx,
                    heightPx,
                    overlayType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );
            floatingOverlayParams.gravity = Gravity.BOTTOM | Gravity.END;
            floatingOverlayParams.x = (int) (14 * density);
            floatingOverlayParams.y = (int) (82 * density);

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
            setContentView(webView);
        } catch (Exception ignored) {}
    }

    public class AeroNativeBridge {

        @JavascriptInterface
        public void startBackgroundService() {
            isBackgroundEngineRunning = true;
            runOnUiThread(() -> Toast.makeText(
                MainActivity.this,
                "AeroSense Background Engine Active",
                Toast.LENGTH_SHORT
            ).show());
        }

        @JavascriptInterface
        public void stopBackgroundService() {
            isBackgroundEngineRunning = false;
            runOnUiThread(() -> Toast.makeText(
                MainActivity.this,
                "AeroSense Background Engine Stopped",
                Toast.LENGTH_SHORT
            ).show());
        }

        @JavascriptInterface
        public void setFullMobileControl(boolean enabled) {
            isFullMobileControlEnabled = enabled;
            isBackgroundEngineRunning = enabled || isBackgroundEngineRunning;
            runOnUiThread(() -> Toast.makeText(
                MainActivity.this,
                enabled ? "Full Mobile OS Control Enabled" : "Full Mobile Control Disabled",
                Toast.LENGTH_SHORT
            ).show());
        }

        @JavascriptInterface
        public void performSystemAction(String action) {
            runOnUiThread(() -> {
                try {
                    if (AeroAccessibilityService.instance != null) {
                        AeroAccessibilityService.instance.performGlobalPhoneAction(action);
                        return;
                    }
                    if ("HOME".equals(action)) {
                        enterMiniHandFloatingWindow();
                        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                        homeIntent.addCategory(Intent.CATEGORY_HOME);
                        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(homeIntent);
                    } else if ("BACK".equals(action)) {
                        onBackPressed();
                    } else if ("NOTIFICATIONS".equals(action)) {
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
                obj.put("accessibility", accEnabled);
                obj.put("overlay", overlayEnabled);
            } catch (Exception ignored) {}
            return obj.toString();
        }

        @JavascriptInterface
        public void openAccessibilitySettings() {
            runOnUiThread(() -> {
                try {
                    Toast.makeText(MainActivity.this, "Enable 'AeroSense AI' in Installed Services", Toast.LENGTH_LONG).show();
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
        public void updateSelectedApps(String jsonPackages) {
            // Stores selected target apps
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
                } else {
                    Toast.makeText(
                        MainActivity.this,
                        appName + " mode activated in Floating HUD",
                        Toast.LENGTH_SHORT
                    ).show();
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
}
