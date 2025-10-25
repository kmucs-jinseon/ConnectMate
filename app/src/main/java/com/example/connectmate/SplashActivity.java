package com.example.connectmate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

/**
 * SplashActivity
 * Displays the splash screen while the app initializes or loads data.
 * Uses Android 12+ SplashScreen API for smooth transition.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    // Flag to keep the splash screen visible until loading completes
    private boolean isDataLoading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Install the Android 12+ splash screen before super.onCreate()
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Keep the splash screen visible while data is loading
        splashScreen.setKeepOnScreenCondition(() -> isDataLoading);

        // Start a simulated loading operation (replace with real data init)
        startLoadingData();
    }

    /**
     * Simulates data loading (e.g., API calls, DB setup).
     * Replace this with your actual initialization logic if needed.
     */
    private void startLoadingData() {
        // Use Handler tied to the main looper to delay transition
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Data loading complete
            isDataLoading = false;

            // Launch the main activity
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);

            // Finish SplashActivity to prevent back navigation to it
            finish();
        }, 2000); // 2 seconds delay
    }
}