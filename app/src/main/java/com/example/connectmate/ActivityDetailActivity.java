package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.connectmate.models.Activity;
import com.example.connectmate.models.ChatMessage;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.utils.ActivityManager;
import com.example.connectmate.utils.ChatManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ActivityDetailActivity extends AppCompatActivity {

    private static final String TAG = "ActivityDetailActivity";

    private Activity activity;

    // UI Components
    private Toolbar toolbar;
    private TextView detailTitle;
    private Chip detailCategory;
    private TextView detailDescription;
    private TextView detailDateTime;
    private TextView detailLocation;
    private TextView detailParticipants;
    private TextView detailVisibility;
    private TextView detailHashtags;
    private TextView detailCreator;
    private MaterialButton btnJoinActivity;
    private MaterialButton btnDeleteActivity;
    private MaterialButton btnViewOnMap;

    // Cards (for visibility control)
    private MaterialCardView locationCard;
    private MaterialCardView participantsCard;
    private MaterialCardView infoCard;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Get activity from intent
        activity = (Activity) getIntent().getSerializableExtra("activity");

        if (activity == null) {
            // Try loading by ID if activity object not passed
            String activityId = getIntent().getStringExtra("activity_id");
            if (activityId != null) {
                ActivityManager activityManager = ActivityManager.getInstance(this);
                activity = activityManager.getActivityById(activityId);
            }
        }

        if (activity == null) {
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        displayActivityDetails();
        setupButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload details to reflect any changes (like participant count)
        displayActivityDetails();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        detailTitle = findViewById(R.id.detail_title);
        detailCategory = findViewById(R.id.detail_category);
        detailDescription = findViewById(R.id.detail_description);
        detailDateTime = findViewById(R.id.detail_datetime);
        detailLocation = findViewById(R.id.detail_location);
        detailParticipants = findViewById(R.id.detail_participants);
        detailVisibility = findViewById(R.id.detail_visibility);
        detailHashtags = findViewById(R.id.detail_hashtags);
        detailCreator = findViewById(R.id.detail_creator);
        btnJoinActivity = findViewById(R.id.btn_join_activity);
        btnDeleteActivity = findViewById(R.id.btn_delete_activity);
        btnViewOnMap = findViewById(R.id.btn_view_on_map);

        locationCard = findViewById(R.id.location_card);
        participantsCard = findViewById(R.id.participants_card);
        infoCard = findViewById(R.id.info_card);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Activity Details");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void displayActivityDetails() {
        // Title
        if (activity.getTitle() != null && !activity.getTitle().isEmpty()) {
            detailTitle.setText(activity.getTitle());
        }

        // Category
        if (activity.getCategory() != null && !activity.getCategory().isEmpty()) {
            detailCategory.setText(activity.getCategory());
            setCategoryColor(detailCategory, activity.getCategory());
        } else {
            detailCategory.setVisibility(View.GONE);
        }

        // Description
        if (activity.getDescription() != null && !activity.getDescription().isEmpty()) {
            detailDescription.setText(activity.getDescription());
        } else {
            detailDescription.setText("No description provided.");
        }

        // Date & Time
        String dateTime = activity.getDateTime();
        if (dateTime != null && !dateTime.isEmpty()) {
            detailDateTime.setText(dateTime);
        } else {
            detailDateTime.setText("Date & time to be announced");
        }

        // Location
        if (activity.getLocation() != null && !activity.getLocation().isEmpty()) {
            detailLocation.setText(activity.getLocation());
        } else {
            locationCard.setVisibility(View.GONE);
        }

        // Participants (Source of truth is the chat room member count)
        if (activity.getMaxParticipants() > 0) {
            ChatManager chatManager = ChatManager.getInstance(this);
            ChatRoom chatRoom = chatManager.getChatRoomByActivityId(activity.getId());
            int currentParticipants = (chatRoom != null) ? chatRoom.getMemberCount() : 0;

            String participantsText = currentParticipants + " / " +
                activity.getMaxParticipants() + " participants";
            detailParticipants.setText(participantsText);
        } else {
            participantsCard.setVisibility(View.GONE);
        }

        // Visibility
        if (activity.getVisibility() != null && !activity.getVisibility().isEmpty()) {
            detailVisibility.setText(activity.getVisibility());
        } else {
            findViewById(R.id.visibility_layout).setVisibility(View.GONE);
        }

        // Hashtags
        if (activity.getHashtags() != null && !activity.getHashtags().isEmpty()) {
            detailHashtags.setText(activity.getHashtags());
        } else {
            findViewById(R.id.hashtags_layout).setVisibility(View.GONE);
        }

        // Creator
        if (activity.getCreatorName() != null && !activity.getCreatorName().isEmpty()) {
            detailCreator.setText(activity.getCreatorName());
        } else {
            findViewById(R.id.creator_layout).setVisibility(View.GONE);
        }

        // Hide info card if all fields are empty
        if (findViewById(R.id.visibility_layout).getVisibility() == View.GONE &&
            findViewById(R.id.hashtags_layout).getVisibility() == View.GONE &&
            findViewById(R.id.creator_layout).getVisibility() == View.GONE) {
            infoCard.setVisibility(View.GONE);
        }
    }

    private void setupButtons() {
        btnJoinActivity.setOnClickListener(v -> {
            joinActivity();
        });

        // Only show delete button if current user is the creator
        String currentUserId = getCurrentUserId();
        if (activity.getCreatorId() != null && activity.getCreatorId().equals(currentUserId)) {
            btnDeleteActivity.setVisibility(View.VISIBLE);
            btnDeleteActivity.setOnClickListener(v -> {
                deleteActivity();
            });
        } else {
            btnDeleteActivity.setVisibility(View.GONE);
        }

        // Navigation button - only show if activity has coordinates
        if (activity.getLatitude() != 0.0 && activity.getLongitude() != 0.0) {
            btnViewOnMap.setOnClickListener(v -> viewOnMap());
        } else {
            // Hide navigation button if no coordinates
            if (btnViewOnMap != null) btnViewOnMap.setVisibility(View.GONE);
        }
    }

    /**
     * Get current user ID from Firebase Auth or SharedPreferences
     */
    private String getCurrentUserId() {
        // Try Firebase Auth
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            return firebaseUser.getUid();
        }

        // Try SharedPreferences for social login
        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        return prefs.getString("user_id", "");
    }

    /**
     * Delete the activity
     */
    private void deleteActivity() {
        if (activity == null) {
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Activity")
            .setMessage("Are you sure you want to delete this activity?")
            .setPositiveButton("Delete", (dialog, which) -> {
                ActivityManager activityManager = ActivityManager.getInstance(this);
                boolean deleted = activityManager.deleteActivity(activity.getId());

                if (deleted) {
                    Toast.makeText(this, "Activity deleted", Toast.LENGTH_SHORT).show();
                    finish();  // Close the activity detail page
                } else {
                    Toast.makeText(this, "Failed to delete activity", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    /**
     * Join the activity by creating/joining chat room and sending join notification
     */
    private void joinActivity() {
        if (activity == null) {
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user info
        String userId = getCurrentUserId();
        String userName = "Guest";

        // Try Firebase Auth
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            userName = (firebaseUser.getDisplayName() != null) ?
                firebaseUser.getDisplayName() : firebaseUser.getEmail();
        } else {
            // Try SharedPreferences for social login
            SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
            userName = prefs.getString("user_name", "Guest");
        }

        if (userId.isEmpty()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create or get chat room, automatically adding the creator as the first member
        ChatManager chatManager = ChatManager.getInstance(this);
        ChatRoom chatRoom = chatManager.createOrGetChatRoom(
            activity.getId(),
            activity.getTitle(),
            activity.getCreatorId(),
            activity.getCreatorName()
        );

        // Check if the current user is already a member
        boolean isAlreadyMember = chatRoom.getMemberIds().contains(userId);

        // Add the current user as a member only if they aren't already and aren't the creator
        if (!isAlreadyMember) {
            chatManager.addMemberToChatRoom(chatRoom.getId(), userId, userName);

            // Send join notification message
            String joinMessage = userName + "님이 입장하셨습니다";
            ChatMessage systemMessage = ChatMessage.createSystemMessage(chatRoom.getId(), joinMessage);
            chatManager.sendMessage(systemMessage);

            Log.d(TAG, "User joined activity chat: " + userName);
            Toast.makeText(this, "채팅방에 참여했습니다!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "이미 참여중인 채팅방입니다.", Toast.LENGTH_SHORT).show();
        }

        // Navigate to chat room
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("chat_room_id", chatRoom.getId());
        startActivity(intent);
    }

    private void setCategoryColor(Chip chip, String category) {
        int color;
        switch (category) {
            case "운동":
            case "Sports":
                color = Color.parseColor("#FF6B6B"); // Red
                break;
            case "야외활동":
                color = Color.parseColor("#FF8C42"); // Orange
                break;
            case "스터디":
            case "Study":
                color = Color.parseColor("#4ECDC4"); // Teal
                break;
            case "문화":
                color = Color.parseColor("#A8E6CF"); // Light green
                break;
            case "소셜":
            case "Social":
                color = Color.parseColor("#FFD93D"); // Yellow
                break;
            case "맛집":
                color = Color.parseColor("#FF6B9D"); // Pink
                break;
            case "여행":
                color = Color.parseColor("#95E1D3"); // Aqua
                break;
            case "게임":
                color = Color.parseColor("#AA96DA"); // Light purple
                break;
            case "취미":
                color = Color.parseColor("#FCBAD3"); // Light pink
                break;
            case "봉사":
                color = Color.parseColor("#A8D8EA"); // Light blue
                break;
            case "기타":
            default:
                color = Color.parseColor("#6C5CE7"); // Purple
                break;
        }
        chip.setChipBackgroundColorResource(android.R.color.transparent);
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color));
        chip.setTextColor(Color.WHITE);
    }

    /**
     * View activity location on the internal map
     */
    private void viewOnMap() {
        if (activity == null) return;

        double lat = activity.getLatitude();
        double lng = activity.getLongitude();
        String placeName = activity.getTitle();

        // Create intent to open MainActivity with map coordinates
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("navigate_to_map", true);
        intent.putExtra("map_latitude", lat);
        intent.putExtra("map_longitude", lng);
        intent.putExtra("map_title", placeName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);

        Log.d(TAG, "Navigating to map for: " + placeName + " at (" + lat + ", " + lng + ")");
    }
}
