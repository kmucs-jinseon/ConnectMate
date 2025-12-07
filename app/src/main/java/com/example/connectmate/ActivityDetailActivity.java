package com.example.connectmate;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.connectmate.models.Activity;
import com.example.connectmate.models.ChatMessage;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.utils.FirebaseActivityManager;
import com.example.connectmate.utils.FirebaseChatManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ActivityDetailActivity extends AppCompatActivity {

    private static final String TAG = "ActivityDetailActivity";

    private Activity activity;
    private String activityId;

    // UI Components
    private Toolbar toolbar;
    private TextView detailTitle;
    private com.google.android.material.chip.ChipGroup detailCategoryGroup;
    private TextView detailDescription;
    private TextView detailDateTime;
    private TextView detailLocation;
    private TextView detailParticipants;
    private TextView detailVisibility;
    private TextView detailHashtags;
    private TextView detailCreator;
    private MaterialButton btnJoinActivity;
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
        if (activity != null) {
            activityId = activity.getId();
        } else {
            activityId = getIntent().getStringExtra("activity_id");
        }

        initializeViews();
        setupToolbar();

        if (activity == null) {
            // Try loading by ID if activity object not passed
            if (activityId != null) {
                loadActivityFromFirebase(activityId);
            } else {
                Toast.makeText(this, "활동을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            // Display initial data and set up real-time listener
            displayActivityDetails();
            setupButtons();
            setupRealTimeListener(activityId);
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        detailTitle = findViewById(R.id.detail_title);
        detailCategoryGroup = findViewById(R.id.detail_category_group);
        detailDescription = findViewById(R.id.detail_description);
        detailDateTime = findViewById(R.id.detail_datetime);
        detailLocation = findViewById(R.id.detail_location);
        detailParticipants = findViewById(R.id.detail_participants);
        detailVisibility = findViewById(R.id.detail_visibility);
        detailHashtags = findViewById(R.id.detail_hashtags);
        detailCreator = findViewById(R.id.detail_creator);
        btnJoinActivity = findViewById(R.id.btn_join_activity);
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
            getSupportActionBar().setTitle("활동 상세 내용");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // Menu code removed - toolbar menu deleted
    // Delete functionality preserved in showDeleteConfirmationDialog() method
    // Can be triggered from UI button if needed in the future

    /**
     * Check if current user is the creator of this activity
     */
    private boolean isCurrentUserCreator() {
        if (activity == null || activity.getCreatorId() == null) {
            return false;
        }

        // Get current user ID
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            return activity.getCreatorId().equals(firebaseUser.getUid());
        }

        // Check SharedPreferences for social login
        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");
        return activity.getCreatorId().equals(userId);
    }

    /**
     * Show confirmation dialog before deleting activity
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("활동 삭제")
            .setMessage("활동 기록이 남지 않습니다. 활동이 정상적으로 종료되었다면 '활동 종료'를 선택해주세요.\n\n계속하시겠습니까?")
            .setPositiveButton("삭제", (dialog, which) -> deleteActivity(false))
            .setNeutralButton("활동 종료", (dialog, which) -> deleteActivity(true))
            .setNegativeButton("취소", null)
            .show();
    }

    /**
     * Delete the activity and associated chat room
     * @param isCompletion true if activity is being completed (triggers reviews/notifications), false for simple deletion
     */
    private void deleteActivity(boolean isCompletion) {
        if (activity == null) {
            Toast.makeText(this, "활동을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "🗑️ User requested to delete activity: " + activity.getId() + " (completion: " + isCompletion + ")");
        Log.d(TAG, "Activity title: " + activity.getTitle());
        Log.d(TAG, "Creator ID: " + activity.getCreatorId());

        // Show progress
        Toast.makeText(this, isCompletion ? "종료 중..." : "삭제 중...", Toast.LENGTH_SHORT).show();

        FirebaseActivityManager activityManager = FirebaseActivityManager.getInstance();

        if (isCompletion) {
            // Activity completion - create reviews and notifications
            activityManager.deleteActivity(
                activity.getId(),
                true,  // increment participation count
                activity.getTitle(),  // activity title for notifications
                new FirebaseActivityManager.OnCompleteListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "✅ Activity completed successfully!");
                        Toast.makeText(ActivityDetailActivity.this, "활동이 종료되었습니다", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "❌ Failed to complete activity: " + e.getMessage(), e);
                        Toast.makeText(ActivityDetailActivity.this,
                            "활동 종료에 실패했습니다: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    }
                }
            );
        } else {
            // Simple deletion - no reviews or notifications
            activityManager.deleteActivity(activity.getId(), new FirebaseActivityManager.OnCompleteListener<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "✅ Activity deleted successfully!");
                    Toast.makeText(ActivityDetailActivity.this, "활동이 삭제되었습니다", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "❌ Failed to delete activity: " + e.getMessage(), e);
                    Toast.makeText(ActivityDetailActivity.this,
                        "활동 삭제에 실패했습니다: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void displayActivityDetails() {
        // Title
        if (activity.getTitle() != null && !activity.getTitle().isEmpty()) {
            detailTitle.setText(activity.getTitle());
        }

        // Categories - split and display multiple chips
        if (activity.getCategory() != null && !activity.getCategory().isEmpty()) {
            detailCategoryGroup.removeAllViews();
            String[] categories = activity.getCategory().split(",");
            for (String category : categories) {
                String trimmedCategory = category.trim();
                if (!trimmedCategory.isEmpty()) {
                    // Create chip with CategoryChipStyle
                    Chip chip = new Chip(new ContextThemeWrapper(this, R.style.CategoryChipStyle));
                    chip.setText(trimmedCategory);
                    chip.setClickable(false);

                    // Set category-specific pastel color for display
                    int colorRes = com.example.connectmate.utils.CategoryMapper.getCategoryColor(trimmedCategory);
                    int color = getResources().getColor(colorRes, null);
                    chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color));

                    // Set dark gray text and stroke for pastel background (hardcoded to ensure dark in both themes)
                    int textColor = android.graphics.Color.parseColor("#4B5563");  // Dark gray
                    chip.setTextColor(textColor);
                    chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(textColor));
                    chip.setChipStrokeWidth(1 * getResources().getDisplayMetrics().density);

                    detailCategoryGroup.addView(chip);
                }
            }
            detailCategoryGroup.setVisibility(View.VISIBLE);
        } else {
            detailCategoryGroup.setVisibility(View.GONE);
        }

        // Description
        if (activity.getDescription() != null && !activity.getDescription().isEmpty()) {
            detailDescription.setText(activity.getDescription());
        } else {
            detailDescription.setText("작성된 설명이 없습니다.");
        }

        // Date & Time
        String dateTime = activity.getDateTime();
        if (dateTime != null && !dateTime.isEmpty()) {
            detailDateTime.setText(dateTime);
        } else {
            detailDateTime.setText("날짜와 시간은 추후 발표됩니다");
        }

        // Location
        if (activity.getLocation() != null && !activity.getLocation().isEmpty()) {
            detailLocation.setText(activity.getLocation());
        } else {
            locationCard.setVisibility(View.GONE);
        }

        // Participants
        if (activity.getMaxParticipants() > 0) {
            String participantsText = activity.getCurrentParticipants() + " / " +
                activity.getMaxParticipants() + " 명";
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

        // Navigation button - only show if activity has coordinates
        if (activity.getLatitude() != 0.0 && activity.getLongitude() != 0.0) {
            btnViewOnMap.setOnClickListener(v -> viewOnMap());
        } else {
            // Hide navigation button if no coordinates
            if (btnViewOnMap != null) btnViewOnMap.setVisibility(View.GONE);
        }
    }

    /**
     * Load activity from Firebase by ID
     */
    private void loadActivityFromFirebase(String activityId) {
        this.activityId = activityId;
        FirebaseActivityManager activityManager = FirebaseActivityManager.getInstance();
        activityManager.getActivityById(activityId, new FirebaseActivityManager.OnCompleteListener<Activity>() {
            @Override
            public void onSuccess(Activity result) {
                activity = result;
                displayActivityDetails();
                setupButtons();
                setupRealTimeListener(activityId);
                invalidateOptionsMenu(); // Refresh menu to show delete option if user is creator
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading activity", e);
                Toast.makeText(ActivityDetailActivity.this, "활동을 불러오는 데 실패했습니다", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Set up real-time listener for activity updates
     */
    private void setupRealTimeListener(String activityId) {
        FirebaseActivityManager activityManager = FirebaseActivityManager.getInstance();
        activityManager.listenToActivity(activityId, new FirebaseActivityManager.ActivityListener() {
            @Override
            public void onActivityUpdated(Activity updatedActivity) {
                activity = updatedActivity;
                runOnUiThread(() -> {
                    displayActivityDetails();
                    Log.d(TAG, "Activity updated in real-time: " + updatedActivity.getTitle());
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error listening to activity updates", e);
            }
        });
    }

    /**
     * Join the activity by creating/joining chat room and sending join notification
     */
    private void joinActivity() {
        if (activity == null) {
            Toast.makeText(this, "활동을 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            Toast.makeText(this, "인원이 가득 찬 방입니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current user ID
        String userId = "";

        // Try Firebase Auth
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            userId = firebaseUser.getUid();
         } else {
            // Try SharedPreferences for social login
            SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
            userId = prefs.getString("user_id", "");
        }

        if (userId.isEmpty()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalUserId = userId;

        // Fetch user's display name from Firebase Realtime Database (profile settings)
        FirebaseDatabase.getInstance().getReference().child("users").child(finalUserId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String displayName = "Guest";
                    if (snapshot.exists()) {
                        displayName = snapshot.child("displayName").getValue(String.class);
                        if (displayName == null || displayName.isEmpty()) {
                            displayName = snapshot.child("email").getValue(String.class);
                        }
                        if (displayName == null) {
                            displayName = "게스트";
                        }
                    }

                    final String finalUserName = displayName;

                    FirebaseActivityManager activityManager = FirebaseActivityManager.getInstance();
                    FirebaseChatManager chatManager = FirebaseChatManager.getInstance();

        // Check if user is already a participant
        activityManager.isUserParticipant(activity.getId(), finalUserId, new FirebaseActivityManager.OnCompleteListener<Boolean>() {
            @Override
            public void onSuccess(Boolean isParticipant) {
                if (isParticipant) {
                    // User is already a participant, just navigate to chat
                    Log.d(TAG, "User is already a participant, navigating to chat");
                    chatManager.getChatRoomByActivityId(activity.getId(), new FirebaseChatManager.OnCompleteListener<ChatRoom>() {
                        @Override
                        public void onSuccess(ChatRoom chatRoom) {
                            if (chatRoom != null) {
                                navigateToChatRoom(chatRoom);
                            } else {
                                Toast.makeText(ActivityDetailActivity.this, "이미 참여 중인 활동입니다", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(ActivityDetailActivity.this, "이미 참여 중인 활동입니다", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                // User is not a participant, proceed with joining
                chatManager.createOrGetChatRoom(
                        activity.getId(),
                        activity.getTitle(),
                        activity.getCategory(),
                        activity.getCreatorId(),
                        activity.getCreatorName(),
                        new FirebaseChatManager.OnCompleteListener<ChatRoom>() {
                    @Override
                    public void onSuccess(ChatRoom chatRoom) {
                        // First, add user as participant to the activity
                        activityManager.addParticipant(activity.getId(), finalUserId, finalUserName, new FirebaseActivityManager.OnCompleteListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // Then add user as member to chat room
                        chatManager.addMemberToChatRoom(chatRoom.getId(), finalUserId, finalUserName, new FirebaseChatManager.OnCompleteListener<Void>() {
                            @Override
                            public void onSuccess(Void memberResult) {
                                // Send join notification message
                                String joinMessage = finalUserName + "님이 입장하셨습니다";
                                ChatMessage systemMessage = ChatMessage.createSystemMessage(chatRoom.getId(), joinMessage);

                                // Send message and wait for it to be saved before navigating
                                chatManager.sendMessage(systemMessage, new FirebaseChatManager.OnCompleteListener<ChatMessage>() {
                                    @Override
                                    public void onSuccess(ChatMessage result) {
                                        Log.d(TAG, "System message sent successfully");
                                        navigateToChatRoom(chatRoom);
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Log.e(TAG, "Failed to send system message", e);
                                        // Navigate anyway even if message fails
                                        navigateToChatRoom(chatRoom);
                                    }
                                });

                                Log.d(TAG, "User joined activity and chat: " + finalUserName);
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Failed to add member to chat room", e);
                                String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                                Toast.makeText(ActivityDetailActivity.this, "채팅방 멤버 추가 실패: " + errorMsg, Toast.LENGTH_LONG).show();
                                // Don't navigate if adding member failed
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Failed to add participant to activity", e);
                        String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                        Toast.makeText(ActivityDetailActivity.this, "참여 실패: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to create chat room", e);
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                Toast.makeText(ActivityDetailActivity.this, "채팅방 생성 실패: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to check participant status", e);
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                Toast.makeText(ActivityDetailActivity.this, "참여 확인 실패: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to fetch user display name", error.toException());
                    Toast.makeText(ActivityDetailActivity.this, "사용자 정보 로드 실패", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void navigateToChatRoom(ChatRoom chatRoom) {
        // Navigate to chat room
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("chat_room_id", chatRoom.getId());
        intent.putExtra("chat_room", chatRoom);
        startActivity(intent);

        Toast.makeText(this, "채팅방에 참여했습니다!", Toast.LENGTH_SHORT).show();
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
                color = Color.parseColor("#FFD300"); // Yellow
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Firebase listener for this activity
        FirebaseActivityManager.getInstance().removeActivityListener(activityId);
    }
}
