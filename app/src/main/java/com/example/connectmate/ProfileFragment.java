package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kakao.sdk.user.UserApiClient;
import com.navercorp.nid.NaverIdLoginSDK;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // UI elements - Profile Card
    private ImageButton moreButton;
    private CircleImageView profileAvatar;
    private TextView profileName;
    private TextView profileUsername;
    private TextView profileEmail;
    private Chip mbtiChip;
    private TextView profileBio;
    private TextView ratingText;
    private TextView activitiesCount;
    private TextView connectionsCount;
    private TextView badgesCount;
    private MaterialButton editProfileButton;

    // UI elements - Recent Activities
    private TextView monthlyActivitiesCount;
    private TextView chatParticipationCount;
    private TextView newFriendsCount;

    // UI elements - Settings
    private LinearLayout accountSettings;
    private LinearLayout notificationSettings;
    private LinearLayout privacySettings;
    private LinearLayout appInfo;

    // UI elements - Logout
    private MaterialButton logoutButton;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        initializeViews(view);

        // Set up click listeners
        setupClickListeners();

        // Load user data
        loadUserData();
    }

    private void initializeViews(View view) {
        // Header
        moreButton = view.findViewById(R.id.more_button);

        // Profile card
        profileAvatar = view.findViewById(R.id.profile_avatar);
        profileName = view.findViewById(R.id.profile_name);
        profileUsername = view.findViewById(R.id.profile_username);
        profileEmail = view.findViewById(R.id.profile_email);
        mbtiChip = view.findViewById(R.id.mbti_chip);
        profileBio = view.findViewById(R.id.profile_bio);
        ratingText = view.findViewById(R.id.rating_text);
        activitiesCount = view.findViewById(R.id.activities_count);
        connectionsCount = view.findViewById(R.id.connections_count);
        badgesCount = view.findViewById(R.id.badges_count);
        editProfileButton = view.findViewById(R.id.edit_profile_button);

        // Recent activities
        monthlyActivitiesCount = view.findViewById(R.id.monthly_activities_count);
        chatParticipationCount = view.findViewById(R.id.chat_participation_count);
        newFriendsCount = view.findViewById(R.id.new_friends_count);

        // Logout button
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
                Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                startActivity(intent);
            });
        }

        // Settings options
        if (accountSettings != null) {
            accountSettings.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "계정 설정", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to account settings
            });
        }

        if (notificationSettings != null) {
            notificationSettings.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "알림 설정", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to notification settings
            });
        }

        if (privacySettings != null) {
            privacySettings.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "개인정보 설정", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to privacy settings
            });
        }

        if (appInfo != null) {
            appInfo.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "앱 정보", Toast.LENGTH_SHORT).show();
                // TODO: Show app info
            });
        }

        // Logout button
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
        }
    }

    private void loadUserData() {
        // Get user ID based on login method
        String userId = getUserId();

        if (userId == null) {
            // Fallback to SharedPreferences if no user ID found
            loadUserDataFromSharedPreferences();
            return;
        }

        // Load user data from Firestore
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Load data from Firestore
                        String displayName = documentSnapshot.getString("displayName");
                        String email = documentSnapshot.getString("email");
                        String username = documentSnapshot.getString("username");
                        String bio = documentSnapshot.getString("bio");
                        String mbti = documentSnapshot.getString("mbti");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        Double rating = documentSnapshot.getDouble("rating");
                        Long activitiesCountLong = documentSnapshot.getLong("activitiesCount");
                        Long connectionsCountLong = documentSnapshot.getLong("connectionsCount");
                        Long badgesCountLong = documentSnapshot.getLong("badgesCount");

                        // Update UI
                        if (profileName != null && displayName != null) {
                            profileName.setText(displayName);
                        }

                        if (profileUsername != null && username != null) {
                            profileUsername.setText("@" + username);
                        }

                        if (profileEmail != null && email != null) {
                            profileEmail.setText(email);
                        }

                        if (profileBio != null && bio != null) {
                            profileBio.setText(bio);
                        }

                        if (mbtiChip != null && mbti != null) {
                            mbtiChip.setText(mbti);
                        }

                        if (ratingText != null && rating != null) {
                            ratingText.setText(String.format("%.1f", rating));
                        }

                        if (activitiesCount != null && activitiesCountLong != null) {
                            activitiesCount.setText(String.valueOf(activitiesCountLong));
                        }

                        if (connectionsCount != null && connectionsCountLong != null) {
                            connectionsCount.setText(String.valueOf(connectionsCountLong));
                        }

                        if (badgesCount != null && badgesCountLong != null) {
                            badgesCount.setText(String.valueOf(badgesCountLong));
                        }

                        // Load profile image from URL if available
                        if (profileImageUrl != null && !profileImageUrl.isEmpty() && profileAvatar != null) {
                            // For now, just try to load as URI
                            // In production, use an image loading library like Glide or Picasso
                            try {
                                Uri imageUri = Uri.parse(profileImageUrl);
                                profileAvatar.setImageURI(imageUri);
                            } catch (Exception e) {
                                profileAvatar.setImageResource(R.drawable.circle_logo);
                            }
                        } else {
                            // Check if there's a local profile image
                            SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
                            String localImageUri = prefs.getString("profile_image_uri", "");
                            if (!localImageUri.isEmpty() && profileAvatar != null) {
                                try {
                                    Uri imageUri = Uri.parse(localImageUri);
                                    profileAvatar.setImageURI(imageUri);
                                } catch (Exception e) {
                                    profileAvatar.setImageResource(R.drawable.circle_logo);
                                }
                            }
                        }
                    } else {
                        // User not found in Firestore, fallback to SharedPreferences
                        loadUserDataFromSharedPreferences();
                    }
                })
                .addOnFailureListener(e -> {
                    // Error loading from Firestore, fallback to SharedPreferences
                    android.util.Log.e("ProfileFragment", "Error loading user data from Firestore", e);
                    loadUserDataFromSharedPreferences();
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
        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        String loginMethod = prefs.getString("login_method", "");

        // For social logins, we need to construct the user ID
        // This is a workaround since we don't store the social user ID in SharedPreferences
        // In a production app, you should store the user ID when logging in
        if ("google".equals(loginMethod) || "kakao".equals(loginMethod) || "naver".equals(loginMethod)) {
            // Try to get from user_id if we added it (we should add this during login)
            String userId = prefs.getString("user_id", "");
            if (!userId.isEmpty()) {
                return userId;
            }
        }

        return null;
    }

    /**
     * Fallback method to load user data from SharedPreferences
     */
    private void loadUserDataFromSharedPreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);

        String userName = prefs.getString("user_name", "이름");
        if (profileName != null) {
            profileName.setText(userName);
        }

        String username = prefs.getString("user_username", "");
        if (username.isEmpty()) {
            username = userName.toLowerCase().replace(" ", "");
        }
        if (profileUsername != null) {
            profileUsername.setText("@" + username);
        }

        String userEmail = "";
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
            userEmail = mAuth.getCurrentUser().getEmail();
        } else {
            userEmail = prefs.getString("user_email", "user@example.com");
        }
        if (profileEmail != null) {
            profileEmail.setText(userEmail);
        }

        String bio = prefs.getString("user_bio", "안녕하세요! 새로운 사람들과 함께 활동하는 것을 좋아합니다 ✨");
        if (profileBio != null) {
            profileBio.setText(bio);
        }

        String mbti = prefs.getString("user_mbti", "ENFP");
        if (mbtiChip != null) {
            mbtiChip.setText(mbti);
        }

        if (ratingText != null) {
            ratingText.setText("4.8");
        }

        String imageUriString = prefs.getString("profile_image_uri", "");
        if (!imageUriString.isEmpty() && profileAvatar != null) {
            try {
                Uri imageUri = Uri.parse(imageUriString);
                profileAvatar.setImageURI(imageUri);
            } catch (Exception e) {
                profileAvatar.setImageResource(R.drawable.circle_logo);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload user data when returning from edit profile
        loadUserData();
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

        // Get login method before clearing
        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        String loginMethod = prefs.getString("login_method", "");

        // Sign out from social login providers based on login method
        if ("google".equals(loginMethod)) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
            googleSignInClient.signOut();
        } else if ("kakao".equals(loginMethod)) {
            UserApiClient.getInstance().logout(error -> {
                if (error != null) {
                    android.util.Log.e("ProfileFragment", "Kakao logout failed", error);
                }
                return null;
            });
        } else if ("naver".equals(loginMethod)) {
            NaverIdLoginSDK.INSTANCE.logout();
        }

        // Clear login state from SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_logged_in", false);
        editor.putBoolean("auto_login", false);  // Clear auto-login preference
        editor.remove("login_method");
        editor.remove("user_id");  // Also clear user ID
        editor.apply();

        Toast.makeText(requireContext(), "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();

        // Navigate to LoginActivity
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
