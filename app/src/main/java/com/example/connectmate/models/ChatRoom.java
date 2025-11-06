package com.example.connectmate.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ChatRoom model representing a group chat linked to an activity
 */
public class ChatRoom implements Serializable {
    private String id;
    private String name;
    private String activityId;
    private String lastMessage;
    private long lastMessageTime;
    private List<String> memberIds;
    private List<String> memberNames;
    private int unreadCount;
    private String profileImageUrl;
    private long createdTimestamp;

    // Default constructor
    public ChatRoom() {
        this.id = UUID.randomUUID().toString();
        this.memberIds = new ArrayList<>();
        this.memberNames = new ArrayList<>();
        this.createdTimestamp = System.currentTimeMillis();
        this.lastMessageTime = System.currentTimeMillis();
        this.unreadCount = 0;
    }

    // Constructor with essential fields
    public ChatRoom(String name, String activityId) {
        this();
        this.name = name;
        this.activityId = activityId;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public List<String> getMemberNames() {
        return memberNames;
    }

    public void setMemberNames(List<String> memberNames) {
        this.memberNames = memberNames;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    /**
     * Add a member to the chat room
     */
    public void addMember(String memberId, String memberName) {
        if (!memberIds.contains(memberId)) {
            memberIds.add(memberId);
            memberNames.add(memberName);
        }
    }

    /**
     * Remove a member from the chat room
     */
    public void removeMember(String memberId) {
        int index = memberIds.indexOf(memberId);
        if (index >= 0) {
            memberIds.remove(index);
            if (index < memberNames.size()) {
                memberNames.remove(index);
            }
        }
    }

    /**
     * Get member count
     */
    public int getMemberCount() {
        return memberIds.size();
    }
}
