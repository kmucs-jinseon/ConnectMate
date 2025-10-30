package com.example.connectmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

/**
 * MapActivity - Displays a full-screen Kakao Map with location features
 * Shows nearby locations in a bottom sheet and provides search functionality
 */
public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";

    // Fragment
    private MapFragment mapFragment;

    // UI Components
    private FloatingActionButton fabCreateActivity;
    private ImageButton searchButton;
    private TextInputLayout searchInputLayout;
    private RecyclerView locationsRecyclerView;
    private CardView bottomSheet;
    private BottomSheetBehavior<CardView> bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MapActivity onCreate started");

        // Set the content view
        setContentView(R.layout.activity_map);

        // Initialize all UI components
        initializeViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Set up click listeners
        setupClickListeners();

        // Set up bottom sheet
        setupBottomSheet();

        // Initialize and add MapFragment
        setupMapFragment(savedInstanceState);

        Log.d(TAG, "MapActivity initialized - Kakao Map should be visible");
    }

    /**
     * Initialize all UI components from layout
     */
    private void initializeViews() {
        fabCreateActivity = findViewById(R.id.fab_create_activity);
        searchButton = findViewById(R.id.search_button);
        searchInputLayout = findViewById(R.id.search_input_layout);
        locationsRecyclerView = findViewById(R.id.locations_recycler_view);
        bottomSheet = findViewById(R.id.bottom_sheet);

        // Log which components were found
        if (fabCreateActivity == null) {
            Log.e(TAG, "FAB not found in layout");
        }
        if (searchButton == null) {
            Log.e(TAG, "Search button not found in layout");
        }
        if (searchInputLayout == null) {
            Log.e(TAG, "Search input layout not found in layout");
        }
        if (locationsRecyclerView == null) {
            Log.e(TAG, "Locations RecyclerView not found in layout");
        }
        if (bottomSheet == null) {
            Log.e(TAG, "Bottom sheet not found in layout");
        }
    }

    /**
     * Set up the RecyclerView for nearby locations
     */
    private void setupRecyclerView() {
        if (locationsRecyclerView != null) {
            locationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            locationsRecyclerView.setHasFixedSize(true);
            Log.d(TAG, "RecyclerView configured");
            // TODO: Set adapter when location data is available
            // locationsRecyclerView.setAdapter(new LocationAdapter(locationsList));
        } else {
            Log.w(TAG, "Cannot setup RecyclerView - view is null");
        }
    }

    /**
     * Set up all click listeners for UI components
     */
    private void setupClickListeners() {
        // FAB click listener
        if (fabCreateActivity != null) {
            fabCreateActivity.setOnClickListener(v -> {
                Log.d(TAG, "FAB clicked - Opening CreateActivityActivity");
                Intent intent = new Intent(MapActivity.this, CreateActivityActivity.class);
                startActivity(intent);
            });
        }

        // Search button click listener
        if (searchButton != null) {
            searchButton.setOnClickListener(v -> {
                Log.d(TAG, "Search button clicked");
                toggleSearchVisibility();
            });
        }
    }

    /**
     * Toggle search input visibility
     */
    private void toggleSearchVisibility() {
        if (searchInputLayout == null) {
            Log.w(TAG, "Cannot toggle search - search input layout is null");
            return;
        }

        if (searchInputLayout.getVisibility() == View.VISIBLE) {
            searchInputLayout.setVisibility(View.GONE);
            Log.d(TAG, "Search hidden");
        } else {
            searchInputLayout.setVisibility(View.VISIBLE);
            searchInputLayout.requestFocus();
            Log.d(TAG, "Search shown");
        }
    }

    /**
     * Set up the bottom sheet behavior
     */
    private void setupBottomSheet() {
        if (bottomSheet == null) {
            Log.e(TAG, "Cannot setup bottom sheet - view is null");
            return;
        }

        try {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.setPeekHeight(200);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            // Add callback to monitor bottom sheet state changes
            bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    String state = getBottomSheetStateName(newState);
                    Log.d(TAG, "Bottom sheet state changed to: " + state);
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    // Can be used for animations based on slide offset
                }
            });

            Log.d(TAG, "Bottom sheet configured");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom sheet", e);
        }
    }

    /**
     * Get human-readable bottom sheet state name
     */
    private String getBottomSheetStateName(int state) {
        switch (state) {
            case BottomSheetBehavior.STATE_COLLAPSED:
                return "COLLAPSED";
            case BottomSheetBehavior.STATE_EXPANDED:
                return "EXPANDED";
            case BottomSheetBehavior.STATE_DRAGGING:
                return "DRAGGING";
            case BottomSheetBehavior.STATE_SETTLING:
                return "SETTLING";
            case BottomSheetBehavior.STATE_HIDDEN:
                return "HIDDEN";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Set up MapFragment with Kakao Map
     */
    private void setupMapFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            // First time - create new MapFragment
            Log.d(TAG, "Creating new MapFragment");
            mapFragment = new MapFragment();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.map_container, mapFragment);
            transaction.commit();

            Log.d(TAG, "MapFragment added to map_container");
        } else {
            // Restore existing fragment
            Log.d(TAG, "Restoring existing MapFragment");
            mapFragment = (MapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map_container);

            if (mapFragment == null) {
                Log.w(TAG, "MapFragment not found after restore - creating new one");
                mapFragment = new MapFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.map_container, mapFragment);
                transaction.commit();
            }
        }
    }

    /**
     * Expand the bottom sheet to show locations
     */
    public void expandBottomSheet() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            Log.d(TAG, "Bottom sheet expanded");
        }
    }

    /**
     * Collapse the bottom sheet
     */
    public void collapseBottomSheet() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            Log.d(TAG, "Bottom sheet collapsed");
        }
    }

    /**
     * Get the MapFragment instance
     * @return MapFragment or null if not initialized
     */
    public MapFragment getMapFragment() {
        return mapFragment;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MapActivity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MapActivity paused");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MapActivity destroyed");
    }
}
