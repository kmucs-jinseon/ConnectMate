package com.example.connectmate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.Objects;

/**
 * MapFragment - Fragment containing Kakao Map with activity markers
 */
public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Map components
    private MapView mapView;
    private KakaoMap kakaoMap;
    private ProgressBar loadingIndicator;

    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton btnUseLocation;

    // Selected location data
    private PlaceSearchResult selectedPlace;
    private PlaceSearchResult selectedPoi; // POI selected from map click

    // POI Popup Window
    private android.widget.PopupWindow poiPopupWindow;

    // POI Info Card UI components
    private TextView poiName;
    private TextView poiCategory;
    private TextView poiAddress;
    private TextView poiPhone;
    private TextView poiDistance;
    private TextView poiWebsite;
    private LinearLayout poiPhoneLayout;
    private LinearLayout poiDistanceLayout;
    private LinearLayout poiWebsiteLayout;
    private TextView poiActivitiesHeader;
    private RecyclerView poiActivitiesRecycler;


    // HTTP client for Kakao API
    private OkHttpClient httpClient;
    private Gson gson;

    // Data
    private Map<LatLng, List<Activity>> activityGroups;
    private Map<LatLng, Label> markerMap;
    private Label currentLocationLabel; // Label for current location marker
    private LocationManager locationManager;
    private boolean isMapInitialized = false; // Track if map has been initialized

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize views
        mapView = view.findViewById(R.id.map_view);
        loadingIndicator = view.findViewById(R.id.loading_indicator);
        View emulatorWarning = view.findViewById(R.id.emulator_warning);

        // Initialize UI
        btnUseLocation = view.findViewById(R.id.btn_use_location);

        // POI Info Card will be shown as a PopupWindow - no need to initialize here

        // Initialize HTTP client and JSON parser
        httpClient = new OkHttpClient();
        gson = new Gson();

        // Setup "Use Location" button
        btnUseLocation.setOnClickListener(v -> {
            if (selectedPlace != null) {
                openCreateActivityWithLocation();
            }
        });

        // POI buttons are now handled in popup window initialization

        // Detect if running on emulator
        boolean isEmulator = isRunningOnEmulator();

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
            activityGroups = new HashMap<>();
            markerMap = new HashMap<>();
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
        activityGroups = new HashMap<>();
        markerMap = new HashMap<>();
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
            public void onMapReady(@NonNull KakaoMap map) {
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
            LatLng position = label.getPosition();
            if (getContext() != null && position != null) {
                List<Activity> activities = activityGroups.get(position);
                if (activities != null && !activities.isEmpty()) {
                    Activity activity = activities.get(0);
                    PlaceSearchResult place = new PlaceSearchResult();
                    place.setLatitude(position.getLatitude());
                    place.setLongitude(position.getLongitude());
                    place.setPlaceName(activity.getLocation());

                    // Fetch real address via reverse geocoding
                    fetchAddressForPlace(place);
                }
            }
            return true;
        });

        // Set up click listener for map (to search for nearby POIs)
        kakaoMap.setOnMapClickListener((kakaoMap1, position, screenPoint, poi) -> {
            fetchPoiDetails(Objects.requireNonNull(poi).getName(), position.getLatitude(), position.getLongitude());
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
                removeActivityMarker(activity);
                if (activity.getLatitude() != 0 && activity.getLongitude() != 0) {
                    addActivityMarker(activity);
                    Log.d(TAG, "Activity marker updated: " + activity.getTitle());
                }
            }

            @Override
            public void onActivityRemoved(Activity activity) {
                removeActivityMarker(activity);
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

        LatLng position = LatLng.from(activity.getLatitude(), activity.getLongitude());
        List<Activity> activities = activityGroups.get(position);
        if (activities == null) {
            activities = new ArrayList<>();
            activityGroups.put(position, activities);
        }
        activities.add(activity);

        updateMarker(position);
    }

    /**
     * Remove a marker from the map
     */
    private void removeActivityMarker(Activity activity) {
        if (kakaoMap == null || activity == null) return;

        LatLng position = LatLng.from(activity.getLatitude(), activity.getLongitude());
        List<Activity> activities = activityGroups.get(position);
        if (activities != null) {
            activities.removeIf(a -> a.getId().equals(activity.getId()));
            if (activities.isEmpty()) {
                activityGroups.remove(position);
            }
            updateMarker(position);
        }
    }

    private void updateMarker(LatLng position) {
        if (kakaoMap == null) return;

        LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();
        if (labelLayer == null) return;

        Label existingMarker = markerMap.get(position);
        if (existingMarker != null) {
            labelLayer.remove(existingMarker);
            markerMap.remove(position);
        }

        List<Activity> activities = activityGroups.get(position);
        if (activities != null && !activities.isEmpty()) {
            // Create simple marker bitmap without count
            Bitmap markerBitmap = createSimpleMarkerBitmap();
            LabelStyles styles = LabelStyles.from(LabelStyle.from(markerBitmap)
                .setAnchorPoint(0.5f, 1.0f));

            LabelOptions options = LabelOptions.from(position).setStyles(styles);
            Label newMarker = labelLayer.addLabel(options);
            markerMap.put(position, newMarker);
        }
    }

    private Bitmap createSimpleMarkerBitmap() {
        // Create a simple bitmap from the map pin drawable
        android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_pin);
        if (drawable == null) {
            // Fallback: create a simple colored circle if drawable not found
            int size = (int) (48 * getResources().getDisplayMetrics().density);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColor(ContextCompat.getColor(requireContext(), R.color.primary_blue));
            paint.setAntiAlias(true);
            canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint);
            return bitmap;
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();

        // Scale up for better visibility
        int scale = 2;
        width = width * scale;
        height = height * scale;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    private Bitmap createMarkerBitmap(int count) {
        View markerView = LayoutInflater.from(getContext()).inflate(R.layout.custom_marker, null);
        TextView markerText = markerView.findViewById(R.id.marker_text);
        markerText.setText(String.valueOf(count));

        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        markerView.draw(canvas);
        return bitmap;
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
            LabelStyle labelStyle = LabelStyle.from(R.drawable.ic_my_location_marker)
                .setAnchorPoint(0.5f, 0.5f)  // Anchor at center for circular marker
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
            Log.w(TAG, "Location permission not granted, requesting...");
            requestLocationPermission();
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
     * Request location permissions from user
     */
    private void requestLocationPermission() {
        if (getActivity() == null) {
            Log.e(TAG, "Cannot request permissions - activity is null");
            moveToDefaultLocation();
            return;
        }

        String[] permissions = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        };

        requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE);
        Log.d(TAG, "Location permission requested");
    }

    /**
     * Handle permission request result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted!");
                // Permission granted, try to move to current location again
                moveToCurrentLocation();
            } else {
                Log.w(TAG, "Location permission denied by user");
                Toast.makeText(getContext(), "위치 권한이 거부되었습니다. 기본 위치로 이동합니다.", Toast.LENGTH_SHORT).show();
                moveToDefaultLocation();
            }
        }
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

                String responseBody = Objects.requireNonNull(response.body()).string();

                try {
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray documents = jsonObject.getAsJsonArray("documents");

                    if (documents != null && !documents.isEmpty()) {
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

                String responseBody = Objects.requireNonNull(response.body()).string();
                Log.d(TAG, "POI response received");

                try {
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray documents = jsonObject.getAsJsonArray("documents");

                    if (documents != null && !documents.isEmpty()) {
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

        // Hide use location button if visible
        hideUseLocationButton();

        // Dismiss existing popup if any
        if (poiPopupWindow != null && poiPopupWindow.isShowing()) {
            poiPopupWindow.dismiss();
        }

        // Inflate popup layout - use null parent to avoid attaching issues
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(getContext());
        android.view.View popupView = inflater.inflate(R.layout.fragment_map, null, false);
        android.view.View poiInfoCard = popupView.findViewById(R.id.poi_info_card);

        // Remove the popup view from its parent so it can be added to PopupWindow
        if (poiInfoCard.getParent() != null) {
            ((android.view.ViewGroup) poiInfoCard.getParent()).removeView(poiInfoCard);
        }

        // Make the popup view visible
        poiInfoCard.setVisibility(View.VISIBLE);

        // Enhance the card's shadow/elevation for clearer edges
        if (poiInfoCard instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView cardView = (com.google.android.material.card.MaterialCardView) poiInfoCard;
            cardView.setCardElevation(24f); // Increased elevation for more prominent shadow
            cardView.setMaxCardElevation(24f);
        }

        // Initialize UI components from popup view
        initializePoiPopupComponents(poiInfoCard);

        // Calculate popup width with margins
        int horizontalMarginDp = 16; // Left and right margins
        int horizontalMarginPx = (int) (horizontalMarginDp * getResources().getDisplayMetrics().density);

        // Get screen width and subtract margins
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int popupWidth = screenWidth - (horizontalMarginPx * 2);

        // Create popup window with calculated width
        poiPopupWindow = new android.widget.PopupWindow(
            poiInfoCard,
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
                    distanceText = String.format(java.util.Locale.getDefault(), "%.1fkm", distance / 1000.0);
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

        // Find and display activities at this POI
        findAndDisplayActivitiesAtPoi(poi);

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
        ImageButton btnClosePoiInfo = popupView.findViewById(R.id.btn_close_poi_info);
        com.google.android.material.button.MaterialButton btnUsePoiLocation = popupView.findViewById(R.id.btn_use_poi_location);
        poiActivitiesHeader = popupView.findViewById(R.id.poi_activities_header);
        poiActivitiesRecycler = popupView.findViewById(R.id.poi_activities_recycler);


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

    private void findAndDisplayActivitiesAtPoi(PlaceSearchResult poi) {
        if (poi == null) return;
        FirebaseActivityManager.getInstance().getActivitiesByLocation(poi.getLatitude(), poi.getLongitude(), new FirebaseActivityManager.OnCompleteListener<List<Activity>>() {
            @Override
            public void onSuccess(List<Activity> activities) {
                if (activities != null && !activities.isEmpty()) {
                    poiActivitiesHeader.setVisibility(View.VISIBLE);
                    poiActivitiesRecycler.setVisibility(View.VISIBLE);
                    poiActivitiesRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
                    poiActivitiesRecycler.setAdapter(new PoiActivityAdapter(getContext(), activities));
                } else {
                    poiActivitiesHeader.setVisibility(View.GONE);
                    poiActivitiesRecycler.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Exception e) {
                poiActivitiesHeader.setVisibility(View.GONE);
                poiActivitiesRecycler.setVisibility(View.GONE);
            }
        });
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
     * Fetch address for a place using reverse geocoding and display POI info
     */
    private void fetchAddressForPlace(PlaceSearchResult place) {
        if (place == null) return;

        String apiUrl = "https://dapi.kakao.com/v2/local/geo/coord2address.json?x=" +
            place.getLongitude() + "&y=" + place.getLatitude();

        Request request = new Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "KakaoAK " + BuildConfig.KAKAO_REST_API_KEY)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Reverse geocoding failed for place", e);
                // Display POI info even without address
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> displayPoiInfo(place));
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Reverse geocoding response not successful: " + response.code());
                    // Display POI info even without address
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> displayPoiInfo(place));
                    }
                    return;
                }

                String responseBody = Objects.requireNonNull(response.body()).string();

                try {
                    JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);
                    JsonArray documents = jsonObject.getAsJsonArray("documents");

                    if (documents != null && documents.size() > 0) {
                        JsonObject doc = documents.get(0).getAsJsonObject();

                        // Try to get road address first, fallback to regular address
                        if (doc.has("road_address") && !doc.get("road_address").isJsonNull()) {
                            JsonObject roadAddress = doc.getAsJsonObject("road_address");
                            place.setRoadAddressName(roadAddress.get("address_name").getAsString());
                        }

                        if (doc.has("address") && !doc.get("address").isJsonNull()) {
                            JsonObject regAddress = doc.getAsJsonObject("address");
                            place.setAddressName(regAddress.get("address_name").getAsString());
                        }
                    }

                    // Display POI info on UI thread with address
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> displayPoiInfo(place));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing reverse geocoding results for place", e);
                    // Display POI info even if parsing failed
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> displayPoiInfo(place));
                    }
                }
            }
        });
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
        if (markerMap != null) {
            markerMap.clear();
        }
        if (activityGroups != null) {
            activityGroups.clear();
        }
        currentLocationLabel = null;

        Log.d(TAG, "MapFragment view destroyed");
    }
}
