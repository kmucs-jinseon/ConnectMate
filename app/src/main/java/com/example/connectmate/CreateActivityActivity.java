package com.example.connectmate;
import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.example.connectmate.models.Activity;
import com.example.connectmate.models.PlaceSearchResult;
import com.example.connectmate.utils.FirebaseActivityManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CreateActivityActivity extends AppCompatActivity {

    private static final String TAG = "CreateActivityActivity";

    private ImageButton backButton, closeButton;
    private TextInputEditText titleInput, descriptionInput, dateInput, timeInput,
            locationInput, participantLimitInput, hashtagsInput;
    private ChipGroup categoryChipGroup;
    private MaterialButtonToggleGroup visibilityToggle;
    private MaterialButton createButton;
    private MaterialButton deleteButton;
    private List<String> selectedCategories = new ArrayList<>();

    // Location data from map search
    private String locationName;
    private String locationAddress;
    private double locationLatitude;
    private double locationLongitude;
    private boolean suppressLocationTextWatcher = false;
    private boolean hasLinkedLocationCoordinates = false;

    // Location search components
    private CardView locationSearchResultsCard;
    private RecyclerView locationSearchResultsRecycler;
    private PlaceSearchAdapter locationSearchAdapter;
    private List<PlaceSearchResult> locationSearchResults;
    private OkHttpClient httpClient;
    private Gson gson;
    private android.os.Handler searchHandler;
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY_MS = 800;

    // Current location for sorting search results
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    // Edit mode variables
    private boolean isEditMode = false;
    private Activity editingActivity = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_create_activity); // 👉 XML 파일 이름에 맞게 수정하세요

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getCurrentUserLocation();

        initViews();
        setupLocationInput(); // Setup listener before handling intent
        handleEditModeOrLocation(); // Check for edit mode or location data
        setupCategoryDropdown();
        setupDateTimePickers();
        setupButtons();
    }

    /**
     * Handle edit mode or location intent data
     */
    private void handleEditModeOrLocation() {
        if (getIntent() != null) {
            // Check if in edit mode
            isEditMode = getIntent().getBooleanExtra("edit_mode", false);
            editingActivity = (Activity) getIntent().getSerializableExtra("activity");

            if (isEditMode && editingActivity != null) {
                // Pre-populate fields with existing activity data
                populateFieldsForEdit();
            } else {
                // Handle location data from MapFragment
                handleLocationIntent();
            }
        }
    }

    /**
     * Populate fields when editing an existing activity
     */
    private void populateFieldsForEdit() {
        if (editingActivity == null) return;

        // Set title
        if (titleInput != null && editingActivity.getTitle() != null) {
            titleInput.setText(editingActivity.getTitle());
        }

        // Set description
        if (descriptionInput != null && editingActivity.getDescription() != null) {
            descriptionInput.setText(editingActivity.getDescription());
        }

        // Set date
        if (dateInput != null && editingActivity.getDate() != null) {
            dateInput.setText(editingActivity.getDate());
        }

        // Set time
        if (timeInput != null && editingActivity.getTime() != null) {
            timeInput.setText(editingActivity.getTime());
        }

        // Set location
        if (locationInput != null && editingActivity.getLocation() != null) {
            locationName = editingActivity.getLocation();
            locationLatitude = editingActivity.getLatitude();
            locationLongitude = editingActivity.getLongitude();
            hasLinkedLocationCoordinates = locationLatitude != 0.0 || locationLongitude != 0.0;
            setLocationInputText(locationName);
        }

        // Set participant limit
        if (participantLimitInput != null && editingActivity.getMaxParticipants() > 0) {
            participantLimitInput.setText(String.valueOf(editingActivity.getMaxParticipants()));
        }

        // Set hashtags
        if (hashtagsInput != null && editingActivity.getHashtags() != null) {
            hashtagsInput.setText(editingActivity.getHashtags());
        }

        // Set categories (will be selected after chips are created)
        // Split comma-separated categories
        String categoryStr = editingActivity.getCategory();
        if (categoryStr != null && !categoryStr.isEmpty()) {
            String[] categories = categoryStr.split(",");
            for (String cat : categories) {
                selectedCategories.add(cat.trim());
            }
        }

        // Update button text for edit mode
        if (createButton != null) {
            createButton.setText("활동 수정 완료");
        }

        // Show delete button in edit mode
        if (deleteButton != null) {
            deleteButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Setup location input with search functionality
     */
    private void setupLocationInput() {
        if (locationInput == null) {
            Log.e(TAG, "locationInput is null!");
            return;
        }

        // Setup search adapter
        if (locationSearchResultsRecycler != null) {
            locationSearchAdapter = new PlaceSearchAdapter(locationSearchResults, place -> {
                // When place is selected, populate location field
                locationName = place.getPlaceName();
                locationAddress = place.getRoadAddressName() != null && !place.getRoadAddressName().isEmpty()
                    ? place.getRoadAddressName()
                    : place.getAddressName();
                locationLatitude = place.getLatitude();
                locationLongitude = place.getLongitude();
                hasLinkedLocationCoordinates = true;

                // Set the location text
                setLocationInputText(locationName);

                // Hide results
                hideLocationSearchResults();

                // Hide keyboard
                hideKeyboard();

                Log.d(TAG, "Location selected: " + locationName + " (" + locationLatitude + ", " + locationLongitude + ")");
                Toast.makeText(this, "📍 " + locationName + " 선택됨", Toast.LENGTH_SHORT).show();
            });

            locationSearchResultsRecycler.setLayoutManager(new LinearLayoutManager(this));
            locationSearchResultsRecycler.setAdapter(locationSearchAdapter);
        }

        // Real-time search as user types
        locationInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressLocationTextWatcher) {
                    suppressLocationTextWatcher = false;
                    return;
                }

                clearStoredLocationCoordinates();

                // Cancel previous search request
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s.toString().trim();

                // If empty, hide results
                if (query.isEmpty()) {
                    hideLocationSearchResults();
                    return;
                }

                // Schedule new search after delay (debounce)
                searchRunnable = () -> {
                    Log.d(TAG, "Location search triggered for: " + query);
                    performLocationSearch(query);
                };

                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
        });

        // Handle Enter key on keyboard
        locationInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = locationInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    Log.d(TAG, "Keyboard search triggered: " + query);
                    performLocationSearch(query);
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });
    }

    /**
     * Handle location data passed from MapFragment
     */
    private void handleLocationIntent() {
        if (getIntent() != null) {
            locationName = getIntent().getStringExtra("location_name");
            locationAddress = getIntent().getStringExtra("location_address");
            locationLatitude = getIntent().getDoubleExtra("location_latitude", 0.0);
            locationLongitude = getIntent().getDoubleExtra("location_longitude", 0.0);

            // If location data exists, populate the location field
            if (locationName != null && !locationName.isEmpty()) {
                if (locationInput != null) {
                    hasLinkedLocationCoordinates = locationLatitude != 0.0 || locationLongitude != 0.0;
                    setLocationInputText(locationName);
                    // Keep it editable in case user wants to change it
                }
                Log.d(TAG, "Location received: " + locationName + " (" + locationLatitude + ", " + locationLongitude + ")");
                Toast.makeText(this, "📍 " + locationName + " 선택됨", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Sets the location input text without triggering side effects like clearing coordinates.
     */
    private void setLocationInputText(String text) {
        if (locationInput == null) {
            return;
        }
        suppressLocationTextWatcher = true;
        locationInput.setText(text);
        Editable currentText = locationInput.getText();
        if (currentText != null) {
            locationInput.setSelection(currentText.length());
        }
    }

    /**
     * Clears stored coordinates when the user edits the location text manually.
     */
    private void clearStoredLocationCoordinates() {
        if (!hasLinkedLocationCoordinates) {
            return;
        }
        hasLinkedLocationCoordinates = false;
        locationLatitude = 0.0;
        locationLongitude = 0.0;
        locationAddress = null;
        locationName = null;
        Log.d(TAG, "Location input edited manually - cleared stored coordinates.");
    }

    private void initViews() {
        backButton = findViewById(R.id.back_button);
        closeButton = findViewById(R.id.close_button);
        titleInput = findViewById(R.id.title_input);
        descriptionInput = findViewById(R.id.description_input);
        categoryChipGroup = findViewById(R.id.category_chip_group);
        dateInput = findViewById(R.id.date_input);
        timeInput = findViewById(R.id.time_input);
        locationInput = findViewById(R.id.location_input);
        participantLimitInput = findViewById(R.id.participant_limit_input);
        hashtagsInput = findViewById(R.id.hashtags_input);
        visibilityToggle = findViewById(R.id.visibility_toggle);
        createButton = findViewById(R.id.create_button);
        deleteButton = findViewById(R.id.delete_button);

        // Location search components
        locationSearchResultsCard = findViewById(R.id.location_search_results_card);
        locationSearchResultsRecycler = findViewById(R.id.location_search_results_recycler);

        // Initialize HTTP client and search components
        httpClient = new OkHttpClient();
        gson = new Gson();
        locationSearchResults = new ArrayList<>();
        searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    }

    private void setupCategoryDropdown() {
        // 카테고리 칩 생성
        String[] categories = getResources().getStringArray(R.array.category_options);
        Log.d(TAG, "Setting up categories. Count: " + categories.length);

        if (categoryChipGroup == null) {
            Log.e(TAG, "categoryChipGroup is null!");
            return;
        }

        for (String category : categories) {
            // Create chip with category-specific color
            Chip chip = new Chip(this);

            // Get category color from CategoryMapper
            int colorRes = com.example.connectmate.utils.CategoryMapper.getCategoryColor(category);
            int categoryColor = getResources().getColor(colorRes, null);

            // Set background color based on category
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(categoryColor));
            chip.setTextColor(getResources().getColor(android.R.color.white, null));
            chip.setCheckedIconVisible(false);
            chip.setChipStrokeWidth(0f);
            chip.setChipCornerRadius(12f); // Rounded corners like participant count badge
            chip.setText(category);
            chip.setCheckable(true);
            chip.setClickable(true);

            // Add padding similar to toolbar participant count
            chip.setChipMinHeight(40f);
            chip.setTextSize(12f);
            chip.setChipStartPadding(10f);
            chip.setChipEndPadding(10f);
            chip.setTextStartPadding(10f);
            chip.setTextEndPadding(10f);

            // Add vertical padding (top and bottom) - 8dp
            int horizontalPadding = chip.getPaddingLeft();
            chip.setPadding(horizontalPadding, 8, horizontalPadding, 8);

            Log.d(TAG, "Created chip: " + category);

            categoryChipGroup.addView(chip);
        }

        Log.d(TAG, "Finished adding chips. ChipGroup child count: " + categoryChipGroup.getChildCount());

        // Handle chip selection (now supports multiple selections)
        categoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            selectedCategories.clear();
            for (int chipId : checkedIds) {
                Chip selectedChip = findViewById(chipId);
                if (selectedChip != null) {
                    selectedCategories.add(selectedChip.getText().toString());
                }
            }
            Log.d(TAG, "Categories selected: " + selectedCategories);
        });

        // If in edit mode, select the appropriate category chips
        if (isEditMode && !selectedCategories.isEmpty()) {
            for (int i = 0; i < categoryChipGroup.getChildCount(); i++) {
                View view = categoryChipGroup.getChildAt(i);
                if (view instanceof Chip) {
                    Chip chip = (Chip) view;
                    String chipText = chip.getText().toString();
                    if (selectedCategories.contains(chipText)) {
                        chip.setChecked(true);
                    }
                }
            }
        }
    }

    private void setupDateTimePickers() {
        dateInput.setOnClickListener(v -> showDatePicker());
        timeInput.setOnClickListener(v -> showTimePicker());
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, R.style.DatePickerTheme,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
                    dateInput.setText(date);
                }, year, month, day);
        dialog.show();
    }

    private void showTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(this, R.style.TimePickerTheme,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format("%02d:%02d", selectedHour, selectedMinute);
                    timeInput.setText(time);
                }, hour, minute, true);
        dialog.show();
    }

    private void setupButtons() {
        backButton.setOnClickListener(v -> finish());

        closeButton.setOnClickListener(v -> finish());

        // Delete button click listener (only visible in edit mode)
        deleteButton.setOnClickListener(v -> {
            if (isEditMode && editingActivity != null) {
                // Show confirmation dialog
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("활동 삭제")
                    .setMessage("이 활동을 삭제하시겠습니까? 이 작업은 취소할 수 없습니다.")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        deleteActivity();
                    })
                    .setNegativeButton("취소", null)
                    .show();
            }
        });

        createButton.setOnClickListener(v -> {
            Log.d(TAG, "Create button clicked");

            // Disable button to prevent double-click
            createButton.setEnabled(false);

            try {
                String title = (titleInput.getText() != null) ? titleInput.getText().toString().trim() : "";
                String description = (descriptionInput.getText() != null) ? descriptionInput.getText().toString().trim() : "";
                // Join selected categories with comma
                String category = String.join(",", selectedCategories);
                String date = (dateInput.getText() != null) ? dateInput.getText().toString().trim() : "";
                String time = (timeInput.getText() != null) ? timeInput.getText().toString().trim() : "";
                String location = (locationInput.getText() != null) ? locationInput.getText().toString().trim() : "";
                String participantsStr = (participantLimitInput.getText() != null) ? participantLimitInput.getText().toString().trim() : "";
                String hashtags = (hashtagsInput.getText() != null) ? hashtagsInput.getText().toString().trim() : "";

                Log.d(TAG, "Validating fields - Title: " + title + ", Categories: " + category);

                // Validate required fields
                if (title.isEmpty()) {
                    titleInput.setError("제목을 입력하세요");
                    Toast.makeText(this, "제목을 입력하세요", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Validation failed: Title is empty");
                    createButton.setEnabled(true);
                    return;
                }

                if (description.isEmpty()) {
                    descriptionInput.setError("활동 설명을 입력하세요");
                    Toast.makeText(this, "활동 설명을 입력하세요", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Validation failed: Description is empty");
                    createButton.setEnabled(true);
                    return;
                }

                if (category == null || category.isEmpty()) {
                    Toast.makeText(this, "카테고리를 선택하세요", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Validation failed: Category is empty");
                    createButton.setEnabled(true);
                    return;
                }

                if (date.isEmpty()) {
                    dateInput.setError("날짜를 선택하세요");
                    Toast.makeText(this, "날짜를 선택하세요", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Validation failed: Date is empty");
                    createButton.setEnabled(true);
                    return;
                }

                if (time.isEmpty()) {
                    timeInput.setError("시간을 선택하세요");
                    Toast.makeText(this, "시간을 선택하세요", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Validation failed: Time is empty");
                    createButton.setEnabled(true);
                    return;
                }

                if (location.isEmpty()) {
                    locationInput.setError("장소를 입력하세요");
                    Toast.makeText(this, "장소를 입력하세요", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Validation failed: Location is empty");
                    createButton.setEnabled(true);
                    return;
                }

                // Parse and validate participant limit
                int maxParticipants = 0;
                if (participantsStr.isEmpty()) {
                    participantLimitInput.setError("참가 인원을 입력하세요");
                    Toast.makeText(this, "참가 인원을 입력하세요", Toast.LENGTH_LONG).show();
                    createButton.setEnabled(true);
                    return;
                }

                try {
                    maxParticipants = Integer.parseInt(participantsStr);
                    if (maxParticipants <= 0) {
                        participantLimitInput.setError("참가 인원은 1명 이상이어야 합니다");
                        Toast.makeText(this, "참가 인원은 1명 이상이어야 합니다", Toast.LENGTH_LONG).show();
                        createButton.setEnabled(true);
                        return;
                    }
                } catch (NumberFormatException e) {
                    participantLimitInput.setError("올바른 숫자를 입력하세요");
                    Toast.makeText(this, "올바른 숫자를 입력하세요", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Invalid participant number: " + participantsStr, e);
                    createButton.setEnabled(true);
                    return;
                }

                // Get visibility
                int checkedId = visibilityToggle.getCheckedButtonId();
                String visibility = (checkedId == R.id.visibility_public) ? "공개" : "비공개";

                // Get current user info
                String creatorId = "";
                String creatorName = "Anonymous";

                // Try Firebase Auth
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseUser != null) {
                    creatorId = firebaseUser.getUid();
                    creatorName = (firebaseUser.getDisplayName() != null) ?
                        firebaseUser.getDisplayName() : firebaseUser.getEmail();
                    Log.d(TAG, "Firebase user: " + creatorName);
                } else {
                    // Try SharedPreferences for social login
                    SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
                    creatorId = prefs.getString("user_id", "");
                    creatorName = prefs.getString("user_name", "Anonymous");
                    Log.d(TAG, "Social login user: " + creatorName);
                }

                Activity activity;
                FirebaseActivityManager activityManager = FirebaseActivityManager.getInstance();

                if (isEditMode && editingActivity != null) {
                    // UPDATE MODE - preserve existing activity data
                    Log.d(TAG, "Updating activity with ID: " + editingActivity.getId());

                    // Update existing activity with new values
                    activity = editingActivity;
                    activity.setTitle(title);
                    activity.setDescription(description);
                    activity.setCategory(category);
                    activity.setDate(date);
                    activity.setTime(time);
                    activity.setLocation(location);
                    activity.setMaxParticipants(maxParticipants);
                    activity.setVisibility(visibility);
                    activity.setHashtags(hashtags);

                    // Update location coordinates if they were provided from map search
                    if (hasLinkedLocationCoordinates) {
                        activity.setLatitude(locationLatitude);
                        activity.setLongitude(locationLongitude);
                        Log.d(TAG, "Activity location updated to: " + locationLatitude + ", " + locationLongitude);
                    } else {
                        activity.setLatitude(0.0);
                        activity.setLongitude(0.0);
                        Log.d(TAG, "Activity location coordinates cleared due to manual location edits.");
                    }

                    // Show progress feedback
                    Toast.makeText(this, "활동 수정 중...", Toast.LENGTH_SHORT).show();

                    // Update activity using FirebaseActivityManager
                    Log.d(TAG, "Calling Firebase to update activity");
                    activityManager.updateActivity(activity, new FirebaseActivityManager.OnCompleteListener<Activity>() {
                        @Override
                        public void onSuccess(Activity result) {
                            runOnUiThread(() -> {
                                Toast.makeText(CreateActivityActivity.this, "활동이 수정되었습니다!", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Activity updated successfully: " + result.getTitle());
                                // Close activity and return to previous screen
                                finish();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                createButton.setEnabled(true);
                                Toast.makeText(CreateActivityActivity.this, "활동 수정에 실패했습니다: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Failed to update activity", e);
                            });
                        }
                    });
                } else {
                    // CREATE MODE - create new activity
                    Log.d(TAG, "Creating activity with title: " + title);

                    // Create Activity object
                    activity = new Activity(
                        title,
                        description,
                        category,
                        date,
                        time,
                        location,
                        maxParticipants,
                        visibility,
                        hashtags,
                        creatorId,
                        creatorName
                    );

                    // Set location coordinates if they were provided from map search
                    if (hasLinkedLocationCoordinates) {
                        activity.setLatitude(locationLatitude);
                        activity.setLongitude(locationLongitude);
                        Log.d(TAG, "Activity location set to: " + locationLatitude + ", " + locationLongitude);
                    } else {
                        Log.d(TAG, "No map coordinates linked to location text; using default activity coordinates.");
                    }

                    // Show progress feedback
                    Toast.makeText(this, "활동 생성 중...", Toast.LENGTH_SHORT).show();

                    // Save activity using FirebaseActivityManager
                    Log.d(TAG, "Calling Firebase to save activity");
                    activityManager.saveActivity(activity, new FirebaseActivityManager.OnCompleteListener<Activity>() {
                        @Override
                        public void onSuccess(Activity result) {
                            runOnUiThread(() -> {
                                Toast.makeText(CreateActivityActivity.this, "활동이 생성되었습니다!", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Activity created successfully: " + result.getTitle());
                                // Close activity and return to previous screen
                                finish();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                createButton.setEnabled(true);
                                Toast.makeText(CreateActivityActivity.this, "활동 생성에 실패했습니다: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e(TAG, "Failed to save activity", e);
                            });
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Exception in create button click", e);
                Toast.makeText(this, "오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_LONG).show();
                createButton.setEnabled(true);
            }
        });
    }

    /**
     * Search for places using Kakao Local API
     */
    private void performLocationSearch(String query) {
        if (query.isEmpty()) {
            hideLocationSearchResults();
            return;
        }

        Log.d(TAG, "Searching for location: " + query);

        // Build API URL with current location for proximity-based search
        String apiUrl = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" +
            android.net.Uri.encode(query);

        // Add current location for proximity search (Kakao API will return sorted by distance)
        if (currentLocation != null) {
            double longitude = currentLocation.getLongitude();
            double latitude = currentLocation.getLatitude();
            apiUrl += "&x=" + longitude + "&y=" + latitude + "&radius=20000"; // 20km radius
            Log.d(TAG, "Using current location for search: lat=" + latitude + ", lng=" + longitude);
        } else {
            // Fallback to Seoul center if no location available
            apiUrl += "&x=126.9780&y=37.5665&radius=20000";
            Log.d(TAG, "Using default Seoul location for search");
        }

        Request request = new Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "KakaoAK " + BuildConfig.KAKAO_REST_API_KEY)
            .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Location search request failed", e);
                runOnUiThread(() -> Toast.makeText(CreateActivityActivity.this,
                    "검색 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Location search response not successful: " + response.code());
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
                            hideLocationSearchResults();
                            Toast.makeText(CreateActivityActivity.this, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show();
                        } else {
                            displayLocationSearchResults(results);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing location search results", e);
                }
            }
        });
    }

    /**
     * Display location search results (sorted by distance)
     */
    private void displayLocationSearchResults(List<PlaceSearchResult> results) {
        locationSearchResults.clear();

        // Sort results by distance (nearest first)
        // Results from Kakao API already include distance when current location is provided
        results.sort((a, b) -> {
            // If both have distance values, sort by distance
            if (a.getDistance() > 0 && b.getDistance() > 0) {
                return Integer.compare(a.getDistance(), b.getDistance());
            }
            // If only one has distance, prioritize it
            if (a.getDistance() > 0) return -1;
            if (b.getDistance() > 0) return 1;
            // If neither has distance, maintain original order
            return 0;
        });

        locationSearchResults.addAll(results);
        if (locationSearchAdapter != null) {
            locationSearchAdapter.updateResults(locationSearchResults);
        }
        if (locationSearchResultsCard != null) {
            locationSearchResultsCard.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "Displaying " + results.size() + " location search results (sorted by distance)");
    }

    /**
     * Hide location search results
     */
    private void hideLocationSearchResults() {
        if (locationSearchResultsCard != null) {
            locationSearchResultsCard.setVisibility(View.GONE);
        }
        locationSearchResults.clear();
        if (locationSearchAdapter != null) {
            locationSearchAdapter.updateResults(locationSearchResults);
        }
    }

    /**
     * Get current user location for proximity-based search
     */
    private void getCurrentUserLocation() {
        // Check for location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted, using default location");
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLocation = location;
                        Log.d(TAG, "Current location obtained: lat=" + location.getLatitude() + ", lng=" + location.getLongitude());
                    } else {
                        Log.w(TAG, "Current location is null, using default location");
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e(TAG, "Failed to get current location", e);
                });
        } catch (Exception e) {
            Log.e(TAG, "Exception while getting location", e);
        }
    }

    /**
     * Delete the current activity from Firebase
     */
    private void deleteActivity() {
        if (editingActivity == null || editingActivity.getId() == null) {
            Toast.makeText(this, "삭제할 활동을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable delete button to prevent double-click
        deleteButton.setEnabled(false);

        Toast.makeText(this, "활동 삭제 중...", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Deleting activity: " + editingActivity.getId());

        FirebaseActivityManager activityManager = FirebaseActivityManager.getInstance();
        activityManager.deleteActivity(editingActivity.getId(), new FirebaseActivityManager.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    Toast.makeText(CreateActivityActivity.this, "활동이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Activity deleted successfully");
                    // Close activity and return to previous screen
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    deleteButton.setEnabled(true);
                    Toast.makeText(CreateActivityActivity.this, "활동 삭제에 실패했습니다: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to delete activity", e);
                });
            }
        });
    }

    /**
     * Hide keyboard
     */
    private void hideKeyboard() {
        if (locationInput != null) {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(locationInput.getWindowToken(), 0);
            }
        }
    }
}
