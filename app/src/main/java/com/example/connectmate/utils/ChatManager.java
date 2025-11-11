package com.example.connectmate.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.connectmate.models.ChatMessage;
import com.example.connectmate.models.ChatRoom;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manager class for handling ChatRoom and ChatMessage data persistence
 */
public class ChatManager {
    private static final String TAG = "ChatManager";
    private static final String PREF_NAME = "ConnectMateChats";
    private static final String KEY_CHAT_ROOMS = "chat_rooms";
    private static final String KEY_MESSAGES_PREFIX = "messages_";

    private final SharedPreferences prefs;
    private final Gson gson;

    private static ChatManager instance;

    private ChatManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    /**
     * Get singleton instance
     */
    public static synchronized ChatManager getInstance(Context context) {
        if (instance == null) {
            instance = new ChatManager(context);
        }
        return instance;
    }

    /**
     * Create or get existing chat room for an activity, adding the creator automatically.
     */
    public ChatRoom createOrGetChatRoom(String activityId, String activityTitle, String creatorId, String creatorName) {
        ChatRoom existingRoom = getChatRoomByActivityId(activityId);
        if (existingRoom != null) {
            return existingRoom;
        }

        // Create new chat room
        ChatRoom chatRoom = new ChatRoom(activityTitle, activityId);

        // Automatically add the creator as the first member
        if (creatorId != null && !creatorId.isEmpty() && creatorName != null) {
            chatRoom.addMember(creatorId, creatorName);
        }

        saveChatRoom(chatRoom);

        Log.d(TAG, "Created new chat room: " + chatRoom.getName() + " with creator: " + creatorName);
        return chatRoom;
    }

    /**
     * Save a chat room
     */
    public boolean saveChatRoom(ChatRoom chatRoom) {
        try {
            List<ChatRoom> chatRooms = getAllChatRooms();

            // Check if chat room already exists and update it
            boolean updated = false;
            for (int i = 0; i < chatRooms.size(); i++) {
                if (chatRooms.get(i).getId().equals(chatRoom.getId())) {
                    chatRooms.set(i, chatRoom);
                    updated = true;
                    break;
                }
            }

            // If not updated, add as new
            if (!updated) {
                chatRooms.add(0, chatRoom);
            }

            String json = gson.toJson(chatRooms);
            prefs.edit().putString(KEY_CHAT_ROOMS, json).commit(); // Use commit for immediate consistency

            Log.d(TAG, "Chat room saved: " + chatRoom.getName());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving chat room", e);
            return false;
        }
    }

    /**
     * Get all chat rooms
     */
    public List<ChatRoom> getAllChatRooms() {
        try {
            String json = prefs.getString(KEY_CHAT_ROOMS, null);
            if (json != null) {
                Type listType = new TypeToken<ArrayList<ChatRoom>>(){}.getType();
                List<ChatRoom> chatRooms = gson.fromJson(json, listType);

                // Sort by last message time (most recent first)
                Collections.sort(chatRooms, (c1, c2) -> Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));

                return chatRooms;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading chat rooms", e);
        }
        return new ArrayList<>();
    }

    /**
     * Get all chat rooms for a specific user
     */
    public List<ChatRoom> getChatRoomsForUser(String userId) {
        List<ChatRoom> allRooms = getAllChatRooms();
        List<ChatRoom> userRooms = new ArrayList<>();
        for (ChatRoom room : allRooms) {
            if (room.getMemberIds().contains(userId)) {
                userRooms.add(room);
            }
        }
        return userRooms;
    }

    /**
     * Get chat room by ID
     */
    public ChatRoom getChatRoomById(String chatRoomId) {
        List<ChatRoom> chatRooms = getAllChatRooms();
        for (ChatRoom chatRoom : chatRooms) {
            if (chatRoom.getId().equals(chatRoomId)) {
                return chatRoom;
            }
        }
        return null;
    }

    /**
     * Get chat room by activity ID
     */
    public ChatRoom getChatRoomByActivityId(String activityId) {
        List<ChatRoom> chatRooms = getAllChatRooms();
        for (ChatRoom chatRoom : chatRooms) {
            if (activityId.equals(chatRoom.getActivityId())) {
                return chatRoom;
            }
        }
        return null;
    }

    /**
     * Add member to chat room
     */
    public boolean addMemberToChatRoom(String chatRoomId, String memberId, String memberName) {
        ChatRoom chatRoom = getChatRoomById(chatRoomId);
        if (chatRoom != null) {
            chatRoom.addMember(memberId, memberName);
            return saveChatRoom(chatRoom);
        }
        return false;
    }

    /**
     * Remove member from chat room and post a system message.
     */
    public boolean leaveChatRoom(String chatRoomId, String memberId, String memberName) {
        ChatRoom chatRoom = getChatRoomById(chatRoomId);
        if (chatRoom != null) {
            // Remove member
            chatRoom.removeMember(memberId);

            // Create and send a system message
            String notificationMessage = memberName + "님이 나가셨습니다.";
            ChatMessage notification = ChatMessage.createSystemMessage(chatRoomId, notificationMessage);
            sendMessage(notification);

            return saveChatRoom(chatRoom);
        }
        return false;
    }

    /**
     * Send a message to chat room
     */
    public boolean sendMessage(ChatMessage message) {
        try {
            // Save message
            List<ChatMessage> messages = getMessagesForChatRoom(message.getChatRoomId());
            messages.add(message);
            saveMessagesForChatRoom(message.getChatRoomId(), messages);

            // Update chat room's last message
            ChatRoom chatRoom = getChatRoomById(message.getChatRoomId());
            if (chatRoom != null) {
                chatRoom.setLastMessage(message.getMessage());
                chatRoom.setLastMessageTime(message.getTimestamp());
                saveChatRoom(chatRoom);
            }

            Log.d(TAG, "Message sent to chat room: " + message.getChatRoomId());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending message", e);
            return false;
        }
    }

    /**
     * Get all messages for a chat room
     */
    public List<ChatMessage> getMessagesForChatRoom(String chatRoomId) {
        try {
            String key = KEY_MESSAGES_PREFIX + chatRoomId;
            String json = prefs.getString(key, null);
            if (json != null) {
                Type listType = new TypeToken<ArrayList<ChatMessage>>(){}.getType();
                return gson.fromJson(json, listType);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading messages", e);
        }
        return new ArrayList<>();
    }

    /**
     * Save messages for a chat room
     */
    private void saveMessagesForChatRoom(String chatRoomId, List<ChatMessage> messages) {
        String key = KEY_MESSAGES_PREFIX + chatRoomId;
        String json = gson.toJson(messages);
        prefs.edit().putString(key, json).apply();
    }

    /**
     * Delete chat room
     */
    public boolean deleteChatRoom(String chatRoomId) {
        try {
            List<ChatRoom> chatRooms = getAllChatRooms();
            for (int i = 0; i < chatRooms.size(); i++) {
                if (chatRooms.get(i).getId().equals(chatRoomId)) {
                    chatRooms.remove(i);

                    String json = gson.toJson(chatRooms);
                    prefs.edit().putString(KEY_CHAT_ROOMS, json).commit(); // Use commit for immediate consistency

                    // Also delete messages
                    String key = KEY_MESSAGES_PREFIX + chatRoomId;
                    prefs.edit().remove(key).apply();

                    Log.d(TAG, "Chat room deleted: " + chatRoomId);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting chat room", e);
        }
        return false;
    }
}
