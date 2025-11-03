package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;

    // UI elements
    private ImageButton moreButton;
    private ImageView profileAvatar;
    private TextView profileName;
    private TextView profileUsername;
    private TextView profileBio;
    private TextView postsCount;
    private TextView followersCount;
    private TextView followingCount;
    private Button editProfileButton;
    private Button logoutButton;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        initializeViews(view);

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews(View view) {
        // Header
        moreButton = view.findViewById(R.id.more_button);

        // Profile info
        profileAvatar = view.findViewById(R.id.profile_avatar);
        profileName = view.findViewById(R.id.profile_name);
        profileUsername = view.findViewById(R.id.profile_username);
        profileBio = view.findViewById(R.id.profile_bio);

        // Stats
        postsCount = view.findViewById(R.id.posts_count);
        followersCount = view.findViewById(R.id.followers_count);
        followingCount = view.findViewById(R.id.following_count);

        // Buttons
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        logoutButton = view.findViewById(R.id.logout_button);
    }

    private void setupClickListeners() {
        // More button
        if (moreButton != null) {
            moreButton.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "더보기", Toast.LENGTH_SHORT).show();
                // TODO: Implement more options menu
            });
        }

        // Edit profile button
        if (editProfileButton != null) {
            editProfileButton.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "프로필 편집", Toast.LENGTH_SHORT).show();
                // TODO: Implement edit profile functionality
            });
        }

        // Logout button
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
        }
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> performLogout())
                .setNegativeButton("취소", null)
                .show();
    }

    private void performLogout() {
        // Sign out from Firebase
        mAuth.signOut();

        // Clear login state from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_logged_in", false);
        editor.remove("login_method");
        editor.apply();

        // Also sign out from Google if needed
        // GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();

        Toast.makeText(requireContext(), "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();

        // Navigate to LoginActivity
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
