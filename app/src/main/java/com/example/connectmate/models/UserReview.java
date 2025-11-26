package com.example.connectmate.models;

/**
 * Represents a single review submitted for a user.
 */
public class UserReview {
    private String reviewId;
    private String reviewerId;
    private String reviewerName;
    private String activityId;
    private String activityTitle;
    private int rating; // 1-5
    private String comment;
    private long timestamp;

    public UserReview() {
        // Required for Firebase
    }

    public UserReview(String reviewId, String reviewerId, String reviewerName,
                      String activityId, String activityTitle, int rating,
                      String comment, long timestamp) {
        this.reviewId = reviewId;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.activityId = activityId;
        this.activityTitle = activityTitle;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
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

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
