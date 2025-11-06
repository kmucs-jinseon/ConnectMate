package com.example.connectmate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

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

    // Image picker launcher using Intent to allow both gallery and files app
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // Permission launcher for storage access
    private ActivityResultLauncher<String> permissionLauncher;

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
        // Register permission launcher
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // Permission granted, launch image picker
                    launchImagePicker();
                } else {
                    // Permission denied, show message
                    Toast.makeText(this, "사진을 선택하려면 저장소 접근 권한이 필요합니다", Toast.LENGTH_LONG).show();
                }
            }
        );

        // Register the image picker launcher with support for both gallery and file manager
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        selectedImageUri = uri;
                        profileImage.setImageURI(uri);

                        // Take persistable URI permission to access the image later
                        try {
                            getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                        } catch (SecurityException e) {
                            // Some apps don't grant persistable permissions, but we can still use the URI
                        }

                        // Save the image URI to preferences
                        prefs.edit().putString("profile_image_uri", uri.toString()).apply();
                    }
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
            // Check and request permission if needed
            if (checkStoragePermission()) {
                launchImagePicker();
            } else {
                requestStoragePermission();
            }
        });

        // Save button
        saveButton.setOnClickListener(v -> saveProfile());
    }

    /**
     * Check if storage permission is granted
     */
    private boolean checkStoragePermission() {
        // For Android 13+ (API 33+), check READ_MEDIA_IMAGES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                == PackageManager.PERMISSION_GRANTED;
        } else {
            // For Android 12 and below, check READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Request storage permission based on Android version
     */
    private void requestStoragePermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        permissionLauncher.launch(permission);
    }

    /**
     * Launch image picker with chooser for gallery and file manager
     */
    private void launchImagePicker() {
        // Create intent to pick image from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");

        // Create intent to pick image from file manager
        Intent fileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        fileIntent.setType("image/*");

        // Create chooser to let user select between gallery and file manager
        Intent chooserIntent = Intent.createChooser(galleryIntent, "사진 선택");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{fileIntent});

        // Launch the chooser
        imagePickerLauncher.launch(chooserIntent);
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
