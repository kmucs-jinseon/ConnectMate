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

import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private FirebaseAuth mAuth;

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

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

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
        // Get user info from Firebase Auth and SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);

        // Load user name
        String userName = prefs.getString("user_name", "이름");
        if (tvName != null) {
            tvName.setText(userName);
        }

        // Load username (use saved username or create from name)
        String username = prefs.getString("user_username", "");
        if (username.isEmpty()) {
            username = userName.toLowerCase().replace(" ", "");
        }
        if (tvUsername != null) {
            tvUsername.setText("@" + username);
        }

        // Load email from Firebase Auth or SharedPreferences
        String userEmail = "";
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
            userEmail = mAuth.getCurrentUser().getEmail();
        } else {
            userEmail = prefs.getString("user_email", "user@example.com");
        }
        if (tvEmail != null) {
            tvEmail.setText(userEmail);
        }

        // Load profile image if exists (same as ProfileFragment)
        String imageUriString = prefs.getString("profile_image_uri", "");
        if (!imageUriString.isEmpty() && ivAvatar != null) {
            try {
                android.net.Uri imageUri = android.net.Uri.parse(imageUriString);
                ivAvatar.setImageURI(imageUri);
            } catch (Exception e) {
                // If loading fails, keep the default image
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
}
