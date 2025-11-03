package com.example.connectmate.models;

import java.io.Serializable;
import java.util.UUID;

/**
 * Activity model representing a community activity/event
 */
public class Activity implements Serializable {
    private String id;
    private String title;
    private String description;
    private String category;
    private String date;
    private String time;
    private String location;
    private int currentParticipants;
    private int maxParticipants;
    private String visibility;
    private String hashtags;
    private String creatorId;
    private String creatorName;
    private long createdTimestamp;

    // Default constructor
    public Activity() {
        this.id = UUID.randomUUID().toString();
        this.createdTimestamp = System.currentTimeMillis();
    }

    // Constructor with essential fields
    public Activity(String title, String description, String category, String date, String time,
                   String location, int maxParticipants, String visibility, String hashtags,
                   String creatorId, String creatorName) {
        this();
        this.title = title;
        this.description = description;
        this.category = category;
        this.date = date;
        this.time = time;
        this.location = location;
        this.currentParticipants = 0;
        this.maxParticipants = maxParticipants;
        this.visibility = visibility;
        this.hashtags = hashtags;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
    }

    // Constructor for compatibility with existing code
    public Activity(String id, String title, String location, String time,
                   String description, int currentParticipants, int maxParticipants,
                   String category) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.time = time;
        this.description = description;
        this.currentParticipants = currentParticipants;
        this.maxParticipants = maxParticipants;
        this.category = category;
        this.createdTimestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getCurrentParticipants() {
        return currentParticipants;
    }

    public void setCurrentParticipants(int currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getHashtags() {
        return hashtags;
    }

    public void setHashtags(String hashtags) {
        this.hashtags = hashtags;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    /**
     * Get formatted date and time string
     */
    public String getDateTime() {
        if (date != null && time != null) {
            return date + " " + time;
        } else if (date != null) {
            return date;
        } else if (time != null) {
            return time;
        }
        return "";
    }
}
