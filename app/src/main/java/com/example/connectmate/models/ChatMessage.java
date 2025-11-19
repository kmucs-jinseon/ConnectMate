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
    public static final int TYPE_DOCUMENT = 3;

    private String id;
    private String chatRoomId;
    private String senderId;
    private String senderName;
    private String senderProfileUrl;
    private String message;
    private String imageUrl; // 추가: 이미지 URL 필드
    private String fileUrl; // 추가: 문서 파일 Base64 데이터
    private String fileName; // 추가: 파일 이름
    private String fileType; // 추가: 파일 MIME 타입
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

    // 추가: 이미지 URL getter 및 setter
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // 추가: 문서 파일 getter 및 setter
    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
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

    /**
     * Check if this is a document message
     */
    public boolean isDocumentMessage() {
        return messageType == TYPE_DOCUMENT;
    }
}
