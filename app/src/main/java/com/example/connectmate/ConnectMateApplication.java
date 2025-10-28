package com.example.connectmate;

import android.app.Application;
import android.util.Log;

import com.kakao.sdk.common.KakaoSdk;
import com.navercorp.nid.NaverIdLoginSDK;

/**
 * ConnectMateApplication
 * Custom Application class for initializing SDKs that need to be set up before any Activity.

 * According to official documentation:
 * - Kakao SDK must be initialized in Application.onCreate()
 * - Naver SDK should be initialized early in the app lifecycle
 */
public class ConnectMateApplication extends Application {

    private static final String TAG = "ConnectMateApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Kakao SDK (Required by Kakao documentation)
        // Must be called before any Kakao login operations
        initializeKakaoSdk();

        // Initialize Naver SDK (Best practice)
        // Can also be initialized in Activity, but Application is preferred
        initializeNaverSdk();
    }

    /**
     * Initialize Kakao SDK with the native app key.
     * This MUST be called in Application.onCreate() per Kakao documentation.
     */
    private void initializeKakaoSdk() {
        if (isValidApiKey(BuildConfig.KAKAO_APP_KEY)) {
            try {
                // Initialize Kakao SDK with native app key
                KakaoSdk.init(this, BuildConfig.KAKAO_APP_KEY);
                Log.d(TAG, "Kakao SDK initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Kakao SDK", e);
            }
        } else {
            Log.w(TAG, "Kakao SDK not initialized - API key not configured in local.properties");
        }
    }

    /**
     * Initialize Naver Login SDK with client credentials.
     * Requires NAVER_CLIENT_ID and NAVER_CLIENT_SECRET.
     */
    private void initializeNaverSdk() {
        if (isValidApiKey(BuildConfig.NAVER_CLIENT_ID) && isValidApiKey(BuildConfig.NAVER_CLIENT_SECRET)) {
            try {
                // Initialize Naver SDK with client ID, secret, and app name
                NaverIdLoginSDK.INSTANCE.initialize(
                    this,
                    BuildConfig.NAVER_CLIENT_ID,
                    BuildConfig.NAVER_CLIENT_SECRET,
                    "ConnectMate"
                );
                Log.d(TAG, "Naver SDK initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Naver SDK", e);
            }
        } else {
            Log.w(TAG, "Naver SDK not initialized - credentials not configured in local.properties");
        }
    }

    /**
     * Validate API key from BuildConfig.
     */
    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
