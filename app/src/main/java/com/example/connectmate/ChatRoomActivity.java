package com.example.connectmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.ChatMessage;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.utils.FirebaseChatManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomActivity extends AppCompatActivity {

    private static final String TAG = "ChatRoomActivity";

    private ChatRoom chatRoom;
    private String currentUserId;
    private String currentUserName;

    // UI Components
    private Toolbar toolbar;
    private RecyclerView messagesRecyclerView;
    private LinearLayout emptyState;
    private TextInputEditText messageInput;
    private FloatingActionButton btnSendMessage;

    // Adapter
    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messages;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        // Get chat room from intent
        chatRoom = (ChatRoom) getIntent().getSerializableExtra("chat_room");

        // Get current user info
        getCurrentUserInfo();

        // Initialize UI
        initializeViews();
        setupRecyclerView();
        setupMessageInput();

        if (chatRoom == null) {
            String chatRoomId = getIntent().getStringExtra("chat_room_id");
            if (chatRoomId != null) {
                loadChatRoomFromFirebase(chatRoomId);
            } else {
                Toast.makeText(this, "Chat room not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            setupToolbar();
            loadMessagesFromFirebase();
            listenForChatRoomUpdates();
        }
    }

    private void getCurrentUserInfo() {
        // Try Firebase Auth
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
            currentUserName = (firebaseUser.getDisplayName() != null) ?
                firebaseUser.getDisplayName() : firebaseUser.getEmail();
        } else {
            // Try SharedPreferences for social login
            SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
            currentUserId = prefs.getString("user_id", "");
            currentUserName = prefs.getString("user_name", "Guest");
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        emptyState = findViewById(R.id.empty_state);
        messageInput = findViewById(R.id.message_input);
        btnSendMessage = findViewById(R.id.btn_send_message);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(chatRoom.getName());
            // Display member count as subtitle
            int memberCount = chatRoom.getMemberCount();
            getSupportActionBar().setSubtitle(memberCount + "명 참여 중");
        }

        // Set navigation icon tint to white using DrawableCompat for compatibility
        Drawable navigationIcon = toolbar.getNavigationIcon();
        if (navigationIcon != null) {
            Drawable wrappedIcon = DrawableCompat.wrap(navigationIcon);
            DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(this, R.color.white));
            toolbar.setNavigationIcon(wrappedIcon);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(messages, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void setupMessageInput() {
        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    /**
     * Load chat room from Firebase
     */
    private void loadChatRoomFromFirebase(String chatRoomId) {
        FirebaseChatManager chatManager = FirebaseChatManager.getInstance();
        chatManager.getChatRoomById(chatRoomId, new FirebaseChatManager.OnCompleteListener<ChatRoom>() {
            @Override
            public void onSuccess(ChatRoom result) {
                chatRoom = result;
                setupToolbar();
                loadMessagesFromFirebase();
                listenForChatRoomUpdates();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading chat room", e);
                Toast.makeText(ChatRoomActivity.this, "Failed to load chat room", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Load messages from Firebase with real-time updates
     */
    private void loadMessagesFromFirebase() {
        if (chatRoom == null) return;

        FirebaseChatManager chatManager = FirebaseChatManager.getInstance();

        // Listen for new messages in real-time
        chatManager.listenForNewMessages(chatRoom.getId(), new FirebaseChatManager.MessageChangeListener() {
            @Override
            public void onMessageAdded(ChatMessage message) {
                // Check if message already exists to avoid duplicates
                boolean exists = false;
                for (ChatMessage m : messages) {
                    if (m.getId() != null && m.getId().equals(message.getId())) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    messages.add(message);
                    messageAdapter.notifyItemInserted(messages.size() - 1);

                    // Auto-scroll to bottom if near the bottom or if it's the user's own message
                    if (shouldAutoScroll() || message.getSenderId().equals(currentUserId)) {
                        messagesRecyclerView.scrollToPosition(messages.size() - 1);
                    }

                    updateUI();
                    Log.d(TAG, "New message added: " + message.getMessage());
                }
            }

            @Override
            public void onMessageChanged(ChatMessage message) {
                // Update existing message
                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).getId() != null && messages.get(i).getId().equals(message.getId())) {
                        messages.set(i, message);
                        messageAdapter.notifyItemChanged(i);
                        Log.d(TAG, "Message updated: " + message.getMessage());
                        break;
                    }
                }
            }

            @Override
            public void onMessageRemoved(ChatMessage message) {
                // Remove message
                for (int i = 0; i < messages.size(); i++) {
                    if (messages.get(i).getId() != null && messages.get(i).getId().equals(message.getId())) {
                        messages.remove(i);
                        messageAdapter.notifyItemRemoved(i);
                        updateUI();
                        Log.d(TAG, "Message removed");
                        break;
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading messages", e);
                Toast.makeText(ChatRoomActivity.this, "메시지 로드 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Listen for chat room updates (member count, etc.)
     */
    private void listenForChatRoomUpdates() {
        if (chatRoom == null) return;

        FirebaseChatManager chatManager = FirebaseChatManager.getInstance();
        chatManager.getChatRoomById(chatRoom.getId(), new FirebaseChatManager.OnCompleteListener<ChatRoom>() {
            @Override
            public void onSuccess(ChatRoom updatedRoom) {
                chatRoom = updatedRoom;
                runOnUiThread(() -> {
                    if (getSupportActionBar() != null) {
                        int memberCount = chatRoom.getMemberCount();
                        getSupportActionBar().setSubtitle(memberCount + "명 참여 중");
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error updating chat room", e);
            }
        });
    }

    /**
     * Check if should auto-scroll to bottom
     */
    private boolean shouldAutoScroll() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) messagesRecyclerView.getLayoutManager();
        if (layoutManager == null) return true;

        int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
        int totalItems = layoutManager.getItemCount();

        // Auto-scroll if user is at the bottom or within 2 messages of the bottom
        return totalItems == 0 || lastVisiblePosition >= totalItems - 3;
    }

    private void sendMessage() {
        String messageText = (messageInput.getText() != null) ?
            messageInput.getText().toString().trim() : "";

        if (messageText.isEmpty()) {
            return;
        }

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create message
        ChatMessage message = new ChatMessage(
            chatRoom.getId(),
            currentUserId,
            currentUserName,
            messageText
        );

        // Send message to Firebase
        FirebaseChatManager chatManager = FirebaseChatManager.getInstance();
        chatManager.sendMessage(message, new FirebaseChatManager.OnCompleteListener<ChatMessage>() {
            @Override
            public void onSuccess(ChatMessage result) {
                // Clear input on successful send
                runOnUiThread(() -> {
                    messageInput.setText("");
                    Log.d(TAG, "Message sent: " + messageText);
                });
                // Message will be added to list via real-time listener
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Failed to send message", e);
                runOnUiThread(() -> {
                    Toast.makeText(ChatRoomActivity.this, "메시지 전송 실패", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateUI() {
        if (messages.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            messagesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            messagesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Firebase listeners
        if (chatRoom != null) {
            FirebaseChatManager.getInstance().removeMessageListener(chatRoom.getId());
        }
    }
}
