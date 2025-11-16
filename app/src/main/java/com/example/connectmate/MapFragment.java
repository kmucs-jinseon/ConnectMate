package com.example.connectmate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyles;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.Label;
import com.example.connectmate.models.Activity;
import com.example.connectmate.models.PlaceSearchResult;
import com.example.connectmate.utils.CategoryMapper;
import com.example.connectmate.utils.FirebaseActivityManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MapFragment - Fragment containing Kakao Map with activity markers
 */
public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";

    // Map components
    private MapView mapView;
    private KakaoMap kakaoMap;
    private ProgressBar loadingIndicator;
    private View emulatorWarning;

    // Search UI components
    private com.google.android.material.textfield.TextInputLayout searchLayout;
    private com.google.android.material.textfield.TextInputEditText searchInput;
    private CardView searchResultsCard;
    private RecyclerView searchResultsRecycler;
    private PlaceSearchAdapter searchAdapter;
    private List<PlaceSearchResult> searchResults;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton btnUseLocation;

    // Selected location data
    private PlaceSearchResult selectedPlace;
    private PlaceSearchResult selectedPoi; // POI selected from map click

    // POI Popup Window
    private android.widget.PopupWindow poiPopupWindow;

    // POI Info Card UI components
    private com.google.android.material.card.MaterialCardView poiInfoCard;
    private TextView poiName;
    private TextView poiCategory;
    private TextView poiAddress;
    private TextView poiPhone;
    private TextView poiDistance;
    private TextView poiWebsite;
    private LinearLayout poiPhoneLayout;
    private LinearLayout poiDistanceLayout;
    private LinearLayout poiWebsiteLayout;
    private ImageButton btnClosePoiInfo;
    private com.google.android.material.button.MaterialButton btnUsePoiLocation;

    // HTTP client for Kakao API
    private OkHttpClient httpClient;
    private Gson gson;

    // Data
    private List<ActivityMarker> activityMarkers;
    private Map<String, Label> labelMap; // Map activity ID to label
    private Label currentLocationLabel; // Label for current location marker
    private LocationManager locationManager;
    private boolean isEmulator = false;
    private boolean isMapInitialized = false; // Track if map has been initialized

    // Real-time search with debounce
    private android.os.Handler searchHandler;
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY_MS = 800; // Wait 800ms after user stops typing

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize views
        mapView = view.findViewById(R.id.map_view);
        loadingIndicator = view.findViewById(R.id.loading_indicator);
        emulatorWarning = view.findViewById(R.id.emulator_warning);

        // Initialize search UI
        searchLayout = view.findViewById(R.id.search_layout);
        searchInput = view.findViewById(R.id.search_input);
        searchResultsCard = view.findViewById(R.id.search_results_card);
        searchResultsRecycler = view.findViewById(R.id.search_results_recycler);
        btnUseLocation = view.findViewById(R.id.btn_use_location);

        // POI Info Card will be shown as a PopupWindow - no need to initialize here

        // Debug: Check if search components were found
        if (searchInput == null) {
            Log.e(TAG, "❌ Search input NOT FOUND!");
        } else {
            Log.d(TAG, "✓ Search input found");
        }

        // Initialize HTTP client and JSON parser
        httpClient = new OkHttpClient();
        gson = new Gson();
        searchResults = new ArrayList<>();

        // Initialize search handler for debounced real-time search
        searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        // Setup search functionality
        setupSearchUI();

        // Setup "Use Location" button
        btnUseLocation.setOnClickListener(v -> {
            if (selectedPlace != null) {
                openCreateActivityWithLocation();
            }
        });

        // POI buttons are now handled in popup window initialization

        // Detect if running on emulator
        isEmulator = isRunningOnEmulator();

        if (isEmulator) {
            // Show emulator warning and hide map
            Log.w(TAG, "Running on emulator - Kakao Maps may not work properly");
            if (emulatorWarning != null) {
                emulatorWarning.setVisibility(View.VISIBLE);
            }
            if (mapView != null) {
                mapView.setVisibility(View.GONE);
            }
            // Still initialize data for when switching to real device
            activityMarkers = new ArrayList<>();
            labelMap = new HashMap<>();
            locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            return view;
        }

        // Ensure MapView is visible from the start
        if (mapView != null) {
            mapView.setVisibility(android.view.View.VISIBLE);
            Log.d(TAG, "MapView found and set to VISIBLE in onCreateView");
        } else {
            Log.e(TAG, "ERROR: MapView is NULL!");
        }

        // Initialize data
        activityMarkers = new ArrayList<>();
        labelMap = new HashMap<>();
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        // Wait for view to be laid out before initializing map
        mapView.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Remove listener to avoid multiple calls
                mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                Log.d(TAG, "MapView layout complete - dimensions: " + mapView.getWidth() + "x" + mapView.getHeight());

                // Now initialize the map
                initializeMap();
            }
        });

        return view;
    }

    /**
     * Initialize Kakao Map
     */
    private void initializeMap() {
        if (mapView == null) {
            Log.e(TAG, "Cannot initialize map - MapView is null");
            return;
        }

        // Ensure MapView is visible
        mapView.setVisibility(android.view.View.VISIBLE);

        Log.d(TAG, "Initializing Kakao Map in MapFragment...");
        Log.d(TAG, "MapView visibility: " + mapView.getVisibility());
        Log.d(TAG, "MapView dimensions: " + mapView.getWidth() + "x" + mapView.getHeight());
        showLoading(true);

        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                Log.d(TAG, "Map destroyed");
            }

            @Override
            public void onMapError(Exception error) {
                showLoading(false);
                Log.e(TAG, "Map initialization error", error);

                if (getContext() != null && error != null) {
                    String errorMsg = "Map Error: " + error.getMessage();
                    if (error.getMessage() != null) {
                        if (error.getMessage().contains("auth") || error.getMessage().contains("Authentication")) {
                            errorMsg = "Authentication Failed! Check Kakao Console registration.";
                        } else if (error.getMessage().contains("network") || error.getMessage().contains("Network")) {
                            errorMsg = "Network Error - Check internet connection";
                        }
                    }
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap map) {
                Log.d(TAG, "═══════════════════════════════════");
                Log.d(TAG, "✓ MAP READY in MapFragment!");
                Log.d(TAG, "MapView visibility: " + mapView.getVisibility());
                Log.d(TAG, "MapView dimensions: " + mapView.getWidth() + "x" + mapView.getHeight());
                Log.d(TAG, "═══════════════════════════════════");

                // Store map reference
                kakaoMap = map;
                isMapInitialized = true; // Mark map as initialized

                // Map is now configured and ready to use
                Log.d(TAG, "Map configured successfully");

                showLoading(false);

                // Ensure MapView is visible and on top
                mapView.setVisibility(android.view.View.VISIBLE);
                mapView.bringToFront();
                mapView.requestLayout();

                if (getContext() != null) {
                    Toast.makeText(getContext(), "✓ Map loaded!", Toast.LENGTH_SHORT).show();
                }

                // Center on current location or default
                moveToCurrentLocation();

                // Load activity markers from Firebase with real-time updates
                loadActivitiesFromFirebase();
            }
        });
    }

    /**
     * Load activities from Firebase and display as markers with real-time updates
     */
    private void loadActivitiesFromFirebase() {
        if (kakaoMap == null) {
            Log.e(TAG, "KakaoMap is null, cannot load activities");
            return;
        }

        FirebaseActivityManager activityManager = FirebaseActivityManager.getInstance();

        // Set up click listener for activity markers
        kakaoMap.setOnLabelClickListener((map, layer, label) -> {
            String activityId = label.getLabelId();
            if (getContext() != null && activityId != null) {
                // Find activity details
                for (ActivityMarker marker : activityMarkers) {
                    if (marker.getId().equals(activityId)) {
                        Toast.makeText(getContext(),
                            marker.getTitle() + " (" + marker.getCurrentParticipants() + "/" + marker.getMaxParticipants() + ")",
                            Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
            return true;
        });

        // Set up click listener for map (to search for nearby POIs)
        kakaoMap.setOnMapClickListener((kakaoMap1, position, screenPoint, poi) -> {
            if (poi != null) {
                // User clicked on a POI marker
                Log.d(TAG, "POI clicked: " + poi.getName() + " at (" + position.getLatitude() + ", " + position.getLongitude() + ")");
                fetchPoiDetails(poi.getName(), position.getLatitude(), position.getLongitude());
            } else {
                // User clicked on empty map area - search for nearby places
                Log.d(TAG, "Map clicked at: (" + position.getLatitude() + ", " + position.getLongitude() + ")");
                searchNearbyPlaces(position.getLatitude(), position.getLongitude());
            }
        });

        // Listen for real-time activity changes
        activityManager.listenForActivityChanges(new FirebaseActivityManager.ActivityChangeListener() {
            @Override
            public void onActivityAdded(Activity activity) {
                if (activity.getLatitude() != 0 && activity.getLongitude() != 0) {
                    addActivityMarker(activity);
                    Log.d(TAG, "Activity marker added: " + activity.getTitle());
                }
            }

            @Override
            public void onActivityChanged(Activity activity) {
                // Remove old marker and add updated one
                removeActivityMarker(activity.getId());
                if (activity.getLatitude() != 0 && activity.getLongitude() != 0) {
                    addActivityMarker(activity);
                    Log.d(TAG, "Activity marker updated: " + activity.getTitle());
                }
            }

            @Override
            public void onActivityRemoved(Activity activity) {
                removeActivityMarker(activity.getId());
                Log.d(TAG, "Activity marker removed: " + activity.getTitle());
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading activities from Firebase", e);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "활동 로드 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Add a marker for an activity to the map
     */
    private void addActivityMarker(Activity activity) {
        if (kakaoMap == null || activity == null) return;

        try {
            if (kakaoMap.getLabelManager() != null) {
                LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();

                if (labelLayer != null) {
                    // Create marker style
                    LabelStyles styles = kakaoMap.getLabelManager()
                        .addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.ic_map_pin)));

                    // Create label options
                    LabelOptions options = LabelOptions.from(
                        activity.getId(),
                        LatLng.from(activity.getLatitude(), activity.getLongitude())
                    ).setStyles(styles);

                    // Add label to map
                    Label label = labelLayer.addLabel(options);
                    label.setClickable(true);

                    // Store label reference for later updates/removal
                    labelMap.put(activity.getId(), label);

                    // Also add to activityMarkers list for compatibility
                    ActivityMarker marker = new ActivityMarker(
                        activity.getId(),
                        activity.getTitle(),
                        activity.getLocation(),
                        activity.getTime(),
                        activity.getDescription(),
                        activity.getCurrentParticipants(),
                        activity.getMaxParticipants(),
                        activity.getLatitude(),
                        activity.getLongitude(),
                        activity.getCategory()
                    );
                    activityMarkers.add(marker);

                    Log.d(TAG, "Added marker for: " + activity.getTitle() + " at (" +
                        activity.getLatitude() + ", " + activity.getLongitude() + ")");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add marker for activity: " + activity.getTitle(), e);
        }
    }

    /**
     * Remove a marker from the map
     */
    private void removeActivityMarker(String activityId) {
        if (kakaoMap == null || activityId == null) return;

        try {
            // Remove label from map
            Label label = labelMap.get(activityId);
            if (label != null && kakaoMap.getLabelManager() != null) {
                LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();
                if (labelLayer != null) {
                    labelLayer.remove(label);
                }
                labelMap.remove(activityId);
            }

            // Remove from activityMarkers list
            activityMarkers.removeIf(marker -> marker.getId().equals(activityId));

            Log.d(TAG, "Removed marker for activity: " + activityId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove marker for activity: " + activityId, e);
        }
    }

    /**
     * Setup search UI and functionality
     */
    private void setupSearchUI() {
        // Check if all required views are initialized
        if (searchInput == null) {
            Log.e(TAG, "❌ searchInput is NULL!");
            return;
        }
        if (searchResultsRecycler == null) {
            Log.e(TAG, "❌ searchResultsRecycler is NULL!");
            return;
        }

        Log.d(TAG, "✓ All search UI components found, setting up...");

        // Setup RecyclerView for search results
        searchAdapter = new PlaceSearchAdapter(searchResults, place -> {
            // When a place is clicked, move to that location and hide results
            selectedPlace = place;  // Save selected place
            moveToLocation(place.getLatitude(), place.getLongitude(), place.getPlaceName());
            hideSearchResults();
            hideKeyboard();
            showUseLocationButton();  // Show button to use this location
        });

        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsRecycler.setAdapter(searchAdapter);

        // Text change listener for real-time search
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TextInputLayout handles clear button automatically
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Cancel previous search request
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s.toString().trim();

                // If empty, hide results
                if (query.isEmpty()) {
                    hideSearchResults();
                    hideUseLocationButton();
                    selectedPlace = null;
                    return;
                }

                // Schedule new search after delay (debounce)
                searchRunnable = () -> {
                    Log.d(TAG, "Real-time search triggered for: " + query);
                    performSearchSilent(query);  // Use silent version without toast
                };

                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
        });

        // Handle IME search action (when user presses search on keyboard)
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    /**
     * Perform place search using Kakao Local API
     */
    private void performSearch() {
        Log.d(TAG, "performSearch() called");

        if (searchInput == null) {
            Log.e(TAG, "❌ Cannot search: searchInput is null");
            return;
        }

        String query = searchInput.getText().toString().trim();
        Log.d(TAG, "Search query: '" + query + "'");

        if (query.isEmpty()) {
            Toast.makeText(getContext(), "검색어를 입력하세요", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Search cancelled: empty query");
            return;
        }

        // Hide keyboard
        hideKeyboard();

        // Show searching feedback
        Toast.makeText(getContext(), "'" + query + "' 검색 중...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "🔍 Searching for: " + query);

        // Hide previous results
        hideSearchResults();

        // Get current location for proximity search
        String apiUrl = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" +
            android.net.Uri.encode(query);

        // Add current location to search for nearby results
        Location currentLocation = getCurrentLocation();
        if (currentLocation != null) {
            double longitude = currentLocation.getLongitude();
            double latitude = currentLocation.getLatitude();
            apiUrl += "&x=" + longitude + "&y=" + latitude + "&radius=20000&sort=accuracy"; // 20km radius, sort by relevance
            Log.d(TAG, "Searching near current location: " + latitude + ", " + longitude);
        } else {
            // Default to Seoul center if no location available
            apiUrl += "&x=126.9780&y=37.5665&radius=20000&sort=accuracy";
            Log.d(TAG, "Using default location (Seoul) for search");
        }

        Log.d(TAG, "API URL: " + apiUrl);
        Log.d(TAG, "Using API Key: " + BuildConfig.KAKAO_REST_API_KEY.substring(0, 10) + "...");

        Request request = new Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "KakaoAK " + BuildConfig.KAKAO_REST_API_KEY)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Search request failed", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "❌ 검색 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Search response not successful: " + response.code());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "검색 오류 (Code: " + response.code() + ")", Toast.LENGTH_SHORT).show()
                        );
                    }
                    return;
                }

                String responseBody = response.body().string();
                Log.d(TAG, "Search response received, length: " + responseBody.length());

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
                            String categoryName = doc.get("category_name").getAsString();
                            place.setCategoryName(categoryName);
                            // Map Kakao category to our activity category
                            place.setMappedCategory(CategoryMapper.mapKakaoCategoryToActivity(categoryName));
                        }

                        if (doc.has("phone") && !doc.get("phone").isJsonNull()) {
                            place.setPhone(doc.get("phone").getAsString());
                        }

                        // Kakao API returns x=longitude, y=latitude
                        place.setLatitude(doc.get("y").getAsDouble());
                        place.setLongitude(doc.get("x").getAsDouble());

                        if (doc.has("place_url") && !doc.get("place_url").isJsonNull()) {
                            place.setPlaceUrl(doc.get("place_url").getAsString());
                        }

                        if (doc.has("distance") && !doc.get("distance").isJsonNull()) {
                            place.setDistance(doc.get("distance").getAsInt());
                        }

                        results.add(place);
                    }

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (results.isEmpty()) {
                                Toast.makeText(getContext(), "검색 결과가 없습니다", Toast.LENGTH_SHORT).show();
                                hideSearchResults();
                            } else {
                                displaySearchResults(results);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing search results", e);
                }
            }
        });
    }

    /**
     * Perform place search silently (for real-time search without toast notifications)
     */
    private void performSearchSilent(String query) {
        if (query.isEmpty()) {
            hideSearchResults();
            return;
        }

        Log.d(TAG, "Silent search for: " + query);

        // Get current location for proximity search
        String apiUrl = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" +
            android.net.Uri.encode(query);

        // Add current location to search for nearby results
        Location currentLocation = getCurrentLocation();
        if (currentLocation != null) {
            double longitude = currentLocation.getLongitude();
            double latitude = currentLocation.getLatitude();
            apiUrl += "&x=" + longitude + "&y=" + latitude + "&radius=20000&sort=accuracy"; // 20km radius, sort by relevance
        } else {
            // Default to Seoul center if no location available
            apiUrl += "&x=126.9780&y=37.5665&radius=20000&sort=accuracy";
        }

        Request request = new Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "KakaoAK " + BuildConfig.KAKAO_REST_API_KEY)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Silent search request failed", e);
                // Don't show toast for real-time search failures
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Silent search response not successful: " + response.code());
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
                            String categoryName = doc.get("category_name").getAsString();
                            place.setCategoryName(categoryName);
                            // Map Kakao category to our activity category
                            place.setMappedCategory(CategoryMapper.mapKakaoCategoryToActivity(categoryName));
                        }

                        if (doc.has("phone") && !doc.get("phone").isJsonNull()) {
                            place.setPhone(doc.get("phone").getAsString());
                        }

                        // Kakao API returns x=longitude, y=latitude
                        place.setLatitude(doc.get("y").getAsDouble());
                        place.setLongitude(doc.get("x").getAsDouble());

                        if (doc.has("place_url") && !doc.get("place_url").isJsonNull()) {
                            place.setPlaceUrl(doc.get("place_url").getAsString());
                        }

                        if (doc.has("distance") && !doc.get("distance").isJsonNull()) {
                            place.setDistance(doc.get("distance").getAsInt());
                        }

                        results.add(place);
                    }

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (results.isEmpty()) {
                                hideSearchResults();
                            } else {
                                displaySearchResults(results);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing silent search results", e);
                }
            }
        });
    }

    /**
     * Display search results
     */
    private void displaySearchResults(List<PlaceSearchResult> results) {
        if (searchResults != null) {
            searchResults.clear();
            searchResults.addAll(results);
        }
        if (searchAdapter != null) {
            searchAdapter.updateResults(searchResults);
        }
        if (searchResultsCard != null) {
            searchResultsCard.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "Displaying " + results.size() + " search results");
    }

    /**
     * Hide search results
     */
    private void hideSearchResults() {
        if (searchResultsCard != null) {
            searchResultsCard.setVisibility(View.GONE);
        }
        if (searchResults != null) {
            searchResults.clear();
        }
        if (searchAdapter != null) {
            searchAdapter.updateResults(searchResults);
        }
    }

    /**
     * Move camera to a specific location and add marker
     */
    private void moveToLocation(double latitude, double longitude, String placeName) {
        if (kakaoMap == null) {
            Log.e(TAG, "Cannot move to location - map not ready");
            return;
        }

        try {
            LatLng location = LatLng.from(latitude, longitude);
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(location, 17));

            // Add marker at searched location
            if (kakaoMap.getLabelManager() != null) {
                LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();
                if (labelLayer != null) {
                    LabelStyles styles = kakaoMap.getLabelManager()
                        .addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.ic_map_pin)));

                    LabelOptions options = LabelOptions.from(
                        "search_result_" + System.currentTimeMillis(),
                        location
                    ).setStyles(styles);

                    labelLayer.addLabel(options);
                    Log.d(TAG, "Marker added for: " + placeName);
                }
            }

            Toast.makeText(getContext(), placeName, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error moving to location", e);
        }
    }

    /**
     * Get current location for search
     */
    private Location getCurrentLocation() {
        if (lacksLocationPermission()) {
            return null;
        }

        try {
            Location location = null;

            // Try GPS first
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            // If GPS location is null, try network provider
            if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            return location;
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error", e);
            return null;
        }
    }

    /**
     * Hide keyboard
     */
    private void hideKeyboard() {
        if (getActivity() != null && searchInput != null) {
            InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
            }
        }
    }

    /**
     * Show "Use This Location" button
     */
    private void showUseLocationButton() {
        if (btnUseLocation != null) {
            btnUseLocation.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide "Use This Location" button
     */
    private void hideUseLocationButton() {
        if (btnUseLocation != null) {
            btnUseLocation.setVisibility(View.GONE);
        }
    }

    /**
     * Open CreateActivityActivity with selected location data
     */
    private void openCreateActivityWithLocation() {
        if (selectedPlace == null || getContext() == null) {
            return;
        }

        Intent intent = new Intent(getContext(), CreateActivityActivity.class);
        intent.putExtra("location_name", selectedPlace.getPlaceName());
        intent.putExtra("location_address", selectedPlace.getAddressName());
        intent.putExtra("location_latitude", selectedPlace.getLatitude());
        intent.putExtra("location_longitude", selectedPlace.getLongitude());

        startActivity(intent);

        // Hide the button after using it
        hideUseLocationButton();
        selectedPlace = null;
    }

    /**
     * Add or update current location marker on the map
     */
    private void addCurrentLocationMarker(double latitude, double longitude) {
        if (kakaoMap == null || kakaoMap.getLabelManager() == null) {
            Log.e(TAG, "Cannot add current location marker - map not ready");
            return;
        }

        try {
            LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();
            if (labelLayer == null) {
                Log.e(TAG, "LabelLayer is null");
                return;
            }

            // Remove existing marker if present
            if (currentLocationLabel != null) {
                labelLayer.remove(currentLocationLabel);
                currentLocationLabel = null;
            }

            // Create marker style for current location
            LabelStyle labelStyle = LabelStyle.from(R.drawable.ic_location_marker)
                .setAnchorPoint(0.5f, 1.0f)  // Anchor at bottom center
                .setZoomLevel(0);  // Visible at all zoom levels

            LabelStyles styles = kakaoMap.getLabelManager()
                .addLabelStyles(LabelStyles.from(labelStyle));

            // Create label options
            LabelOptions options = LabelOptions.from(
                "current_location",
                LatLng.from(latitude, longitude)
            ).setStyles(styles)
             .setClickable(false)  // Not clickable
             .setRank(1);  // Higher rank to appear on top of other markers

            // Add marker to map
            currentLocationLabel = labelLayer.addLabel(options);

            if (currentLocationLabel != null) {
                currentLocationLabel.show();  // Ensure marker is visible
                Log.d(TAG, "Current location marker displayed at: " + latitude + ", " + longitude);
            } else {
                Log.e(TAG, "Failed to create current location marker");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add current location marker", e);
        }
    }

    /**
     * Move camera to current location or default location
     */
    private void moveToCurrentLocation() {
        if (kakaoMap == null) {
            Log.e(TAG, "KakaoMap is null, cannot move camera");
            return;
        }

        if (lacksLocationPermission()) {
            Log.w(TAG, "Location permission not granted, using default location");
            moveToDefaultLocation();
            return;
        }

        Log.d(TAG, "Location permission granted, attempting to get location...");

        try {
            Location location = null;

            // Try GPS first
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.d(TAG, "GPS provider is enabled");
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location != null) {
                    Log.d(TAG, "Got location from GPS provider");
                }
            } else {
                Log.d(TAG, "GPS provider is NOT enabled");
            }

            // If GPS location is null, try network provider
            if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Log.d(TAG, "Trying network provider...");
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    Log.d(TAG, "Got location from network provider");
                }
            }

            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                LatLng currentLocation = LatLng.from(latitude, longitude);

                Log.d(TAG, "=== MOVING TO CURRENT LOCATION ===");
                Log.d(TAG, "Latitude: " + latitude + ", Longitude: " + longitude);

                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(currentLocation, 17));

                // Add current location marker
                addCurrentLocationMarker(latitude, longitude);

                Log.d(TAG, "Camera moved to current location: " + latitude + ", " + longitude);
            } else {
                Log.w(TAG, "Location is null, using default location");
                moveToDefaultLocation();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error", e);
            moveToDefaultLocation();
        }
    }

    /**
     * Move camera to default location (Seoul)
     */
    private void moveToDefaultLocation() {
        if (kakaoMap != null) {
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(
                    LatLng.from(37.5665, 126.9780), 17));
            Log.d(TAG, "Camera moved to default location (Seoul)");
        }
    }

    /**
     * Check if location permissions are missing
     */
    private boolean lacksLocationPermission() {
        return getContext() == null || (
            ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        );
    }

    /**
     * Show/hide loading indicator
     */
    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Get the activity markers
     */
    public List<ActivityMarker> getActivityMarkers() {
        return activityMarkers;
    }

    /**
     * Get current location for external search (e.g., MainActivity search)
     */
    public Location getCurrentLocationForSearch() {
        return getCurrentLocation();
    }

    /**
     * Zoom in on the map
     */
    public void zoomIn() {
        if (kakaoMap != null) {
            try {
                kakaoMap.moveCamera(com.kakao.vectormap.camera.CameraUpdateFactory.zoomIn());
                Log.d(TAG, "Zoomed in");
            } catch (Exception e) {
                Log.e(TAG, "Failed to zoom in", e);
            }
        }
    }

    /**
     * Zoom out on the map
     */
    public void zoomOut() {
        if (kakaoMap != null) {
            try {
                kakaoMap.moveCamera(com.kakao.vectormap.camera.CameraUpdateFactory.zoomOut());
                Log.d(TAG, "Zoomed out");
            } catch (Exception e) {
                Log.e(TAG, "Failed to zoom out", e);
            }
        }
    }

    /**
     * Move camera to current location
     */
    public void moveToCurrent() {
        moveToCurrentLocation();
    }

    /**
     * Toggle map type (normal/satellite/hybrid)
     */
    public void toggleMapType() {
        if (kakaoMap != null) {
            // TODO: Implement map type toggle when needed
            Log.d(TAG, "Map type toggle requested");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Map type toggle", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Navigate to a specific location on the map
     * @param latitude Target latitude
     * @param longitude Target longitude
     * @param title Location title/name
     */
    public void navigateToLocation(double latitude, double longitude, String title) {
        Log.d(TAG, "Navigating to location: " + title + " at (" + latitude + ", " + longitude + ")");

        if (kakaoMap == null) {
            Log.w(TAG, "KakaoMap is not ready yet, waiting...");
            // Wait for map to be ready
            if (mapView != null) {
                mapView.post(() -> navigateToLocation(latitude, longitude, title));
            }
            return;
        }

        try {
            LatLng targetLocation = LatLng.from(latitude, longitude);

            // Move camera to target location with appropriate zoom level
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(targetLocation, 17));

            // Add a marker for the target location
            if (kakaoMap.getLabelManager() != null) {
                LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();

                if (labelLayer != null) {
                    LabelStyles styles = kakaoMap.getLabelManager()
                        .addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.ic_map_pin)));

                    LabelOptions options = LabelOptions.from(
                        "target_" + System.currentTimeMillis(),
                        targetLocation
                    ).setStyles(styles);

                    labelLayer.addLabel(options);
                }
            }

            if (getContext() != null) {
                Toast.makeText(getContext(), "Navigated to: " + title, Toast.LENGTH_SHORT).show();
            }

            Log.d(TAG, "Successfully navigated to location");
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to location", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to navigate to location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Show walking route to a specific location
     * @param latitude Target latitude
     * @param longitude Target longitude
     * @param title Location title/name
     */
    public void showRouteToLocation(double latitude, double longitude, String title) {
        Log.d(TAG, "Showing route to location: " + title + " at (" + latitude + ", " + longitude + ")");

        if (kakaoMap == null) {
            Log.w(TAG, "KakaoMap is not ready yet, waiting...");
            // Wait for map to be ready
            if (mapView != null) {
                mapView.post(() -> showRouteToLocation(latitude, longitude, title));
            }
            return;
        }

        // First navigate to the location
        navigateToLocation(latitude, longitude, title);

        // TODO: Implement route drawing using T Map Pedestrian API
        // This would require:
        // 1. Get current location
        // 2. Call T Map Pedestrian API to get route
        // 3. Draw polyline on map with the route
        // For now, just show a message
        if (getContext() != null) {
            Toast.makeText(getContext(),
                "Route to " + title + " (Route drawing coming soon)",
                Toast.LENGTH_LONG).show();
        }

        Log.d(TAG, "Route display requested - full implementation pending");
    }

    /**
     * Search for nearby places when user clicks on empty map area
     */
    private void searchNearbyPlaces(double latitude, double longitude) {
        Log.d(TAG, "Searching for nearby places at: (" + latitude + ", " + longitude + ")");

        // Build API URL for category search (search for any type of place)
        String apiUrl = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" +
            android.net.Uri.encode("맛집") + // Default search for restaurants
            "&x=" + longitude +
            "&y=" + latitude +
            "&radius=100&sort=distance"; // Search within 100 meters, sort by distance

        Log.d(TAG, "Nearby places API URL: " + apiUrl);

        Request request = new Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "KakaoAK " + BuildConfig.KAKAO_REST_API_KEY)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to search nearby places", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Nearby search response not successful: " + response.code());
                    return;
                }

                String responseBody = response.body().string();

                try {
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray documents = jsonObject.getAsJsonArray("documents");

                    if (documents != null && documents.size() > 0) {
                        // Get the first (closest) result
                        JsonObject doc = documents.get(0).getAsJsonObject();

                        PlaceSearchResult poi = new PlaceSearchResult();
                        poi.setId(doc.get("id").getAsString());
                        poi.setPlaceName(doc.get("place_name").getAsString());
                        poi.setAddressName(doc.get("address_name").getAsString());

                        if (doc.has("road_address_name") && !doc.get("road_address_name").isJsonNull()) {
                            poi.setRoadAddressName(doc.get("road_address_name").getAsString());
                        }

                        if (doc.has("category_name") && !doc.get("category_name").isJsonNull()) {
                            String categoryName = doc.get("category_name").getAsString();
                            poi.setCategoryName(categoryName);
                            poi.setMappedCategory(CategoryMapper.mapKakaoCategoryToActivity(categoryName));
                        }

                        if (doc.has("phone") && !doc.get("phone").isJsonNull()) {
                            poi.setPhone(doc.get("phone").getAsString());
                        }

                        poi.setLatitude(doc.get("y").getAsDouble());
                        poi.setLongitude(doc.get("x").getAsDouble());

                        if (doc.has("place_url") && !doc.get("place_url").isJsonNull()) {
                            poi.setPlaceUrl(doc.get("place_url").getAsString());
                        }

                        if (doc.has("distance") && !doc.get("distance").isJsonNull()) {
                            poi.setDistance(doc.get("distance").getAsInt());
                        }

                        // Display POI info on UI thread
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> displayPoiInfo(poi));
                        }

                        Log.d(TAG, "Nearby place found: " + poi.getPlaceName());
                    } else {
                        Log.d(TAG, "No nearby places found");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing nearby places", e);
                }
            }
        });
    }

    /**
     * Fetch POI (Point of Interest) details from Kakao Local API
     * This is called when user clicks on a real place on the map
     */
    private void fetchPoiDetails(String poiName, double latitude, double longitude) {
        Log.d(TAG, "Fetching POI details for: " + poiName + " at (" + latitude + ", " + longitude + ")");

        // Build API URL for keyword search with coordinates
        // Sort by accuracy (relevance) first to get the most relevant match
        String apiUrl = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" +
            android.net.Uri.encode(poiName) +
            "&x=" + longitude +
            "&y=" + latitude +
            "&radius=50" + // Search within 50 meters
            "&sort=accuracy"; // Sort by relevance

        Log.d(TAG, "POI API URL: " + apiUrl);

        Request request = new Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "KakaoAK " + BuildConfig.KAKAO_REST_API_KEY)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to fetch POI details", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "POI 정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "POI fetch response not successful: " + response.code());
                    return;
                }

                String responseBody = response.body().string();
                Log.d(TAG, "POI response received");

                try {
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray documents = jsonObject.getAsJsonArray("documents");

                    if (documents != null && documents.size() > 0) {
                        // Get the first (closest) result
                        JsonObject doc = documents.get(0).getAsJsonObject();

                        PlaceSearchResult poi = new PlaceSearchResult();
                        poi.setId(doc.get("id").getAsString());
                        poi.setPlaceName(doc.get("place_name").getAsString());
                        poi.setAddressName(doc.get("address_name").getAsString());

                        if (doc.has("road_address_name") && !doc.get("road_address_name").isJsonNull()) {
                            poi.setRoadAddressName(doc.get("road_address_name").getAsString());
                        }

                        if (doc.has("category_name") && !doc.get("category_name").isJsonNull()) {
                            String categoryName = doc.get("category_name").getAsString();
                            poi.setCategoryName(categoryName);
                            poi.setMappedCategory(CategoryMapper.mapKakaoCategoryToActivity(categoryName));
                        }

                        if (doc.has("phone") && !doc.get("phone").isJsonNull()) {
                            poi.setPhone(doc.get("phone").getAsString());
                        }

                        // Kakao API returns x=longitude, y=latitude
                        poi.setLatitude(doc.get("y").getAsDouble());
                        poi.setLongitude(doc.get("x").getAsDouble());

                        if (doc.has("place_url") && !doc.get("place_url").isJsonNull()) {
                            poi.setPlaceUrl(doc.get("place_url").getAsString());
                        }

                        if (doc.has("distance") && !doc.get("distance").isJsonNull()) {
                            poi.setDistance(doc.get("distance").getAsInt());
                        }

                        // Display POI info on UI thread
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> displayPoiInfo(poi));
                        }

                        Log.d(TAG, "POI details fetched successfully: " + poi.getPlaceName());
                    } else {
                        Log.w(TAG, "No POI results found");
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), poiName, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing POI details", e);
                }
            }
        });
    }

    /**
     * Display POI information in a popup window
     */
    private void displayPoiInfo(PlaceSearchResult poi) {
        if (poi == null || getContext() == null || getView() == null) {
            return;
        }

        Log.d(TAG, "Displaying POI info: " + poi.getPlaceName());

        // Store selected POI
        selectedPoi = poi;

        // Hide search results if visible
        hideSearchResults();
        hideUseLocationButton();

        // Dismiss existing popup if any
        if (poiPopupWindow != null && poiPopupWindow.isShowing()) {
            poiPopupWindow.dismiss();
        }

        // Inflate popup layout - inflate just the fragment to get a fresh copy
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(getContext());
        android.view.View fullLayout = inflater.inflate(R.layout.fragment_map, null, false);
        android.view.View popupView = fullLayout.findViewById(R.id.poi_info_card);

        // Remove the popup view from its parent so it can be added to PopupWindow
        if (popupView.getParent() != null) {
            ((android.view.ViewGroup) popupView.getParent()).removeView(popupView);
        }

        // Make the popup view visible
        popupView.setVisibility(View.VISIBLE);

        // Enhance the card's shadow/elevation for clearer edges
        if (popupView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView cardView = (com.google.android.material.card.MaterialCardView) popupView;
            cardView.setCardElevation(24f); // Increased elevation for more prominent shadow
            cardView.setMaxCardElevation(24f);
        }

        // Initialize UI components from popup view
        initializePoiPopupComponents(popupView);

        // Calculate popup width with margins
        int horizontalMarginDp = 16; // Left and right margins
        int horizontalMarginPx = (int) (horizontalMarginDp * getResources().getDisplayMetrics().density);

        // Get screen width and subtract margins
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int popupWidth = screenWidth - (horizontalMarginPx * 2);

        // Create popup window with calculated width
        poiPopupWindow = new android.widget.PopupWindow(
            popupView,
            popupWidth,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        );

        // Set background (semi-transparent to allow shadow rendering)
        poiPopupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Set high elevation to ensure it appears on top
        poiPopupWindow.setElevation(100f);

        // Allow outside touch to dismiss
        poiPopupWindow.setOutsideTouchable(true);

        // Set POI name
        if (poiName != null) {
            poiName.setText(poi.getPlaceName());
        }

        // Set category
        if (poiCategory != null) {
            String category = poi.getCategoryName();
            if (category != null && !category.isEmpty()) {
                // Show only the last part of the category (e.g., "음식점 > 카페" -> "카페")
                String[] parts = category.split(" > ");
                poiCategory.setText(parts[parts.length - 1]);
                poiCategory.setVisibility(View.VISIBLE);
            } else {
                poiCategory.setVisibility(View.GONE);
            }
        }

        // Set address (prefer road address if available)
        if (poiAddress != null) {
            String address = poi.getRoadAddressName();
            if (address == null || address.isEmpty()) {
                address = poi.getAddressName();
            }
            poiAddress.setText(address);
        }

        // Set phone number (if available)
        if (poiPhoneLayout != null && poiPhone != null) {
            String phone = poi.getPhone();
            if (phone != null && !phone.isEmpty()) {
                poiPhone.setText(phone);
                poiPhoneLayout.setVisibility(View.VISIBLE);
            } else {
                poiPhoneLayout.setVisibility(View.GONE);
            }
        }

        // Set distance (if available)
        if (poiDistanceLayout != null && poiDistance != null) {
            int distance = poi.getDistance();
            if (distance > 0) {
                String distanceText;
                if (distance < 1000) {
                    distanceText = distance + "m";
                } else {
                    distanceText = String.format("%.1fkm", distance / 1000.0);
                }
                poiDistance.setText(distanceText);
                poiDistanceLayout.setVisibility(View.VISIBLE);
            } else {
                poiDistanceLayout.setVisibility(View.GONE);
            }
        }

        // Set website URL (if available - note: Kakao API doesn't provide this, but structure is here for future enhancement)
        if (poiWebsiteLayout != null && poiWebsite != null) {
            // This would be populated if we had website data from the API
            // For now, it will remain hidden
            poiWebsiteLayout.setVisibility(View.GONE);
        }

        // Show popup window at bottom of screen, above the menu bar
        // Convert dp to pixels for bottom margin
        int bottomMarginDp = 140; // Height to clear the bottom navigation bar with extra spacing
        int bottomMarginPx = (int) (bottomMarginDp * getResources().getDisplayMetrics().density);

        poiPopupWindow.showAtLocation(
            getView(),
            android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL,
            0,
            bottomMarginPx
        );

        Log.d(TAG, "POI popup displayed successfully");
        Log.d(TAG, "POI Details - Name: " + poi.getPlaceName() + ", Category: " + poi.getCategoryName() +
                ", Phone: " + poi.getPhone() + ", Distance: " + poi.getDistance() + "m");
    }

    /**
     * Initialize POI popup UI components
     */
    private void initializePoiPopupComponents(android.view.View popupView) {
        // Find views in popup
        poiName = popupView.findViewById(R.id.poi_name);
        poiCategory = popupView.findViewById(R.id.poi_category);
        poiAddress = popupView.findViewById(R.id.poi_address);
        poiPhone = popupView.findViewById(R.id.poi_phone);
        poiDistance = popupView.findViewById(R.id.poi_distance);
        poiWebsite = popupView.findViewById(R.id.poi_website);
        poiPhoneLayout = popupView.findViewById(R.id.poi_phone_layout);
        poiDistanceLayout = popupView.findViewById(R.id.poi_distance_layout);
        poiWebsiteLayout = popupView.findViewById(R.id.poi_website_layout);
        btnClosePoiInfo = popupView.findViewById(R.id.btn_close_poi_info);
        btnUsePoiLocation = popupView.findViewById(R.id.btn_use_poi_location);

        // Setup close button
        if (btnClosePoiInfo != null) {
            btnClosePoiInfo.setOnClickListener(v -> {
                if (poiPopupWindow != null && poiPopupWindow.isShowing()) {
                    poiPopupWindow.dismiss();
                }
                selectedPoi = null;
            });
        }

        // Setup use location button
        if (btnUsePoiLocation != null) {
            btnUsePoiLocation.setOnClickListener(v -> {
                if (selectedPoi != null) {
                    openCreateActivityWithPoi();
                    if (poiPopupWindow != null && poiPopupWindow.isShowing()) {
                        poiPopupWindow.dismiss();
                    }
                }
            });
        }
    }

    /**
     * Hide POI information popup
     */
    private void hidePoiInfo() {
        if (poiPopupWindow != null && poiPopupWindow.isShowing()) {
            poiPopupWindow.dismiss();
        }
        selectedPoi = null;
        Log.d(TAG, "POI popup hidden");
    }

    /**
     * Open CreateActivityActivity with selected POI data
     */
    private void openCreateActivityWithPoi() {
        if (selectedPoi == null || getContext() == null) {
            return;
        }

        Intent intent = new Intent(getContext(), CreateActivityActivity.class);
        intent.putExtra("location_name", selectedPoi.getPlaceName());

        // Use road address if available, otherwise use regular address
        String address = selectedPoi.getRoadAddressName();
        if (address == null || address.isEmpty()) {
            address = selectedPoi.getAddressName();
        }
        intent.putExtra("location_address", address);
        intent.putExtra("location_latitude", selectedPoi.getLatitude());
        intent.putExtra("location_longitude", selectedPoi.getLongitude());

        // Add category if available
        if (selectedPoi.getMappedCategory() != null) {
            intent.putExtra("suggested_category", selectedPoi.getMappedCategory());
        }

        startActivity(intent);

        // Hide the POI card after using it
        hidePoiInfo();

        Log.d(TAG, "Opened CreateActivityActivity with POI: " + selectedPoi.getPlaceName());
    }

    /**
     * Detect if running on an Android emulator
     * This is important because Kakao Maps may not work properly on emulators
     */
    private boolean isRunningOnEmulator() {
        return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
            || android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("unknown")
            || android.os.Build.HARDWARE.contains("goldfish")
            || android.os.Build.HARDWARE.contains("ranchu")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
            || android.os.Build.MANUFACTURER.contains("Genymotion")
            || android.os.Build.PRODUCT.contains("sdk_google")
            || android.os.Build.PRODUCT.contains("google_sdk")
            || android.os.Build.PRODUCT.contains("sdk")
            || android.os.Build.PRODUCT.contains("sdk_x86")
            || android.os.Build.PRODUCT.contains("vbox86p")
            || android.os.Build.PRODUCT.contains("emulator")
            || android.os.Build.PRODUCT.contains("simulator");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Only resume if map has been fully initialized
        if (mapView != null && isMapInitialized) {
            try {
                mapView.resume();
                Log.d(TAG, "MapView resumed");
            } catch (Exception e) {
                Log.e(TAG, "Error resuming MapView", e);
            }
        } else {
            Log.d(TAG, "MapView not yet initialized, skipping resume");
        }
        Log.d(TAG, "MapFragment resumed");
    }

    @Override
    public void onPause() {
        super.onPause();

        // Dismiss POI popup if showing
        if (poiPopupWindow != null && poiPopupWindow.isShowing()) {
            poiPopupWindow.dismiss();
        }

        // Only pause if map has been fully initialized
        if (mapView != null && isMapInitialized) {
            try {
                mapView.pause();
                Log.d(TAG, "MapView paused");
            } catch (Exception e) {
                Log.e(TAG, "Error pausing MapView", e);
            }
        }
        Log.d(TAG, "MapFragment paused");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Dismiss POI popup if showing
        if (poiPopupWindow != null && poiPopupWindow.isShowing()) {
            poiPopupWindow.dismiss();
        }
        poiPopupWindow = null;

        // Clean up search handler to prevent memory leaks
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        // Clean up Firebase listeners
        FirebaseActivityManager.getInstance().removeAllListeners();

        // Clean up map resources
        if (mapView != null && isMapInitialized) {
            try {
                mapView.finish();
                Log.d(TAG, "MapView finished");
            } catch (Exception e) {
                Log.e(TAG, "Error finishing MapView", e);
            }
        }
        mapView = null;
        kakaoMap = null;
        isMapInitialized = false; // Reset initialization flag

        // Clear marker collections
        if (labelMap != null) {
            labelMap.clear();
        }
        if (activityMarkers != null) {
            activityMarkers.clear();
        }
        currentLocationLabel = null;

        Log.d(TAG, "MapFragment view destroyed");
    }
}
