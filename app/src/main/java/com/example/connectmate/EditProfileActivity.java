package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    // UI elements
    private Toolbar toolbar;
    private CircleImageView profileImage;
    private MaterialButton changePhotoButton;
    private TextInputEditText nameInput;
    private TextInputEditText usernameInput;
    private TextInputEditText mbtiInput;
    private TextInputEditText bioInput;
    private MaterialButton saveButton;

    // Data
    private Uri selectedImageUri;
    private SharedPreferences prefs;

    // Image picker launcher
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);

        // Initialize views
        initializeViews();

        // Setup toolbar
        setupToolbar();

        // Setup image picker
        setupImagePicker();

        // Load current data
        loadCurrentData();

        // Setup click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        profileImage = findViewById(R.id.profile_image);
        changePhotoButton = findViewById(R.id.change_photo_button);
        nameInput = findViewById(R.id.name_input);
        usernameInput = findViewById(R.id.username_input);
        mbtiInput = findViewById(R.id.mbti_input);
        bioInput = findViewById(R.id.bio_input);
        saveButton = findViewById(R.id.save_button);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupImagePicker() {
        // Register the image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    profileImage.setImageURI(uri);

                    // Save the image URI to preferences
                    prefs.edit().putString("profile_image_uri", uri.toString()).apply();
                }
            }
        );
    }

    private void loadCurrentData() {
        // Load current user data from SharedPreferences
        String currentName = prefs.getString("user_name", "");
        String currentUsername = prefs.getString("user_username", "");
        String currentMbti = prefs.getString("user_mbti", "");
        String currentBio = prefs.getString("user_bio", "");
        String imageUriString = prefs.getString("profile_image_uri", "");

        // Set current values to inputs
        if (nameInput != null) nameInput.setText(currentName);
        if (usernameInput != null) usernameInput.setText(currentUsername);
        if (mbtiInput != null) mbtiInput.setText(currentMbti);
        if (bioInput != null) bioInput.setText(currentBio);

        // Load profile image if exists
        if (!imageUriString.isEmpty()) {
            try {
                Uri imageUri = Uri.parse(imageUriString);
                profileImage.setImageURI(imageUri);
                selectedImageUri = imageUri;
            } catch (Exception e) {
                // If loading fails, keep the default image
                profileImage.setImageResource(R.drawable.circle_logo);
            }
        }
    }

    private void setupClickListeners() {
        // Change photo button
        changePhotoButton.setOnClickListener(v -> {
            // Launch image picker
            imagePickerLauncher.launch("image/*");
        });

        // Save button
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        // Get input values
        String name = (nameInput.getText() != null) ? nameInput.getText().toString().trim() : "";
        String username = (usernameInput.getText() != null) ? usernameInput.getText().toString().trim() : "";
        String mbti = (mbtiInput.getText() != null) ? mbtiInput.getText().toString().trim().toUpperCase() : "";
        String bio = (bioInput.getText() != null) ? bioInput.getText().toString().trim() : "";

        // Validation
        if (name.isEmpty()) {
            Toast.makeText(this, "이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            nameInput.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            Toast.makeText(this, "사용자 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            usernameInput.requestFocus();
            return;
        }

        // Validate MBTI format (4 characters)
        if (!mbti.isEmpty() && mbti.length() != 4) {
            Toast.makeText(this, "MBTI는 4자리여야 합니다 (예: ENFP)", Toast.LENGTH_SHORT).show();
            mbtiInput.requestFocus();
            return;
        }

        // Save to SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_name", name);
        editor.putString("user_username", username);
        editor.putString("user_mbti", mbti.isEmpty() ? "ENFP" : mbti);
        editor.putString("user_bio", bio.isEmpty() ? "안녕하세요! 새로운 사람들과 함께 활동하는 것을 좋아합니다 ✨" : bio);

        if (selectedImageUri != null) {
            editor.putString("profile_image_uri", selectedImageUri.toString());
        }

        editor.apply();

        Toast.makeText(this, "프로필이 저장되었습니다", Toast.LENGTH_SHORT).show();

        // Return to previous screen
        finish();
    }
}
