# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\shuai\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt

# Keep JavascriptInterface for WebView bridge
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.aerosense.touchless.ai.** { *; }
