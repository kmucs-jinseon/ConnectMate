package com.example.connectmate;

import android.app.Application;
import android.util.Log;

import com.kakao.sdk.common.KakaoSdk;
import com.kakao.vectormap.KakaoMapSdk;
import com.navercorp.nid.NaverIdLoginSDK;

public class ConnectMateApplication extends Application {

    private static final String TAG = "ConnectMateApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize both Kakao Login and Kakao Map SDKs
        initializeKakaoSdks();

        // Initialize Naver SDK (Restoring original functionality)
        initializeNaverSdk();
    }

    private void initializeKakaoSdks() {
        if (isValidApiKey(BuildConfig.KAKAO_APP_KEY)) {
            try {
                // Initialize Kakao Login SDK
                KakaoSdk.init(this, BuildConfig.KAKAO_APP_KEY);
                // Initialize Kakao Vector Map SDK
                KakaoMapSdk.init(this, BuildConfig.KAKAO_APP_KEY);
                Log.d(TAG, "Kakao SDKs (Login & Map) initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize Kakao SDKs", e);
            }
        } else {
            Log.w(TAG, "Kakao SDKs not initialized - API key not configured in local.properties");
        }
    }

    private void initializeNaverSdk() {
        if (isValidApiKey(BuildConfig.NAVER_CLIENT_ID) && isValidApiKey(BuildConfig.NAVER_CLIENT_SECRET)) {
            try {
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

    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
