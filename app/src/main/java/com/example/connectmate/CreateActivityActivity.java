package com.example.connectmate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.connectmate.models.Activity;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.utils.ActivityManager;
import com.example.connectmate.utils.ChatManager;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

public class CreateActivityActivity extends AppCompatActivity {

    private static final String TAG = "CreateActivityActivity";

    private ImageButton backButton, closeButton;
    private TextInputEditText titleInput, descriptionInput, dateInput, timeInput,
            locationInput, participantLimitInput, hashtagsInput;
    private ChipGroup categoryChipGroup;
    private MaterialButtonToggleGroup visibilityToggle;
    private Button createButton;
    private String selectedCategory = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_create_activity);

        initViews();
        setupCategoryDropdown();
        setupDateTimePickers();
        setupButtons();
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
    }

    private void setupCategoryDropdown() {
        String[] categories = getResources().getStringArray(R.array.category_options);
        Log.d(TAG, "Setting up categories. Count: " + categories.length);

        if (categoryChipGroup == null) {
            Log.e(TAG, "categoryChipGroup is null!");
            return;
        }

        for (String category : categories) {
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
            String title = (titleInput.getText() != null) ? titleInput.getText().toString().trim() : "";
            String description = (descriptionInput.getText() != null) ? descriptionInput.getText().toString().trim() : "";
            String category = selectedCategory;
            String date = (dateInput.getText() != null) ? dateInput.getText().toString().trim() : "";
            String time = (timeInput.getText() != null) ? timeInput.getText().toString().trim() : "";
            String location = (locationInput.getText() != null) ? locationInput.getText().toString().trim() : "";
            String participantsStr = (participantLimitInput.getText() != null) ? participantLimitInput.getText().toString().trim() : "";
            String hashtags = (hashtagsInput.getText() != null) ? hashtagsInput.getText().toString().trim() : "";

            // Validate required fields
            if (title.isEmpty()) {
                Toast.makeText(this, "제목을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (category.isEmpty()) {
                Toast.makeText(this, "카테고리를 선택하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            int maxParticipants = 0;
            if (!participantsStr.isEmpty()) {
                try {
                    maxParticipants = Integer.parseInt(participantsStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "올바른 인원 수를 입력하세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            int checkedId = visibilityToggle.getCheckedButtonId();
            String visibility = (checkedId == R.id.visibility_public) ? "공개" : "비공개";

            // Get current user info
            String creatorId = "";
            String creatorName = "Anonymous";

            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                creatorId = firebaseUser.getUid();
                creatorName = (firebaseUser.getDisplayName() != null) ?
                    firebaseUser.getDisplayName() : firebaseUser.getEmail();
            } else {
                SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
                creatorId = prefs.getString("user_id", "");
                creatorName = prefs.getString("user_name", "Anonymous");
            }

            Activity activity = new Activity(
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

            ActivityManager activityManager = ActivityManager.getInstance(this);
            boolean saved = activityManager.saveActivity(activity);

            if (saved) {
                // Now, create the chat room and add the creator automatically
                ChatManager chatManager = ChatManager.getInstance(this);
                chatManager.createOrGetChatRoom(
                    activity.getId(),
                    activity.getTitle(),
                    creatorId,
                    creatorName
                );

                Toast.makeText(this, "활동이 생성되었습니다!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Activity created and chat room initialized: " + activity.getTitle());

                finish();
            } else {
                Toast.makeText(this, "활동 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to save activity");
            }
        });
    }
}
