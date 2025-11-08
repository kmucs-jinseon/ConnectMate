package com.example.connectmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SettingsFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;

    // UI elements
    private ImageView ivAvatar;
    private TextView tvName;
    private TextView tvUsername;
    private TextView tvEmail;

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth and Realtime Database
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        initializeViews(view);

        // Load user data
        loadUserData();

        // Find the Account item
        LinearLayout itemAccount = view.findViewById(R.id.itemAccount);

        // Set click listener to navigate to ProfileFragment
        if (itemAccount != null) {
            itemAccount.setOnClickListener(v -> {
                // Create ProfileFragment instance
                ProfileFragment profileFragment = new ProfileFragment();

                // Navigate to ProfileFragment
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.main_container, profileFragment);
                transaction.addToBackStack(null);  // Add to back stack so user can go back
                transaction.commit();
            });
        }
    }

    private void initializeViews(View view) {
        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvName = view.findViewById(R.id.tvName);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
    }

    private void loadUserData() {
        // Get user ID based on login method
        String userId = getUserId();

        if (userId == null) {
            // Fallback to SharedPreferences if no user ID found
            // But also log this for debugging
            android.util.Log.d("SettingsFragment", "No user ID found, loading from SharedPreferences only");
            loadUserDataFromSharedPreferences();
            return;
        }

        android.util.Log.d("SettingsFragment", "Loading user data for userId: " + userId);

        // Load user data from Realtime Database
        dbRef.child("users").child(userId)
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        android.util.Log.d("SettingsFragment", "User data found in database");
                        // Load data from Realtime Database
                        User user = dataSnapshot.getValue(User.class);
                        if (user == null) {
                            loadUserDataFromSharedPreferences();
                            return;
                        }

                        // Update UI
                        if (tvName != null && user.getDisplayName() != null) {
                            tvName.setText(user.getDisplayName());
                        }

                        if (tvUsername != null && user.getUsername() != null) {
                            tvUsername.setText("@" + user.getUsername());
                        }

                        if (tvEmail != null && user.getEmail() != null) {
                            tvEmail.setText(user.getEmail());
                        }

                        // Sync data to SharedPreferences for offline access
                        syncToSharedPreferences(user);

                        // Load profile image from URL if available
                        String profileImageUrl = user.getProfileImageUrl();
                        if (profileImageUrl != null && !profileImageUrl.isEmpty() && ivAvatar != null) {
                            // Use Glide to load remote or local images
                            Glide.with(requireContext())
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.circle_logo)
                                    .error(R.drawable.circle_logo)
                                    .circleCrop()
                                    .into(ivAvatar);
                        } else {
                            // Check for local profile image
                            loadLocalProfileImage();
                        }
                    } else {
                        // User not found in database, fallback to SharedPreferences
                        loadUserDataFromSharedPreferences();
                    }
                })
                .addOnFailureListener(e -> {
                    // Error loading from database, fallback to SharedPreferences
                    loadUserDataFromSharedPreferences();
                });
    }

    /**
     * Sync user data from database to SharedPreferences
     */
    private void syncToSharedPreferences(User user) {
        if (user == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (user.getUserId() != null) editor.putString("user_id", user.getUserId());
        if (user.getDisplayName() != null) editor.putString("user_name", user.getDisplayName());
        if (user.getEmail() != null) editor.putString("user_email", user.getEmail());
        if (user.getUsername() != null) editor.putString("user_username", user.getUsername());
        if (user.getBio() != null) editor.putString("user_bio", user.getBio());
        if (user.getMbti() != null) editor.putString("user_mbti", user.getMbti());
        if (user.getProfileImageUrl() != null) editor.putString("profile_image_url", user.getProfileImageUrl());

        editor.apply();
        android.util.Log.d("SettingsFragment", "User data synced to SharedPreferences - Name: " + user.getDisplayName() + ", Email: " + user.getEmail());
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
        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        if (!userId.isEmpty()) {
            return userId;
        }

        return null;
    }

    /**
     * Fallback method to load user data from SharedPreferences
     */
    private void loadUserDataFromSharedPreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);

        // Load user name
        String userName = prefs.getString("user_name", "");
        if (userName.isEmpty()) {
            // Fallback to Firebase Auth display name
            if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getDisplayName() != null) {
                userName = mAuth.getCurrentUser().getDisplayName();
            } else {
                userName = "사용자";  // Default name
            }
        }
        if (tvName != null) {
            tvName.setText(userName);
        }
        android.util.Log.d("SettingsFragment", "Loaded user name: " + userName);

        // Load username (use saved username or create from name)
        String username = prefs.getString("user_username", "");
        if (username.isEmpty()) {
            username = userName.toLowerCase().replace(" ", "").replaceAll("[^a-z0-9]", "");
            if (username.isEmpty()) username = "user";
        }
        if (tvUsername != null) {
            tvUsername.setText("@" + username);
        }
        android.util.Log.d("SettingsFragment", "Loaded username: @" + username);

        // Load email - Priority: Firebase Auth > SharedPreferences > Default
        String userEmail = "";
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
            userEmail = mAuth.getCurrentUser().getEmail();
            android.util.Log.d("SettingsFragment", "Email from Firebase Auth: " + userEmail);
        } else {
            userEmail = prefs.getString("user_email", "");
            android.util.Log.d("SettingsFragment", "Email from SharedPreferences: " + userEmail);
        }

        // If still empty, use placeholder
        if (userEmail.isEmpty()) {
            userEmail = "이메일 정보 없음";
        }

        if (tvEmail != null) {
            tvEmail.setText(userEmail);
        }
        android.util.Log.d("SettingsFragment", "Final displayed email: " + userEmail);

        // Load profile image
        loadLocalProfileImage();
    }

    /**
     * Load profile image from local storage
     */
    private void loadLocalProfileImage() {
        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        String imageUriString = prefs.getString("profile_image_uri", "");
        String profileImageUrl = prefs.getString("profile_image_url", "");

        if (ivAvatar != null) {
            // Try to load from URL first (for social logins), then local URI
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                Glide.with(requireContext())
                        .load(profileImageUrl)
                        .placeholder(R.drawable.circle_logo)
                        .error(R.drawable.circle_logo)
                        .circleCrop()
                        .into(ivAvatar);
            } else if (!imageUriString.isEmpty()) {
                Glide.with(requireContext())
                        .load(android.net.Uri.parse(imageUriString))
                        .placeholder(R.drawable.circle_logo)
                        .error(R.drawable.circle_logo)
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.circle_logo);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload user data when returning (e.g., after profile edit)
        loadUserData();
    }

    /**
     * Public method to refresh user data
     * Can be called by MainActivity to force refresh
     */
    public void refreshUserData() {
        loadUserData();
    }
}
