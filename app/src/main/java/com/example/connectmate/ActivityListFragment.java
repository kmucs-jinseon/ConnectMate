package com.example.connectmate;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connectmate.models.Activity;
import com.example.connectmate.utils.FirebaseActivityManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
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
        loadActivitiesFromFirebase();
        updateUI();
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

        // Filter chips
        filterScrollView = view.findViewById(R.id.filter_scroll_view);
        filterChips = view.findViewById(R.id.filter_chips);

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
        activityAdapter = new ActivityAdapter(filteredActivities, this::onActivityClick);
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
        if (filterChips == null) return;

        // Get selected categories from chips
        List<String> selectedCategories = new ArrayList<>();
        if (filterChips.findViewById(R.id.chip_all) != null && filterChips.findViewById(R.id.chip_all).isSelected() ||
            filterChips.getCheckedChipIds().isEmpty()) {
            // Show all
            filteredActivities.clear();
            filteredActivities.addAll(allActivities);
        } else {
            filteredActivities.clear();
            // Filter by selected categories
            // TODO: Implement actual filtering logic based on chips
            filteredActivities.addAll(allActivities);
        }

        activityAdapter.notifyDataSetChanged();
        updateUI();
    }

    private void searchActivities(String query) {
        filteredActivities.clear();

        if (query.isEmpty()) {
            filteredActivities.addAll(allActivities);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Activity activity : allActivities) {
                if (activity.getTitle().toLowerCase().contains(lowerQuery) ||
                    activity.getLocation().toLowerCase().contains(lowerQuery) ||
                    activity.getDescription().toLowerCase().contains(lowerQuery)) {
                    filteredActivities.add(activity);
                }
            }
        }

        activityAdapter.notifyDataSetChanged();
        updateUI();
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
                // Add new activity to the beginning of the list
                if (!allActivities.contains(activity)) {
                    allActivities.add(0, activity);

                    // Update filtered list if no search/filter is active
                    String currentSearch = searchInput != null && searchInput.getText() != null ?
                        searchInput.getText().toString() : "";
                    if (currentSearch.isEmpty()) {
                        filteredActivities.add(0, activity);
                    } else {
                        searchActivities(currentSearch);
                    }

                    activityAdapter.notifyDataSetChanged();
                    updateUI();
                    Log.d(TAG, "Activity added: " + activity.getTitle());
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

                // Update filtered list
                for (int i = 0; i < filteredActivities.size(); i++) {
                    if (filteredActivities.get(i).getId().equals(activity.getId())) {
                        filteredActivities.set(i, activity);
                        break;
                    }
                }

                activityAdapter.notifyDataSetChanged();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up Firebase listeners when fragment is destroyed
        FirebaseActivityManager.getInstance().removeAllListeners();
    }
}
