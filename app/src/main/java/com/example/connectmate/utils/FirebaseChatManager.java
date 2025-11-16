package com.example.connectmate.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.connectmate.models.ChatMessage;
import com.example.connectmate.models.ChatRoom;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase-based Chat Manager with real-time multi-user sync
 * Replaces SharedPreferences-based ChatManager
 */
public class FirebaseChatManager {
    private static final String TAG = "FirebaseChatManager";

    // Firebase paths
    private static final String PATH_CHAT_ROOMS = "chatRooms";
    private static final String PATH_MESSAGES = "messages";

    private final DatabaseReference chatRoomsRef;
    private final DatabaseReference messagesRef;
    private final FirebaseAuth auth;

    private static FirebaseChatManager instance;

    // Listeners for real-time updates
    private final Map<String, ChildEventListener> messageListeners = new HashMap<>();
    private ChildEventListener chatRoomsChildListener;

    private FirebaseChatManager() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // Enable offline persistence (if not already enabled)
        try {
            database.setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.w(TAG, "Persistence already enabled or failed to enable", e);
        }

        chatRoomsRef = database.getReference(PATH_CHAT_ROOMS);
        messagesRef = database.getReference(PATH_MESSAGES);
        auth = FirebaseAuth.getInstance();

        // Keep chat data synced locally
        chatRoomsRef.keepSynced(true);
    }

    /**
     * Get singleton instance
     */
    public static synchronized FirebaseChatManager getInstance() {
        if (instance == null) {
            instance = new FirebaseChatManager();
        }
        return instance;
    }

    /**
     * Create or get existing chat room for an activity
     */
    public void createOrGetChatRoom(String activityId, String activityTitle, String activityCategory, OnCompleteListener<ChatRoom> listener) {
        // Query for existing chat room with this activity ID
        chatRoomsRef.orderByChild("activityId").equalTo(activityId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Chat room already exists
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ChatRoom existingRoom = child.getValue(ChatRoom.class);
                            if (existingRoom != null && listener != null) {
                                Log.d(TAG, "Chat room already exists for activity: " + activityId);
                                listener.onSuccess(existingRoom);
                                return;
                            }
                        }
                    }

                    // Create new chat room with category
                    ChatRoom chatRoom = new ChatRoom(activityTitle, activityId);
                    chatRoom.setCategory(activityCategory);
                    saveChatRoom(chatRoom, listener);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error checking for existing chat room", error.toException());
                    if (listener != null) {
                        listener.onError(error.toException());
                    }
                }
            });
    }

    /**
     * Get existing chat room by activity ID (without creating one)
     */
    public void getChatRoomByActivityId(String activityId, OnCompleteListener<ChatRoom> listener) {
        chatRoomsRef.orderByChild("activityId").equalTo(activityId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            ChatRoom existingRoom = child.getValue(ChatRoom.class);
                            if (existingRoom != null && listener != null) {
                                Log.d(TAG, "Found chat room for activity: " + activityId);
                                listener.onSuccess(existingRoom);
                                return;
                            }
                        }
                    }
                    // No chat room found
                    if (listener != null) {
                        listener.onSuccess(null);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error finding chat room", error.toException());
                    if (listener != null) {
                        listener.onError(error.toException());
                    }
                }
            });
    }

    /**
     * Save a chat room to Firebase
     */
    public void saveChatRoom(ChatRoom chatRoom, OnCompleteListener<ChatRoom> listener) {
        if (chatRoom.getId() == null || chatRoom.getId().isEmpty()) {
            // Generate new ID if not set
            String id = chatRoomsRef.push().getKey();
            chatRoom.setId(id);
        }

        // Update timestamp
        if (chatRoom.getCreatedTimestamp() == 0) {
            chatRoom.setCreatedTimestamp(System.currentTimeMillis());
        }

        chatRoomsRef.child(chatRoom.getId()).setValue(chatRoom)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Chat room saved: " + chatRoom.getName());
                if (listener != null) {
                    listener.onSuccess(chatRoom);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error saving chat room", e);
                if (listener != null) {
                    listener.onError(e);
                }
            });
    }

    /**
     * Get all chat rooms with real-time updates
     */
    public void getAllChatRooms(ChatRoomListListener listener) {
        chatRoomsRef.orderByChild("lastMessageTime")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<ChatRoom> chatRooms = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        ChatRoom chatRoom = child.getValue(ChatRoom.class);
                        if (chatRoom != null) {
                            chatRooms.add(0, chatRoom); // Add to beginning (newest first)
                        }
                    }

                    Log.d(TAG, "Loaded " + chatRooms.size() + " chat rooms from Firebase");
                    listener.onChatRoomsLoaded(chatRooms);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading chat rooms", error.toException());
                    listener.onError(error.toException());
                }
            });
    }

    /**
     * Listen for chat room changes in real-time
     */
    public void listenForChatRoomChanges(ChatRoomChangeListener listener) {
        // Remove previous listener if exists
        if (chatRoomsChildListener != null) {
            chatRoomsRef.removeEventListener(chatRoomsChildListener);
        }

        chatRoomsChildListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatRoom chatRoom = snapshot.getValue(ChatRoom.class);
                if (chatRoom != null) {
                    listener.onChatRoomAdded(chatRoom);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatRoom chatRoom = snapshot.getValue(ChatRoom.class);
                if (chatRoom != null) {
                    listener.onChatRoomChanged(chatRoom);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                ChatRoom chatRoom = snapshot.getValue(ChatRoom.class);
                if (chatRoom != null) {
                    listener.onChatRoomRemoved(chatRoom);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Not used for chat rooms
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.toException());
            }
        };

        chatRoomsRef.addChildEventListener(chatRoomsChildListener);
    }

    /**
     * Get chat room by ID
     */
    public void getChatRoomById(String chatRoomId, OnCompleteListener<ChatRoom> listener) {
        chatRoomsRef.child(chatRoomId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ChatRoom chatRoom = snapshot.getValue(ChatRoom.class);
                if (chatRoom != null && listener != null) {
                    listener.onSuccess(chatRoom);
                } else if (listener != null) {
                    listener.onError(new Exception("Chat room not found"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) {
                    listener.onError(error.toException());
                }
            }
        });
    }

    /**
     * Listen to a specific chat room for real-time updates (member count, etc.)
     */
    public void listenToChatRoom(String chatRoomId, OnCompleteListener<ChatRoom> listener) {
        chatRoomsRef.child(chatRoomId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ChatRoom chatRoom = snapshot.getValue(ChatRoom.class);
                if (chatRoom != null && listener != null) {
                    listener.onSuccess(chatRoom);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) {
                    listener.onError(error.toException());
                }
            }
        });
    }

    /**
     * Add member to chat room
     */
    public void addMemberToChatRoom(String chatRoomId, String memberId, String memberName,
                                   OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("members/" + memberId + "/name", memberName);
        updates.put("members/" + memberId + "/unreadCount", 0);

        chatRoomsRef.child(chatRoomId).updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Member added to chat room: " + chatRoomId);
                if (listener != null) {
                    listener.onSuccess(null);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding member to chat room", e);
                if (listener != null) {
                    listener.onError(e);
                }
            });
    }

    /**
     * Remove member from chat room
     */
    public void removeMemberFromChatRoom(String chatRoomId, String memberId,
                                         OnCompleteListener<Void> listener) {
        chatRoomsRef.child(chatRoomId).child("members").child(memberId)
            .removeValue()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Member removed from chat room: " + chatRoomId);
                if (listener != null) {
                    listener.onSuccess(null);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error removing member from chat room", e);
                if (listener != null) {
                    listener.onError(e);
                }
            });
    }

    /**
     * Send a message to a chat room
     */
    public void sendMessage(ChatMessage message, OnCompleteListener<ChatMessage> listener) {
        if (message.getId() == null || message.getId().isEmpty()) {
            String id = messagesRef.child(message.getChatRoomId()).push().getKey();
            message.setId(id);
        }

        // Set timestamp
        if (message.getTimestamp() == 0) {
            message.setTimestamp(System.currentTimeMillis());
        }

        // Save message to /messages/{chatRoomId}/{messageId}
        messagesRef.child(message.getChatRoomId()).child(message.getId()).setValue(message)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Message sent to chat room: " + message.getChatRoomId());

                // Update chat room's last message info
                Map<String, Object> updates = new HashMap<>();
                updates.put("lastMessage", message.getMessage());
                updates.put("lastMessageTime", message.getTimestamp());
                updates.put("lastMessageSenderId", message.getSenderId());
                updates.put("lastMessageSenderName", message.getSenderName());

                // Include sender profile URL if available
                if (message.getSenderProfileUrl() != null) {
                    updates.put("lastMessageSenderProfileUrl", message.getSenderProfileUrl());
                }

                chatRoomsRef.child(message.getChatRoomId()).updateChildren(updates);

                // Increment unread count for all members except the sender
                // BUT only if this is not a system message
                if (!message.isSystemMessage()) {
                    incrementUnreadCountForOthers(message.getChatRoomId(), message.getSenderId());
                } else {
                    Log.d(TAG, "System message - skipping unread count increment");
                }

                if (listener != null) {
                    listener.onSuccess(message);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error sending message", e);
                if (listener != null) {
                    listener.onError(e);
                }
            });
    }

    /**
     * Increment unread count for all members except the specified user (sender)
     */
    private void incrementUnreadCountForOthers(String chatRoomId, String senderId) {
        chatRoomsRef.child(chatRoomId).child("members")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Map<String, Object> updates = new HashMap<>();

                    for (DataSnapshot memberSnapshot : snapshot.getChildren()) {
                        String memberId = memberSnapshot.getKey();

                        // Skip the sender - they don't need unread count incremented
                        if (memberId != null && !memberId.equals(senderId)) {
                            Long currentUnreadCount = memberSnapshot.child("unreadCount").getValue(Long.class);
                            int newUnreadCount = (currentUnreadCount != null ? currentUnreadCount.intValue() : 0) + 1;
                            updates.put(memberId + "/unreadCount", newUnreadCount);
                        }
                    }

                    if (!updates.isEmpty()) {
                        chatRoomsRef.child(chatRoomId).child("members").updateChildren(updates)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Unread counts incremented for chat room: " + chatRoomId))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to increment unread counts", e));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error incrementing unread counts", error.toException());
                }
            });
    }

    /**
     * Mark all messages as read for a specific user in a chat room
     * Resets their unread count to 0
     */
    public void markMessagesAsRead(String chatRoomId, String userId) {
        if (chatRoomId == null || userId == null || userId.isEmpty()) {
            Log.w(TAG, "Cannot mark messages as read - invalid chatRoomId or userId");
            return;
        }

        chatRoomsRef.child(chatRoomId).child("members").child(userId).child("unreadCount")
            .setValue(0)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Marked all messages as read for user " + userId + " in chat room: " + chatRoomId))
            .addOnFailureListener(e -> Log.e(TAG, "Failed to mark messages as read", e));
    }

    /**
     * Get unread count for a specific user in a chat room
     */
    public void getUnreadCount(String chatRoomId, String userId, OnCompleteListener<Integer> listener) {
        if (chatRoomId == null || userId == null || userId.isEmpty()) {
            if (listener != null) {
                listener.onSuccess(0);
            }
            return;
        }

        chatRoomsRef.child(chatRoomId).child("members").child(userId).child("unreadCount")
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Long unreadCount = snapshot.getValue(Long.class);
                    int count = unreadCount != null ? unreadCount.intValue() : 0;
                    if (listener != null) {
                        listener.onSuccess(count);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error getting unread count", error.toException());
                    if (listener != null) {
                        listener.onError(error.toException());
                    }
                }
            });
    }

    /**
     * Get all messages for a chat room with real-time updates
     */
    public void getMessagesForChatRoom(String chatRoomId, MessageListListener listener) {
        messagesRef.child(chatRoomId).orderByChild("timestamp")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<ChatMessage> messages = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        ChatMessage message = child.getValue(ChatMessage.class);
                        if (message != null) {
                            messages.add(message);
                        }
                    }

                    Log.d(TAG, "Loaded " + messages.size() + " messages for chat room: " + chatRoomId);
                    listener.onMessagesLoaded(messages);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading messages", error.toException());
                    listener.onError(error.toException());
                }
            });
    }

    /**
     * Listen for new messages in real-time
     */
    public void listenForNewMessages(String chatRoomId, MessageChangeListener listener) {
        // Remove previous listener if exists
        ChildEventListener existingListener = messageListeners.get(chatRoomId);
        if (existingListener != null) {
            messagesRef.child(chatRoomId).removeEventListener(existingListener);
        }

        ChildEventListener messageListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    listener.onMessageAdded(message);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    listener.onMessageChanged(message);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    listener.onMessageRemoved(message);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Not used for messages
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.toException());
            }
        };

        messagesRef.child(chatRoomId).orderByChild("timestamp").addChildEventListener(messageListener);
        messageListeners.put(chatRoomId, messageListener);
    }

    /**
     * Delete a chat room
     */
    public void deleteChatRoom(String chatRoomId, OnCompleteListener<Void> listener) {
        // Delete chat room
        chatRoomsRef.child(chatRoomId).removeValue()
            .addOnSuccessListener(aVoid -> {
                // Also delete all messages
                messagesRef.child(chatRoomId).removeValue()
                    .addOnSuccessListener(aVoid2 -> {
                        Log.d(TAG, "Chat room and messages deleted: " + chatRoomId);
                        if (listener != null) {
                            listener.onSuccess(null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting messages", e);
                        if (listener != null) {
                            listener.onError(e);
                        }
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting chat room", e);
                if (listener != null) {
                    listener.onError(e);
                }
            });
    }

    /**
     * Remove listener for a specific chat room
     */
    public void removeMessageListener(String chatRoomId) {
        ChildEventListener listener = messageListeners.get(chatRoomId);
        if (listener != null) {
            messagesRef.child(chatRoomId).removeEventListener(listener);
            messageListeners.remove(chatRoomId);
        }
    }

    /**
     * Remove all listeners
     */
    public void removeAllListeners() {
        if (chatRoomsChildListener != null) {
            chatRoomsRef.removeEventListener(chatRoomsChildListener);
        }

        for (Map.Entry<String, ChildEventListener> entry : messageListeners.entrySet()) {
            messagesRef.child(entry.getKey()).removeEventListener(entry.getValue());
        }
        messageListeners.clear();
    }

    // Callback interfaces
    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public interface ChatRoomListListener {
        void onChatRoomsLoaded(List<ChatRoom> chatRooms);
        void onError(Exception e);
    }

    public interface ChatRoomChangeListener {
        void onChatRoomAdded(ChatRoom chatRoom);
        void onChatRoomChanged(ChatRoom chatRoom);
        void onChatRoomRemoved(ChatRoom chatRoom);
        void onError(Exception e);
    }

    public interface MessageListListener {
        void onMessagesLoaded(List<ChatMessage> messages);
        void onError(Exception e);
    }

    public interface MessageChangeListener {
        void onMessageAdded(ChatMessage message);
        void onMessageChanged(ChatMessage message);
        void onMessageRemoved(ChatMessage message);
        void onError(Exception e);
    }
}
