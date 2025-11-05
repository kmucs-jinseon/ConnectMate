package com.example.connectmate;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Base64;
import android.util.Log;

import com.kakao.sdk.common.KakaoSdk;
import com.kakao.vectormap.KakaoMapSdk;
import com.navercorp.nid.NaverIdLoginSDK;

import java.security.MessageDigest;

public class ConnectMateApplication extends Application {

    private static final String TAG = "ConnectMateApp";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "═══════════════════════════════════════════");
        Log.d(TAG, "ConnectMate Application Starting...");
        Log.d(TAG, "═══════════════════════════════════════════");

        // Print configuration status
        printConfigurationStatus();

        // Print key hash for Kakao registration
        printKeyHash();

        // Initialize both Kakao Login and Kakao Map SDKs
        initializeKakaoSdks();

        // Initialize Naver SDK (Restoring original functionality)
        initializeNaverSdk();

        // Check network connectivity
        checkNetworkConnectivity();
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

    /**
     * Print configuration status for all social login providers
     */
    private void printConfigurationStatus() {
        Log.d(TAG, "═══════════════════════════════════════════");
        Log.d(TAG, "SOCIAL LOGIN CONFIGURATION STATUS");
        Log.d(TAG, "═══════════════════════════════════════════");

        // Kakao
        boolean kakaoConfigured = isValidApiKey(BuildConfig.KAKAO_APP_KEY);
        Log.d(TAG, "✓ Kakao: " + (kakaoConfigured ? "CONFIGURED" : "NOT CONFIGURED"));
        if (kakaoConfigured) {
            Log.d(TAG, "  - App Key: " + BuildConfig.KAKAO_APP_KEY.substring(0, 8) + "...");
            Log.d(TAG, "  - Package: " + getPackageName());
            Log.d(TAG, "  - Next: Register key hash in Kakao Console");
        }

        // Naver
        boolean naverConfigured = isValidApiKey(BuildConfig.NAVER_CLIENT_ID) &&
                                  isValidApiKey(BuildConfig.NAVER_CLIENT_SECRET);
        Log.d(TAG, "✓ Naver: " + (naverConfigured ? "CONFIGURED" : "NOT CONFIGURED"));
        if (naverConfigured) {
            Log.d(TAG, "  - Client ID: " + BuildConfig.NAVER_CLIENT_ID);
            Log.d(TAG, "  - Package: " + getPackageName());
        }

        Log.d(TAG, "═══════════════════════════════════════════");
    }

    /**
     * Print the key hash that needs to be registered in Kakao Developers Console
     * This is CRITICAL for Kakao Maps to work
     */
    private void printKeyHash() {
        Log.d(TAG, "Attempting to generate key hash...");
        Log.d(TAG, "Android SDK: " + android.os.Build.VERSION.SDK_INT);
        Log.d(TAG, "Package name: " + getPackageName());

        try {
            PackageInfo info;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                // Android 9.0 (API 28) and above
                Log.d(TAG, "Using GET_SIGNING_CERTIFICATES (API 28+)");
                info = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_SIGNING_CERTIFICATES);
                if (info.signingInfo != null) {
                    Signature[] signatures = info.signingInfo.getApkContentsSigners();
                    Log.d(TAG, "Found " + signatures.length + " signature(s)");
                    for (Signature signature : signatures) {
                        printSignatureHash(signature);
                    }
                } else {
                    Log.e(TAG, "SigningInfo is null!");
                }
            } else {
                // Below Android 9.0
                Log.d(TAG, "Using GET_SIGNATURES (API < 28)");
                info = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_SIGNATURES);
                Log.d(TAG, "Found " + info.signatures.length + " signature(s)");
                for (Signature signature : info.signatures) {
                    printSignatureHash(signature);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + getPackageName(), e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get key hash - Exception: " + e.getClass().getName(), e);
            Log.e(TAG, "Error message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printSignatureHash(Signature signature) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(signature.toByteArray());
            String keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            Log.d(TAG, "═══════════════════════════════════════════");
            Log.d(TAG, "KAKAO KEY HASH (Register this in Kakao Console):");
            Log.d(TAG, keyHash);
            Log.d(TAG, "Package Name: " + getPackageName());
            Log.d(TAG, "═══════════════════════════════════════════");
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate hash from signature", e);
        }
    }

    /**
     * Check network connectivity for debugging
     * This helps identify if the device can reach Kakao servers
     */
    private void checkNetworkConnectivity() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
                boolean hasInternet = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                boolean hasValidated = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

                Log.d(TAG, "═══════════════════════════════════════════");
                Log.d(TAG, "NETWORK CONNECTIVITY STATUS");
                Log.d(TAG, "═══════════════════════════════════════════");
                Log.d(TAG, "Connected to network: " + (caps != null));
                Log.d(TAG, "Has internet capability: " + hasInternet);
                Log.d(TAG, "Internet validated: " + hasValidated);

                if (caps != null) {
                    Log.d(TAG, "WiFi: " + caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                    Log.d(TAG, "Cellular: " + caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
                    Log.d(TAG, "Ethernet: " + caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
                }

                if (!hasInternet || !hasValidated) {
                    Log.w(TAG, "⚠️ WARNING: Device may not have working internet!");
                    Log.w(TAG, "Map tiles will fail to load without internet access");
                }
                Log.d(TAG, "═══════════════════════════════════════════");
            } else {
                Log.e(TAG, "❌ ConnectivityManager is null!");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check network connectivity", e);
        }
    }
}
