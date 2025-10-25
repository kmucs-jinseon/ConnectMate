package com.example.connectmate;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;

public class CreateActivityActivity extends AppCompatActivity {

    private TextInputEditText titleInput, descriptionInput, dateInput, timeInput, locationInput, participantLimitInput, hashtagsInput;
    private AutoCompleteTextView categoryInput;
    private MaterialButtonToggleGroup visibilityToggle;
    private Button createButton;
    private ImageButton backButton, closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_create_activity);

        initializeViews();
        setupListeners();
        setupCategoryDropdown();
    }

    private void initializeViews() {
        titleInput = findViewById(R.id.title_input);
        descriptionInput = findViewById(R.id.description_input);
        categoryInput = findViewById(R.id.category_input);
        dateInput = findViewById(R.id.date_input);
        timeInput = findViewById(R.id.time_input);
        locationInput = findViewById(R.id.location_input);
        participantLimitInput = findViewById(R.id.participant_limit_input);
        hashtagsInput = findViewById(R.id.hashtags_input);
        visibilityToggle = findViewById(R.id.visibility_toggle);
        createButton = findViewById(R.id.create_button);
        backButton = findViewById(R.id.back_button);
        closeButton = findViewById(R.id.close_button);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        closeButton.setOnClickListener(v -> finish());

        dateInput.setOnClickListener(v -> showDatePicker());
        timeInput.setOnClickListener(v -> showTimePicker());

        createButton.setOnClickListener(v -> createActivity());
    }

    private void setupCategoryDropdown() {
        String[] categories = {"Sports", "Study", "Food", "Travel", "Gaming", "Music", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        categoryInput.setAdapter(adapter);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    dateInput.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format("%02d:%02d", selectedHour, selectedMinute);
                    timeInput.setText(time);
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void createActivity() {
        String title = titleInput.getText() != null ? titleInput.getText().toString() : "";
        String description = descriptionInput.getText() != null ? descriptionInput.getText().toString() : "";
        String category = categoryInput.getText() != null ? categoryInput.getText().toString() : "";
        String date = dateInput.getText() != null ? dateInput.getText().toString() : "";
        String time = timeInput.getText() != null ? timeInput.getText().toString() : "";

        if (title.isEmpty() || description.isEmpty() || category.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Implement actual activity creation logic (e.g., save to database or send to backend)
        Toast.makeText(this, "Activity created successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
