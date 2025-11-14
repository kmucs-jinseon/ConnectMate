package com.example.connectmate.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ChatRoom model representing a group chat linked to an activity
 */
public class ChatRoom implements Serializable {
    private String id;
    private String name;
    private String activityId;
    private String category; // Category of the associated activity
    private String lastMessage;
    private long lastMessageTime;
    private List<String> memberIds;
    private List<String> memberNames;
    private Map<String, Member> members; // Firebase structure for members
    private int unreadCount;
    private String profileImageUrl;
    private long createdTimestamp;

    /**
     * Member inner class for Firebase structure
     */
    public static class Member implements Serializable {
        private String name;
        private int unreadCount;

        public Member() {
        }

        public Member(String name, int unreadCount) {
            this.name = name;
            this.unreadCount = unreadCount;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getUnreadCount() {
            return unreadCount;
        }

        public void setUnreadCount(int unreadCount) {
            this.unreadCount = unreadCount;
        }
    }

    // Default constructor
    public ChatRoom() {
        this.id = UUID.randomUUID().toString();
        this.memberIds = new ArrayList<>();
        this.memberNames = new ArrayList<>();
        this.members = new HashMap<>();
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
        // If using new members map structure, convert to list
        if (members != null && !members.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Member member : members.values()) {
                if (member.getName() != null) {
                    names.add(member.getName());
                }
            }
            return names;
        }
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
     * Get a member's name by their ID
     */
    public String getMemberName(String memberId) {
        int index = memberIds.indexOf(memberId);
        if (index != -1 && index < memberNames.size()) {
            return memberNames.get(index);
        }
        return null; // Or a default name
    }

    /**
     * Get members map
     */
    public Map<String, Member> getMembers() {
        return members;
    }

    /**
     * Set members map
     */
    public void setMembers(Map<String, Member> members) {
        this.members = members;
    }

    /**
     * Get member count (uses members map if available, falls back to memberIds list)
     */
    public int getMemberCount() {
        if (members != null && !members.isEmpty()) {
            return members.size();
        }
        return memberIds != null ? memberIds.size() : 0;
    }
}
