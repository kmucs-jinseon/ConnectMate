package com.example.connectmate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * SplashActivity
 * Displays a custom splash screen with logo, app title, and tagline.
 * Shows for 2 seconds while checking user authentication status.
 * Navigates to MainActivity if user is logged in, otherwise to LoginActivity.
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the custom splash screen layout
        // Layout contains: logo, app title "Connect Mate", and tagline
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Start the authentication check and navigation
        navigateAfterDelay();
    }

    /**
     * Waits 2 seconds to display the splash screen, then checks authentication
     * and navigates to the appropriate screen.
     */
    private void navigateAfterDelay() {
        // Use Handler to delay the navigation by 2 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check if user is already logged in with Firebase
            FirebaseUser currentUser = mAuth.getCurrentUser();

            Intent intent;
            if (currentUser != null) {
                // User is authenticated - go directly to MainActivity
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                // User is not authenticated - go to LoginActivity
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);

            // Finish SplashActivity to prevent back navigation
            finish();
        }, 2000); // 2 seconds splash screen duration
    }
}