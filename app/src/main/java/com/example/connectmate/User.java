package com.example.connectmate;

import com.google.firebase.firestore.PropertyName;

/**
 * User model class for Firestore
 * Represents a user profile with authentication and social login information
 */
public class User {
    private String userId;           // Firebase Auth UID or social login ID
    private String email;
    private String displayName;
    private String username;
    private String profileImageUrl;
    private String loginMethod;      // "firebase", "google", "kakao", "naver"
    private String bio;
    private String mbti;
    private double rating;
    private int activitiesCount;
    private int connectionsCount;
    private int badgesCount;
    private long createdAt;
    private long lastLoginAt;

    // Required empty constructor for Firestore
    public User() {
    }

    public User(String userId, String email, String displayName, String loginMethod) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.loginMethod = loginMethod;
        this.username = generateUsername(displayName);
        this.bio = "안녕하세요! 새로운 사람들과 함께 활동하는 것을 좋아합니다 ✨";
        this.mbti = "ENFP";
        this.rating = 4.8;
        this.activitiesCount = 0;
        this.connectionsCount = 0;
        this.badgesCount = 0;
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = System.currentTimeMillis();
    }

    private String generateUsername(String displayName) {
        if (displayName == null || displayName.isEmpty()) {
            return "user" + System.currentTimeMillis();
        }
        return displayName.toLowerCase().replace(" ", "").replaceAll("[^a-z0-9]", "");
    }

    // Getters and Setters
    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    @PropertyName("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("email")
    public String getEmail() {
        return email;
    }

    @PropertyName("email")
    public void setEmail(String email) {
        this.email = email;
    }

    @PropertyName("displayName")
    public String getDisplayName() {
        return displayName;
    }

    @PropertyName("displayName")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @PropertyName("username")
    public String getUsername() {
        return username;
    }

    @PropertyName("username")
    public void setUsername(String username) {
        this.username = username;
    }

    @PropertyName("profileImageUrl")
    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    @PropertyName("profileImageUrl")
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    @PropertyName("loginMethod")
    public String getLoginMethod() {
        return loginMethod;
    }

    @PropertyName("loginMethod")
    public void setLoginMethod(String loginMethod) {
        this.loginMethod = loginMethod;
    }

    @PropertyName("bio")
    public String getBio() {
        return bio;
    }

    @PropertyName("bio")
    public void setBio(String bio) {
        this.bio = bio;
    }

    @PropertyName("mbti")
    public String getMbti() {
        return mbti;
    }

    @PropertyName("mbti")
    public void setMbti(String mbti) {
        this.mbti = mbti;
    }

    @PropertyName("rating")
    public double getRating() {
        return rating;
    }

    @PropertyName("rating")
    public void setRating(double rating) {
        this.rating = rating;
    }

    @PropertyName("activitiesCount")
    public int getActivitiesCount() {
        return activitiesCount;
    }

    @PropertyName("activitiesCount")
    public void setActivitiesCount(int activitiesCount) {
        this.activitiesCount = activitiesCount;
    }

    @PropertyName("connectionsCount")
    public int getConnectionsCount() {
        return connectionsCount;
    }

    @PropertyName("connectionsCount")
    public void setConnectionsCount(int connectionsCount) {
        this.connectionsCount = connectionsCount;
    }

    @PropertyName("badgesCount")
    public int getBadgesCount() {
        return badgesCount;
    }

    @PropertyName("badgesCount")
    public void setBadgesCount(int badgesCount) {
        this.badgesCount = badgesCount;
    }

    @PropertyName("createdAt")
    public long getCreatedAt() {
        return createdAt;
    }

    @PropertyName("createdAt")
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("lastLoginAt")
    public long getLastLoginAt() {
        return lastLoginAt;
    }

    @PropertyName("lastLoginAt")
    public void setLastLoginAt(long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
