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
import com.example.connectmate.utils.ChatManager;
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

        if (chatRoom == null) {
            String chatRoomId = getIntent().getStringExtra("chat_room_id");
            if (chatRoomId != null) {
                ChatManager chatManager = ChatManager.getInstance(this);
                chatRoom = chatManager.getChatRoomById(chatRoomId);
            }
        }

        if (chatRoom == null) {
            Toast.makeText(this, "Chat room not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get current user info
        getCurrentUserInfo();

        // Initialize UI
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupMessageInput();

        // Load messages
        loadMessages();
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

    private void loadMessages() {
        ChatManager chatManager = ChatManager.getInstance(this);
        messages.clear();
        messages.addAll(chatManager.getMessagesForChatRoom(chatRoom.getId()));

        messageAdapter.notifyDataSetChanged();
        updateUI();

        // Scroll to bottom
        if (!messages.isEmpty()) {
            messagesRecyclerView.scrollToPosition(messages.size() - 1);
        }

        Log.d(TAG, "Loaded " + messages.size() + " messages");
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

        // Send message
        ChatManager chatManager = ChatManager.getInstance(this);
        boolean sent = chatManager.sendMessage(message);

        if (sent) {
            // Add to list and update UI
            messages.add(message);
            messageAdapter.notifyItemInserted(messages.size() - 1);
            messagesRecyclerView.scrollToPosition(messages.size() - 1);

            // Clear input
            messageInput.setText("");

            updateUI();

            Log.d(TAG, "Message sent: " + messageText);
        } else {
            Toast.makeText(this, "메시지 전송 실패", Toast.LENGTH_SHORT).show();
        }
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
    protected void onResume() {
        super.onResume();
        // Reload chat room data to get updated member count
        ChatManager chatManager = ChatManager.getInstance(this);
        ChatRoom updatedChatRoom = chatManager.getChatRoomById(chatRoom.getId());
        if (updatedChatRoom != null) {
            chatRoom = updatedChatRoom;
            // Update toolbar with new member count
            if (getSupportActionBar() != null) {
                int memberCount = chatRoom.getMemberCount();
                getSupportActionBar().setSubtitle(memberCount + "명 참여 중");
            }
        }
        // Reload messages when returning to chat
        loadMessages();
    }
}
