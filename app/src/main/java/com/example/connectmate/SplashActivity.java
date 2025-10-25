package com.example.connectmate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private boolean isDataLoading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Install Android 12+ Splash Screen API
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Keep splash on-screen until data is loaded
        splashScreen.setKeepOnScreenCondition(() -> isDataLoading);

        // Optional: set custom layout background if needed (not mandatory)
        setContentView(R.layout.activity_splash);

        // Start simulated loading
        startLoadingData();
    }

    private void startLoadingData() {
        // Simulate a short loading delay (2 seconds)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isDataLoading = false;

            // Navigate to MainActivity
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Prevent back navigation to splash
        }, 2000);
    }
}