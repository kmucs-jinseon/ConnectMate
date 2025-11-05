package com.example.connectmate.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.connectmate.models.Activity;
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

    private static ActivityManager instance;

    private ActivityManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
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

        // Return sample data if no activities found
        return getSampleActivities();
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
     * Delete an activity
     */
    public boolean deleteActivity(String id) {
        try {
            List<Activity> activities = getAllActivities();
            for (int i = 0; i < activities.size(); i++) {
                if (activities.get(i).getId().equals(id)) {
                    activities.remove(i);

                    String json = gson.toJson(activities);
                    prefs.edit().putString(KEY_ACTIVITIES, json).apply();

                    Log.d(TAG, "Activity deleted: " + id);
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

    /**
     * Get sample activities for first-time use - with Seoul coordinates
     */
    private List<Activity> getSampleActivities() {
        List<Activity> sampleActivities = new ArrayList<>();

        Activity activity1 = new Activity(
            "1", "Weekly Soccer Match", "Seoul National Park",
            "Today, 3:00 PM", "Join us for a friendly soccer match!",
            5, 10, "운동"
        );
        activity1.setLatitude(37.5665);  // Seoul City Hall area
        activity1.setLongitude(126.9780);
        sampleActivities.add(activity1);

        Activity activity2 = new Activity(
            "2", "Study Group - Java", "Gangnam Library",
            "Tomorrow, 2:00 PM", "Let's study Java together",
            3, 8, "스터디"
        );
        activity2.setLatitude(37.5172);  // Gangnam area
        activity2.setLongitude(127.0473);
        sampleActivities.add(activity2);

        Activity activity3 = new Activity(
            "3", "Coffee Meetup", "Hongdae Cafe",
            "Saturday, 4:00 PM", "Casual coffee and chat",
            6, 12, "소셜"
        );
        activity3.setLatitude(37.5563);  // Hongdae area
        activity3.setLongitude(126.9238);
        sampleActivities.add(activity3);

        Activity activity4 = new Activity(
            "4", "Morning Yoga", "Han River Park",
            "Sunday, 7:00 AM", "Start your day with yoga and meditation",
            8, 15, "운동"
        );
        activity4.setLatitude(37.5286);  // Yeouido Han River Park
        activity4.setLongitude(126.9260);
        sampleActivities.add(activity4);

        Activity activity5 = new Activity(
            "5", "Python Workshop", "Tech Hub Seoul",
            "Next Friday, 6:00 PM", "Learn Python basics and build a project",
            12, 20, "스터디"
        );
        activity5.setLatitude(37.5642);  // Mapo/Tech area
        activity5.setLongitude(126.9770);
        sampleActivities.add(activity5);

        Log.d(TAG, "Returning " + sampleActivities.size() + " sample activities with Seoul coordinates");
        return sampleActivities;
    }
}
