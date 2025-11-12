package com.example.connectmate;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.Activity;
import com.example.connectmate.models.PlaceSearchResult;
import com.example.connectmate.utils.FirebaseActivityManager;
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
    private Button createButton;
    private String selectedCategory = "";

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

    // Edit mode variables
    private boolean isEditMode = false;
    private Activity editingActivity = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_create_activity); // 👉 XML 파일 이름에 맞게 수정하세요

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

        // Set category (will be selected after chips are created)
        selectedCategory = editingActivity.getCategory();

        // Update button text for edit mode
        if (createButton != null) {
            createButton.setText("Update Activity");
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
            // Create chip with FilterChipStyle
            Chip chip = new Chip(this);
            chip.setChipBackgroundColorResource(R.color.filter_chip_background);
            chip.setTextColor(getResources().getColorStateList(R.color.filter_chip_text, null));
            chip.setCheckedIconVisible(false);
            chip.setChipStrokeWidth(0f);
            chip.setChipCornerRadius(getResources().getDimension(R.dimen.chip_corner_radius));
            chip.setText(category);
            chip.setCheckable(true);
            chip.setClickable(true);
            Log.d(TAG, "Created chip: " + category);

            categoryChipGroup.addView(chip);
        }

        Log.d(TAG, "Finished adding chips. ChipGroup child count: " + categoryChipGroup.getChildCount());

        // Handle chip selection
        categoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int chipId = checkedIds.get(0);
                Chip selectedChip = findViewById(chipId);
                if (selectedChip != null) {
                    selectedCategory = selectedChip.getText().toString();
                    Log.d(TAG, "Category selected: " + selectedCategory);
                }
            } else {
                selectedCategory = "";
            }
        });

        // If in edit mode, select the appropriate category chip
        if (isEditMode && selectedCategory != null && !selectedCategory.isEmpty()) {
            for (int i = 0; i < categoryChipGroup.getChildCount(); i++) {
                View view = categoryChipGroup.getChildAt(i);
                if (view instanceof Chip) {
                    Chip chip = (Chip) view;
                    if (chip.getText().toString().equals(selectedCategory)) {
                        chip.setChecked(true);
                        break;
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

        createButton.setOnClickListener(v -> {
            Log.d(TAG, "Create button clicked");

            // Disable button to prevent double-click
            createButton.setEnabled(false);

            try {
                String title = (titleInput.getText() != null) ? titleInput.getText().toString().trim() : "";
                String description = (descriptionInput.getText() != null) ? descriptionInput.getText().toString().trim() : "";
                String category = selectedCategory;
                String date = (dateInput.getText() != null) ? dateInput.getText().toString().trim() : "";
                String time = (timeInput.getText() != null) ? timeInput.getText().toString().trim() : "";
                String location = (locationInput.getText() != null) ? locationInput.getText().toString().trim() : "";
                String participantsStr = (participantLimitInput.getText() != null) ? participantLimitInput.getText().toString().trim() : "";
                String hashtags = (hashtagsInput.getText() != null) ? hashtagsInput.getText().toString().trim() : "";

                Log.d(TAG, "Validating fields - Title: " + title + ", Category: " + category);

                // Validate required fields
                if (title.isEmpty()) {
                    Toast.makeText(this, "제목을 입력하세요.", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Validation failed: Title is empty");
                    createButton.setEnabled(true);
                    return;
                }

                if (category == null || category.isEmpty()) {
                    Toast.makeText(this, "카테고리를 선택하세요.", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Validation failed: Category is empty");
                    createButton.setEnabled(true);
                    return;
                }

                // Parse participant limit
                int maxParticipants = 0;
                if (!participantsStr.isEmpty()) {
                    try {
                        maxParticipants = Integer.parseInt(participantsStr);
                        if (maxParticipants < 0) {
                            Toast.makeText(this, "참가 인원은 0보다 커야 합니다.", Toast.LENGTH_LONG).show();
                            createButton.setEnabled(true);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "올바른 인원 수를 입력하세요.", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Invalid participant number: " + participantsStr, e);
                        createButton.setEnabled(true);
                        return;
                    }
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

        // Use Seoul as default location for search
        String apiUrl = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" +
            android.net.Uri.encode(query) + "&x=126.9780&y=37.5665&radius=20000";

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
     * Display location search results
     */
    private void displayLocationSearchResults(List<PlaceSearchResult> results) {
        locationSearchResults.clear();
        locationSearchResults.addAll(results);
        if (locationSearchAdapter != null) {
            locationSearchAdapter.updateResults(locationSearchResults);
        }
        if (locationSearchResultsCard != null) {
            locationSearchResultsCard.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "Displaying " + results.size() + " location search results");
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
