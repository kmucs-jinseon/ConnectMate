package com.example.connectmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.ChatMessage;
import com.example.connectmate.models.ChatRoom;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatRoomActivity extends AppCompatActivity {

    private static final String TAG = "ChatRoomActivity";

    private ChatRoom chatRoom;
    private String currentUserId;
    private String currentUserName;

    private Toolbar toolbar;
    private RecyclerView messagesRecyclerView;
    private LinearLayout emptyState;
    private TextInputEditText messageInput;
    private FloatingActionButton btnSendMessage;

    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messages;

    private DatabaseReference messagesRef;
    private DatabaseReference roomRef;
    private ChildEventListener messagesListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        chatRoom = (ChatRoom) getIntent().getSerializableExtra("chat_room");
        if (chatRoom == null || chatRoom.getId() == null) {
            Toast.makeText(this, "Chat room not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        getCurrentUserInfo();

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        roomRef = db.child("chat_rooms").child(chatRoom.getId());
        messagesRef = roomRef.child("messages");

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupMessageInput();
        loadMessages();
    }

    private void getCurrentUserInfo() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
            currentUserName = Objects.requireNonNullElse(firebaseUser.getDisplayName(), "Anonymous");
        } else {
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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(chatRoom.getName());
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_chat_room, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem deleteItem = menu.findItem(R.id.action_delete_chat_room);
        MenuItem leaveItem = menu.findItem(R.id.action_leave_chat_room);

        if (currentUserId != null && currentUserId.equals(chatRoom.getCreatorId())) {
            // User is the creator
            deleteItem.setVisible(true);
            leaveItem.setVisible(false);
        } else {
            // User is a member
            deleteItem.setVisible(false);
            leaveItem.setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_delete_chat_room) {
            confirmDeleteChatRoom();
            return true;
        } else if (itemId == R.id.action_leave_chat_room) {
            confirmLeaveChatRoom();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDeleteChatRoom() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Chat Room")
            .setMessage("Are you sure you want to permanently delete this chat room?")
            .setPositiveButton("Delete", (dialog, which) -> deleteChatRoom())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteChatRoom() {
        roomRef.removeValue()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Chat room deleted.", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete chat room.", Toast.LENGTH_SHORT).show());
    }

    private void confirmLeaveChatRoom() {
        new AlertDialog.Builder(this)
            .setTitle("Leave Chat Room")
            .setMessage("Are you sure you want to leave this chat room?")
            .setPositiveButton("Leave", (dialog, which) -> leaveChatRoom())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void leaveChatRoom() {
        // This is a simplified leave implementation. A more robust system would use Cloud Functions
        // to manage membership and potentially delete the room if the last member leaves.
        roomRef.child("memberIds").child(currentUserId).removeValue()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "You have left the chat room.", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to leave chat room.", Toast.LENGTH_SHORT).show());
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(messages, currentUserId);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ((LinearLayoutManager) messagesRecyclerView.getLayoutManager()).setStackFromEnd(true);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void setupMessageInput() {
        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    messages.add(message);
                    messageAdapter.notifyItemInserted(messages.size() - 1);
                    messagesRecyclerView.scrollToPosition(messages.size() - 1);
                    updateUI();
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load messages.", error.toException());
            }
        };
        messagesRef.addChildEventListener(messagesListener);
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) { updateUI(); }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty() || currentUserId.isEmpty()) {
            return;
        }

        ChatMessage message = new ChatMessage(chatRoom.getId(), currentUserId, currentUserName, messageText);

        messagesRef.push().setValue(message)
            .addOnSuccessListener(aVoid -> messageInput.setText(""))
            .addOnFailureListener(e -> Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show());
    }

    private void updateUI() {
        emptyState.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        messagesRecyclerView.setVisibility(messages.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}
