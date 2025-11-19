package com.example.connectmate;

import java.util.HashMap;
import java.util.Map;

/**
 * User model class for Firebase Realtime Database
 * Represents a user profile with authentication and social login information
 */
public class User {
    public String userId;           // Firebase Auth UID or social login ID
    public String email;
    public String displayName;
    public String username;
    public String profileImageUrl;
    public String loginMethod;      // "firebase", "google", "kakao", "naver"
    public String bio;
    public String mbti;
    public boolean profileCompleted;  // Whether user has completed profile setup
    public double rating;
    public int activitiesCount;
    public int connectionsCount;
    public int badgesCount;
    public long createdAt;
    public long lastLoginAt;
    public Map<String, Boolean> friends = new HashMap<>(); // Key: friend's user ID, Value: true
    public Map<String, Boolean> friendRequests = new HashMap<>(); // Key: user ID who sent the request, Value: true

    // Required empty constructor for Firebase Realtime Database
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
        this.profileCompleted = false;
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
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getLoginMethod() {
        return loginMethod;
    }

    public void setLoginMethod(String loginMethod) {
        this.loginMethod = loginMethod;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getMbti() {
        return mbti;
    }

    public void setMbti(String mbti) {
        this.mbti = mbti;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getActivitiesCount() {
        return activitiesCount;
    }

    public void setActivitiesCount(int activitiesCount) {
        this.activitiesCount = activitiesCount;
    }

    public int getConnectionsCount() {
        return connectionsCount;
    }

    public void setConnectionsCount(int connectionsCount) {
        this.connectionsCount = connectionsCount;
    }

    public int getBadgesCount() {
        return badgesCount;
    }

    public void setBadgesCount(int badgesCount) {
        this.badgesCount = badgesCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public Map<String, Boolean> getFriends() {
        return friends;
    }

    public void setFriends(Map<String, Boolean> friends) {
        this.friends = friends;
    }

    public Map<String, Boolean> getFriendRequests() {
        return friendRequests;
    }

    public void setFriendRequests(Map<String, Boolean> friendRequests) {
        this.friendRequests = friendRequests;
    }
}
