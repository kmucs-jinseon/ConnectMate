package com.example.connectmate.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.connectmate.models.Activity;
import com.example.connectmate.models.ChatMessage;
import com.example.connectmate.models.ChatRoom;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager class for handling Activity data persistence using SharedPreferences
 */
public class ActivityManager {
    private static final String TAG = "ActivityManager";
    private static final String PREF_NAME = "ConnectMateActivities";
    private static final String KEY_ACTIVITIES = "activities";

    private final SharedPreferences prefs;
    private final Gson gson;
    private final Context context;

    private static ActivityManager instance;

    private ActivityManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Get singleton instance
     */
    public static synchronized ActivityManager getInstance(Context context) {
        if (instance == null) {
            instance = new ActivityManager(context);
        }
        return instance;
    }

    /**
     * Save a new activity
     */
    public boolean saveActivity(Activity activity) {
        try {
            List<Activity> activities = getAllActivities();
            activities.add(0, activity); // Add to the beginning of the list

            String json = gson.toJson(activities);
            prefs.edit().putString(KEY_ACTIVITIES, json).apply();

            Log.d(TAG, "Activity saved: " + activity.getTitle());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving activity", e);
            return false;
        }
    }

    /**
     * Get all activities
     */
    public List<Activity> getAllActivities() {
        try {
            String json = prefs.getString(KEY_ACTIVITIES, null);
            if (json != null) {
                Type listType = new TypeToken<ArrayList<Activity>>(){}.getType();
                List<Activity> activities = gson.fromJson(json, listType);
                Log.d(TAG, "Loaded " + activities.size() + " activities");
                return activities;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading activities", e);
        }

        // Return empty list for new users
        return new ArrayList<>();
    }

    /**
     * Get activity by ID
     */
    public Activity getActivityById(String id) {
        List<Activity> activities = getAllActivities();
        for (Activity activity : activities) {
            if (activity.getId().equals(id)) {
                return activity;
            }
        }
        return null;
    }

    /**
     * Update an existing activity
     */
    public boolean updateActivity(Activity updatedActivity) {
        try {
            List<Activity> activities = getAllActivities();
            for (int i = 0; i < activities.size(); i++) {
                if (activities.get(i).getId().equals(updatedActivity.getId())) {
                    activities.set(i, updatedActivity);

                    String json = gson.toJson(activities);
                    prefs.edit().putString(KEY_ACTIVITIES, json).apply();

                    Log.d(TAG, "Activity updated: " + updatedActivity.getTitle());
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating activity", e);
        }
        return false;
    }

    /**
     * Delete an activity and its associated chat room.
     */
    public boolean deleteActivity(String activityId) {
        try {
            ChatManager chatManager = ChatManager.getInstance(context);
            ChatRoom chatRoom = chatManager.getChatRoomByActivityId(activityId);

            if (chatRoom != null) {
                // Send a final message to the chat room
                String farewellMessage = "채팅방이 삭제되었습니다";
                ChatMessage systemMessage = ChatMessage.createSystemMessage(chatRoom.getId(), farewellMessage);
                chatManager.sendMessage(systemMessage);

                // Delete the chat room itself
                chatManager.deleteChatRoom(chatRoom.getId());
            }

            List<Activity> activities = getAllActivities();
            for (int i = 0; i < activities.size(); i++) {
                if (activities.get(i).getId().equals(activityId)) {
                    activities.remove(i);
                    String json = gson.toJson(activities);
                    prefs.edit().putString(KEY_ACTIVITIES, json).commit(); // Use commit for consistency
                    Log.d(TAG, "Activity deleted: " + activityId);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting activity", e);
        }
        return false;
    }


    /**
     * Get activities by category
     */
    public List<Activity> getActivitiesByCategory(String category) {
        List<Activity> allActivities = getAllActivities();
        List<Activity> filteredActivities = new ArrayList<>();

        for (Activity activity : allActivities) {
            if (activity.getCategory() != null && activity.getCategory().equals(category)) {
                filteredActivities.add(activity);
            }
        }

        return filteredActivities;
    }

    /**
     * Search activities by query
     */
    public List<Activity> searchActivities(String query) {
        List<Activity> allActivities = getAllActivities();
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

        return results;
    }

    /**
     * Clear all activities
     */
    public void clearAllActivities() {
        prefs.edit().remove(KEY_ACTIVITIES).apply();
        Log.d(TAG, "All activities cleared");
    }
}
