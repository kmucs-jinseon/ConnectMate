package com.example.connectmate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * SplashActivity
 * Displays the splash screen while the app initializes or loads data.
 * Uses Android 12+ SplashScreen API for smooth transition.
 * Checks if user is already authenticated before navigating.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    // Flag to keep the splash screen visible until loading completes
    private boolean isDataLoading = true;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Install the Android 12+ splash screen before super.onCreate()
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Keep the splash screen visible while data is loading
        splashScreen.setKeepOnScreenCondition(() -> isDataLoading);

        // Start a simulated loading operation (replace with real data init)
        startLoadingData();
    }

    /**
     * Simulates data loading (e.g., API calls, DB setup).
     * Replace this with your actual initialization logic if needed.
     * Also checks if user is already authenticated.
     */
    private void startLoadingData() {
        // Use Handler tied to the main looper to delay transition
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Data loading complete
            isDataLoading = false;

            // Check if user is already logged in
            FirebaseUser currentUser = mAuth.getCurrentUser();

            Intent intent;
            if (currentUser != null) {
                // User is already authenticated, go to MainActivity
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                // User is not authenticated, go to LoginActivity
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);

            // Finish SplashActivity to prevent back navigation to it
            finish();
        }, 2000); // 2 seconds delay
    }
}