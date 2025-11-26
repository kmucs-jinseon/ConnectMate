package com.example.connectmate.models;

/**
 * Represents a pending review task for a user.
 * reviewerId is implied by the parent node path and is not stored here.
 */
public class PendingReviewItem {
    private String id;
    private String targetUserId;
    private String activityId;
    private String activityTitle;
    private long timestamp;
    private String status; // e.g. "pending", "completed"

    // Populated on the client for UI purposes
    private String targetDisplayName;
    private String targetProfileImageUrl;

    public PendingReviewItem() {
        // Required for Firebase
    }

    public PendingReviewItem(String id, String targetUserId, String activityId,
                             String activityTitle, long timestamp, String status) {
        this.id = id;
        this.targetUserId = targetUserId;
        this.activityId = activityId;
        this.activityTitle = activityTitle;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getActivityTitle() {
        return activityTitle;
    }

    public void setActivityTitle(String activityTitle) {
        this.activityTitle = activityTitle;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTargetDisplayName() {
        return targetDisplayName;
    }

    public void setTargetDisplayName(String targetDisplayName) {
        this.targetDisplayName = targetDisplayName;
    }

    public String getTargetProfileImageUrl() {
        return targetProfileImageUrl;
    }

    public void setTargetProfileImageUrl(String targetProfileImageUrl) {
        this.targetProfileImageUrl = targetProfileImageUrl;
    }
}
