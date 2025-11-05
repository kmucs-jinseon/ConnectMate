package com.example.connectmate.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatRoom model representing a group chat linked to an activity or location.
 */
public class ChatRoom implements Serializable {
    private String id;
    private String name;
    private String lastMessage;
    private long lastMessageTimestamp;
    private long createdTimestamp;
    private String creatorId; // ID of the user who created the room

    // Location fields
    private double latitude;
    private double longitude;

    // Member info
    private List<String> memberIds;

    // Default constructor for Firebase
    public ChatRoom() {
        this.memberIds = new ArrayList<>();
    }

    // Constructor for creating a new room with location
    public ChatRoom(String id, String name, String creatorId, String lastMessage, long timestamp, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.creatorId = creatorId;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = timestamp;
        this.createdTimestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.memberIds = new ArrayList<>();
        // The creator is also a member
        this.memberIds.add(creatorId);
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

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
    
    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public List<String> getMemberIds() {
        // Ensure memberIds is never null
        if (memberIds == null) {
            memberIds = new ArrayList<>();
        }
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    // Helper methods
    public void addMember(String userId) {
        if (memberIds == null) {
            memberIds = new ArrayList<>();
        }
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
        }
    }

    public void removeMember(String userId) {
        if (memberIds != null) {
            memberIds.remove(userId);
        }
    }
}
