package com.example.connectmate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connectmate.models.Activity;
import com.example.connectmate.utils.FirebaseActivityManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ActivityListFragment extends Fragment {

    private static final String TAG = "ActivityListFragment";

    // UI Components
    private ImageButton btnSearch;
    private ImageButton btnFilter;
    private TextInputLayout searchLayout;
    private TextInputEditText searchInput;
    private View filterScrollView;
    private ChipGroup filterChips;

    private RecyclerView activityRecyclerView;
    private LinearLayout emptyState;
    private MaterialButton btnCreateActivity;

    // Adapter
    private ActivityAdapter activityAdapter;

    // Data
    private List<Activity> allActivities;
    private List<Activity> filteredActivities;
    private boolean isUpdatingChipSelection = false;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentUserLocation;
    private static final double DEFAULT_LAT = 37.5665; // Seoul
    private static final double DEFAULT_LNG = 126.9780;

    public ActivityListFragment() {
        super(R.layout.fragment_activity_list);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupClickListeners();
        initializeLocation();
        loadActivitiesFromFirebase();
        updateUI();
    }

    private void initializeLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        getCurrentUserLocation();
    }

    private void getCurrentUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Use default location if no permission
            currentUserLocation = new Location("default");
            currentUserLocation.setLatitude(DEFAULT_LAT);
            currentUserLocation.setLongitude(DEFAULT_LNG);
            Log.d(TAG, "No location permission, using default location");
            return;
        }

        // Get last known location first
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentUserLocation = location;
                Log.d(TAG, "Got user location: " + location.getLatitude() + ", " + location.getLongitude());
                // Re-sort activities with new location
                applyFiltersAndSearch();
            } else {
                // Request fresh location
                requestFreshLocation();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get location", e);
            // Use default location
            currentUserLocation = new Location("default");
            currentUserLocation.setLatitude(DEFAULT_LAT);
            currentUserLocation.setLongitude(DEFAULT_LNG);
        });
    }

    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdates(1)
                .build();

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentUserLocation = location;
                    Log.d(TAG, "Got fresh location: " + location.getLatitude() + ", " + location.getLongitude());
                    // Re-sort activities with new location
                    applyFiltersAndSearch();
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * Calculate distance between two points using Haversine formula
     * @return distance in meters
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth's radius in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Get distance from user's current location to activity
     */
    private double getDistanceToActivity(Activity activity) {
        double userLat = currentUserLocation != null ? currentUserLocation.getLatitude() : DEFAULT_LAT;
        double userLng = currentUserLocation != null ? currentUserLocation.getLongitude() : DEFAULT_LNG;
        return calculateDistance(userLat, userLng, activity.getLatitude(), activity.getLongitude());
    }

    /**
     * Parse activity date string to comparable timestamp
     */
    private long parseDateToTimestamp(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return Long.MAX_VALUE; // Put activities without dates at the end
        }
        try {
            // Expected format: "2024년 12월 25일" or similar
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy년 MM월 dd일", java.util.Locale.KOREAN);
            java.util.Date date = sdf.parse(dateStr);
            return date != null ? date.getTime() : Long.MAX_VALUE;
        } catch (Exception e) {
            // Try alternative format
            try {
                java.text.SimpleDateFormat sdf2 = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.util.Date date = sdf2.parse(dateStr);
                return date != null ? date.getTime() : Long.MAX_VALUE;
            } catch (Exception e2) {
                return Long.MAX_VALUE;
            }
        }
    }

    private void initializeViews(View view) {
        // Hide the AppBar header when in MainActivity (since MainActivity has its own overlay)
        View appBar = view.findViewById(R.id.appbar);
        if (appBar != null) {
            appBar.setVisibility(View.GONE);
        }

        // Header buttons
        btnSearch = view.findViewById(R.id.btn_search);
        btnFilter = view.findViewById(R.id.btn_filter);

        // Search
        searchLayout = view.findViewById(R.id.search_layout);
        searchInput = view.findViewById(R.id.search_input);

        // Filter chips - use MainActivity's chips if this fragment is in MainActivity
        filterScrollView = view.findViewById(R.id.filter_scroll_view);
        filterChips = view.findViewById(R.id.filter_chips);

        // Try to get MainActivity's filter chips if available
        if (getActivity() != null && getActivity() instanceof MainActivity) {
            ChipGroup mainActivityChips = getActivity().findViewById(R.id.activity_filter_chips);
            if (mainActivityChips != null) {
                filterChips = mainActivityChips;
                Log.d(TAG, "Using MainActivity's filter chips");
            }
        }
        setupFilterChipSelectionBehavior();

        // Activity list
        activityRecyclerView = view.findViewById(R.id.activity_recycler_view);
        emptyState = view.findViewById(R.id.empty_state);
        btnCreateActivity = view.findViewById(R.id.btn_create_activity);
    }

    private void setupRecyclerView() {
        // Initialize data lists
        allActivities = new ArrayList<>();
        filteredActivities = new ArrayList<>();

        // Setup RecyclerView
        activityAdapter = new ActivityAdapter(filteredActivities, new ActivityAdapter.OnActivityClickListener() {
            @Override
            public void onActivityClick(Activity activity) {
                ActivityListFragment.this.onActivityClick(activity);
            }

            @Override
            public void onEditActivity(Activity activity) {
                ActivityListFragment.this.onEditActivity(activity);
            }
        });
        activityRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        activityRecyclerView.setAdapter(activityAdapter);
    }

    private void setupClickListeners() {
        // Search button - toggle search input visibility
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> toggleSearch());
        }

        // Filter button - toggle filter chips
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> toggleFilter());
        }

        // Filter chips
        if (filterChips != null) {
            filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                filterActivities();
            });
        }

        // Search input text watcher
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchActivities(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Create activity button (in empty state)
        btnCreateActivity.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CreateActivityActivity.class);
            startActivity(intent);
        });
    }

    private void setupFilterChipSelectionBehavior() {
        if (filterChips == null) {
            return;
        }

        for (int i = 0; i < filterChips.getChildCount(); i++) {
            View child = filterChips.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setOnCheckedChangeListener((button, isChecked) -> {
                    if (isUpdatingChipSelection) {
                        return;
                    }
                    handleChipSelectionChange(button.getId(), isChecked);
                });
            }
        }
    }

    private void handleChipSelectionChange(int chipId, boolean isChecked) {
        if (filterChips == null) {
            return;
        }

        if (isAllChip(chipId)) {
            if (isChecked) {
                selectOnlyAllChip();
            }
            return;
        }

        if (isChecked) {
            deselectAllChip();
        } else if (!hasAnyCategoryChipChecked()) {
            selectOnlyAllChip();
        }
    }

    private void selectOnlyAllChip() {
        if (filterChips == null) {
            return;
        }
        isUpdatingChipSelection = true;
        for (int i = 0; i < filterChips.getChildCount(); i++) {
            View child = filterChips.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setChecked(isAllChip(chip.getId()));
            }
        }
        isUpdatingChipSelection = false;
    }

    private void deselectAllChip() {
        Chip allChip = findAllChip();
        if (allChip == null) {
            return;
        }
        isUpdatingChipSelection = true;
        allChip.setChecked(false);
        isUpdatingChipSelection = false;
    }

    private Chip findAllChip() {
        if (filterChips == null) {
            return null;
        }
        Chip allChip = filterChips.findViewById(R.id.chip_all);
        if (allChip == null) {
            allChip = filterChips.findViewById(R.id.activity_chip_all);
        }
        return allChip;
    }

    private boolean hasAnyCategoryChipChecked() {
        if (filterChips == null) {
            return false;
        }
        List<Integer> checkedIds = filterChips.getCheckedChipIds();
        for (Integer id : checkedIds) {
            if (!isAllChip(id)) {
                return true;
            }
        }
        return false;
    }

    private void toggleSearch() {
        if (searchLayout == null) return;

        if (searchLayout.getVisibility() == View.VISIBLE) {
            searchLayout.setVisibility(View.GONE);
            if (searchInput != null) {
                searchInput.setText("");
            }
            searchActivities("");
        } else {
            searchLayout.setVisibility(View.VISIBLE);
            if (searchInput != null) {
                searchInput.requestFocus();
            }
        }
    }

    private void toggleFilter() {
        if (filterScrollView == null) return;

        if (filterScrollView.getVisibility() == View.VISIBLE) {
            filterScrollView.setVisibility(View.GONE);
        } else {
            filterScrollView.setVisibility(View.VISIBLE);
        }
    }

    private void filterActivities() {
        applyFiltersAndSearch();
    }

    private void searchActivities(String query) {
        applyFiltersAndSearch();
    }

    private void applyFiltersAndSearch() {
        filteredActivities.clear();

        // Get current search query
        String searchQuery = "";
        if (searchInput != null && searchInput.getText() != null) {
            searchQuery = searchInput.getText().toString().toLowerCase();
        }

        // Get selected categories from chips
        List<String> selectedCategories = getSelectedCategories();

        // Apply both filters
        for (Activity activity : allActivities) {
            // Check category filter
            boolean matchesCategory = false;
            if (selectedCategories.isEmpty()) {
                // No category filter or "All" selected - show all categories
                matchesCategory = true;
            } else {
                // Handle comma-separated categories (e.g., "운동,스터디")
                String activityCategory = activity.getCategory();
                if (activityCategory != null && !activityCategory.isEmpty()) {
                    // Split by comma and check if any category matches
                    String[] categories = activityCategory.split(",");
                    for (String cat : categories) {
                        if (selectedCategories.contains(cat.trim())) {
                            matchesCategory = true;
                            break;
                        }
                    }
                }
            }

            // Check search filter
            boolean matchesSearch = false;
            if (searchQuery.isEmpty()) {
                matchesSearch = true;
            } else {
                String title = activity.getTitle() != null ? activity.getTitle().toLowerCase() : "";
                String location = activity.getLocation() != null ? activity.getLocation().toLowerCase() : "";
                String description = activity.getDescription() != null ? activity.getDescription().toLowerCase() : "";
                matchesSearch = title.contains(searchQuery) ||
                              location.contains(searchQuery) ||
                              description.contains(searchQuery);
            }

            // Add activity if it matches both filters
            if (matchesCategory && matchesSearch) {
                filteredActivities.add(activity);
            }
        }

        // Sort by distance (closest first), then by date (soonest first) for same distance
        Collections.sort(filteredActivities, (a1, a2) -> {
            double dist1 = getDistanceToActivity(a1);
            double dist2 = getDistanceToActivity(a2);

            // Compare distances (within 100m considered same location)
            if (Math.abs(dist1 - dist2) < 100) {
                // Same location - sort by date (soonest first)
                long date1 = parseDateToTimestamp(a1.getDate());
                long date2 = parseDateToTimestamp(a2.getDate());
                return Long.compare(date1, date2);
            }

            // Different locations - sort by distance (closest first)
            return Double.compare(dist1, dist2);
        });

        activityAdapter.notifyDataSetChanged();
        updateUI();
    }

    private List<String> getSelectedCategories() {
        List<String> selectedCategories = new ArrayList<>();

        if (filterChips == null) {
            return selectedCategories;
        }

        List<Integer> checkedChipIds = filterChips.getCheckedChipIds();

        // If "All" is checked or no chips are checked, return empty list (show all)
        if (checkedChipIds.contains(R.id.chip_all) ||
            checkedChipIds.contains(R.id.activity_chip_all) ||
            checkedChipIds.isEmpty()) {
            return selectedCategories;
        }

        // Map chip IDs to category names
        for (Integer chipId : checkedChipIds) {
            String category = getCategoryFromChipId(chipId);
            if (category != null) {
                selectedCategories.add(category);
            }
        }

        return selectedCategories;
    }

    private boolean isAllChip(int chipId) {
        return chipId == R.id.chip_all || chipId == R.id.activity_chip_all;
    }

    private String getCategoryFromChipId(int chipId) {
        // Fragment's chip IDs
        if (chipId == R.id.chip_sports) return "운동";
        if (chipId == R.id.chip_outdoor) return "야외활동";
        if (chipId == R.id.chip_study) return "스터디";
        if (chipId == R.id.chip_culture) return "문화";
        if (chipId == R.id.chip_social) return "소셜";
        if (chipId == R.id.chip_food) return "맛집";
        if (chipId == R.id.chip_travel) return "여행";
        if (chipId == R.id.chip_game) return "게임";
        if (chipId == R.id.chip_hobby) return "취미";
        if (chipId == R.id.chip_volunteer) return "봉사";
        if (chipId == R.id.chip_other) return "기타";

        // MainActivity's chip IDs (activity_chip_ prefix)
        if (chipId == R.id.activity_chip_sports) return "운동";
        if (chipId == R.id.activity_chip_outdoor) return "야외활동";
        if (chipId == R.id.activity_chip_study) return "스터디";
        if (chipId == R.id.activity_chip_culture) return "문화";
        if (chipId == R.id.activity_chip_social) return "소셜";
        if (chipId == R.id.activity_chip_food) return "맛집";
        if (chipId == R.id.activity_chip_travel) return "여행";
        if (chipId == R.id.activity_chip_game) return "게임";
        if (chipId == R.id.activity_chip_hobby) return "취미";
        if (chipId == R.id.activity_chip_volunteer) return "봉사";
        if (chipId == R.id.activity_chip_other) return "기타";

        return null;
    }

    private void updateUI() {
        // Show/hide empty state
        if (filteredActivities.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            activityRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            activityRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void loadActivitiesFromFirebase() {
        // Load activities from Firebase with real-time updates
        FirebaseActivityManager activityManager = FirebaseActivityManager.getInstance();

        activityManager.listenForActivityChanges(new FirebaseActivityManager.ActivityChangeListener() {
            @Override
            public void onActivityAdded(Activity activity) {
                // Check if activity already exists by ID
                boolean exists = false;
                for (Activity a : allActivities) {
                    if (a.getId() != null && a.getId().equals(activity.getId())) {
                        exists = true;
                        break;
                    }
                }

                // Add new activity to the beginning of the list if it doesn't exist
                if (!exists) {
                    allActivities.add(0, activity);

                    // Re-apply filters and search to update the filtered list
                    applyFiltersAndSearch();

                    Log.d(TAG, "Activity added: " + activity.getTitle());
                } else {
                    Log.d(TAG, "Activity already exists: " + activity.getTitle());
                }
            }

            @Override
            public void onActivityChanged(Activity activity) {
                // Update existing activity
                for (int i = 0; i < allActivities.size(); i++) {
                    if (allActivities.get(i).getId().equals(activity.getId())) {
                        allActivities.set(i, activity);
                        break;
                    }
                }

                // Re-apply filters and search to update the filtered list
                applyFiltersAndSearch();

                Log.d(TAG, "Activity updated: " + activity.getTitle());
            }

            @Override
            public void onActivityRemoved(Activity activity) {
                // Remove activity from lists
                allActivities.removeIf(a -> a.getId().equals(activity.getId()));
                filteredActivities.removeIf(a -> a.getId().equals(activity.getId()));

                activityAdapter.notifyDataSetChanged();
                updateUI();
                Log.d(TAG, "Activity removed: " + activity.getTitle());
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading activities", e);
                Toast.makeText(requireContext(), "활동 로드 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onActivityClick(Activity activity) {
        // Open activity detail screen
        Intent intent = new Intent(requireContext(), ActivityDetailActivity.class);
        intent.putExtra("activity_id", activity.getId());
        intent.putExtra("activity", activity);
        startActivity(intent);
    }

    private void onEditActivity(Activity activity) {
        // Open edit activity screen
        Intent intent = new Intent(requireContext(), CreateActivityActivity.class);
        intent.putExtra("edit_mode", true);
        intent.putExtra("activity_id", activity.getId());
        intent.putExtra("activity", activity);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up Firebase listeners when fragment is destroyed
        FirebaseActivityManager.getInstance().removeAllListeners();
    }
}
