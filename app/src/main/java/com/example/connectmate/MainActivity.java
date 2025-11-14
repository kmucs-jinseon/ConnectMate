package com.example.connectmate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.connectmate.models.PlaceSearchResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    // UI Overlays
    private LinearLayout mapUiOverlay;
    private LinearLayout mapControls;

    // Map UI components
    private EditText searchInput;
    private ImageButton searchSubmitButton;
    private ImageButton searchClearButton;
    private ChipGroup filterChips;
    private CardView currentLocationCard;
    private TextView currentLocationText;
    private FloatingActionButton btnCurrentLocation;
    private FloatingActionButton btnMapType;
    private FloatingActionButton btnZoomIn;
    private FloatingActionButton btnZoomOut;

    // Activity UI components
    private LinearLayout activityUiOverlay;
    private ChipGroup activityFilterChips;

    // Navigation & FAB
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabCreateActivity;

    // Search components
    private CardView mainSearchResultsCard;
    private RecyclerView mainSearchResultsRecycler;
    private PlaceSearchAdapter mainSearchAdapter;
    private List<PlaceSearchResult> mainSearchResults;
    private OkHttpClient httpClient;
    private Gson gson;
    private android.os.Handler searchHandler;
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY_MS = 800;
    private boolean profileSetupRedirectPending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "MainActivity onCreate started");

        // Check if user is authenticated (Firebase, Kakao, or Naver)
        if (!isUserAuthenticated()) {
            Log.d(TAG, "User not authenticated, redirecting to LoginActivity");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        Log.d(TAG, "User authenticated successfully, proceeding with MainActivity setup");

        setContentView(R.layout.activity_main);

        if (maybeRedirectToProfileSetup()) {
            return;
        }

        Log.d(TAG, "MainActivity onCreate started");

        // Initialize UI components
        initializeViews();

        // Initialize all fragments
        initializeFragments(savedInstanceState);

        // Set up map controls
        setupMapControls();

        // Set up activity controls
        setupActivityControls();

        // Set up bottom navigation
        setupBottomNavigation();

        // Set up floating action button
        setupFloatingActionButton();

        // Handle map navigation intent
        handleMapNavigationIntent();

        Log.d(TAG, "MainActivity initialized with background map");
    }

    /**
     * Handle intent to navigate to specific location on map
     */
    private void handleMapNavigationIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("navigate_to_map", false)) {
            double latitude = intent.getDoubleExtra("map_latitude", 0.0);
            double longitude = intent.getDoubleExtra("map_longitude", 0.0);
            String title = intent.getStringExtra("map_title");
            boolean showDirections = intent.getBooleanExtra("show_directions", false);
            String transportMode = intent.getStringExtra("transport_mode");  // "car" or "walk"

            if (latitude != 0.0 && longitude != 0.0) {
                if (showDirections) {
                    Log.d(TAG, "Walking route directions requested to: " + title + " at (" + latitude + ", " + longitude + ")");
                } else {
                    Log.d(TAG, "Map navigation requested: " + title + " at (" + latitude + ", " + longitude + ")");
                }

                // Switch to map tab
                if (bottomNavigationView != null) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_map);
                }

                // Navigate to location or show walking route on map (MapFragment will handle timing)
                if (mapFragment instanceof MapFragment) {
                    if (showDirections) {
                        // Show walking route using T Map Pedestrian API
                        ((MapFragment) mapFragment).showRouteToLocation(latitude, longitude, title);
                    } else {
                        // Just navigate to location without route
                        ((MapFragment) mapFragment).navigateToLocation(latitude, longitude, title);
                    }
                }
            }
        }
    }

    private boolean maybeRedirectToProfileSetup() {
        if (profileSetupRedirectPending) {
            return true;
        }

        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        boolean profileCompleted = prefs.getBoolean("profile_completed", true);
        if (profileCompleted) {
            return false;
        }

        profileSetupRedirectPending = true;

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        String userId = prefs.getString("user_id", firebaseUser != null ? firebaseUser.getUid() : null);
        String email = prefs.getString("user_email", firebaseUser != null ? firebaseUser.getEmail() : null);
        String displayName = prefs.getString("user_name", firebaseUser != null ? firebaseUser.getDisplayName() : null);
        String loginMethod = prefs.getString("login_method", "email");

        prefs.edit().putBoolean("profile_completed", false).apply();

        Intent intent = new Intent(this, ProfileSetupActivity.class);
        if (!TextUtils.isEmpty(userId)) {
            intent.putExtra(ProfileSetupActivity.EXTRA_USER_ID, userId);
        }
        if (!TextUtils.isEmpty(email)) {
            intent.putExtra(ProfileSetupActivity.EXTRA_DEFAULT_EMAIL, email);
        }
        if (!TextUtils.isEmpty(displayName)) {
            intent.putExtra(ProfileSetupActivity.EXTRA_DEFAULT_NAME, displayName);
        }
        if (!TextUtils.isEmpty(loginMethod)) {
            intent.putExtra(ProfileSetupActivity.EXTRA_LOGIN_METHOD, loginMethod);
        }
        startActivity(intent);
        finish();
        return true;
    }

    /**
     * Initialize UI components from layout
     */
    private void initializeViews() {
        // Navigation & FAB
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fabCreateActivity = findViewById(R.id.fabCreateActivity);

        // UI Overlays
        mapUiOverlay = findViewById(R.id.map_ui_overlay);
        mapControls = findViewById(R.id.map_controls);

        // Map UI controls
        searchInput = findViewById(R.id.search_input);
        searchSubmitButton = findViewById(R.id.search_submit_button);
        searchClearButton = findViewById(R.id.search_clear_button);
        filterChips = findViewById(R.id.filter_chips);
        currentLocationCard = findViewById(R.id.current_location_card);
        currentLocationText = findViewById(R.id.current_location_text);
        btnCurrentLocation = findViewById(R.id.btn_current_location);
        btnZoomIn = findViewById(R.id.btn_zoom_in);
        btnZoomOut = findViewById(R.id.btn_zoom_out);

        // Activity UI components
        activityUiOverlay = findViewById(R.id.activity_ui_overlay);
        activityFilterChips = findViewById(R.id.activity_filter_chips);

        // Search results components
        mainSearchResultsCard = findViewById(R.id.main_search_results_card);
        mainSearchResultsRecycler = findViewById(R.id.main_search_results_recycler);

        // Initialize HTTP client and search components
        httpClient = new OkHttpClient();
        gson = new Gson();
        mainSearchResults = new ArrayList<>();
        searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        if (bottomNavigationView == null) {
            Log.e(TAG, "BottomNavigationView not found in layout");
        }
    }

    // Map initialization methods removed - MapFragment now handles all map logic

    /**
     * Setup map controls and listeners
     */
    private void setupMapControls() {
        // Filter chips
        if (filterChips != null) {
            filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                Toast.makeText(this, "Filter changed", Toast.LENGTH_SHORT).show();
            });
        }

        // Setup search adapter
        if (mainSearchResultsRecycler != null) {
            mainSearchAdapter = new PlaceSearchAdapter(mainSearchResults, place -> {
                // When place is clicked, navigate to it on map
                hideMainSearchResults();

                // Switch to map tab
                if (bottomNavigationView != null) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_map);
                }

                // Navigate MapFragment to this location
                if (mapFragment instanceof MapFragment) {
                    ((MapFragment) mapFragment).navigateToLocation(
                        place.getLatitude(),
                        place.getLongitude(),
                        place.getPlaceName()
                    );
                }
            });

            mainSearchResultsRecycler.setLayoutManager(new LinearLayoutManager(this));
            mainSearchResultsRecycler.setAdapter(mainSearchAdapter);
        }

        // Search clear button - CLEAR SEARCH
        if (searchClearButton != null) {
            searchClearButton.setOnClickListener(v -> {
                if (searchInput != null) {
                    searchInput.setText("");
                    hideMainSearchResults();
                    searchClearButton.setVisibility(View.GONE);
                    Log.d(TAG, "Search cleared");
                }
            });
        }

        // Search submit button - MANUAL SEARCH
        if (searchSubmitButton != null) {
            searchSubmitButton.setOnClickListener(v -> {
                if (searchInput != null) {
                    String query = searchInput.getText().toString().trim();
                    if (!query.isEmpty()) {
                        Log.d(TAG, "Manual search triggered: " + query);
                        performMainSearch(query);
                        hideKeyboard();
                    } else {
                        Toast.makeText(this, "검색어를 입력하세요", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Search input with real-time search
        if (searchInput != null) {
            // Handle Enter key on keyboard
            searchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    String query = searchInput.getText().toString().trim();
                    if (!query.isEmpty()) {
                        Log.d(TAG, "Keyboard search triggered: " + query);
                        performMainSearch(query);
                        hideKeyboard();
                    } else {
                        Toast.makeText(this, "검색어를 입력하세요", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            });

            // Real-time search as user types
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Not needed
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // Cancel previous search request
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }

                    String query = s.toString().trim();

                    // Show/hide clear button based on text
                    if (searchClearButton != null) {
                        searchClearButton.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    // If empty, hide results
                    if (query.isEmpty()) {
                        hideMainSearchResults();
                        return;
                    }

                    // Schedule new search after delay (debounce)
                    searchRunnable = () -> {
                        Log.d(TAG, "Real-time search triggered for: " + query);
                        performMainSearch(query);
                    };

                    searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
                }
            });
        }

        // Map control buttons - Call MapFragment methods
        if (btnCurrentLocation != null) {
            btnCurrentLocation.setOnClickListener(v -> {
                if (mapFragment instanceof MapFragment) {
                    ((MapFragment) mapFragment).moveToCurrent();
                    // Update location display when user clicks current location button
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        updateCurrentLocationDisplay();
                    }, 500);
                }
            });
        }
        if (btnMapType != null) {
            btnMapType.setOnClickListener(v -> {
                if (mapFragment instanceof MapFragment) {
                    ((MapFragment) mapFragment).toggleMapType();
                }
            });
        }
        if (btnZoomIn != null) {
            btnZoomIn.setOnClickListener(v -> {
                if (mapFragment instanceof MapFragment) {
                    ((MapFragment) mapFragment).zoomIn();
                }
            });
        }
        if (btnZoomOut != null) {
            btnZoomOut.setOnClickListener(v -> {
                if (mapFragment instanceof MapFragment) {
                    ((MapFragment) mapFragment).zoomOut();
                }
            });
        }
    }

    /**
     * Setup activity controls and listeners
     */
    private void setupActivityControls() {
        // Activity filter chips
        if (activityFilterChips != null) {
            activityFilterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                // Notify ActivityListFragment about filter change
                if (activityFragment instanceof ActivityListFragment) {
                    // The fragment will handle its own filtering
                    // This is just for MainActivity-level coordination
                    Log.d(TAG, "Activity filter changed, checkedIds: " + checkedIds.size());
                }
            });
        }
    }

    // addSampleActivityMarkers removed - MapFragment now handles markers

    private void toggleFilterChips() {
        if (filterChips.getVisibility() == View.VISIBLE) {
            filterChips.setVisibility(View.GONE);
        } else {
            filterChips.setVisibility(View.VISIBLE);
        }
    }

    // Map control methods removed - MapFragment now handles all map operations

    /**
     * Initialize all fragments and set MapFragment as default
     */
    private void initializeFragments(Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            // First time - create fragments
            Log.d(TAG, "Creating fragments");

            // Create MapFragment as default visible fragment
            mapFragment = new MapFragment();

            // Pre-initialize ActivityListFragment so it starts loading data from Firebase
            activityFragment = new ActivityListFragment();

            // Use FragmentTransaction to add fragments
            FragmentTransaction transaction = fm.beginTransaction();

            // Add MapFragment as visible
            transaction.add(R.id.main_container, mapFragment, TAG_MAP);

            // Add ActivityListFragment as hidden (will start loading data in background)
            transaction.add(R.id.main_container, activityFragment, TAG_ACTIVITY);
            transaction.hide(activityFragment);

            transaction.commit();

            // Set MapFragment as active
            activeFragment = mapFragment;

            Log.d(TAG, "MapFragment set as default, ActivityListFragment pre-initialized");
        } else {
            // Restore active fragment after configuration change
            Log.d(TAG, "Restoring fragments after configuration change");

            Fragment currentFragment = fm.findFragmentById(R.id.main_container);
            if (currentFragment != null) {
                activeFragment = currentFragment;

                // Restore fragment references based on tag
                if (TAG_MAP.equals(currentFragment.getTag())) {
                    mapFragment = currentFragment;
                } else if (TAG_CHAT.equals(currentFragment.getTag())) {
                    chatFragment = currentFragment;
                } else if (TAG_ACTIVITY.equals(currentFragment.getTag())) {
                    activityFragment = currentFragment;
                } else if (TAG_SETTING.equals(currentFragment.getTag())) {
                    settingFragment = currentFragment;
                }
            }

            // Also restore hidden fragments
            if (activityFragment == null) {
                activityFragment = fm.findFragmentByTag(TAG_ACTIVITY);
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
            String targetTag = null;

            // Determine which fragment to show based on tag
            if (itemId == R.id.nav_map) {
                targetTag = TAG_MAP;
            } else if (itemId == R.id.nav_chat) {
                targetTag = TAG_CHAT;
            } else if (itemId == R.id.nav_activity) {
                targetTag = TAG_ACTIVITY;
            } else if (itemId == R.id.nav_settings) {
                targetTag = TAG_SETTING;
            }

            // Switch fragments if target is different from active
            if (targetTag != null) {
                String currentTag = (activeFragment != null) ? activeFragment.getTag() : null;
                if (!targetTag.equals(currentTag)) {
                    Log.d(TAG, "Switching from " + currentTag + " to " + targetTag);
                    switchFragmentByTag(targetTag);
                }
            }

            return true;
        });

        // Set Map as the default selected item
        bottomNavigationView.setSelectedItemId(R.id.nav_map);
        Log.d(TAG, "Bottom navigation set to Map tab by default");
    }

    /**
     * Switch to fragment by tag
     * Uses show/hide to preserve fragment state and enable pre-initialization
     */
    private void switchFragmentByTag(String tag) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment targetFragment = null;

        // Create or get the target fragment based on tag
        if (TAG_MAP.equals(tag)) {
            if (mapFragment == null) mapFragment = new MapFragment();
            targetFragment = mapFragment;
        } else if (TAG_CHAT.equals(tag)) {
            if (chatFragment == null) chatFragment = new ChatListFragment();
            targetFragment = chatFragment;
        } else if (TAG_ACTIVITY.equals(tag)) {
            if (activityFragment == null) activityFragment = new ActivityListFragment();
            targetFragment = activityFragment;
        } else if (TAG_SETTING.equals(tag)) {
            if (settingFragment == null) settingFragment = new SettingsFragment();
            targetFragment = settingFragment;
        }

        if (targetFragment == null) {
            Log.e(TAG, "Failed to create fragment for tag: " + tag);
            return;
        }

        // Don't switch if already active
        if (activeFragment == targetFragment) {
            return;
        }

        FragmentTransaction transaction = fm.beginTransaction();

        // Hide current fragment if it exists
        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }

        // Show target fragment if already added, otherwise add it
        if (targetFragment.isAdded()) {
            transaction.show(targetFragment);
        } else {
            transaction.add(R.id.main_container, targetFragment, tag);
        }

        // For MapFragment, update current location display after view is created
        if (TAG_MAP.equals(tag)) {
            transaction.runOnCommit(() -> {
                // Use Handler to post location update after fragment is fully attached
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    updateCurrentLocationDisplay();
                }, 500); // Small delay to ensure map is loaded
            });
        }

        // For SettingsFragment, add a callback to refresh data after view is created
        if (TAG_SETTING.equals(tag) && targetFragment instanceof SettingsFragment) {
            final SettingsFragment settingsFragment = (SettingsFragment) targetFragment;
            transaction.runOnCommit(() -> {
                // Use Handler to post refresh after fragment is fully attached
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    settingsFragment.refreshUserData();
                });
            });
        }

        transaction.commit();

        // Update active fragment reference
        activeFragment = targetFragment;

        // Show/hide UI overlays based on selected fragment
        updateMapUIVisibility(targetFragment);

        Log.d(TAG, "Switched to fragment with tag: " + tag);
    }

    /**
     * Update UI overlay visibility based on active fragment
     * This ensures each tab has its own distinct appearance without overlapping
     */
    private void updateMapUIVisibility(Fragment fragment) {
        // Determine which tab is active based on fragment tag
        String fragmentTag = (fragment != null) ? fragment.getTag() : null;
        boolean isMapTab = TAG_MAP.equals(fragmentTag);
        boolean isActivityTab = TAG_ACTIVITY.equals(fragmentTag);
        boolean isChatTab = TAG_CHAT.equals(fragmentTag);
        boolean isSettingTab = TAG_SETTING.equals(fragmentTag);

        // Show map UI overlay ONLY when Map tab is active
        if (mapUiOverlay != null) {
            mapUiOverlay.setVisibility(isMapTab ? View.VISIBLE : View.GONE);
        }
        if (mapControls != null) {
            mapControls.setVisibility(isMapTab ? View.VISIBLE : View.GONE);
        }

        // Show activity UI overlay ONLY when Activity tab is active
        if (activityUiOverlay != null) {
            activityUiOverlay.setVisibility(isActivityTab ? View.VISIBLE : View.GONE);
        }

        // Update fragment container background and padding for each tab
        View fragmentContainer = findViewById(R.id.main_container);
        if (fragmentContainer != null) {
            // Reset to default state first
            fragmentContainer.setPadding(0, 0, 0, 0);

            if (isMapTab) {
                // MapFragment now contains its own map, no special background needed
                fragmentContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                Log.d(TAG, "Map tab active - showing MapFragment");
            } else if (isActivityTab) {
                // White background for Activity tab
                fragmentContainer.setBackgroundColor(getResources().getColor(android.R.color.white));

                // Add top padding to push content below the activity overlay
                if (activityUiOverlay != null) {
                    activityUiOverlay.post(() -> {
                        int overlayHeight = activityUiOverlay.getHeight();
                        if (overlayHeight > 0) {
                            fragmentContainer.setPadding(0, overlayHeight, 0, 0);
                            Log.d(TAG, "Activity tab active - white background, top padding: " + overlayHeight + "px");
                        } else {
                            // Fallback if height not measured yet
                            int fallbackPadding = (int) (150 * getResources().getDisplayMetrics().density);
                            fragmentContainer.setPadding(0, fallbackPadding, 0, 0);
                            Log.d(TAG, "Activity tab active - using fallback padding: " + fallbackPadding + "px");
                        }
                    });
                }
            } else if (isChatTab) {
                // White background for Chat tab, no padding
                fragmentContainer.setBackgroundColor(getResources().getColor(android.R.color.white));
                Log.d(TAG, "Chat tab active - white background, no padding");
            } else if (isSettingTab) {
                // White background for Settings tab, no padding
                fragmentContainer.setBackgroundColor(getResources().getColor(android.R.color.white));
                Log.d(TAG, "Settings tab active - white background, no padding");
            }
        }

        // Hide FAB on Settings tab, show on others
        if (fabCreateActivity != null) {
            fabCreateActivity.setVisibility(isSettingTab ? View.GONE : View.VISIBLE);
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

        // Ensure UI visibility is correct when resuming (e.g., after creating an activity)
        if (activeFragment != null) {
            updateMapUIVisibility(activeFragment);
        }

        Log.d(TAG, "MainActivity resumed");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);  // Update the intent
        Log.d(TAG, "MainActivity onNewIntent called");
        handleMapNavigationIntent();
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

    // Location methods removed - MapFragment now handles all location logic

    /**
     * Perform place search using Kakao Local API
     */
    private void performMainSearch(String query) {
        if (query.isEmpty()) {
            hideMainSearchResults();
            return;
        }

        Log.d(TAG, "Searching for: " + query);

        // Get current location for proximity search
        String apiUrl = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" +
            android.net.Uri.encode(query);

        // Try to get current location from MapFragment
        Location currentLocation = getCurrentLocationFromMap();
        if (currentLocation != null) {
            double longitude = currentLocation.getLongitude();
            double latitude = currentLocation.getLatitude();
            apiUrl += "&x=" + longitude + "&y=" + latitude + "&radius=20000"; // 20km radius
            Log.d(TAG, "Searching near current location: " + latitude + ", " + longitude);
        } else {
            // Default to Seoul center if no location available
            apiUrl += "&x=126.9780&y=37.5665&radius=20000";
            Log.d(TAG, "Using default location (Seoul) for search");
        }

        Request request = new Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "KakaoAK " + BuildConfig.KAKAO_REST_API_KEY)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Main search request failed", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                    "검색 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Main search response not successful: " + response.code());
                    return;
                }

                String responseBody = response.body().string();

                try {
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray documents = jsonObject.getAsJsonArray("documents");

                    List<PlaceSearchResult> results = new ArrayList<>();
                    for (int i = 0; i < documents.size(); i++) {
                        JsonObject doc = documents.get(i).getAsJsonObject();

                        PlaceSearchResult place = new PlaceSearchResult();
                        place.setId(doc.get("id").getAsString());
                        place.setPlaceName(doc.get("place_name").getAsString());
                        place.setAddressName(doc.get("address_name").getAsString());

                        if (doc.has("road_address_name") && !doc.get("road_address_name").isJsonNull()) {
                            place.setRoadAddressName(doc.get("road_address_name").getAsString());
                        }

                        if (doc.has("category_name") && !doc.get("category_name").isJsonNull()) {
                            place.setCategoryName(doc.get("category_name").getAsString());
                        }

                        // Kakao API returns x=longitude, y=latitude
                        place.setLatitude(doc.get("y").getAsDouble());
                        place.setLongitude(doc.get("x").getAsDouble());

                        if (doc.has("distance") && !doc.get("distance").isJsonNull()) {
                            place.setDistance(doc.get("distance").getAsInt());
                        }

                        results.add(place);
                    }

                    runOnUiThread(() -> {
                        if (results.isEmpty()) {
                            hideMainSearchResults();
                            Toast.makeText(MainActivity.this, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show();
                        } else {
                            displayMainSearchResults(results);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing main search results", e);
                }
            }
        });
    }

    /**
     * Get current location from MapFragment if available
     */
    private Location getCurrentLocationFromMap() {
        if (mapFragment instanceof MapFragment) {
            return ((MapFragment) mapFragment).getCurrentLocationForSearch();
        }
        return null;
    }

    /**
     * Display search results in RecyclerView
     */
    private void displayMainSearchResults(List<PlaceSearchResult> results) {
        mainSearchResults.clear();
        mainSearchResults.addAll(results);
        if (mainSearchAdapter != null) {
            mainSearchAdapter.updateResults(mainSearchResults);
        }
        if (mainSearchResultsCard != null) {
            mainSearchResultsCard.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "Displaying " + results.size() + " main search results");
    }

    /**
     * Hide search results
     */
    private void hideMainSearchResults() {
        if (mainSearchResultsCard != null) {
            mainSearchResultsCard.setVisibility(View.GONE);
        }
        mainSearchResults.clear();
        if (mainSearchAdapter != null) {
            mainSearchAdapter.updateResults(mainSearchResults);
        }
    }

    /**
     * Hide keyboard
     */
    private void hideKeyboard() {
        if (searchInput != null) {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
            }
        }
    }

    /**
     * Check if user is authenticated via any login method (Firebase, Kakao, or Naver)
     * Only check auto-login if this is a fresh app start (not from LoginActivity)
     */
    private boolean isUserAuthenticated() {
        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);

        // Check if user is coming from a fresh login (skip auto-login check in this case)
        boolean justLoggedIn = getIntent().getBooleanExtra("just_logged_in", false);
        Log.d(TAG, "isUserAuthenticated - justLoggedIn flag: " + justLoggedIn);

        // Check Firebase Auth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            Log.d(TAG, "Firebase user found: " + firebaseUser.getEmail());
            return true;
        }

        // Check SharedPreferences for social login state
        boolean isSocialLoggedIn = prefs.getBoolean("is_logged_in", false);
        String loginMethod = prefs.getString("login_method", "");
        Log.d(TAG, "Social login state - is_logged_in: " + isSocialLoggedIn + ", method: " + loginMethod);

        if (isSocialLoggedIn) {
            // If user just logged in, allow access regardless of auto-login setting
            if (justLoggedIn) {
                Log.d(TAG, "User just logged in: " + loginMethod + " - allowing access");
                return true;
            }

            // For existing sessions, check auto-login preference
            boolean autoLoginEnabled = prefs.getBoolean("auto_login", false);
            Log.d(TAG, "Auto-login preference: " + autoLoginEnabled);
            if (!autoLoginEnabled) {
                Log.d(TAG, "Auto-login is disabled - redirecting to login page");
                return false;
            }

            Log.d(TAG, "Social login session found with auto-login enabled: " + loginMethod);
            return true;
        }

        Log.d(TAG, "No valid user session found");
        return false;
    }

    /**
     * Update current location display using reverse geocoding
     */
    public void updateCurrentLocationDisplay() {
        Location currentLocation = getCurrentLocationFromMap();
        if (currentLocation != null) {
            reverseGeocode(currentLocation.getLatitude(), currentLocation.getLongitude());
        }
        // Don't show "Getting location..." - card stays hidden until we have actual location
    }

    /**
     * Reverse geocode coordinates to get address using Kakao API
     */
    private void reverseGeocode(double latitude, double longitude) {
        String apiUrl = "https://dapi.kakao.com/v2/local/geo/coord2address.json?x=" +
            longitude + "&y=" + latitude;

        Request request = new Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "KakaoAK " + BuildConfig.KAKAO_REST_API_KEY)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Reverse geocoding failed", e);
                // Keep card hidden on failure
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Reverse geocoding response not successful: " + response.code());
                    return;
                }

                String responseBody = response.body().string();

                try {
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray documents = jsonObject.getAsJsonArray("documents");

                    if (documents.size() > 0) {
                        JsonObject doc = documents.get(0).getAsJsonObject();

                        // Try to get road address first, fallback to regular address
                        String address = null;
                        if (doc.has("road_address") && !doc.get("road_address").isJsonNull()) {
                            JsonObject roadAddress = doc.getAsJsonObject("road_address");
                            address = roadAddress.get("address_name").getAsString();
                        } else if (doc.has("address") && !doc.get("address").isJsonNull()) {
                            JsonObject regAddress = doc.getAsJsonObject("address");
                            address = regAddress.get("address_name").getAsString();
                        }

                        if (address != null) {
                            // Shorten the address for display
                            String shortAddress = shortenAddress(address);
                            String finalAddress = shortAddress;
                            runOnUiThread(() -> {
                                if (currentLocationText != null) {
                                    currentLocationText.setText(finalAddress);
                                }
                                if (currentLocationCard != null) {
                                    currentLocationCard.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing reverse geocoding results", e);
                    runOnUiThread(() -> {
                        if (currentLocationText != null) {
                            currentLocationText.setText("현재 위치");
                        }
                    });
                }
            }
        });
    }

    /**
     * Shorten address for compact display
     * Example: "서울특별시 강남구 역삼동" -> "강남구 역삼동"
     */
    private String shortenAddress(String fullAddress) {
        if (fullAddress == null) return "현재 위치";

        String[] parts = fullAddress.split(" ");
        if (parts.length >= 3) {
            // Return last 2-3 parts (district and neighborhood)
            return parts[parts.length - 2] + " " + parts[parts.length - 1];
        } else if (parts.length == 2) {
            return parts[0] + " " + parts[1];
        }
        return fullAddress;
    }
}
