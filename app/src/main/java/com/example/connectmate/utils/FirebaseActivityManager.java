package com.example.connectmate.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.connectmate.models.Activity;
import com.example.connectmate.models.ChatRoom;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase-based Activity Manager with real-time multi-user sync
 * Replaces SharedPreferences-based ActivityManager
 */
public class FirebaseActivityManager {
    private static final String TAG = "FirebaseActivityManager";

    // Firebase paths
    private static final String PATH_ACTIVITIES = "activities";
    private static final String PATH_USER_ACTIVITIES = "userActivities";
    private static final String PATH_PARTICIPANTS = "participants";
    private static final String PATH_USER_NOTIFICATIONS = "userNotifications";
    private static final String PATH_USERS = "users";

    private final DatabaseReference activitiesRef;
    private final DatabaseReference userActivitiesRef;
    private final DatabaseReference usersRef;
    private final DatabaseReference userNotificationsRef;
    private final FirebaseAuth auth;

    private static FirebaseActivityManager instance;

    // Listeners for real-time updates
    private final Map<String, ActivityListener> activityListeners = new HashMap<>();
    private ChildEventListener activitiesChildListener;

    private FirebaseActivityManager() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // Enable offline persistence
        try {
            database.setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.w(TAG, "Persistence already enabled or failed to enable", e);
        }

        activitiesRef = database.getReference(PATH_ACTIVITIES);
        userActivitiesRef = database.getReference(PATH_USER_ACTIVITIES);
        usersRef = database.getReference(PATH_USERS);
        userNotificationsRef = database.getReference(PATH_USER_NOTIFICATIONS);
        auth = FirebaseAuth.getInstance();

        // Keep data synced locally
        activitiesRef.keepSynced(true);
        userActivitiesRef.keepSynced(true);
        usersRef.keepSynced(true);
        userNotificationsRef.keepSynced(true);
    }

    /**
     * Get singleton instance
     */
    public static synchronized FirebaseActivityManager getInstance() {
        if (instance == null) {
            instance = new FirebaseActivityManager();
        }
        return instance;
    }

    /**
     * Save a new activity to Firebase
     */
    public void saveActivity(Activity activity, OnCompleteListener<Activity> listener) {
        if (activity.getId() == null || activity.getId().isEmpty()) {
            String id = activitiesRef.push().getKey();
            activity.setId(id);
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && activity.getCreatorId() == null) {
            activity.setCreatorId(currentUser.getUid());
            String creatorName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : currentUser.getEmail();
            activity.setCreatorName(creatorName);
        }

        if (activity.getCreatedTimestamp() == 0) {
            activity.setCreatedTimestamp(System.currentTimeMillis());
        }

        // Step 1: Save the main activity data
        activitiesRef.child(activity.getId()).setValue(activity)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Activity data saved successfully: " + activity.getTitle());

                    // Step 2: Automatically join the creator to the activity and create chat room
                    joinActivityAndCreateChat(activity, listener);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving activity data", e);
                    if (listener != null) {
                        listener.onError(e);
                    }
                });
    }

    private void joinActivityAndCreateChat(Activity activity, OnCompleteListener<Activity> finalListener) {
        // Get user info from the activity (already set by the creator)
        String userId = activity.getCreatorId();
        String userName = activity.getCreatorName();

        if (userId == null || userId.isEmpty()) {
            if (finalListener != null) finalListener.onError(new Exception("User not authenticated."));
            return;
        }

        // Step 2a: Add creator as a participant
        addParticipant(activity.getId(), userId, userName, new OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Creator successfully added as participant.");

                // Update the activity object's participant count to reflect the creator being added
                activity.setCurrentParticipants(1);

                // Step 2b: Create the chat room and add the creator as a member
                FirebaseChatManager chatManager = FirebaseChatManager.getInstance();
                chatManager.createOrGetChatRoom(
                        activity.getId(),
                        activity.getTitle(),
                        activity.getCategory(),
                        activity.getCreatorId(),
                        activity.getCreatorName(),
                        new FirebaseChatManager.OnCompleteListener<ChatRoom>() {
                    @Override
                    public void onSuccess(ChatRoom chatRoom) {
                        Log.d(TAG, "Chat room created or retrieved: " + chatRoom.getId());

                        // Step 2c: Add creator to the chat room members
                        chatManager.addMemberToChatRoom(chatRoom.getId(), userId, userName, new FirebaseChatManager.OnCompleteListener<Void>() {
                            @Override
                            public void onSuccess(Void memberResult) {
                                Log.d(TAG, "Creator added to chat room members.");
                                // All steps successful
                                if (finalListener != null) {
                                    finalListener.onSuccess(activity);
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Failed to add creator to chat room members", e);
                                if (finalListener != null) {
                                    finalListener.onError(e);
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Failed to create or get chat room", e);
                        if (finalListener != null) {
                            finalListener.onError(e);
                        }
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to add creator as participant", e);
                if (finalListener != null) {
                    finalListener.onError(e);
                }
            }
        });
    }

    // ... (The rest of the FirebaseActivityManager methods remain unchanged) ...

    /**
     * Get all activities with real-time updates
     */
    public void getAllActivities(ActivityListListener listener) {
        activitiesRef.orderByChild("createdTimestamp")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Activity> activities = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Activity activity = child.getValue(Activity.class);
                        if (activity != null) {
                            activities.add(0, activity); // Add to beginning (newest first)
                        }
                    }

                    Log.d(TAG, "Loaded " + activities.size() + " activities from Firebase");
                    listener.onActivitiesLoaded(activities);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading activities", error.toException());
                    listener.onError(error.toException());
                }
            });
    }

    /**
     * Listen for activity changes in real-time (add, update, remove)
     */
    public void listenForActivityChanges(ActivityChangeListener listener) {
        // Remove previous listener if exists
        if (activitiesChildListener != null) {
            activitiesRef.removeEventListener(activitiesChildListener);
        }

        activitiesChildListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Activity activity = snapshot.getValue(Activity.class);
                if (activity != null) {
                    listener.onActivityAdded(activity);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Activity activity = snapshot.getValue(Activity.class);
                if (activity != null) {
                    listener.onActivityChanged(activity);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Activity activity = snapshot.getValue(Activity.class);
                if (activity != null) {
                    listener.onActivityRemoved(activity);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Not used for activities
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.toException());
            }
        };

        activitiesRef.addChildEventListener(activitiesChildListener);
    }

    /**
     * Get activity by ID
     */
    public void getActivityById(String id, OnCompleteListener<Activity> listener) {
        activitiesRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Activity activity = snapshot.getValue(Activity.class);
                if (activity != null && listener != null) {
                    listener.onSuccess(activity);
                } else if (listener != null) {
                    listener.onError(new Exception("Activity not found"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) {
                    listener.onError(error.toException());
                }
            }
        });
    }

    /**
     * Listen to a specific activity for real-time updates
     */
    public void listenToActivity(String activityId, ActivityListener listener) {
        ValueEventListener valueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Activity activity = snapshot.getValue(Activity.class);
                if (activity != null) {
                    listener.onActivityUpdated(activity);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.toException());
            }
        };

        activitiesRef.child(activityId).addValueEventListener(valueListener);
        activityListeners.put(activityId, listener);
    }

    /**
     * Update an existing activity
     * Only updates editable fields, preserving currentParticipants and participants list
     */
    public void updateActivity(Activity activity, OnCompleteListener<Activity> listener) {
        if (activity.getId() == null) {
            if (listener != null) {
                listener.onError(new Exception("Activity ID is required for update"));
            }
            return;
        }

        // Create a map of only the fields that should be updated
        // This preserves currentParticipants and participants list
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", activity.getTitle());
        updates.put("description", activity.getDescription());
        updates.put("category", activity.getCategory());
        updates.put("date", activity.getDate());
        updates.put("time", activity.getTime());
        updates.put("location", activity.getLocation());
        updates.put("maxParticipants", activity.getMaxParticipants());
        updates.put("visibility", activity.getVisibility());
        updates.put("hashtags", activity.getHashtags());
        updates.put("latitude", activity.getLatitude());
        updates.put("longitude", activity.getLongitude());
        // Note: We do NOT update currentParticipants or participants - these are managed separately

        activitiesRef.child(activity.getId()).updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Activity updated: " + activity.getTitle());
                if (listener != null) {
                    listener.onSuccess(activity);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating activity", e);
                if (listener != null) {
                    listener.onError(e);
                }
            });
    }

    /**
     * Delete an activity and its associated chat room.
     */
    public void deleteActivity(String activityId, OnCompleteListener<Void> listener) {
        deleteActivity(activityId, false, null, listener);
    }

    public void deleteActivity(String activityId, boolean incrementParticipationCount, OnCompleteListener<Void> listener) {
        deleteActivity(activityId, incrementParticipationCount, ActivityDeletionMode.SILENT, null, listener);
    }

    public void deleteActivity(String activityId, boolean incrementParticipationCount, @Nullable String activityTitle, OnCompleteListener<Void> listener) {
        deleteActivity(activityId, incrementParticipationCount, ActivityDeletionMode.WITH_NOTIFICATIONS, activityTitle, listener);
    }

    public void deleteActivity(String activityId, boolean incrementParticipationCount, ActivityDeletionMode mode, @Nullable String activityTitle, OnCompleteListener<Void> listener) {
        Log.d(TAG, "🗑️ deleteActivity() called for activityId: " + activityId);

        activitiesRef.child(activityId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String resolvedTitle = activityTitle;
                    if (TextUtils.isEmpty(resolvedTitle)) {
                        resolvedTitle = snapshot.child("title").getValue(String.class);
                    }

                    DataSnapshot participantsSnapshot = snapshot.child(PATH_PARTICIPANTS);
                    // Store participant IDs for cleanup
                    List<String> participantIds = new ArrayList<>();
                    for (DataSnapshot child : participantsSnapshot.getChildren()) {
                        String userId = child.getKey();
                        if (userId != null) {
                            participantIds.add(userId);
                        }
                    }
                    Log.d(TAG, "Found " + participantIds.size() + " participants to clean up");

                    if (incrementParticipationCount && !participantIds.isEmpty()) {
                        incrementParticipationCounts(participantIds);
                        if (mode == ActivityDeletionMode.WITH_NOTIFICATIONS) {
                            addNotificationsForParticipants(resolvedTitle, participantIds);
                        }
                    }

                    // Step 2: Delete the chat room
                    deleteChatRoomForActivity(activityId, () -> {
                        // Step 3: Delete the activity
                        activitiesRef.child(activityId).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Activity deleted from Firebase: " + activityId);

                                // Step 4: Clean up userActivities references
                                for (String userId : participantIds) {
                                    userActivitiesRef.child(userId).child(activityId).removeValue()
                                        .addOnSuccessListener(v -> Log.d(TAG, "✅ Cleaned up userActivity for user: " + userId))
                                        .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to clean up userActivity for user: " + userId, e));
                                }

                                if (listener != null) {
                                    listener.onSuccess(null);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "❌ Error deleting activity from Firebase", e);
                                if (listener != null) {
                                    listener.onError(e);
                                }
                            });
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "❌ Failed to get participants", error.toException());
                    // Still try to delete even if we can't get participants
                    deleteChatRoomForActivity(activityId, () -> {
                        activitiesRef.child(activityId).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                if (listener != null) listener.onSuccess(null);
                            })
                            .addOnFailureListener(e -> {
                                if (listener != null) listener.onError(e);
                            });
                    });
                }
            });
    }

    private void incrementParticipationCounts(List<String> participantIds) {
        for (String userId : participantIds) {
            if (userId == null || userId.isEmpty()) {
                continue;
            }
            usersRef.child(userId).child("participationCount").runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                    Long current = currentData.getValue(Long.class);
                    if (current == null) {
                        current = 0L;
                    }
                    currentData.setValue(current + 1);
                    return Transaction.success(currentData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                    if (error != null) {
                        Log.e(TAG, "Failed to increment participation count for user: " + userId, error.toException());
                    }
                }
            });
        }
    }

    private void addNotificationsForParticipants(@Nullable String activityTitle, List<String> participantIds) {
        String title = !TextUtils.isEmpty(activityTitle) ? activityTitle : "활동";
        String endMessage = title + " 활동이 종료되었습니다.";
        String reviewMessage = "함께한 멤버를 평가해주세요.";

        for (String userId : participantIds) {
            if (userId == null || userId.isEmpty()) continue;
            DatabaseReference userRef = userNotificationsRef.child(userId);
            long timestamp = System.currentTimeMillis();

            String endId = userRef.push().getKey();
            if (endId != null) {
                Map<String, Object> endNotif = new HashMap<>();
                endNotif.put("id", endId);
                endNotif.put("title", "활동 종료");
                endNotif.put("message", endMessage);
                endNotif.put("timestamp", timestamp);
                userRef.child(endId).setValue(endNotif);
            }

            String reviewId = userRef.push().getKey();
            if (reviewId != null) {
                Map<String, Object> reviewNotif = new HashMap<>();
                reviewNotif.put("id", reviewId);
                reviewNotif.put("title", "참여자 평가 요청");
                reviewNotif.put("message", reviewMessage);
                reviewNotif.put("timestamp", timestamp + 1);
                userRef.child(reviewId).setValue(reviewNotif);
            }
        }
    }

    public enum ActivityDeletionMode {
        SILENT,
        WITH_NOTIFICATIONS
    }

    /**
     * Helper method to delete chat room associated with an activity
     */
    private void deleteChatRoomForActivity(String activityId, Runnable onComplete) {
        FirebaseChatManager chatManager = FirebaseChatManager.getInstance();
        chatManager.getChatRoomByActivityId(activityId, new FirebaseChatManager.OnCompleteListener<ChatRoom>() {
            @Override
            public void onSuccess(ChatRoom chatRoom) {
                if (chatRoom != null) {
                    Log.d(TAG, "Found chat room to delete: " + chatRoom.getId());
                    chatManager.deleteChatRoom(chatRoom.getId(), new FirebaseChatManager.OnCompleteListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "✅ Chat room deleted: " + chatRoom.getId());
                            onComplete.run();
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "❌ Failed to delete chat room, but continuing: " + e.getMessage());
                            onComplete.run();
                        }
                    });
                } else {
                    Log.d(TAG, "No chat room found for activity: " + activityId);
                    onComplete.run();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Error finding chat room, but continuing: " + e.getMessage());
                onComplete.run();
            }
        });
    }

    /**
     * Get activities by category
     */
    public void getActivitiesByCategory(String category, ActivityListListener listener) {
        activitiesRef.orderByChild("category").equalTo(category)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Activity> activities = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Activity activity = child.getValue(Activity.class);
                        if (activity != null) {
                            activities.add(activity);
                        }
                    }
                    listener.onActivitiesLoaded(activities);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    listener.onError(error.toException());
                }
            });
    }

    /**
     * Get activities by location with tolerance for floating-point comparison.
     * Uses a small epsilon value to account for GPS coordinate precision.
     */
    public void getActivitiesByLocation(double latitude, double longitude, OnCompleteListener<List<Activity>> listener) {
        // Small tolerance for double comparison (approximately 1 meter)
        final double EPSILON = 0.00001;

        // Firebase doesn't support range queries on multiple fields, so we need to fetch all activities
        // and filter by both latitude and longitude with tolerance
        activitiesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Activity> matchingActivities = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Activity activity = snapshot.getValue(Activity.class);
                    // Check if both latitude and longitude match within tolerance
                    if (activity != null &&
                        Math.abs(activity.getLatitude() - latitude) < EPSILON &&
                        Math.abs(activity.getLongitude() - longitude) < EPSILON) {
                        matchingActivities.add(activity);
                        Log.d(TAG, "Found activity at location: " + activity.getTitle() +
                              " (" + activity.getLatitude() + ", " + activity.getLongitude() + ")");
                    }
                }
                Log.d(TAG, "Found " + matchingActivities.size() + " activities at location (" + latitude + ", " + longitude + ")");
                if (listener != null) {
                    listener.onSuccess(matchingActivities);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (listener != null) {
                    listener.onError(databaseError.toException());
                }
            }
        });
    }

    /**
     * Search activities (Note: Firebase doesn't support full-text search,
     * so we load all and filter locally. For production, consider using Algolia or Elasticsearch)
     */
    public void searchActivities(String query, ActivityListListener listener) {
        getAllActivities(new ActivityListListener() {
            @Override
            public void onActivitiesLoaded(List<Activity> allActivities) {
                List<Activity> results = new ArrayList<>();
                String lowerQuery = query.toLowerCase();

                for (Activity activity : allActivities) {
                    if ((activity.getTitle() != null && activity.getTitle().toLowerCase().contains(lowerQuery)) ||
                        (activity.getLocation() != null && activity.getLocation().toLowerCase().contains(lowerQuery)) ||
                        (activity.getDescription() != null && activity.getDescription().toLowerCase().contains(lowerQuery)) ||
                        (activity.getCategory() != null && activity.getCategory().toLowerCase().contains(lowerQuery))) {
                        results.add(activity);
                    }
                }

                listener.onActivitiesLoaded(results);
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e);
            }
        });
    }

    /**
     * Get activities created by a specific user
     */
    public void getUserActivities(String userId, ActivityListListener listener) {
        userActivitiesRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Activity> activities = new ArrayList<>();
                int[] pendingLoads = {(int) snapshot.getChildrenCount()};

                if (pendingLoads[0] == 0) {
                    listener.onActivitiesLoaded(activities);
                    return;
                }

                for (DataSnapshot child : snapshot.getChildren()) {
                    String activityId = child.getKey();
                    getActivityById(activityId, new OnCompleteListener<Activity>() {
                        @Override
                        public void onSuccess(Activity activity) {
                            activities.add(activity);
                            pendingLoads[0]--;
                            if (pendingLoads[0] == 0) {
                                listener.onActivitiesLoaded(activities);
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            pendingLoads[0]--;
                            if (pendingLoads[0] == 0) {
                                listener.onActivitiesLoaded(activities);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.toException());
            }
        });
    }

    /**
     * Add a participant to an activity
     */
    public void addParticipant(String activityId, String userId, String userName,
                              OnCompleteListener<Void> listener) {
        activitiesRef.child(activityId).child(PATH_PARTICIPANTS).child(userId)
            .setValue(userName)
            .addOnSuccessListener(aVoid -> {
                // Update participant count
                incrementParticipantCount(activityId, 1);

                // Add to user's activities
                userActivitiesRef.child(userId).child(activityId).setValue(true);

                Log.d(TAG, "Participant added to activity: " + activityId);
                if (listener != null) {
                    listener.onSuccess(null);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding participant", e);
                if (listener != null) {
                    listener.onError(e);
                }
            });
    }

    /**
     * Remove a participant from an activity
     */
    public void removeParticipant(String activityId, String userId, OnCompleteListener<Void> listener) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference activitiesRef = rootRef.child("activities");
        DatabaseReference chatRoomsRef = rootRef.child("chatRooms");

        Log.d(TAG, "🔹 removeParticipant() called for user: " + userId + " in activity: " + activityId);

        // 1️⃣ 활동 참가자 제거
        activitiesRef.child(activityId).child("participants").child(userId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ 참가자 제거 완료 (activities)");

                    // 2️⃣ userActivities에서도 제거
                    rootRef.child("userActivities").child(userId).child(activityId).removeValue();

                    // 3️⃣ 채팅방 멤버 제거
                    chatRoomsRef.orderByChild("activityId").equalTo(activityId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (!snapshot.exists()) {
                                        Log.w(TAG, "⚠️ No chat room found for activityId: " + activityId);
                                        if (listener != null) listener.onSuccess(null);
                                        return;
                                    }

                                    for (DataSnapshot chatRoomSnapshot : snapshot.getChildren()) {
                                        String chatRoomId = chatRoomSnapshot.getKey();
                                        if (chatRoomId == null) continue;

                                        DatabaseReference membersRef = chatRoomsRef.child(chatRoomId).child("members");
                                        membersRef.child(userId).removeValue().addOnSuccessListener(v -> {
                                            Log.d(TAG, "✅ " + userId + " removed from chat room members");

                                            // 4️⃣ 남은 멤버 수 확인 → 0명이면 방 삭제
                                            membersRef.get().addOnSuccessListener(membersSnapshot -> {
                                                long remaining = membersSnapshot.getChildrenCount();
                                                Log.d(TAG, "👥 Remaining members: " + remaining);

                                                if (remaining == 0) {
                                                    // 채팅방 삭제
                                                    chatRoomsRef.child(chatRoomId).removeValue()
                                                            .addOnSuccessListener(del -> {
                                                                Log.d(TAG, "🔥 Chat room deleted (no members left): " + chatRoomId);

                                                                // 관련 메시지 노드도 삭제
                                                                rootRef.child("messages").child(chatRoomId).removeValue()
                                                                        .addOnSuccessListener(msgDel -> Log.d(TAG, "🧹 Messages deleted for " + chatRoomId))
                                                                        .addOnFailureListener(e -> Log.e(TAG, "❌ Message delete failed", e));
                                                            })
                                                            .addOnFailureListener(e -> Log.e(TAG, "❌ Chat room delete failed", e));
                                                } else {
                                                    Log.d(TAG, "⏸ Chat room kept (members remain).");
                                                }
                                            });
                                        });
                                    }

                                    // 5️⃣ 참가자 수 감소
                                    incrementParticipantCount(activityId, -1);

                                    if (listener != null) listener.onSuccess(null);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "❌ Failed to access chat room for removal", error.toException());
                                    if (listener != null) listener.onError(error.toException());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to remove participant", e);
                    if (listener != null) listener.onError(e);
                });
    }


    /**
     * User leaves the activity (chat room)
     * Removes user from participants, and deletes the activity if no one remains.
     */
    public void leaveActivity(String activityId, String userId, OnCompleteListener<Void> listener) {
        DatabaseReference participantsRef = activitiesRef.child(activityId).child(PATH_PARTICIPANTS);

        participantsRef.child(userId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Update participant count
                    incrementParticipantCount(activityId, -1);

                    // Remove from user's activities
                    userActivitiesRef.child(userId).child(activityId).removeValue();

                    // Check if there are any participants left
                    participantsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                                // No participants left -> delete the entire activity
                                activitiesRef.child(activityId).removeValue()
                                        .addOnSuccessListener(unused -> {
                                            Log.d(TAG, "Activity deleted because no participants remain: " + activityId);
                                            if (listener != null) listener.onSuccess(null);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error deleting empty activity", e);
                                            if (listener != null) listener.onError(e);
                                        });
                            } else {
                                if (listener != null) listener.onSuccess(null);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error checking remaining participants", error.toException());
                            if (listener != null) listener.onError(error.toException());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error leaving activity", e);
                    if (listener != null) listener.onError(e);
                });
    }

    /**
     * Get participants for an activity
     */
    public void getParticipants(String activityId, ParticipantsListener listener) {
        activitiesRef.child(activityId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Map<String, String> participants = new HashMap<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String userId = child.getKey();
                        String userName = child.getValue(String.class);
                        if (userId != null && userName != null) {
                            participants.put(userId, userName);
                        }
                    }
                    listener.onParticipantsLoaded(participants);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    listener.onError(error.toException());
                }
            });
    }

    /**
     * Check if a user is already a participant
     */
    public void isUserParticipant(String activityId, String userId, OnCompleteListener<Boolean> listener) {
        activitiesRef.child(activityId).child(PATH_PARTICIPANTS).child(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean exists = snapshot.exists();
                    Log.d(TAG, "User " + userId + " is participant: " + exists);
                    if (listener != null) {
                        listener.onSuccess(exists);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error checking participant status", error.toException());
                    if (listener != null) {
                        listener.onError(error.toException());
                    }
                }
            });
    }

    public void deleteIfNoParticipants(String activityId) {
        activitiesRef.child(activityId).child(PATH_PARTICIPANTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.hasChildren()) {
                            activitiesRef.child(activityId).removeValue();
                            Log.d(TAG, "Empty activity deleted: " + activityId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error checking participants", error.toException());
                    }
                });
    }


    /**
     * Increment/decrement participant count
     */
    private void incrementParticipantCount(String activityId, int delta) {
        activitiesRef.child(activityId).child("currentParticipants")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer current = snapshot.getValue(Integer.class);
                    if (current == null) current = 0;
                    activitiesRef.child(activityId).child("currentParticipants")
                        .setValue(Math.max(0, current + delta));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error updating participant count", error.toException());
                }
            });
    }

    /**
     * Listen to participant count changes for a specific activity in realtime
     */
    public void listenToParticipantCount(String activityId, ParticipantCountListener listener) {
        activitiesRef.child(activityId).child("currentParticipants")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer count = snapshot.getValue(Integer.class);
                    if (count == null) count = 0;
                    listener.onCountChanged(count);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    listener.onError(error.toException());
                }
            });
    }

    /**
     * Get all activities with location information.
     */
    public void getActivitiesWithLocation(ActivityLocationListener listener) {
        activitiesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Map<String, Object>> activityLocations = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Activity activity = child.getValue(Activity.class);
                    if (activity != null && activity.getLatitude() != 0 && activity.getLongitude() != 0) {
                        Map<String, Object> locationData = new HashMap<>();
                        locationData.put("id", activity.getId());
                        locationData.put("latitude", activity.getLatitude());
                        locationData.put("longitude", activity.getLongitude());
                        activityLocations.add(locationData);
                    }
                }
                listener.onSuccess(activityLocations);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.toException());
            }
        });
    }

    /**
     * Remove all listeners
     */
    public void removeAllListeners() {
        if (activitiesChildListener != null) {
            activitiesRef.removeEventListener(activitiesChildListener);
        }
        activityListeners.clear();
    }

    // Callback interfaces
    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public interface ActivityListListener {
        void onActivitiesLoaded(List<Activity> activities);
        void onError(Exception e);
    }

    public interface ActivityLocationListener {
        void onSuccess(List<Map<String, Object>> activityLocations);
        void onError(Exception e);
    }

    public interface ActivityListener {
        void onActivityUpdated(Activity activity);
        void onError(Exception e);
    }

    public interface ActivityChangeListener {
        void onActivityAdded(Activity activity);
        void onActivityChanged(Activity activity);
        void onActivityRemoved(Activity activity);
        void onError(Exception e);
    }

    public interface ParticipantsListener {
        void onParticipantsLoaded(Map<String, String> participants);
        void onError(Exception e);
    }

    public interface ParticipantCountListener {
        void onCountChanged(int count);
        void onError(Exception e);
    }
}
