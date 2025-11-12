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
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.kakao.sdk.user.UserApiClient;
import com.navercorp.nid.NaverIdLoginSDK;
import com.bumptech.glide.Glide;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private DatabaseReference userRef;
    private ValueEventListener userListener;

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

        // Initialize Firebase Auth and Realtime Database
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();

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

        android.util.Log.d("ProfileFragment", "loadUserData - userId: " + userId);

        if (userId == null) {
            clearUserListener();
            // Fallback to SharedPreferences if no user ID found
            android.util.Log.d("ProfileFragment", "No userId found, loading from SharedPreferences");
            loadUserDataFromSharedPreferences();
            return;
        }

        // Load user data from Realtime Database
        android.util.Log.d("ProfileFragment", "Loading user data from Firebase for userId: " + userId);
        clearUserListener();
        userRef = databaseRef.child("users").child(userId);
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                android.util.Log.d("ProfileFragment", "Firebase snapshot exists: " + snapshot.exists());
                if (!snapshot.exists()) {
                    android.util.Log.d("ProfileFragment", "Snapshot doesn't exist, falling back to SharedPreferences");
                    loadUserDataFromSharedPreferences();
                    return;
                }

                User u = snapshot.getValue(User.class);
                android.util.Log.d("ProfileFragment", "User object from Firebase: " + (u != null ? "not null" : "null"));
                if (u != null) {
                    android.util.Log.d("ProfileFragment", "User email from Firebase: " + u.email);
                    bindUserToUi(u);
                } else {
                    loadUserDataFromSharedPreferences();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("ProfileFragment", "Firebase error: " + error.getMessage());
                loadUserDataFromSharedPreferences();
            }
        };

        userRef.addValueEventListener(userListener);
    }

    private void bindUserToUi(User u){
        setText(profileName, or(u.displayName, "이름"));
        setText(profileUsername, "@" + or(u.username, or(safeId(u.displayName), "user")));
        setText(profileEmail, or(u.email, "user@example.com"));
        setText(profileBio, or(u.bio, "안녕하세요! 새로운 사람들과 함께 활동하는 것을 좋아합니다 ✨"));
        if (mbtiChip != null) mbtiChip.setText(or(u.mbti, "ENFP"));

        if (ratingText != null) ratingText.setText(String.format("%.1f", or(u.rating, 4.8)));
        if (activitiesCount != null) activitiesCount.setText(String.valueOf(or(u.activitiesCount, 0L)));
        if (connectionsCount != null) connectionsCount.setText(String.valueOf(or(u.connectionsCount, 0L)));
        if (badgesCount != null) badgesCount.setText(String.valueOf(or(u.badgesCount, 0L)));

        // Load profile image using Glide
        if (profileAvatar != null) {
            String url = u.profileImageUrl;
            if (!TextUtils.isEmpty(url)) {
                Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.circle_logo)
                    .error(R.drawable.circle_logo)
                    .into(profileAvatar);
            } else {
                profileAvatar.setImageResource(R.drawable.circle_logo);
            }
        }
    }

    private void setText(TextView tv, String text) { if (tv != null) tv.setText(text); }
    private String or(String v, String d) { return v == null || v.isEmpty() ? d : v; }
    private Double or(Double v, Double d) { return v == null ? d : v; }
    private Long or(Long v, Long d) { return v == null ? d : v; }
    private long or(Long v, long def) { return v != null ? v : def; }
    private long or(Integer v, long def) { return v != null ? v.longValue() : def; }
    private String safeId(String s) { return s == null ? "" : s.toLowerCase().replace(" ", ""); }

    /*
     * Get the user ID based on login method
     */
    private String getUserId() {
        // Check if user is logged in via Firebase Auth
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            android.util.Log.d("ProfileFragment", "getUserId - Firebase Auth UID: " + uid);
            return uid;
        }

        // Check SharedPreferences for social login user ID
        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        String loginMethod = prefs.getString("login_method", "");
        android.util.Log.d("ProfileFragment", "getUserId - login_method: " + loginMethod);

        // For social logins and email login, get the user_id from SharedPreferences
        if ("google".equals(loginMethod) || "kakao".equals(loginMethod) || "naver".equals(loginMethod) || "email".equals(loginMethod)) {
            String userId = prefs.getString("user_id", "");
            android.util.Log.d("ProfileFragment", "getUserId - user_id from prefs: " + userId);
            if (!userId.isEmpty()) {
                return userId;
            }
        }

        android.util.Log.d("ProfileFragment", "getUserId - returning null");
        return null;
    }

    /**
     * Fallback method to load user data from SharedPreferences
     */
    private void loadUserDataFromSharedPreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);

        android.util.Log.d("ProfileFragment", "loadUserDataFromSharedPreferences called");

        String userName = prefs.getString("user_name", "이름");
        android.util.Log.d("ProfileFragment", "user_name from prefs: " + userName);
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
            android.util.Log.d("ProfileFragment", "Email from Firebase Auth: " + userEmail);
        } else {
            userEmail = prefs.getString("user_email", "user@example.com");
            android.util.Log.d("ProfileFragment", "Email from SharedPreferences: " + userEmail);
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

        // Load profile image using Glide
        String imageUrlString = prefs.getString("profile_image_url", "");
        if (!imageUrlString.isEmpty() && profileAvatar != null) {
            try {
                Glide.with(this)
                    .load(imageUrlString)
                    .placeholder(R.drawable.circle_logo)
                    .error(R.drawable.circle_logo)
                    .into(profileAvatar);
            } catch (Exception e) {
                profileAvatar.setImageResource(R.drawable.circle_logo);
            }
        } else if (profileAvatar != null) {
            profileAvatar.setImageResource(R.drawable.circle_logo);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload user data when returning from edit profile
        loadUserData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        clearUserListener();
    }

    private void clearUserListener() {
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
        userRef = null;
        userListener = null;
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
        // Get SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        String loginMethod = prefs.getString("login_method", "");

        // Sign out from social providers based on login method
        try {
            switch (loginMethod) {
                case "google":
                    // Sign out from Google
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .build();
                    GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
                    googleSignInClient.signOut().addOnCompleteListener(task -> {
                        android.util.Log.d("ProfileFragment", "Google sign out completed");
                    });
                    break;

                case "kakao":
                    // Sign out from Kakao
                    UserApiClient.getInstance().logout(error -> {
                        if (error != null) {
                            android.util.Log.e("ProfileFragment", "Kakao logout failed", error);
                        } else {
                            android.util.Log.d("ProfileFragment", "Kakao logout success");
                        }
                        return null;
                    });
                    break;

                case "naver":
                    // Sign out from Naver
                    NaverIdLoginSDK.INSTANCE.logout();
                    android.util.Log.d("ProfileFragment", "Naver logout completed");
                    break;

                case "firebase":
                case "email":
                    // Firebase Auth only login
                    android.util.Log.d("ProfileFragment", "Firebase email login logout");
                    break;

                default:
                    android.util.Log.w("ProfileFragment", "Unknown login method: " + loginMethod);
                    break;
            }
        } catch (Exception e) {
            android.util.Log.e("ProfileFragment", "Error during social provider logout", e);
        }

        // Sign out from Firebase Auth (for all login methods)
        mAuth.signOut();

        // Clear ALL user data from SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // Clear everything
        editor.apply();

        android.util.Log.d("ProfileFragment", "Logout completed - All data cleared");

        Toast.makeText(requireContext(), "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();

        // Navigate to LoginActivity
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
