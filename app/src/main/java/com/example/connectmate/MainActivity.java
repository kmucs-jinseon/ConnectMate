package com.example.connectmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * MainActivity - Main container activity with bottom navigation
 * Displays Kakao Map by default in the Map tab
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Fragment tags for identification
    private static final String TAG_MAP = "TAG_MAP";
    private static final String TAG_CHAT = "TAG_CHAT";
    private static final String TAG_PROFILE = "TAG_PROFILE";
    private static final String TAG_SETTING = "TAG_SETTING";

    // Fragment instances
    private Fragment mapFragment;
    private Fragment chatFragment;
    private Fragment profileFragment;
    private Fragment settingFragment;
    private Fragment activeFragment;

    // UI components
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabCreateActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity onCreate started");

        // Initialize UI components
        initializeViews();

        // Initialize all fragments
        initializeFragments(savedInstanceState);

        // Set up bottom navigation
        setupBottomNavigation();

        // Set up floating action button
        setupFloatingActionButton();

        Log.d(TAG, "MainActivity initialized - Kakao Map should be visible");
    }

    /**
     * Initialize UI components from layout
     */
    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fabCreateActivity = findViewById(R.id.fabCreateActivity);

        if (bottomNavigationView == null) {
            Log.e(TAG, "BottomNavigationView not found in layout");
        }
        if (fabCreateActivity == null) {
            Log.e(TAG, "FloatingActionButton not found in layout");
        }
    }

    /**
     * Initialize all fragments and set MapFragment as default
     */
    private void initializeFragments(Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            // First time - create all fragments
            Log.d(TAG, "Creating fragments for the first time");

            // Create fragment instances
            mapFragment = new MapFragment();
            chatFragment = new ChatListFragment();
            profileFragment = new ProfileFragment();
            settingFragment = new SettingsFragment();

            // Add all fragments to container
            // MapFragment is added without hide(), so it's visible by default
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.add(R.id.main_container, mapFragment, TAG_MAP);
            transaction.add(R.id.main_container, chatFragment, TAG_CHAT);
            transaction.add(R.id.main_container, profileFragment, TAG_PROFILE);
            transaction.add(R.id.main_container, settingFragment, TAG_SETTING);

            // Hide all except map
            transaction.hide(chatFragment);
            transaction.hide(profileFragment);
            transaction.hide(settingFragment);

            // Commit immediately
            transaction.commitNow();

            // Set MapFragment as active
            activeFragment = mapFragment;

            Log.d(TAG, "MapFragment set as default - Kakao Map initialized");
        } else {
            // Restore fragments after configuration change
            Log.d(TAG, "Restoring fragments after configuration change");

            mapFragment = fm.findFragmentByTag(TAG_MAP);
            chatFragment = fm.findFragmentByTag(TAG_CHAT);
            profileFragment = fm.findFragmentByTag(TAG_PROFILE);
            settingFragment = fm.findFragmentByTag(TAG_SETTING);

            // Find which fragment is currently visible
            if (mapFragment != null && mapFragment.isVisible()) {
                activeFragment = mapFragment;
            } else if (chatFragment != null && chatFragment.isVisible()) {
                activeFragment = chatFragment;
            } else if (profileFragment != null && profileFragment.isVisible()) {
                activeFragment = profileFragment;
            } else if (settingFragment != null && settingFragment.isVisible()) {
                activeFragment = settingFragment;
            } else {
                activeFragment = mapFragment;
            }
        }
    }

    /**
     * Set up bottom navigation bar to switch between fragments
     */
    private void setupBottomNavigation() {
        if (bottomNavigationView == null) {
            Log.e(TAG, "Cannot setup navigation - BottomNavigationView is null");
            return;
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment targetFragment = null;
            String fragmentName = "";

            // Determine which fragment to show
            if (itemId == R.id.nav_map) {
                targetFragment = mapFragment;
                fragmentName = "Map (Kakao Map)";
            } else if (itemId == R.id.nav_chat) {
                targetFragment = chatFragment;
                fragmentName = "Chat";
            } else if (itemId == R.id.nav_profile) {
                targetFragment = profileFragment;
                fragmentName = "Profile";
            } else if (itemId == R.id.nav_settings) {
                targetFragment = settingFragment;
                fragmentName = "Settings";
            }

            // Switch fragments if target is different from active
            if (targetFragment != null && targetFragment != activeFragment) {
                Log.d(TAG, "Switching to " + fragmentName + " fragment");
                switchFragment(targetFragment);
            }

            return true;
        });

        // Set Map as the default selected item
        bottomNavigationView.setSelectedItemId(R.id.nav_map);
        Log.d(TAG, "Bottom navigation set to Map tab by default");
    }

    /**
     * Switch from active fragment to target fragment
     */
    private void switchFragment(@NonNull Fragment targetFragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        // Hide current active fragment
        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }

        // Show target fragment
        transaction.show(targetFragment);
        transaction.commit();

        // Update active fragment reference
        activeFragment = targetFragment;
    }

    /**
     * Set up floating action button to create new activities
     */
    private void setupFloatingActionButton() {
        if (fabCreateActivity == null) {
            Log.e(TAG, "Cannot setup FAB - FloatingActionButton is null");
            return;
        }

        fabCreateActivity.setOnClickListener(v -> {
            Log.d(TAG, "FAB clicked - Opening CreateActivityActivity");
            Intent intent = new Intent(MainActivity.this, CreateActivityActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity paused");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed");
    }
}
