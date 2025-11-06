package com.example.connectmate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapGravity;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.camera.CameraAnimation;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyles;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.Label;
import com.example.connectmate.models.Activity;
import com.example.connectmate.utils.ActivityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * MapFragment - Fragment containing Kakao Map with activity markers
 *
 * KNOWN SDK LIMITATIONS:
 * - Kakao Maps SDK (v2.12.18) has poor error handling in its internal delegate pattern
 * - The KakaoMap.isDev() and other delegate methods catch ALL RuntimeExceptions and
 *   return default values, which masks configuration errors
 * - This class adds defensive validation and explicit error logging to work around
 *   these SDK design flaws
 * - See: https://devtalk.kakao.com for known issues with delegate null safety
 *
 * MITIGATION STRATEGIES:
 * - Validate map initialization state with isMapViewReady flag
 * - Explicit null checks before all KakaoMap API calls
 * - Try-catch blocks with specific error logging for debugging
 * - Early validation of app key and configuration in onMapReady callback
 */
public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Map components
    private MapView mapView;
    private KakaoMap kakaoMap;
    private ProgressBar loadingIndicator;
    private boolean hasStartedMap = false;
    private boolean isMapViewReady = false; // Track if MapView internal components are ready

    // Search components
    private ImageButton searchIconButton;
    private EditText searchEditText;
    private ImageButton clearSearchButton;
    private CardView searchResultsCard;
    private RecyclerView searchResultsRecycler;
    private SearchResultsAdapter searchResultsAdapter;
    private ExecutorService searchExecutor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // Data
    private List<Activity> activities;
    private LocationManager locationManager;
    private LatLng initialCameraPosition = null; // Store initial position before map loads

    // Pending navigation (for when map is not ready yet)
    private Double pendingNavigationLat = null;
    private Double pendingNavigationLng = null;
    private String pendingNavigationTitle = null;

    // Pending route display (for when map is not ready yet)
    private Double pendingRouteLat = null;
    private Double pendingRouteLng = null;
    private String pendingRouteTitle = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize views
        mapView = view.findViewById(R.id.map_view);
        loadingIndicator = view.findViewById(R.id.loading_indicator);
        searchIconButton = view.findViewById(R.id.search_icon_button);
        searchEditText = view.findViewById(R.id.search_edit_text);
        clearSearchButton = view.findViewById(R.id.clear_search_button);
        searchResultsCard = view.findViewById(R.id.search_results_card);
        searchResultsRecycler = view.findViewById(R.id.search_results_recycler);

        // Debug: Check if search views are found
        Log.d(TAG, "Search view initialization:");
        Log.d(TAG, "  searchIconButton: " + (searchIconButton != null ? "OK" : "NULL"));
        Log.d(TAG, "  searchEditText: " + (searchEditText != null ? "OK" : "NULL"));
        Log.d(TAG, "  clearSearchButton: " + (clearSearchButton != null ? "OK" : "NULL"));
        Log.d(TAG, "  searchResultsCard: " + (searchResultsCard != null ? "OK" : "NULL"));
        Log.d(TAG, "  searchResultsRecycler: " + (searchResultsRecycler != null ? "OK" : "NULL"));

        // Ensure MapView is visible from the start
        if (mapView != null) {
            mapView.setVisibility(android.view.View.VISIBLE);
            Log.d(TAG, "MapView found and set to VISIBLE in onCreateView");
        } else {
            Log.e(TAG, "ERROR: MapView is NULL!");
        }

        // Initialize data
        activities = new ArrayList<>();
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        // Request location permissions if not granted
        requestLocationPermissionsIfNeeded();

        // Set up search functionality
        setupSearchBar();

        // Defer map start until the view has been attached to avoid missing callbacks
        if (mapView != null) {
            mapView.post(() -> {
                if (hasStartedMap) {
                    Log.d(TAG, "initializeMap skipped - map already started");
                    return;
                }
                Log.d(TAG, "MapView post() callback - width: " + mapView.getWidth()
                        + ", height: " + mapView.getHeight());
                initializeMap();
            });
        }

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

        hasStartedMap = true;

        // Ensure MapView is visible
        mapView.setVisibility(android.view.View.VISIBLE);

        Log.d(TAG, "═══════════════════════════════════");
        Log.d(TAG, "Initializing Kakao Map in MapFragment...");
        Log.d(TAG, "MapView visibility: " + visibilityToString(mapView.getVisibility()));
        Log.d(TAG, "MapView dimensions: " + mapView.getWidth() + "x" + mapView.getHeight());
        Log.d(TAG, "Package name: " + requireContext().getPackageName());
        Log.d(TAG, "═══════════════════════════════════");

        // Determine initial camera position BEFORE map loads
        // This prevents the "jump" from default location to current location
        determineInitialLocation();

        showLoading(true);

        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                Log.d(TAG, "Map destroyed");
                isMapViewReady = false;
            }

            @Override
            public void onMapError(Exception error) {
                showLoading(false);
                isMapViewReady = false;

                Log.e(TAG, "═══════════════════════════════════");
                Log.e(TAG, "❌ MAP INITIALIZATION ERROR");
                Log.e(TAG, "═══════════════════════════════════");

                String errorMsg = "Map initialization failed";
                String detailedDiagnostic = "";

                if (error == null) {
                    Log.e(TAG, "Error object is NULL!");
                    errorMsg = "❌ Unknown map error (null exception)";
                } else {
                    Log.e(TAG, "Error class: " + error.getClass().getName());
                    Log.e(TAG, "Error message: " + error.getMessage());
                    Log.e(TAG, "Error cause: " + (error.getCause() != null ? error.getCause().getMessage() : "null"));
                    error.printStackTrace();

                    // Handle specific exception types using reflection
                    // MapAuthException and RenderViewException are not public API
                    String errorClassName = error.getClass().getSimpleName();

                    if ("MapAuthException".equals(errorClassName)) {
                        // Use reflection to get error code from MapAuthException
                        try {
                            java.lang.reflect.Field errorCodeField = error.getClass().getField("errorCode");
                            int errorCode = errorCodeField.getInt(error);

                            Log.e(TAG, "MapAuthException errorCode: " + errorCode);

                            // Decode error codes based on SDK documentation
                            // Error code constants (from decompiled SDK):
                            // APP_KEY_INVALID_ERROR = 7
                            // INITIALIZE_FAILURE = 5
                            // SOCKET_TIMEOUT_EXCEPTION = 2
                            // CONNECT_TIMEOUT_EXCEPTION = 3
                            // CONNECT_ERROR = 6
                            // CONNECT_INITIATE_FAILURE = 1
                            // RENDER_VIEW_FAILURE = 8
                            // UNKNOWN_ERROR = 0

                            switch (errorCode) {
                                case 7: // APP_KEY_INVALID_ERROR
                                    errorMsg = "❌ INVALID API KEY";
                                    detailedDiagnostic = "Your Kakao Maps API key is invalid.\n\n" +
                                        "Fix:\n" +
                                        "1. Check local.properties\n" +
                                        "2. Verify key at developers.kakao.com\n" +
                                        "3. Ensure KAKAO_APP_KEY matches console";
                                    Log.e(TAG, "CAUSE: Invalid API Key");
                                    break;

                                case 5: // INITIALIZE_FAILURE
                                    errorMsg = "❌ AUTHENTICATION SETUP FAILED";
                                    detailedDiagnostic = "Authentication failed before connecting.\n\n" +
                                        "Common causes:\n" +
                                        "1. Invalid API key\n" +
                                        "2. Wrong key hash (SHA-1)\n" +
                                        "3. Package name not registered\n\n" +
                                        "Package: " + requireContext().getPackageName();
                                    Log.e(TAG, "CAUSE: Invalid API key or key hash");
                                    break;

                                case 2: // SOCKET_TIMEOUT_EXCEPTION
                                    errorMsg = "❌ NETWORK TIMEOUT";
                                    detailedDiagnostic = "Could not reach Kakao servers.\n\n" +
                                        "Check:\n" +
                                        "1. Internet connection\n" +
                                        "2. Firewall settings\n" +
                                        "3. VPN if enabled";
                                    Log.e(TAG, "CAUSE: Socket timeout during authentication");
                                    break;

                                case 3: // CONNECT_TIMEOUT_EXCEPTION
                                    errorMsg = "❌ CONNECTION TIMEOUT";
                                    detailedDiagnostic = "Connection to Kakao servers timed out.\n\n" +
                                        "Check network connectivity and retry.";
                                    Log.e(TAG, "CAUSE: Connection timeout");
                                    break;

                                case 6: // CONNECT_ERROR
                                    errorMsg = "❌ CONNECTION ERROR";
                                    detailedDiagnostic = "Failed during authentication setup.\n\n" +
                                        "Check internet connection.";
                                    Log.e(TAG, "CAUSE: Connection error during auth setup");
                                    break;

                                case 1: // CONNECT_INITIATE_FAILURE
                                    errorMsg = "❌ CONNECTION INITIATION FAILED";
                                    detailedDiagnostic = "Could not start HTTPS connection.\n\n" +
                                        "This is a network-level error.";
                                    Log.e(TAG, "CAUSE: HttpsURLConnection.openConnection() failed");
                                    break;

                                case 8: // RENDER_VIEW_FAILURE
                                    errorMsg = "❌ RENDER VIEW FAILED";
                                    detailedDiagnostic = "Map rendering failed after authentication.\n\n" +
                                        "Device may not support map rendering.";
                                    Log.e(TAG, "CAUSE: RenderView callback failure");
                                    break;

                                case 0: // UNKNOWN_ERROR
                                default:
                                    errorMsg = "❌ AUTHENTICATION ERROR (Code: " + errorCode + ")";
                                    detailedDiagnostic = "Unknown authentication error.\n\n" +
                                        "Error code: " + errorCode + "\n" +
                                        "Message: " + error.getMessage();
                                    Log.e(TAG, "CAUSE: Unknown error code " + errorCode);
                                    break;
                            }
                        } catch (Exception reflectionError) {
                            Log.e(TAG, "Could not extract MapAuthException error code via reflection", reflectionError);
                            errorMsg = "❌ AUTHENTICATION ERROR";
                            detailedDiagnostic = "Map authentication failed.\n\n" +
                                "Details: " + error.getMessage();
                        }

                    } else if ("RenderViewException".equals(errorClassName)) {
                        errorMsg = "❌ MAP RENDERING FAILED";
                        detailedDiagnostic = "The map failed to initialize rendering.\n\n" +
                            "Possible causes:\n" +
                            "1. Device GPU incompatibility\n" +
                            "2. Insufficient memory\n" +
                            "3. OpenGL ES issues\n\n" +
                            "Details: " + error.getMessage();
                        Log.e(TAG, "CAUSE: RenderViewException - " + error.getMessage());

                    } else if (error.getMessage() != null) {
                        // Fallback for other exception types
                        String msg = error.getMessage();
                        if (msg.contains("auth") || msg.contains("Authentication")) {
                            errorMsg = "❌ Authentication Failed";
                            detailedDiagnostic = "Package: " + requireContext().getPackageName() + "\n\n" +
                                "Check:\n1. API Key\n2. Package name\n3. Key hash";
                        } else if (msg.contains("network") || msg.contains("Network")) {
                            errorMsg = "❌ Network Error";
                            detailedDiagnostic = "Check internet connection";
                        } else {
                            errorMsg = "❌ Map Error";
                            detailedDiagnostic = msg;
                        }
                    }
                }

                Log.e(TAG, "User-facing error: " + errorMsg);
                Log.e(TAG, "Diagnostic: " + detailedDiagnostic);
                Log.e(TAG, "Package name: " + requireContext().getPackageName());
                Log.e(TAG, "═══════════════════════════════════");

                // Show user-friendly error with detailed diagnostic
                if (getContext() != null) {
                    String toastMsg = errorMsg;
                    if (!detailedDiagnostic.isEmpty()) {
                        toastMsg = errorMsg + "\n\n" + detailedDiagnostic;
                    }
                    Toast.makeText(getContext(), toastMsg, Toast.LENGTH_LONG).show();
                }
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull KakaoMap map) {
                Log.d(TAG, "═══════════════════════════════════");
                Log.d(TAG, "✓ MAP READY in MapFragment!");
                Log.d(TAG, "MapView visibility: " + visibilityToString(mapView.getVisibility()));
                Log.d(TAG, "MapView dimensions: " + mapView.getWidth() + "x" + mapView.getHeight());
                Log.d(TAG, "═══════════════════════════════════");

                // Store map reference for use throughout the fragment lifecycle
                kakaoMap = map;
                isMapViewReady = true; // MapView internal components are now ready

                // IMPORTANT: Set viewport to match screen size for proper rendering
                try {
                    // Get actual screen dimensions
                    int screenWidth = mapView.getWidth();
                    int screenHeight = mapView.getHeight();

                    if (screenWidth > 0 && screenHeight > 0) {
                        // Set viewport to match screen size
                        kakaoMap.setViewport(screenWidth, screenHeight);
                        Log.d(TAG, "Viewport set to screen size: " + screenWidth + "x" + screenHeight);
                    } else {
                        Log.w(TAG, "MapView dimensions not ready, will use default viewport");
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to set viewport", e);
                }

                // Set viewport change listener to handle dynamic resizing
                try {
                    kakaoMap.setOnViewportChangeListener((kakaoMapInstance, rect) -> {
                        Log.d(TAG, "Viewport changed: " + rect.width() + "x" + rect.height());
                    });
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to set viewport change listener", e);
                }

                // Enable POI (Point of Interest) labels for better map detail
                try {
                    kakaoMap.setPoiVisible(true); // Show POI labels (stores, landmarks, etc.)
                    kakaoMap.setPoiClickable(true); // Make POIs clickable
                    Log.d(TAG, "POI labels enabled and clickable");
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to enable POI", e);
                }

                // Move Kakao logo to bottom-left (away from potential UI elements on right)
                try {
                    // Convert 16dp to pixels for proper padding
                    float density = getResources().getDisplayMetrics().density;
                    int paddingPx = (int) (16 * density);

                    // Position logo at bottom-left corner with proper pixel padding
                    // MapGravity: Use bitwise OR to combine BOTTOM and LEFT
                    int gravity = MapGravity.BOTTOM | MapGravity.LEFT;
                    kakaoMap.getLogo().setPosition(gravity, paddingPx, paddingPx);
                    Log.d(TAG, "Kakao logo repositioned to bottom-left (" + paddingPx + "px padding)");
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to reposition logo", e);
                }

                // Configure camera zoom levels for optimal map viewing
                try {
                    // Get the map's zoom level capabilities
                    int currentZoom = kakaoMap.getZoomLevel();
                    int minZoomLevel = kakaoMap.getCameraMinLevel(); // 실제 최소 줌 레벨 (가장 축소)
                    int maxZoomLevel = kakaoMap.getCameraMaxLevel(); // 실제 최대 줌 레벨 (가장 확대)

                    Log.d(TAG, "Map zoom capabilities:");
                    Log.d(TAG, "  Current zoom: " + currentZoom);
                    Log.d(TAG, "  Min zoom (most zoomed out): " + minZoomLevel);
                    Log.d(TAG, "  Max zoom (most zoomed in): " + maxZoomLevel);

                    // Set camera zoom limits for better user experience
                    // Prevent zooming out too far (city-level minimum, not country-level)
                    // Prevent zooming in too much (maintain reasonable detail)
                    try {
                        // Set minimum zoom to level 10 (city/district level) instead of maximum zoom out
                        // This prevents users from zooming out to see entire countries
                        kakaoMap.setCameraMinLevel(10);

                        // Set maximum zoom to level 20 (building/street level detail)
                        // This provides good detail without excessive zoom
                        kakaoMap.setCameraMaxLevel(20);

                        Log.d(TAG, "Camera zoom limits configured: min=10 (city), max=20 (building)");
                    } catch (RuntimeException e) {
                        Log.e(TAG, "Failed to set camera zoom limits", e);
                    }

                    // Camera position will be set by moveToCurrentLocation() later
                    // This ensures we use GPS location if available, otherwise default to Seoul
                    Log.d(TAG, "Camera position will be set after loading activities");
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to configure zoom levels", e);
                }

                // Validate map is properly initialized by exercising KakaoMap delegate methods.
                // We still call kakaoMap.isDev() but allow any RuntimeException to bubble so that
                // configuration issues (e.g., missing API key, null delegate) fail fast.
                validateMapDelegate(kakaoMap);

                // Additional sanity check to ensure the delegate can return core components.
                try {
                    // This will throw if delegate is misconfigured
                    if (kakaoMap.getLabelManager() != null) {
                        Log.d(TAG, "Map delegate validated successfully");
                    } else {
                        Log.w(TAG, "LabelManager is null - map may have initialization issues");
                    }
                } catch (RuntimeException e) {
                    // Let configuration errors propagate - don't mask them
                    Log.e(TAG, "⚠️ CRITICAL: Map delegate failure - configuration error!", e);
                    if (e instanceof IllegalStateException) {
                        Log.e(TAG, "Likely cause: Missing Kakao Map API key or invalid configuration");
                        throw e; // Re-throw so it's visible - fail fast on config errors
                    } else if (e instanceof NullPointerException) {
                        Log.e(TAG, "Likely cause: Internal KakaoMap delegate is null");
                        throw e; // Re-throw - this is a critical error
                    }
                    throw e; // Re-throw any other RuntimeException
                }

                // Map is now configured and ready to use
                Log.d(TAG, "Map configured successfully");

                // Try to make map render with explicit settings
                if (kakaoMap != null) {
                    Log.d(TAG, "Attempting to force map render...");

                    // Log the map object state
                    Log.d(TAG, "KakaoMap object: " + kakaoMap.toString());

                    // Force a repaint
                    mapView.invalidate();
                    mapView.postInvalidate();
                }

                showLoading(false);

                // Ensure MapView is visible and on top
                mapView.setVisibility(android.view.View.VISIBLE);
                mapView.bringToFront();
                mapView.requestLayout();

                // Force parent view visibility
                if (getView() != null) {
                    getView().setVisibility(android.view.View.VISIBLE);
                    getView().bringToFront();
                    getView().requestLayout();
                    Log.d(TAG, "Fragment root view: visible=" + (getView().getVisibility() == android.view.View.VISIBLE)
                        + ", alpha=" + getView().getAlpha()
                        + ", dimensions=" + getView().getWidth() + "x" + getView().getHeight());
                }

                // Log detailed visibility info
                Log.d(TAG, "MapView parent: " + (mapView.getParent() != null ? mapView.getParent().getClass().getSimpleName() : "null"));
                Log.d(TAG, "MapView z-order: bringToFront called");
                Log.d(TAG, "MapView alpha: " + mapView.getAlpha());

                if (getContext() != null) {
                    Toast.makeText(getContext(), "✓ Map loaded! Can you see it?", Toast.LENGTH_LONG).show();
                }

                // Set initial camera position (determined before map loaded)
                setInitialCameraPosition();

                // Add sample activity markers
                addSampleActivityMarkers();
            }
        });
    }

    /**
     * Add sample activity markers to the map
     */
    private void addSampleActivityMarkers() {
        if (kakaoMap == null) {
            Log.e(TAG, "Cannot add markers - KakaoMap is null");
            return;
        }

        // Load activities from ActivityManager
        activities.clear();

        ActivityManager activityManager = ActivityManager.getInstance(requireContext());
        activities.addAll(activityManager.getAllActivities());

        Log.d(TAG, "Loaded " + activities.size() + " activities from ActivityManager");

        try {
            // Defensive check for label manager
            if (kakaoMap.getLabelManager() == null) {
                Log.e(TAG, "LabelManager is null - cannot add markers");
                return;
            }

            LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();
            if (labelLayer == null) {
                Log.e(TAG, "LabelLayer is null - cannot add markers");
                return;
            }

            for (Activity activity : activities) {
                try {
                    // Only add marker if activity has valid coordinates
                    if (activity.getLatitude() != 0.0 && activity.getLongitude() != 0.0) {
                        LabelStyles styles = kakaoMap.getLabelManager()
                            .addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.ic_map_pin)));

                        LabelOptions options = LabelOptions.from(
                            activity.getId(),
                            LatLng.from(activity.getLatitude(), activity.getLongitude())
                        ).setStyles(styles);

                        Label label = labelLayer.addLabel(options);
                        if (label != null) {
                            label.setClickable(true);
                        } else {
                            Log.w(TAG, "Failed to create label for activity: " + activity.getId());
                        }
                    } else {
                        Log.w(TAG, "Activity has no coordinates: " + activity.getTitle());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to add marker for activity: " + activity.getId(), e);
                    // Continue with next marker
                }
            }

            kakaoMap.setOnLabelClickListener((kakaoMapInstance, layer, label) -> {
                try {
                    String labelId = label.getLabelId();
                    Log.d(TAG, "Label clicked: " + labelId);

                    // Find the activity by ID
                    Activity clickedActivity = null;
                    for (Activity activity : activities) {
                        if (activity.getId().equals(labelId)) {
                            clickedActivity = activity;
                            break;
                        }
                    }

                    if (clickedActivity != null && getContext() != null) {
                        // Open ActivityDetailActivity with the clicked activity
                        Intent intent = new Intent(getContext(), ActivityDetailActivity.class);
                        intent.putExtra("activity", clickedActivity);
                        startActivity(intent);
                        Log.d(TAG, "Opening details for: " + clickedActivity.getTitle());
                    } else {
                        Log.w(TAG, "Activity not found for label ID: " + labelId);
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                "Activity details not available",
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in label click listener", e);
                }
                return true;
            });

            Log.d(TAG, "Added " + activities.size() + " activity markers");

            // Execute pending navigation if there is one
            if (pendingNavigationLat != null && pendingNavigationLng != null) {
                Log.d(TAG, "Executing pending navigation: " + pendingNavigationTitle);
                navigateToLocation(pendingNavigationLat, pendingNavigationLng, pendingNavigationTitle);
            }

            // Execute pending route display if there is one
            if (pendingRouteLat != null && pendingRouteLng != null) {
                Log.d(TAG, "Executing pending route display: " + pendingRouteTitle);
                showRouteToLocation(pendingRouteLat, pendingRouteLng, pendingRouteTitle);
            }
        } catch (RuntimeException e) {
            // Catch any exceptions from the KakaoMap library's internal delegate issues
            Log.e(TAG, "RuntimeException while adding markers (possible KakaoMap delegate issue)", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to add map markers", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add markers", e);
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

        if (!hasLocationPermission()) {
            Log.d(TAG, "Location permission not granted, using default location");
            moveToDefaultLocation();
            return;
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

            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                LatLng currentLocation = LatLng.from(latitude, longitude);

                try {
                    // Use smooth animation when moving to current location (1 second duration)
                    CameraAnimation animation = CameraAnimation.from(1000, true, true);
                    kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(currentLocation, 17), animation);
                    Log.d(TAG, "Camera moved to current location: " + latitude + ", " + longitude + " at zoom level 17 with animation");
                } catch (RuntimeException e) {
                    Log.e(TAG, "RuntimeException moving camera (possible delegate issue)", e);
                    moveToDefaultLocation();
                }
            } else {
                Log.d(TAG, "Location is null, using default location");
                moveToDefaultLocation();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error", e);
            moveToDefaultLocation();
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException in moveToCurrentLocation", e);
            moveToDefaultLocation();
        }
    }

    /**
     * Move camera to default location (Seoul) with zoom level 17 for detailed view
     */
    private void moveToDefaultLocation() {
        if (kakaoMap != null) {
            try {
                // Use smooth animation when moving to default Seoul location (1 second duration)
                CameraAnimation animation = CameraAnimation.from(1000, true, true);
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(
                        LatLng.from(37.5665, 126.9780), 17), animation);
                Log.d(TAG, "Camera moved to default location (Seoul) at zoom level 17 with animation");
            } catch (RuntimeException e) {
                Log.e(TAG, "RuntimeException moving to default location (possible delegate issue)", e);
            }
        }
    }

    /**
     * Determine initial camera location BEFORE map loads
     * This prevents the "jump" from default to current location
     */
    private void determineInitialLocation() {
        if (!hasLocationPermission()) {
            Log.d(TAG, "Location permission not granted, will use default Seoul location");
            initialCameraPosition = LatLng.from(37.5665, 126.9780); // Seoul
            return;
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

            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                initialCameraPosition = LatLng.from(latitude, longitude);
                Log.d(TAG, "Initial location determined: " + latitude + ", " + longitude);
            } else {
                Log.d(TAG, "Location is null, will use default Seoul location");
                initialCameraPosition = LatLng.from(37.5665, 126.9780); // Seoul
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error", e);
            initialCameraPosition = LatLng.from(37.5665, 126.9780); // Seoul
        } catch (Exception e) {
            Log.e(TAG, "Error determining initial location", e);
            initialCameraPosition = LatLng.from(37.5665, 126.9780); // Seoul
        }
    }

    /**
     * Set initial camera position when map becomes ready
     * Uses the location determined before map initialization
     */
    private void setInitialCameraPosition() {
        if (kakaoMap == null) {
            Log.e(TAG, "KakaoMap is null, cannot set initial camera position");
            return;
        }

        if (initialCameraPosition == null) {
            Log.w(TAG, "Initial camera position is null, using default");
            initialCameraPosition = LatLng.from(37.5665, 126.9780); // Seoul
        }

        try {
            // Set camera position without animation for immediate display
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(initialCameraPosition, 17));
            Log.d(TAG, "Initial camera position set to: " + initialCameraPosition.getLatitude() +
                  ", " + initialCameraPosition.getLongitude() + " at zoom level 17");
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException setting initial camera position", e);
        }
    }

    /**
     * Check if location permissions are granted
     */
    private boolean hasLocationPermission() {
        return getContext() != null && (
            ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        );
    }

    /**
     * Request location permissions if not already granted
     */
    private void requestLocationPermissionsIfNeeded() {
        if (!hasLocationPermission()) {
            Log.d(TAG, "Location permissions not granted, requesting...");
            requestPermissions(
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            Log.d(TAG, "Location permissions already granted");
        }
    }

    /**
     * Handle permission request result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                Log.d(TAG, "Location permission granted by user");
                Toast.makeText(requireContext(), "위치 권한이 허용되었습니다", Toast.LENGTH_SHORT).show();

                // Re-initialize map with current location if map is already ready
                if (isMapViewReady && kakaoMap != null) {
                    determineInitialLocation();
                    setInitialCameraPosition();
                }
            } else {
                Log.d(TAG, "Location permission denied by user");
                Toast.makeText(requireContext(),
                    "위치 권한이 거부되었습니다. 서울 위치로 표시됩니다.",
                    Toast.LENGTH_LONG).show();
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
     * Exercise KakaoMap.isDev() and allow RuntimeExceptions to surface
     * so that configuration issues are not masked.
     */
    private void validateMapDelegate(@NonNull KakaoMap map) {
        try {
            boolean devMode = map.isDev();
            Log.d(TAG, "KakaoMap delegate responded to isDev(): " + devMode);
        } catch (RuntimeException e) {
            Log.e(TAG, "⚠️ CRITICAL: kakaoMap.isDev() threw a RuntimeException", e);
            if (e instanceof IllegalStateException) {
                Log.e(TAG, "Likely cause: Missing Kakao Map API key or invalid configuration");
            } else if (e instanceof NullPointerException) {
                Log.e(TAG, "Likely cause: Internal KakaoMap delegate is null");
            }
            throw e;
        }
    }

    private String visibilityToString(int visibility) {
        switch (visibility) {
            case View.VISIBLE:
                return "VISIBLE";
            case View.INVISIBLE:
                return "INVISIBLE";
            case View.GONE:
                return "GONE";
            default:
                return "UNKNOWN(" + visibility + ")";
        }
    }

    /**
     * Set up search bar functionality
     */
    private void setupSearchBar() {
        // Check if all views are initialized
        if (searchIconButton == null || searchEditText == null || clearSearchButton == null ||
            searchResultsCard == null || searchResultsRecycler == null) {
            Log.e(TAG, "ERROR: Search views not initialized!");
            Log.e(TAG, "  searchIconButton: " + searchIconButton);
            Log.e(TAG, "  searchEditText: " + searchEditText);
            Log.e(TAG, "  clearSearchButton: " + clearSearchButton);
            Log.e(TAG, "  searchResultsCard: " + searchResultsCard);
            Log.e(TAG, "  searchResultsRecycler: " + searchResultsRecycler);
            Toast.makeText(getContext(), "Search functionality unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Setting up search bar...");

        // Set up RecyclerView for search results
        searchResultsAdapter = new SearchResultsAdapter(result -> {
            // Handle result selection
            onSearchResultSelected(result);
        });

        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsRecycler.setAdapter(searchResultsAdapter);

        // Text change listener for live search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    clearSearchButton.setVisibility(View.VISIBLE);
                } else {
                    clearSearchButton.setVisibility(View.GONE);
                    searchResultsCard.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Search icon button click listener
        searchIconButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                // If there's text, perform search
                Log.d(TAG, "Search icon clicked, performing search...");
                performSearch(query);
                hideKeyboard();
            } else {
                // If empty, focus on EditText and show keyboard
                searchEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        // Search action listener (keyboard Enter key)
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });

        // Clear button listener
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchResultsCard.setVisibility(View.GONE);
            searchResultsAdapter.clearResults();
        });
    }

    /**
     * Perform search using Kakao Local API
     */
    private void performSearch(String query) {
        Log.d(TAG, "Performing search for: " + query);

        searchExecutor.execute(() -> {
            try {
                // Build Kakao Local API URL
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                String apiUrl = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" + encodedQuery;

                // Get REST API key from BuildConfig (different from Native App key used for Maps)
                String apiKey = BuildConfig.KAKAO_REST_API_KEY;
                if (apiKey == null || apiKey.isEmpty()) {
                    Log.e(TAG, "KAKAO_REST_API_KEY not configured!");
                    mainHandler.post(() -> {
                        Toast.makeText(getContext(),
                            "검색 기능을 사용하려면 KAKAO_REST_API_KEY를 local.properties에 추가하세요",
                            Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                Log.d(TAG, "API URL: " + apiUrl);
                Log.d(TAG, "API Key length: " + apiKey.length());

                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "KakaoAK " + apiKey);
                connection.setConnectTimeout(10000); // 10 seconds timeout
                connection.setReadTimeout(10000);

                Log.d(TAG, "Connecting to Kakao API...");
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Parse JSON response
                    List<SearchResult> results = parseSearchResults(response.toString());

                    // Update UI on main thread
                    mainHandler.post(() -> {
                        if (results.isEmpty()) {
                            Toast.makeText(getContext(), "검색 결과가 없습니다", Toast.LENGTH_SHORT).show();
                            searchResultsCard.setVisibility(View.GONE);
                        } else {
                            searchResultsAdapter.setResults(results);
                            searchResultsCard.setVisibility(View.VISIBLE);
                            Log.d(TAG, "Found " + results.size() + " search results");
                        }
                    });
                } else {
                    // Read error response
                    BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    errorReader.close();

                    Log.e(TAG, "Search API error: " + responseCode);
                    Log.e(TAG, "Error response: " + errorResponse.toString());

                    mainHandler.post(() -> {
                        Toast.makeText(getContext(), "검색 오류 (code: " + responseCode + ")", Toast.LENGTH_LONG).show();
                    });
                }

                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Search failed", e);
                mainHandler.post(() -> {
                    Toast.makeText(getContext(), "검색 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Parse Kakao Local API JSON response
     */
    private List<SearchResult> parseSearchResults(String jsonResponse) {
        List<SearchResult> results = new ArrayList<>();

        try {
            JSONObject json = new JSONObject(jsonResponse);
            JSONArray documents = json.getJSONArray("documents");

            for (int i = 0; i < documents.length(); i++) {
                JSONObject doc = documents.getJSONObject(i);

                String placeName = doc.getString("place_name");
                String addressName = doc.optString("address_name", "");
                String roadAddressName = doc.optString("road_address_name", "");
                String categoryName = doc.optString("category_name", "");
                double latitude = doc.getDouble("y");
                double longitude = doc.getDouble("x");
                String phone = doc.optString("phone", "");
                String placeUrl = doc.optString("place_url", "");

                SearchResult result = new SearchResult(
                    placeName, addressName, roadAddressName,
                    categoryName, latitude, longitude,
                    phone, placeUrl
                );

                results.add(result);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse search results", e);
        }

        return results;
    }

    /**
     * Handle search result selection
     */
    private void onSearchResultSelected(SearchResult result) {
        Log.d(TAG, "Selected: " + result.getPlaceName());

        // Hide search results
        searchResultsCard.setVisibility(View.GONE);
        hideKeyboard();

        // Move camera to selected location with zoom level 17 for detailed view
        if (kakaoMap != null && isMapViewReady) {
            try {
                LatLng location = LatLng.from(result.getLatitude(), result.getLongitude());

                // Use smooth animation when moving to search result (800ms duration)
                CameraAnimation animation = CameraAnimation.from(800, true, true);
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(location, 17), animation);

                // Add marker for the selected location
                addSearchMarker(result);

                Toast.makeText(getContext(), result.getPlaceName(), Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to move camera to search result", e);
                Toast.makeText(getContext(), "위치 이동 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Add a marker for the searched location
     */
    private void addSearchMarker(SearchResult result) {
        if (kakaoMap == null || kakaoMap.getLabelManager() == null) return;

        try {
            LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();
            if (labelLayer == null) return;

            // Create marker style
            LabelStyles styles = kakaoMap.getLabelManager()
                .addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.ic_map_pin)));

            // Create label options
            LatLng position = LatLng.from(result.getLatitude(), result.getLongitude());
            LabelOptions options = LabelOptions.from("search_" + System.currentTimeMillis(), position)
                .setStyles(styles);

            // Add label to map
            Label label = labelLayer.addLabel(options);
            if (label != null) {
                label.setClickable(true);
                Log.d(TAG, "Added search marker at: " + result.getPlaceName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to add search marker", e);
        }
    }

    /**
     * Hide soft keyboard
     */
    private void hideKeyboard() {
        if (getActivity() != null && searchEditText != null) {
            InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }
        }
    }

    /**
     * Get the KakaoMap instance (for external control if needed)
     * @return KakaoMap instance or null if not initialized
     */
    public KakaoMap getKakaoMap() {
        if (kakaoMap == null) {
            Log.w(TAG, "getKakaoMap() called but map is not initialized");
        }
        return kakaoMap;
    }

    /**
     * Check if the map is ready to use
     * @return true if map is initialized and ready
     */
    public boolean isMapReady() {
        return kakaoMap != null;
    }

    /**
     * Get the activities displayed on the map
     */
    public List<Activity> getActivities() {
        return activities;
    }

    /**
     * Zoom in on the map with smooth animation
     */
    public void zoomIn() {
        if (kakaoMap == null) {
            Log.w(TAG, "Cannot zoom in - map not initialized");
            return;
        }
        try {
            // Use animated camera movement with 300ms duration and smooth gesture
            CameraAnimation animation = CameraAnimation.from(300, true, true);
            kakaoMap.moveCamera(CameraUpdateFactory.zoomIn(), animation);
            Log.d(TAG, "Zoomed in with animation");
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException during zoom in (possible delegate issue)", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to zoom in", e);
        }
    }

    /**
     * Zoom out on the map with smooth animation
     */
    public void zoomOut() {
        if (kakaoMap == null) {
            Log.w(TAG, "Cannot zoom out - map not initialized");
            return;
        }
        try {
            // Use animated camera movement with 300ms duration and smooth gesture
            CameraAnimation animation = CameraAnimation.from(300, true, true);
            kakaoMap.moveCamera(CameraUpdateFactory.zoomOut(), animation);
            Log.d(TAG, "Zoomed out with animation");
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException during zoom out (possible delegate issue)", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to zoom out", e);
        }
    }

    /**
     * Zoom to a specific level with animation
     * @param zoomLevel Target zoom level (between getCameraMinLevel() and getCameraMaxLevel())
     */
    public void zoomTo(int zoomLevel) {
        if (kakaoMap == null) {
            Log.w(TAG, "Cannot zoom - map not initialized");
            return;
        }
        try {
            CameraAnimation animation = CameraAnimation.from(500, true, true);
            kakaoMap.moveCamera(CameraUpdateFactory.zoomTo(zoomLevel), animation);
            Log.d(TAG, "Zoomed to level " + zoomLevel + " with animation");
        } catch (RuntimeException e) {
            Log.e(TAG, "RuntimeException during zoom (possible delegate issue)", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to zoom to level " + zoomLevel, e);
        }
    }

    /**
     * Move camera to current location
     */
    public void moveToCurrent() {
        if (kakaoMap == null) {
            Log.w(TAG, "Cannot move to current location - map not initialized");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Map not ready", Toast.LENGTH_SHORT).show();
            }
            return;
        }
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
     * Navigate to specific coordinates and show a marker
     * @param latitude Target latitude
     * @param longitude Target longitude
     * @param title Optional title for the marker
     */
    public void navigateToLocation(double latitude, double longitude, String title) {
        if (kakaoMap == null || !isMapViewReady) {
            Log.d(TAG, "Map not ready yet, storing pending navigation: " + title + " at (" + latitude + ", " + longitude + ")");
            // Store for later when map is ready
            pendingNavigationLat = latitude;
            pendingNavigationLng = longitude;
            pendingNavigationTitle = title;
            return;
        }

        try {
            LatLng targetLocation = LatLng.from(latitude, longitude);

            // Move camera to target location with animation
            CameraAnimation animation = CameraAnimation.from(1000, true, true);
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(targetLocation, 17), animation);

            Log.d(TAG, "Navigated to location: " + latitude + ", " + longitude);

            if (getContext() != null && title != null) {
                Toast.makeText(getContext(), title + " 위치로 이동", Toast.LENGTH_SHORT).show();
            }

            // Clear pending navigation
            pendingNavigationLat = null;
            pendingNavigationLng = null;
            pendingNavigationTitle = null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate to location", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "위치로 이동할 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Show route from current location to destination
     * @param destinationLat Destination latitude
     * @param destinationLng Destination longitude
     * @param title Optional title for the destination
     */
    public void showRouteToLocation(double destinationLat, double destinationLng, String title) {
        if (kakaoMap == null || !isMapViewReady) {
            Log.d(TAG, "Map not ready yet, storing pending route request: " + title);
            // Store for later when map is ready
            pendingRouteLat = destinationLat;
            pendingRouteLng = destinationLng;
            pendingRouteTitle = title;
            return;
        }

        // Check location permission
        if (!hasLocationPermission()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
            }
            Log.w(TAG, "Location permission not granted, cannot show route");
            return;
        }

        // Get current location
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

            if (location != null) {
                double currentLat = location.getLatitude();
                double currentLng = location.getLongitude();

                Log.d(TAG, "Fetching route from (" + currentLat + ", " + currentLng + ") to (" + destinationLat + ", " + destinationLng + ")");

                // Fetch route from Kakao Mobility API in background thread
                fetchAndDisplayRoute(currentLng, currentLat, destinationLng, destinationLat, title);

                // Clear pending route
                pendingRouteLat = null;
                pendingRouteLng = null;
                pendingRouteTitle = null;
            } else {
                Log.w(TAG, "Current location is null, cannot show route");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "현재 위치를 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error while getting route", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "위치 권한 오류", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show route", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "경로를 표시할 수 없습니다", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Fetch route from Kakao Mobility API and display it on the map
     */
    private void fetchAndDisplayRoute(double originLng, double originLat, double destLng, double destLat, String destTitle) {
        searchExecutor.execute(() -> {
            HttpURLConnection conn = null;
            BufferedReader reader = null;

            try {
                // Build API URL
                String apiUrl = String.format(java.util.Locale.US,
                    "https://apis-navi.kakaomobility.com/v1/directions?origin=%f,%f&destination=%f,%f&priority=RECOMMEND",
                    originLng, originLat, destLng, destLat);

                Log.d(TAG, "Fetching route from Kakao Mobility API: " + apiUrl);

                URL url = new URL(apiUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "KakaoAK " + BuildConfig.KAKAO_REST_API_KEY);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Kakao Mobility API response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    Log.d(TAG, "Route API response received, length: " + response.length());

                    // Parse and display route
                    parseAndDisplayRoute(response.toString(), destTitle);
                } else {
                    // Read error response
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    Log.e(TAG, "Kakao Mobility API error " + responseCode + ": " + errorResponse.toString());

                    mainHandler.post(() -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "경로 조회 실패 (" + responseCode + ")", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching route from Kakao Mobility API", e);
                mainHandler.post(() -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "경로 조회 중 오류 발생", Toast.LENGTH_SHORT).show();
                    }
                });
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        Log.e(TAG, "Error closing reader", e);
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    /**
     * Parse route response and display on map
     */
    private void parseAndDisplayRoute(String jsonResponse, String destTitle) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            JSONArray routes = root.getJSONArray("routes");

            if (routes.length() == 0) {
                Log.w(TAG, "No routes found in response");
                mainHandler.post(() -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "경로를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }

            // Get first route
            JSONObject route = routes.getJSONObject(0);
            JSONArray sections = route.getJSONArray("sections");

            // Collect all route coordinates
            List<LatLng> routePoints = new ArrayList<>();

            for (int i = 0; i < sections.length(); i++) {
                JSONObject section = sections.getJSONObject(i);
                JSONArray roads = section.getJSONArray("roads");

                for (int j = 0; j < roads.length(); j++) {
                    JSONObject road = roads.getJSONObject(j);
                    JSONArray vertexes = road.getJSONArray("vertexes");

                    // Vertexes array contains [lng1, lat1, lng2, lat2, ...]
                    for (int k = 0; k < vertexes.length(); k += 2) {
                        double lng = vertexes.getDouble(k);
                        double lat = vertexes.getDouble(k + 1);
                        routePoints.add(LatLng.from(lat, lng));
                    }
                }
            }

            Log.d(TAG, "Parsed " + routePoints.size() + " route points");

            // Get route summary
            JSONObject summary = route.getJSONObject("summary");
            int distance = summary.getInt("distance"); // meters
            int duration = summary.getInt("duration"); // seconds

            String summaryText = String.format(java.util.Locale.getDefault(),
                "%s까지: %.1fkm, 약 %d분",
                destTitle != null ? destTitle : "목적지",
                distance / 1000.0,
                duration / 60);

            // Display route on map (on UI thread)
            mainHandler.post(() -> displayRouteOnMap(routePoints, summaryText));

        } catch (Exception e) {
            Log.e(TAG, "Error parsing route response", e);
            mainHandler.post(() -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "경로 파싱 오류", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Display route polyline on the map (must be called on UI thread)
     */
    private void displayRouteOnMap(List<LatLng> routePoints, String summaryText) {
        if (kakaoMap == null || routePoints == null || routePoints.size() < 2) {
            Log.w(TAG, "Cannot display route - invalid state");
            return;
        }

        try {
            // TODO: Draw polyline on map using Kakao Map SDK's route/polyline layer
            // For now, just move camera to show the route bounds and show message

            // Calculate bounds to fit all route points
            double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
            double minLng = Double.MAX_VALUE, maxLng = -Double.MAX_VALUE;

            for (LatLng point : routePoints) {
                minLat = Math.min(minLat, point.getLatitude());
                maxLat = Math.max(maxLat, point.getLatitude());
                minLng = Math.min(minLng, point.getLongitude());
                maxLng = Math.max(maxLng, point.getLongitude());
            }

            // Center of bounding box
            double centerLat = (minLat + maxLat) / 2;
            double centerLng = (minLng + maxLng) / 2;

            // Move camera to show route
            LatLng center = LatLng.from(centerLat, centerLng);
            CameraAnimation animation = CameraAnimation.from(1000, true, true);
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(center, 14), animation);

            Log.d(TAG, "Route displayed on map: " + routePoints.size() + " points");

            if (getContext() != null) {
                Toast.makeText(getContext(), summaryText, Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error displaying route on map", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Only call resume if MapView is ready and its internal components are initialized
        if (mapView != null && isMapViewReady) {
            try {
                mapView.resume();
                Log.d(TAG, "MapFragment resumed - MapView resumed successfully");
            } catch (NullPointerException e) {
                Log.e(TAG, "NullPointerException in mapView.resume() - internal MapSurfaceView not ready", e);
                isMapViewReady = false;
            } catch (Exception e) {
                Log.e(TAG, "Exception in mapView.resume()", e);
            }
        } else {
            Log.d(TAG, "MapFragment resumed - MapView not ready yet (mapView=" + (mapView != null) + ", isReady=" + isMapViewReady + ")");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Only call pause if MapView is ready and its internal components are initialized
        if (mapView != null && isMapViewReady) {
            try {
                mapView.pause();
                Log.d(TAG, "MapFragment paused - MapView paused successfully");
            } catch (NullPointerException e) {
                Log.e(TAG, "NullPointerException in mapView.pause() - internal MapSurfaceView not ready", e);
            } catch (Exception e) {
                Log.e(TAG, "Exception in mapView.pause()", e);
            }
        } else {
            Log.d(TAG, "MapFragment paused - MapView not ready (mapView=" + (mapView != null) + ", isReady=" + isMapViewReady + ")");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Mark as not ready before destroying
        isMapViewReady = false;

        if (mapView != null) {
            try {
                mapView.finish();
                Log.d(TAG, "MapView finished successfully");
            } catch (Exception e) {
                Log.e(TAG, "Exception in mapView.finish()", e);
            }
        }

        mapView = null;
        kakaoMap = null;
        hasStartedMap = false;
        Log.d(TAG, "MapFragment view destroyed");
    }
}
