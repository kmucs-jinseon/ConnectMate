package com.example.connectmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.connectmate.utils.ThemeManager;

public class ProfileSetupActivity extends AppCompatActivity implements ProfileSetupFragment.ProfileSetupHost {

    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_DEFAULT_EMAIL = "extra_default_email";
    public static final String EXTRA_DEFAULT_NAME = "extra_default_name";
    public static final String EXTRA_LOGIN_METHOD = "extra_login_method";

    private String userId;
    private String defaultEmail;
    private String defaultName;
    private String loginMethod;
    private ImageButton themeToggleButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        userId = getIntent().getStringExtra(EXTRA_USER_ID);
        defaultEmail = getIntent().getStringExtra(EXTRA_DEFAULT_EMAIL);
        defaultName = getIntent().getStringExtra(EXTRA_DEFAULT_NAME);
        loginMethod = getIntent().getStringExtra(EXTRA_LOGIN_METHOD);

        Toolbar toolbar = findViewById(R.id.profile_setup_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // Set up theme toggle button
        themeToggleButton = findViewById(R.id.theme_toggle_button);
        if (themeToggleButton != null) {
            updateThemeIcon();
            themeToggleButton.setOnClickListener(v -> toggleTheme());
        }

        if (savedInstanceState == null) {
            ProfileSetupFragment fragment = ProfileSetupFragment.newInstance(
                    userId,
                    defaultEmail,
                    defaultName,
                    loginMethod
            );

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.profile_setup_container, fragment)
                    .commit();
        }
    }

    @Override
    public void onProfileSetupCompleted() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("just_logged_in", true);
        startActivity(intent);
        finish();
    }

    /**
     * Toggle between light and dark mode
     */
    private void toggleTheme() {
        int currentMode = ThemeManager.getThemeMode(this);
        int newMode;

        // Cycle through: Light -> Dark -> Light
        if (currentMode == ThemeManager.MODE_LIGHT) {
            newMode = ThemeManager.MODE_DARK;
        } else {
            newMode = ThemeManager.MODE_LIGHT;
        }

        ThemeManager.setThemeMode(this, newMode);
        // Activity will be recreated automatically by AppCompatDelegate
    }

    /**
     * Update the theme toggle icon based on current theme
     */
    private void updateThemeIcon() {
        if (themeToggleButton == null) return;

        boolean isDarkMode = ThemeManager.isDarkModeActive(this);

        // Show dark mode icon (moon) when in dark mode
        // Show light mode icon (sun) when in light mode
        if (isDarkMode) {
            themeToggleButton.setImageResource(R.drawable.ic_dark_mode);
        } else {
            themeToggleButton.setImageResource(R.drawable.ic_light_mode);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateThemeIcon();
    }
}
