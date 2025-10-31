package com.example.connectmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.camera.CameraUpdate;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyles;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.Label;
import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity - Main container activity with bottom navigation
 * Contains background map view and manages fragments
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Fragment tags for identification
    private static final String TAG_MAP = "TAG_MAP";
    private static final String TAG_CHAT = "TAG_CHAT";
    private static final String TAG_ACTIVITY = "TAG_ACTIVITY";
    private static final String TAG_SETTING = "TAG_SETTING";

    // Fragment instances
    private Fragment mapFragment;
    private Fragment chatFragment;
    private Fragment activityFragment;
    private Fragment settingFragment;
    private Fragment activeFragment;

    // Map components
    private MapView mainMapView;
    private KakaoMap kakaoMap;
    private LinearLayout mapUiOverlay;
    private LinearLayout mapControls;
    private ProgressBar loadingIndicator;

    // Map UI components
    private EditText searchInput;
    private ImageButton filterButton;
    private ChipGroup filterChips;
    private FloatingActionButton btnCurrentLocation;
    private FloatingActionButton btnMapType;
    private FloatingActionButton btnZoomIn;
    private FloatingActionButton btnZoomOut;

    // Navigation & FAB
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabCreateActivity;

    // Data
    private List<MapFragment.ActivityMarker> activityMarkers;
    private boolean isNormalMapType = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity onCreate started");

        // Initialize data
        activityMarkers = new ArrayList<>();

        // Initialize UI components
        initializeViews();

        // Initialize map
        initializeMap();

        // Initialize all fragments
        initializeFragments(savedInstanceState);

        // Set up map controls
        setupMapControls();

        // Set up bottom navigation
        setupBottomNavigation();

        // Set up floating action button
        setupFloatingActionButton();

        Log.d(TAG, "MainActivity initialized with background map");
    }

    /**
     * Initialize UI components from layout
     */
    private void initializeViews() {
        // Navigation & FAB
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fabCreateActivity = findViewById(R.id.fabCreateActivity);

        // Map components
        mainMapView = findViewById(R.id.main_map_view);
        mapUiOverlay = findViewById(R.id.map_ui_overlay);
        mapControls = findViewById(R.id.map_controls);
        loadingIndicator = findViewById(R.id.loading_indicator);

        // Map UI controls
        searchInput = findViewById(R.id.search_input);
        filterButton = findViewById(R.id.filter_button);
        filterChips = findViewById(R.id.filter_chips);
        btnCurrentLocation = findViewById(R.id.btn_current_location);
        btnMapType = findViewById(R.id.btn_map_type);
        btnZoomIn = findViewById(R.id.btn_zoom_in);
        btnZoomOut = findViewById(R.id.btn_zoom_out);

        if (bottomNavigationView == null) {
            Log.e(TAG, "BottomNavigationView not found in layout");
        }
        if (mainMapView == null) {
            Log.e(TAG, "Main MapView not found in layout");
        }
    }

    /**
     * Initialize Kakao Map
     */
    private void initializeMap() {
        if (mainMapView == null) {
            Log.e(TAG, "Cannot initialize map - MapView is null");
            return;
        }

        Log.d(TAG, "Initializing background Kakao Map...");
        showLoading(true);

        mainMapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                Log.d(TAG, "Background map destroyed");
            }

            @Override
            public void onMapError(Exception error) {
                showLoading(false);
                if (error != null) {
                    Log.e(TAG, "═══════════════════════════════════════════");
                    Log.e(TAG, "MAP INITIALIZATION ERROR:");
                    Log.e(TAG, "Error message: " + error.getMessage());
                    Log.e(TAG, "Error type: " + error.getClass().getSimpleName());
                    Log.e(TAG, "═══════════════════════════════════════════");
                    error.printStackTrace();

                    String errorMsg = "Map failed: " + error.getMessage();
                    if (error.getMessage() != null && error.getMessage().contains("auth")) {
                        errorMsg = "Map authentication failed. Check Logcat for key hash.";
                    }
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap map) {
                Log.d(TAG, "═══════════════════════════════════════════");
                Log.d(TAG, "✓ MAP READY - Map initialized successfully!");
                Log.d(TAG, "MapView is visible: " + (mainMapView.getVisibility() == View.VISIBLE));
                Log.d(TAG, "MapView dimensions: " + mainMapView.getWidth() + "x" + mainMapView.getHeight());
                Log.d(TAG, "═══════════════════════════════════════════");

                MainActivity.this.kakaoMap = map;
                showLoading(false);

                // Center on Seoul
                map.moveCamera(CameraUpdateFactory.newCenterPosition(
                    LatLng.from(37.5665, 126.9780), 13));
                Log.d(TAG, "✓ Camera moved to Seoul");

                // Add sample activity markers
                addSampleActivityMarkers();
            }
        });
    }

    /**
     * Setup map controls and listeners
     */
    private void setupMapControls() {
        // Filter button
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> toggleFilterChips());
        }

        // Filter chips
        if (filterChips != null) {
            filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                Toast.makeText(this, "Filter changed", Toast.LENGTH_SHORT).show();
            });
        }

        // Search input
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!s.toString().isEmpty()) {
                        Log.d(TAG, "Searching for: " + s);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Map control buttons
        if (btnCurrentLocation != null) {
            btnCurrentLocation.setOnClickListener(v -> moveToCurrentLocation());
        }
        if (btnMapType != null) {
            btnMapType.setOnClickListener(v -> toggleMapType());
        }
        if (btnZoomIn != null) {
            btnZoomIn.setOnClickListener(v -> zoomIn());
        }
        if (btnZoomOut != null) {
            btnZoomOut.setOnClickListener(v -> zoomOut());
        }
    }

    private void addSampleActivityMarkers() {
        if (kakaoMap == null) return;

        // Sample activities
        activityMarkers.add(new MapFragment.ActivityMarker(
            "1", "Weekly Soccer Match", "Seoul National Park",
            "Today, 3:00 PM", "Join us for a friendly soccer match!",
            5, 10, 37.5665, 126.9780, "Sports"
        ));
        activityMarkers.add(new MapFragment.ActivityMarker(
            "2", "Study Group - Java", "Gangnam Library",
            "Tomorrow, 2:00 PM", "Let's study Java together",
            3, 8, 37.5700, 126.9850, "Study"
        ));
        activityMarkers.add(new MapFragment.ActivityMarker(
            "3", "Coffee Meetup", "Hongdae Cafe",
            "Saturday, 4:00 PM", "Casual coffee and chat",
            6, 12, 37.5550, 126.9200, "Social"
        ));

        try {
            if (kakaoMap.getLabelManager() != null) {
                LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();

                for (MapFragment.ActivityMarker activity : activityMarkers) {
                    LabelStyles styles = kakaoMap.getLabelManager()
                        .addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.ic_map_pin)));

                    LabelOptions options = LabelOptions.from(
                        activity.getId(),
                        LatLng.from(activity.getLatitude(), activity.getLongitude())
                    ).setStyles(styles);

                    if (labelLayer != null) {
                        Label label = labelLayer.addLabel(options);
                        label.setClickable(true);
                    }
                }

                kakaoMap.setOnLabelClickListener((kakaoMap, layer, label) -> {
                    String labelId = label.getLabelId();
                    Toast.makeText(MainActivity.this,
                        "Activity: " + labelId,
                        Toast.LENGTH_SHORT).show();
                    return true;
                });

                Log.d(TAG, "✓ Added " + activityMarkers.size() + " activity markers");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add markers", e);
        }
    }

    private void toggleFilterChips() {
        if (filterChips.getVisibility() == View.VISIBLE) {
            filterChips.setVisibility(View.GONE);
        } else {
            filterChips.setVisibility(View.VISIBLE);
        }
    }

    private void moveToCurrentLocation() {
        if (kakaoMap == null) return;
        Toast.makeText(this, "Moving to current location...", Toast.LENGTH_SHORT).show();
        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(
            LatLng.from(37.5665, 126.9780), 15));
    }

    private void toggleMapType() {
        if (kakaoMap == null) return;
        isNormalMapType = !isNormalMapType;
        Toast.makeText(this,
            "Map type: " + (isNormalMapType ? "Normal" : "Satellite"),
            Toast.LENGTH_SHORT).show();
    }

    private void zoomIn() {
        if (kakaoMap == null) return;
        try {
            CameraUpdate update = CameraUpdateFactory.zoomIn();
            kakaoMap.moveCamera(update);
        } catch (Exception e) {
            Log.e(TAG, "Failed to zoom in", e);
        }
    }

    private void zoomOut() {
        if (kakaoMap == null) return;
        try {
            CameraUpdate update = CameraUpdateFactory.zoomOut();
            kakaoMap.moveCamera(update);
        } catch (Exception e) {
            Log.e(TAG, "Failed to zoom out", e);
        }
    }

    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
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
            activityFragment = new ActivityListFragment();
            settingFragment = new SettingsFragment();

            // Add all fragments to container
            // MapFragment is added without hide(), so it's visible by default
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.add(R.id.main_container, mapFragment, TAG_MAP);
            transaction.add(R.id.main_container, chatFragment, TAG_CHAT);
            transaction.add(R.id.main_container, activityFragment, TAG_ACTIVITY);
            transaction.add(R.id.main_container, settingFragment, TAG_SETTING);

            // Hide all except map
            transaction.hide(chatFragment);
            transaction.hide(activityFragment);
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
            activityFragment = fm.findFragmentByTag(TAG_ACTIVITY);
            settingFragment = fm.findFragmentByTag(TAG_SETTING);

            // Find which fragment is currently visible
            if (mapFragment != null && mapFragment.isVisible()) {
                activeFragment = mapFragment;
            } else if (chatFragment != null && chatFragment.isVisible()) {
                activeFragment = chatFragment;
            } else if (activityFragment != null && activityFragment.isVisible()) {
                activeFragment = activityFragment;
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
            } else if (itemId == R.id.nav_activity) {
                targetFragment = activityFragment;
                fragmentName = "Activity";
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

        // Show/hide map UI overlay based on selected fragment
        updateMapUIVisibility(targetFragment);
    }

    /**
     * Update map UI overlay visibility based on active fragment
     */
    private void updateMapUIVisibility(Fragment fragment) {
        boolean isMapTab = (fragment == mapFragment);

        // Show map UI overlay only when Map tab is active
        if (mapUiOverlay != null) {
            mapUiOverlay.setVisibility(isMapTab ? View.VISIBLE : View.GONE);
        }
        if (mapControls != null) {
            mapControls.setVisibility(isMapTab ? View.VISIBLE : View.GONE);
        }

        // Update fragment container background
        View fragmentContainer = findViewById(R.id.main_container);
        if (fragmentContainer != null) {
            if (isMapTab) {
                // Transparent background to show map
                fragmentContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            } else {
                // White background for other fragments
                fragmentContainer.setBackgroundColor(getResources().getColor(android.R.color.white));
            }
        }
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
        if (mainMapView != null) {
            mainMapView.resume();
        }
        Log.d(TAG, "MainActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mainMapView != null) {
            mainMapView.pause();
        }
        Log.d(TAG, "MainActivity paused");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Properly finish the map to prevent crashes
        if (mainMapView != null) {
            mainMapView.finish();
        }
        mainMapView = null;
        kakaoMap = null;
        Log.d(TAG, "MainActivity destroyed");
    }
}
