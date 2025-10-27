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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;

public class CreateActivityActivity extends AppCompatActivity {

    private ImageButton backButton, closeButton;
    private TextInputEditText titleInput, descriptionInput, dateInput, timeInput,
            locationInput, participantLimitInput, hashtagsInput;
    private AutoCompleteTextView categoryInput;
    private MaterialButtonToggleGroup visibilityToggle;
    private Button createButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_create_activity); // 👉 XML 파일 이름에 맞게 수정하세요

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
        categoryInput = findViewById(R.id.category_input);
        dateInput = findViewById(R.id.date_input);
        timeInput = findViewById(R.id.time_input);
        locationInput = findViewById(R.id.location_input);
        participantLimitInput = findViewById(R.id.participant_limit_input);
        hashtagsInput = findViewById(R.id.hashtags_input);
        visibilityToggle = findViewById(R.id.visibility_toggle);
        createButton = findViewById(R.id.create_button);
    }

    private void setupCategoryDropdown() {
        // 카테고리 드롭다운 예시 데이터
        String[] categories = {"스터디", "운동", "취미", "봉사", "기타"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories);
        categoryInput.setAdapter(adapter);
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

        DatePickerDialog dialog = new DatePickerDialog(this,
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

        TimePickerDialog dialog = new TimePickerDialog(this,
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
            String title = (titleInput.getText() != null) ? titleInput.getText().toString() : "";
            String description = (descriptionInput.getText() != null) ? descriptionInput.getText().toString() : "";
            String category = (categoryInput.getText() != null) ? categoryInput.getText().toString() : "";
            String date = (dateInput.getText() != null) ? dateInput.getText().toString() : "";
            String time = (timeInput.getText() != null) ? timeInput.getText().toString() : "";
            String location = (locationInput.getText() != null) ? locationInput.getText().toString() : "";
            String participants = (participantLimitInput.getText() != null) ? participantLimitInput.getText().toString() : "";
            String hashtags = (hashtagsInput.getText() != null) ? hashtagsInput.getText().toString() : "";

            if (title.isEmpty()) {
                Toast.makeText(this, "제목을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            int checkedId = visibilityToggle.getCheckedButtonId();
            String visibility = (checkedId == R.id.visibility_public)
                    ? "공개" : "비공개";

            String summary = "제목: " + title + "\n"
                    + "설명: " + description + "\n"
                    + "카테고리: " + category + "\n"
                    + "날짜: " + date + " " + time + "\n"
                    + "장소: " + location + "\n"
                    + "인원 제한: " + participants + "\n"
                    + "공개 여부: " + visibility + "\n"
                    + "해시태그: " + hashtags;

            Toast.makeText(this, "활동 생성 완료!\n" + summary, Toast.LENGTH_LONG).show();

            // TODO: 실제 저장 로직 추가
        });
    }
}
