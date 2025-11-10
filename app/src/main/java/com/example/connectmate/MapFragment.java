package com.example.connectmate;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
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
import java.util.ArrayList;
import java.util.List;

/**
 * MapFragment - Fragment containing Kakao Map with activity markers
 */
public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";

    // Map components
    private MapView mapView;
    private KakaoMap kakaoMap;
    private ProgressBar loadingIndicator;

    // Data
    private List<ActivityMarker> activityMarkers;
    private LocationManager locationManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize views
        mapView = view.findViewById(R.id.map_view);
        loadingIndicator = view.findViewById(R.id.loading_indicator);

        // Ensure MapView is visible from the start
        if (mapView != null) {
            mapView.setVisibility(android.view.View.VISIBLE);
            Log.d(TAG, "MapView found and set to VISIBLE in onCreateView");
        } else {
            Log.e(TAG, "ERROR: MapView is NULL!");
        }

        // Initialize data
        activityMarkers = new ArrayList<>();
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

                // Add sample activity markers
                addSampleActivityMarkers();
            }
        });
    }

    /**
     * Add sample activity markers to the map
     */
    private void addSampleActivityMarkers() {
        if (kakaoMap == null) return;

        // Sample activities
        activityMarkers.add(new ActivityMarker(
            "1", "Weekly Soccer Match", "Seoul National Park",
            "Today, 3:00 PM", "Join us for a friendly soccer match!",
            5, 10, 37.5665, 126.9780, "Sports"
        ));
        activityMarkers.add(new ActivityMarker(
            "2", "Study Group - Java", "Gangnam Library",
            "Tomorrow, 2:00 PM", "Let's study Java together",
            3, 8, 37.5700, 126.9850, "Study"
        ));
        activityMarkers.add(new ActivityMarker(
            "3", "Coffee Meetup", "Hongdae Cafe",
            "Saturday, 4:00 PM", "Casual coffee and chat",
            6, 12, 37.5550, 126.9200, "Social"
        ));

        try {
            if (kakaoMap.getLabelManager() != null) {
                LabelLayer labelLayer = kakaoMap.getLabelManager().getLayer();

                for (ActivityMarker activity : activityMarkers) {
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
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                            "Activity: " + labelId,
                            Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });

                Log.d(TAG, "Added " + activityMarkers.size() + " activity markers");
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

                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(currentLocation, 15));
                Log.d(TAG, "Camera moved to current location: " + latitude + ", " + longitude);
            } else {
                Log.d(TAG, "Location is null, using default location");
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
                    LatLng.from(37.5665, 126.9780), 13));
            Log.d(TAG, "Camera moved to default location (Seoul)");
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
     * Show/hide loading indicator
     */
    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Get the KakaoMap instance (for external control if needed)
     */
    public KakaoMap getKakaoMap() {
        return kakaoMap;
    }

    /**
     * Get the activity markers
     */
    public List<ActivityMarker> getActivityMarkers() {
        return activityMarkers;
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
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(targetLocation, 15));

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

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.resume();
        }
        Log.d(TAG, "MapFragment resumed");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.pause();
        }
        Log.d(TAG, "MapFragment paused");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.finish();
        }
        mapView = null;
        kakaoMap = null;
        Log.d(TAG, "MapFragment view destroyed");
    }
}
