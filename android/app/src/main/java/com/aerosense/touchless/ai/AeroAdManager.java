package com.aerosense.touchless.ai;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AeroAdManager {
    private static final String TAG = "AeroAdManager";
    private static volatile AeroAdManager sInstance;

    // Official Google AdMob Test Ad Unit IDs (Safe for Development & Review)
    public static final String DEFAULT_TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String DEFAULT_TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";

    private boolean isAdsEnabled = true;
    private String bannerAdUnitId = DEFAULT_TEST_BANNER_ID;
    private String interstitialAdUnitId = DEFAULT_TEST_INTERSTITIAL_ID;

    private boolean isInitialized = false;
    private InterstitialAd mInterstitialAd = null;
    private boolean isInterstitialLoading = false;
    private AdView mBannerAdView = null;
    private ViewGroup mBannerContainer = null;
    private long lastInterstitialShowTime = 0;
    private static final long MIN_INTERSTITIAL_INTERVAL_MS = 60 * 1000; // 60s cooldown

    private AeroAdManager() {}

    public static AeroAdManager getInstance() {
        if (sInstance == null) {
            synchronized (AeroAdManager.class) {
                if (sInstance == null) {
                    sInstance = new AeroAdManager();
                }
            }
        }
        return sInstance;
    }

    public void init(Context context) {
        if (isInitialized) return;
        try {
            MobileAds.initialize(context, initializationStatus -> {
                Log.d(TAG, "AdMob MobileAds initialized successfully");
                loadInterstitialAd(context);
            });
            isInitialized = true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize AdMob: " + e.getMessage());
        }
    }

    public void setupBanner(Activity activity, ViewGroup container) {
        this.mBannerContainer = container;
        if (!isAdsEnabled || container == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                if (mBannerAdView != null) {
                    mBannerAdView.destroy();
                    mBannerAdView = null;
                }
                container.removeAllViews();
                mBannerAdView = new AdView(activity);
                mBannerAdView.setAdSize(AdSize.BANNER);
                mBannerAdView.setAdUnitId(bannerAdUnitId);
                container.addView(mBannerAdView);
                AdRequest adRequest = new AdRequest.Builder().build();
                mBannerAdView.loadAd(adRequest);
                container.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Banner load error: " + e.getMessage());
            }
        });
    }

    public void setBannerVisibility(boolean visible) {
        if (mBannerContainer != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                mBannerContainer.setVisibility(visible && isAdsEnabled ? View.VISIBLE : View.GONE);
            });
        }
    }

    public void loadInterstitialAd(Context context) {
        if (!isAdsEnabled || isInterstitialLoading || mInterstitialAd != null) return;
        isInterstitialLoading = true;
        try {
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(context, interstitialAdUnitId, adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                    isInterstitialLoading = false;
                    Log.d(TAG, "Interstitial ad loaded successfully");
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mInterstitialAd = null;
                            loadInterstitialAd(context);
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                            mInterstitialAd = null;
                            loadInterstitialAd(context);
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                    isInterstitialLoading = false;
                    Log.w(TAG, "Interstitial failed to load: " + loadAdError.getMessage());
                }
            });
        } catch (Exception e) {
            isInterstitialLoading = false;
        }
    }

    public void showInterstitial(Activity activity) {
        if (!isAdsEnabled || activity == null) return;
        long now = System.currentTimeMillis();
        if (now - lastInterstitialShowTime < MIN_INTERSTITIAL_INTERVAL_MS) {
            Log.d(TAG, "Interstitial cooldown active, skipping");
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mInterstitialAd != null) {
                lastInterstitialShowTime = now;
                mInterstitialAd.show(activity);
            } else {
                loadInterstitialAd(activity);
            }
        });
    }

    public void updateConfig(String bannerId, String interstitialId, boolean enabled) {
        this.isAdsEnabled = enabled;
        if (bannerId != null && !bannerId.trim().isEmpty()) {
            this.bannerAdUnitId = bannerId.trim();
        }
        if (interstitialId != null && !interstitialId.trim().isEmpty()) {
            this.interstitialAdUnitId = interstitialId.trim();
        }
        setBannerVisibility(enabled);
    }
}
