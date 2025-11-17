package com.example.connectmate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

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

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

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

                        // Display the selected image using Glide
                        Glide.with(this)
                                .load(uri)
                                .placeholder(R.drawable.circle_logo)
                                .error(R.drawable.circle_logo)
                                .circleCrop()
                                .into(profileImage);

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
        String profileImageUrl = prefs.getString("profile_image_url", "");

        // Set current values to inputs
        if (nameInput != null) nameInput.setText(currentName);
        if (usernameInput != null) usernameInput.setText(currentUsername);
        if (mbtiInput != null) mbtiInput.setText(currentMbti);
        if (bioInput != null) bioInput.setText(currentBio);

        // Load profile image if exists - try URL first, then local URI
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.circle_logo)
                    .error(R.drawable.circle_logo)
                    .circleCrop()
                    .into(profileImage);
        } else if (!imageUriString.isEmpty()) {
            try {
                Uri imageUri = Uri.parse(imageUriString);
                Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.circle_logo)
                        .error(R.drawable.circle_logo)
                        .circleCrop()
                        .into(profileImage);
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
            // Android 13+ Photo Picker doesn't require permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                launchImagePicker();
            } else {
                // Check and request permission for older Android versions
                if (checkStoragePermission()) {
                    launchImagePicker();
                } else {
                    requestStoragePermission();
                }
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
     * On Android 13+, uses the modern Photo Picker with built-in search
     */
    private void launchImagePicker() {
        Intent pickIntent;

        // Android 13+ (API 33+): Use modern Photo Picker with built-in search
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Use ACTION_PICK_IMAGES for Android 13+
            // This provides a modern UI with search, filtering, and cloud photo support
            pickIntent = new Intent(MediaStore.ACTION_PICK_IMAGES);
            pickIntent.setType("image/*");

            // Launch directly without chooser - Photo Picker handles everything
            imagePickerLauncher.launch(pickIntent);
        } else {
            // Android 12 and below: Use traditional picker with chooser
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

        // Set default values
        String finalMbti = mbti.isEmpty() ? "ENFP" : mbti;
        String finalBio = bio.isEmpty() ? "안녕하세요! 새로운 사람들과 함께 활동하는 것을 좋아합니다 ✨" : bio;

        // Save to SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_name", name);
        editor.putString("user_username", username);
        editor.putString("user_mbti", finalMbti);
        editor.putString("user_bio", finalBio);

        if (selectedImageUri != null) {
            editor.putString("profile_image_uri", selectedImageUri.toString());
        }

        editor.apply();

        // Sync to Realtime Database
        syncToRealtimeDatabase(name, username, finalMbti, finalBio);
    }

    /**
     * Sync profile data to Realtime Database
     * Uses Base64 encoding for images (works on free tier without Firebase Storage)
     */
    private void syncToRealtimeDatabase(String name, String username, String mbti, String bio) {
        String userId = getUserId();

        if (userId == null) {
            // No user ID found, only save locally
            Toast.makeText(this, "프로필이 저장되었습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // If a new image was selected, convert it to Base64 first
        if (selectedImageUri != null) {
            convertImageToBase64AndSave(userId, name, username, mbti, bio);
        } else {
            // No new image, just update the profile data
            saveProfileToFirebase(userId, name, username, mbti, bio, null);
        }
    }

    /**
     * Convert image to Base64 string and save to Realtime Database
     * This works on free tier without Firebase Storage
     */
    private void convertImageToBase64AndSave(String userId, String name, String username, String mbti, String bio) {
        if (selectedImageUri == null) {
            android.util.Log.e("EditProfileActivity", "selectedImageUri is null!");
            Toast.makeText(this, "이미지를 선택해주세요", Toast.LENGTH_SHORT).show();
            saveProfileToFirebase(userId, name, username, mbti, bio, null);
            return;
        }

        android.util.Log.d("EditProfileActivity", "Converting image to Base64...");
        Toast.makeText(this, "이미지 처리 중...", Toast.LENGTH_SHORT).show();

        try {
            // Read image from URI
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap == null) {
                throw new Exception("Failed to decode image");
            }

            // Resize image to reduce size (max 400x400 pixels)
            int maxSize = 400;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scale = Math.min(((float) maxSize / width), ((float) maxSize / height));

            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            android.util.Log.d("EditProfileActivity", "Resized image from " + width + "x" + height + " to " + newWidth + "x" + newHeight);

            // Convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos); // 80% quality
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            android.util.Log.d("EditProfileActivity", "Base64 string size: " + base64Image.length() + " characters");

            // Clean up
            bitmap.recycle();
            resizedBitmap.recycle();
            inputStream.close();

            // Save to Firebase with Base64 string
            saveProfileToFirebase(userId, name, username, mbti, bio, base64Image);

        } catch (Exception e) {
            android.util.Log.e("EditProfileActivity", "Failed to convert image to Base64", e);
            Toast.makeText(this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Continue saving profile without image
            saveProfileToFirebase(userId, name, username, mbti, bio, null);
        }
    }

    /**
     * Save profile data to Firebase Realtime Database
     * imageData can be either a Base64 string or null
     */
    private void saveProfileToFirebase(String userId, String name, String username, String mbti, String bio, String imageData) {
        // Prepare data to update
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", name);
        updates.put("username", username);
        updates.put("mbti", mbti);
        updates.put("bio", bio);

        // If imageData is provided (Base64 string), update it
        if (imageData != null && !imageData.isEmpty()) {
            // Store Base64 string with prefix to identify it
            String base64WithPrefix = "data:image/jpeg;base64," + imageData;
            updates.put("profileImageUrl", base64WithPrefix);

            // Also update SharedPreferences so chat picks it up immediately
            prefs.edit().putString("profile_image_url", base64WithPrefix).apply();
            android.util.Log.d("EditProfileActivity", "Saving Base64 image to Firebase (length: " + base64WithPrefix.length() + ")");
        }

        // Update Realtime Database
        dbRef.child("users").child(userId)
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("EditProfileActivity", "Profile updated successfully in Firebase");
                    Toast.makeText(this, "프로필이 저장되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("EditProfileActivity", "Error updating profile in Realtime Database", e);
                    Toast.makeText(this, "프로필 저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Get the user ID based on login method
     */
    private String getUserId() {
        // Check if user is logged in via Firebase Auth
        if (mAuth.getCurrentUser() != null) {
            return mAuth.getCurrentUser().getUid();
        }

        // Check SharedPreferences for social login user ID
        String userId = prefs.getString("user_id", "");
        if (!userId.isEmpty()) {
            return userId;
        }

        return null;
    }
}
