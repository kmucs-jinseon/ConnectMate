package com.example.connectmate.models;

import java.io.Serializable;
import java.util.UUID;

/**
 * ChatMessage model representing a message in a chat room
 */
public class ChatMessage implements Serializable {
    public static final int TYPE_TEXT = 0;
    public static final int TYPE_SYSTEM = 1;
    public static final int TYPE_IMAGE = 2;

    private String id;
    private String chatRoomId;
    private String senderId;
    private String senderName;
    private String senderProfileUrl;
    private String message;
    private int messageType;
    private long timestamp;
    private boolean isRead;

    // Default constructor
    public ChatMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.messageType = TYPE_TEXT;
        this.isRead = false;
    }

    // Constructor for text messages
    public ChatMessage(String chatRoomId, String senderId, String senderName, String message) {
        this();
        this.chatRoomId = chatRoomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.messageType = TYPE_TEXT;
    }

    // Constructor for system messages
    public static ChatMessage createSystemMessage(String chatRoomId, String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.chatRoomId = chatRoomId;
        chatMessage.senderId = "system";
        chatMessage.senderName = "System";
        chatMessage.message = message;
        chatMessage.messageType = TYPE_SYSTEM;
        return chatMessage;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderProfileUrl() {
        return senderProfileUrl;
    }

    public void setSenderProfileUrl(String senderProfileUrl) {
        this.senderProfileUrl = senderProfileUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    /**
     * Check if this is a system message
     */
    public boolean isSystemMessage() {
        return messageType == TYPE_SYSTEM;
    }
}
