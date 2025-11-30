package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.example.connectmate.models.UserReview;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private static final String ARG_USER_ID = "userId";
    private static final String ARG_SHOW_BUTTONS = "showButtons";
    private String userId;
    private boolean showButtons;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private DatabaseReference userRef;
    private ValueEventListener userListener;

    // UI elements - Profile Card
    private ImageButton backButton;
    private CircleImageView profileAvatar;
    private TextView profileName;
    private TextView profileUsername;
    private TextView profileEmail;
    private Chip mbtiChip;
    private TextView profileBio;
    private TextView ratingText;
    private LinearLayout ratingStarsContainer;
    private TextView activitiesCount;
    private TextView connectionsCount;
    private TextView friendsCount;
    private MaterialButton editProfileButton;
    private LinearLayout reviewsListContainer;
    private TextView reviewsEmptyText;
    private MaterialButton seeAllReviewsButton;

    // UI elements - Settings
    private LinearLayout accountSettings;
    private LinearLayout notificationSettings;
    private LinearLayout privacySettings;
    private LinearLayout appInfo;

    // UI elements - Logout
    private MaterialButton logoutButton;
    private MaterialButton deleteAccountButton;
    private String currentUserId;

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    public static ProfileFragment newInstance(String userId, boolean showButtons) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        args.putBoolean(ARG_SHOW_BUTTONS, showButtons);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
            showButtons = getArguments().getBoolean(ARG_SHOW_BUTTONS, false);
        }
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

        if (showButtons) {
            editProfileButton.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
            deleteAccountButton.setVisibility(View.VISIBLE);
        } else {
            editProfileButton.setVisibility(View.GONE);
            logoutButton.setVisibility(View.GONE);
            deleteAccountButton.setVisibility(View.GONE);
        }

        // Load user data
        loadUserData();
    }

    private void initializeViews(View view) {

        // Header
        backButton = view.findViewById(R.id.back_button);

        // Profile card
        profileAvatar = view.findViewById(R.id.profile_avatar);
        profileName = view.findViewById(R.id.profile_name);
        profileUsername = view.findViewById(R.id.profile_username);
        profileEmail = view.findViewById(R.id.profile_email);
        mbtiChip = view.findViewById(R.id.mbti_chip);
        profileBio = view.findViewById(R.id.profile_bio);
        ratingText = view.findViewById(R.id.rating_text);
        ratingStarsContainer = view.findViewById(R.id.rating_stars_container);
        activitiesCount = view.findViewById(R.id.activities_count);
        connectionsCount = view.findViewById(R.id.connections_count);
        friendsCount = view.findViewById(R.id.friends_count);
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        reviewsListContainer = view.findViewById(R.id.reviews_list_container);
        reviewsEmptyText = view.findViewById(R.id.reviews_empty_text);
        seeAllReviewsButton = view.findViewById(R.id.btn_see_all_reviews);

        // Logout button
        logoutButton = view.findViewById(R.id.logout_button);
        deleteAccountButton = view.findViewById(R.id.delete_account_button);

        showNoReviewsState();
    }

    private void setupClickListeners() {
        // Back button
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                requireActivity().finish();
            });
        }

        // Edit profile button
        if (editProfileButton != null) {
            editProfileButton.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                startActivity(intent);
            });
        }

        if (seeAllReviewsButton != null) {
            seeAllReviewsButton.setOnClickListener(v -> openAllReviews());
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

        if (deleteAccountButton != null) {
            deleteAccountButton.setOnClickListener(v -> showDeleteAccountConfirmationDialog());
        }
    }

    private void openAllReviews() {
        String userId = currentUserId != null ? currentUserId : getUserId();
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(requireContext(), "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        int containerId = getFragmentContainerId();
        if (containerId == View.NO_ID) {
            Toast.makeText(requireContext(), "리뷰 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        UserReviewsFragment fragment = UserReviewsFragment.newInstance(userId);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(containerId, fragment)
                .addToBackStack("UserReviews")
                .commit();
    }

    private int getFragmentContainerId() {
        View mainContainer = requireActivity().findViewById(R.id.main_container);
        if (mainContainer != null) {
            return R.id.main_container;
        }
        View profileContainer = requireActivity().findViewById(R.id.profile_fragment_container);
        if (profileContainer != null) {
            return R.id.profile_fragment_container;
        }
        return View.NO_ID;
    }

    private void loadUserData() {
        // Get user ID based on login method
        String userIdToLoad = (userId != null) ? userId : getUserId();
        currentUserId = userIdToLoad;

        android.util.Log.d("ProfileFragment", "loadUserData - userId: " + userIdToLoad);

        if (userIdToLoad == null) {
            clearUserListener();
            // Fallback to SharedPreferences if no user ID found
            android.util.Log.d("ProfileFragment", "No userId found, loading from SharedPreferences");
            loadUserDataFromSharedPreferences();
            return;
        }

        // Load user data from Realtime Database
        android.util.Log.d("ProfileFragment", "Loading user data from Firebase for userId: " + userIdToLoad);
        clearUserListener();
        userRef = databaseRef.child("users").child(userIdToLoad);
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

        double rating = or(u.rating, 0.0);
        if (ratingText != null) ratingText.setText(String.format("%.1f", rating));
        updateRatingStars(rating);
        int participation = u.participationCount;
        if (activitiesCount != null) {
            activitiesCount.setText(String.valueOf(participation));
        }

        // Calculate actual unique connections (users met through activities)
        calculateUniqueConnections(currentUserId);

        int friendCount = u.getFriends() != null ? u.getFriends().size() : 0;
        if (friendsCount != null) friendsCount.setText(String.valueOf(friendCount));
        displayRecentReviews(u);


        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("participation_count", participation);

        // Load profile image using Glide
        if (profileAvatar != null) {
            String url = u.profileImageUrl;
            if (!TextUtils.isEmpty(url)) {
                Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.circle_logo)
                        .error(R.drawable.circle_logo)
                        .into(profileAvatar);
                editor.putString("profile_image_url", url);
            } else {
                profileAvatar.setImageResource(R.drawable.circle_logo);
                editor.remove("profile_image_url");
            }
        }
        editor.apply();
    }

    private void displayRecentReviews(User user) {
        if (reviewsListContainer == null || reviewsEmptyText == null || seeAllReviewsButton == null) {
            return;
        }

        reviewsListContainer.removeAllViews();
        Map<String, UserReview> reviewMap = user.getReviews();
        if (reviewMap == null || reviewMap.isEmpty()) {
            reviewsEmptyText.setVisibility(View.VISIBLE);
            seeAllReviewsButton.setVisibility(View.GONE);
            seeAllReviewsButton.setEnabled(false);
            return;
        }

        List<UserReview> reviews = new ArrayList<>(reviewMap.values());
        Collections.sort(reviews, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        // Show max 3 reviews in profile
        int limit = Math.min(3, reviews.size());
        for (int i = 0; i < limit; i++) {
            View itemView = inflater.inflate(R.layout.item_user_review, reviewsListContainer, false);
            TextView activityTitle = itemView.findViewById(R.id.review_activity_title);
            TextView rating = itemView.findViewById(R.id.review_rating_text);
            TextView comment = itemView.findViewById(R.id.review_comment_text);
            LinearLayout starsContainer = itemView.findViewById(R.id.review_stars_container);
            UserReview review = reviews.get(i);

            // Set activity title
            String titleText = review.getActivityTitle();
            if (activityTitle != null) {
                if (titleText != null && !titleText.trim().isEmpty()) {
                    activityTitle.setText(titleText);
                    activityTitle.setVisibility(View.VISIBLE);
                } else {
                    activityTitle.setText("활동 정보 없음");
                    activityTitle.setVisibility(View.VISIBLE);
                }
            }

            rating.setText(review.getRating() + "점");

            // Display stars based on rating
            starsContainer.removeAllViews();
            int ratingValue = review.getRating();
            int starCount = ratingValue > 0 ? ratingValue : 1;
            for (int j = 0; j < starCount; j++) {
                ImageView star = new ImageView(requireContext());
                LinearLayout.LayoutParams starParams = new LinearLayout.LayoutParams(
                    (int) (20 * getResources().getDisplayMetrics().density),
                    (int) (20 * getResources().getDisplayMetrics().density)
                );
                if (j > 0) {
                    starParams.setMarginStart((int) (2 * getResources().getDisplayMetrics().density));
                }
                star.setLayoutParams(starParams);
                star.setImageResource(R.drawable.ic_star_filled);
                star.setColorFilter(getResources().getColor(
                    ratingValue > 0 ? R.color.yellow_500 : R.color.gray_100, null
                ));
                starsContainer.addView(star);
            }

            String commentText = review.getComment();
            if (commentText == null || commentText.trim().isEmpty()) {
                commentText = "한줄평이 없습니다.";
            }
            comment.setText(commentText);
            reviewsListContainer.addView(itemView);

            if (i < limit - 1) {
                View divider = new View(requireContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                params.setMargins(0, 8, 0, 8);
                divider.setLayoutParams(params);
                divider.setBackgroundResource(R.color.gray_200);
                reviewsListContainer.addView(divider);
            }
        }
        reviewsEmptyText.setVisibility(View.GONE);

        // Only show "See All Reviews" button if there are more than 3 reviews
        if (reviews.size() > 3) {
            seeAllReviewsButton.setVisibility(View.VISIBLE);
            seeAllReviewsButton.setEnabled(true);
        } else {
            seeAllReviewsButton.setVisibility(View.GONE);
            seeAllReviewsButton.setEnabled(false);
        }
    }

    private void showNoReviewsState() {
        if (reviewsListContainer != null) {
            reviewsListContainer.removeAllViews();
        }
        if (reviewsEmptyText != null) {
            reviewsEmptyText.setVisibility(View.VISIBLE);
        }
        if (seeAllReviewsButton != null) {
            seeAllReviewsButton.setVisibility(View.GONE);
            seeAllReviewsButton.setEnabled(false);
        }
    }

    private void setText(TextView tv, String text) { if (tv != null) tv.setText(text); }

    /**
     * Update rating stars dynamically based on rating value
     * @param rating The rating value (0.0 to 5.0)
     */
    private void updateRatingStars(double rating) {
        if (ratingStarsContainer == null) return;

        ratingStarsContainer.removeAllViews();

        int fullStars = (int) rating;
        double decimal = rating - fullStars;
        int emptyStars = 5 - fullStars - (decimal >= 0.25 ? 1 : 0);

        float density = getResources().getDisplayMetrics().density;
        int starSize = (int) (22 * density);
        int marginEnd = (int) (2 * density);

        // Add full stars
        for (int i = 0; i < fullStars; i++) {
            ImageView star = createStarView(starSize, marginEnd, R.drawable.ic_star_filled, R.color.yellow_500);
            ratingStarsContainer.addView(star);
        }

        // Add half star if needed
        if (decimal >= 0.25 && decimal < 0.75) {
            ImageView halfStar = createStarView(starSize, marginEnd, R.drawable.ic_star_half, R.color.yellow_500);
            ratingStarsContainer.addView(halfStar);
        } else if (decimal >= 0.75) {
            ImageView star = createStarView(starSize, marginEnd, R.drawable.ic_star_filled, R.color.yellow_500);
            ratingStarsContainer.addView(star);
            emptyStars--;
        }

        // Add empty stars
        for (int i = 0; i < emptyStars; i++) {
            ImageView star = createStarView(starSize, i == emptyStars - 1 ? 0 : marginEnd, R.drawable.ic_star_outline, R.color.gray_300);
            ratingStarsContainer.addView(star);
        }
    }

    /**
     * Helper method to create a star ImageView
     */
    private ImageView createStarView(int size, int marginEnd, int drawableRes, int colorRes) {
        ImageView star = new ImageView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginEnd(marginEnd);
        star.setLayoutParams(params);
        star.setImageResource(drawableRes);
        star.setColorFilter(getResources().getColor(colorRes, null));
        return star;
    }

    /**
     * Calculate unique connections (unique users met through activities)
     */
    private void calculateUniqueConnections(String userId) {
        if (userId == null || userId.isEmpty()) {
            if (connectionsCount != null) connectionsCount.setText("0");
            return;
        }

        DatabaseReference activitiesRef = FirebaseDatabase.getInstance().getReference("activities");

        // Use a Set to track unique user IDs
        java.util.Set<String> uniqueConnections = new java.util.HashSet<>();

        activitiesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Go through all activities
                for (DataSnapshot activitySnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot participantsSnapshot = activitySnapshot.child("participants");

                    // Check if this user is a participant
                    if (participantsSnapshot.hasChild(userId)) {
                        // Add all other participants to the set
                        for (DataSnapshot participantSnapshot : participantsSnapshot.getChildren()) {
                            String participantId = participantSnapshot.getKey();
                            if (participantId != null && !participantId.equals(userId)) {
                                uniqueConnections.add(participantId);
                            }
                        }
                    }
                }

                // Update UI with unique connections count
                int count = uniqueConnections.size();
                if (connectionsCount != null) {
                    connectionsCount.setText(String.valueOf(count));
                }

                android.util.Log.d("ProfileFragment", "Unique connections for " + userId + ": " + count);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("ProfileFragment", "Failed to calculate connections: " + error.getMessage());
                if (connectionsCount != null) connectionsCount.setText("0");
            }
        });
    }

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
        showNoReviewsState();

        String storedUserId = prefs.getString("user_id", null);
        if (!TextUtils.isEmpty(storedUserId)) {
            currentUserId = storedUserId;
        }

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
            ratingText.setText("0.0");
        }

        int participationCount = prefs.getInt("participation_count", 0);

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

    private void showDeleteAccountConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("회원 탈퇴")
                .setMessage("정말 ConnectMate 계정을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.")
                .setPositiveButton("탈퇴", (dialog, which) -> performAccountDeletion())
                .setNegativeButton("취소", null)
                .show();
    }

    private void performAccountDeletion() {
        String userId = getUserId();
        if (userId == null) {
            Toast.makeText(requireContext(), "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        deleteFirebaseAccount(userId);
    }

    private void deleteFirebaseAccount(String userId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            android.util.Log.d("ProfileFragment", "No Firebase user, removing database entry directly");
            removeUserDataFromDatabase(userId);
            return;
        }

        currentUser.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                android.util.Log.d("ProfileFragment", "Firebase account deleted");
                removeUserDataFromDatabase(userId);
            } else {
                Exception exception = task.getException();
                if (exception != null) {
                    android.util.Log.e("ProfileFragment", "Failed to delete Firebase account", exception);
                }
                if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
                    Toast.makeText(requireContext(), "보안을 위해 다시 로그인한 뒤 탈퇴를 진행해주세요.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "계정을 삭제하지 못했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void removeUserDataFromDatabase(String userId) {
        databaseRef.child("users").child(userId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                android.util.Log.d("ProfileFragment", "User data removed for userId: " + userId);
                clearLocalSessionAfterDeletion();
            } else {
                Exception exception = task.getException();
                if (exception != null) {
                    android.util.Log.e("ProfileFragment", "Failed to delete user data", exception);
                }
                Toast.makeText(requireContext(), "계정 정보를 삭제하지 못했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearLocalSessionAfterDeletion() {
        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        logoutFromProviders(prefs);
        finalizeSessionTermination(prefs, "계정이 삭제되었습니다");
    }

    private void performLogout() {
        android.util.Log.d("ProfileFragment", "=== Starting logout process ===");

        SharedPreferences prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);

        // Explicitly disable auto-login before clearing session
        prefs.edit().putBoolean("auto_login", false).apply();
        prefs.edit().putBoolean("is_logged_in", false).apply();
        android.util.Log.d("ProfileFragment", "Disabled auto-login and is_logged_in flags");

        // Logout from social providers
        logoutFromProviders(prefs);

        // Finalize logout and redirect to login
        finalizeSessionTermination(prefs, "로그아웃 되었습니다");
    }

    private void logoutFromProviders(SharedPreferences prefs) {
        String loginMethod = prefs.getString("login_method", "");

        try {
            switch (loginMethod) {
                case "google":
                    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .build();
                    GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);
                    googleSignInClient.signOut().addOnCompleteListener(task -> {
                        android.util.Log.d("ProfileFragment", "Google sign out completed");
                    });
                    break;
                case "kakao":
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
                    android.util.Log.d("ProfileFragment", "=== Starting Naver logout process ===");

                    // Log current token status
                    String naverAccessToken = com.navercorp.nid.NaverIdLoginSDK.INSTANCE.getAccessToken();
                    String naverRefreshToken = com.navercorp.nid.NaverIdLoginSDK.INSTANCE.getRefreshToken();
                    android.util.Log.d("ProfileFragment", "Naver access token: " + (naverAccessToken != null ? "EXISTS" : "NULL"));
                    android.util.Log.d("ProfileFragment", "Naver refresh token: " + (naverRefreshToken != null ? "EXISTS" : "NULL"));

                    // Logout from Naver (invalidates session)
                    NaverIdLoginSDK.INSTANCE.logout();
                    android.util.Log.d("ProfileFragment", "Called NaverIdLoginSDK.logout()");

                    // Clear ALL possible Naver SDK SharedPreferences files
                    try {
                        // Clear main OAuth preferences
                        SharedPreferences naverOAuth = requireContext().getSharedPreferences("NaverOAuthSDK", Context.MODE_PRIVATE);
                        naverOAuth.edit().clear().apply();
                        android.util.Log.d("ProfileFragment", "Cleared NaverOAuthSDK prefs");

                        // Clear additional Naver preferences that might exist
                        String[] possibleNaverPrefs = {
                                "NaverOAuth",
                                "naver_oauth",
                                "com.naver.nid.oauth",
                                "com.navercorp.nid.oauth"
                        };

                        for (String prefName : possibleNaverPrefs) {
                            try {
                                SharedPreferences naverPrefs = requireContext().getSharedPreferences(prefName, Context.MODE_PRIVATE);
                                naverPrefs.edit().clear().apply();
                                android.util.Log.d("ProfileFragment", "Cleared " + prefName + " prefs");
                            } catch (Exception e) {
                                // Ignore if doesn't exist
                            }
                        }

                        // Verify token is cleared
                        String tokenAfterLogout = com.navercorp.nid.NaverIdLoginSDK.INSTANCE.getAccessToken();
                        android.util.Log.d("ProfileFragment", "Token after logout: " + (tokenAfterLogout != null ? "STILL EXISTS (ERROR!)" : "NULL (SUCCESS)"));

                    } catch (Exception e) {
                        android.util.Log.e("ProfileFragment", "Error clearing Naver SDK prefs", e);
                    }

                    android.util.Log.d("ProfileFragment", "=== Naver logout completed ===");
                    break;
                case "firebase":
                case "email":
                    android.util.Log.d("ProfileFragment", "Firebase/email login logout");
                    break;
                default:
                    android.util.Log.w("ProfileFragment", "Unknown login method: " + loginMethod);
                    break;
            }
        } catch (Exception e) {
            android.util.Log.e("ProfileFragment", "Error during social provider logout", e);
        }
    }

    private void finalizeSessionTermination(SharedPreferences prefs, String toastMessage) {
        android.util.Log.d("ProfileFragment", "=== Finalizing session termination ===");

        // Sign out from Firebase Auth
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            android.util.Log.d("ProfileFragment", "Signing out Firebase user: " + mAuth.getCurrentUser().getEmail());
            mAuth.signOut();
        }

        // Clear ALL SharedPreferences data
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        android.util.Log.d("ProfileFragment", "Cleared all SharedPreferences data");

        // Clean up Firebase listeners
        clearUserListener();

        // Clean up chat listeners
        try {
            com.example.connectmate.utils.FirebaseChatManager.getInstance().removeAllListeners();
            android.util.Log.d("ProfileFragment", "Removed Firebase chat listeners");
        } catch (Exception e) {
            android.util.Log.e("ProfileFragment", "Error removing chat listeners", e);
        }

        android.util.Log.d("ProfileFragment", "Session terminated - " + toastMessage);

        Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show();

        // Redirect to LoginActivity with flags to clear activity stack
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("from_logout", true); // Flag to indicate this is from logout
        startActivity(intent);
        requireActivity().finish();

        android.util.Log.d("ProfileFragment", "=== Logout complete - redirected to LoginActivity ===");
    }
}
