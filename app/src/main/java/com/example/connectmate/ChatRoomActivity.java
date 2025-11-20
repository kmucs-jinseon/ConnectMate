package com.example.connectmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.ChatMessage;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.utils.FirebaseActivityManager;
import com.example.connectmate.utils.FirebaseChatManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatRoomActivity extends AppCompatActivity {

    private static final String TAG = "ChatRoomActivity";

    private ChatRoom chatRoom;
    private String currentUserId;
    private String currentUserName;
    private String currentUserProfileUrl;

    // UI Components
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private TextView toolbarParticipantCount;
    private RecyclerView messagesRecyclerView;
    private LinearLayout emptyState;
    private TextInputEditText messageInput;
    private FloatingActionButton btnSendMessage;
    private MaterialButton btnUploadPhoto;
    private CardView imagePreviewContainer;
    private ImageView imagePreview;
    private View filePreview; // Document preview
    private TextView fileNamePreview; // Filename preview
    private ImageButton btnRemoveImage;

    // Adapter
    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messages;

    // File Upload
    private java.util.ArrayList<Uri> selectedFileUris; // Multiple file support
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        // Initialize file list
        selectedFileUris = new java.util.ArrayList<>();

        // Get chat room from intent
        chatRoom = (ChatRoom) getIntent().getSerializableExtra("chat_room");

        // Get current user info
        getCurrentUserInfo();

        // Initialize UI
        initializeViews();
        setupRecyclerView();
        setupMessageInput();
        setupImagePicker();

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
            markMessagesAsRead();
        }
    }

    /**
     * Mark all messages in this chat room as read for the current user
     */
    private void markMessagesAsRead() {
        if (chatRoom == null || currentUserId == null || currentUserId.isEmpty()) {
            Log.w(TAG, "Cannot mark messages as read - chatRoom or currentUserId is null");
            return;
        }

        FirebaseChatManager.getInstance().markMessagesAsRead(chatRoom.getId(), currentUserId);
        Log.d(TAG, "Marking messages as read for user: " + currentUserId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_room_menu, menu);
        if (chatRoom != null && "private".equals(chatRoom.getCategory())) {
            MenuItem leaveItem = menu.findItem(R.id.action_leave_room);
            if (leaveItem != null) {
                leaveItem.setTitle("친구 끊기");
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_leave_room) {
            if (chatRoom != null && "private".equals(chatRoom.getCategory())) {
                removeFriend();
            } else {
                leaveChatRoom();
            }
            return true;
        } else if (itemId == R.id.action_view_participants) {
            showParticipantsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeFriend() {
        if (chatRoom == null || chatRoom.getMembers().size() != 2) {
            return;
        }

        String friendId = null;
        for (String memberId : chatRoom.getMembers().keySet()) {
            if (!memberId.equals(currentUserId)) {
                friendId = memberId;
                break;
            }
        }

        if (friendId != null) {
            String finalFriendId = friendId;
            new AlertDialog.Builder(this)
                .setTitle("친구 끊기")
                .setMessage("정말 친구를 끊으시겠습니까? 이 채팅방은 삭제됩니다.")
                .setPositiveButton("끊기", (dialog, which) -> {
                    DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                    usersRef.child(currentUserId).child("friends").child(finalFriendId).removeValue();
                    usersRef.child(finalFriendId).child("friends").child(currentUserId).removeValue();
                    FirebaseChatManager.getInstance().deleteChatRoom(chatRoom.getId(), new FirebaseChatManager.OnCompleteListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(ChatRoomActivity.this, "친구 관계를 끊었습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(ChatRoomActivity.this, "친구 끊기 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("취소", null)
                .show();
        }
    }


    private void showParticipantsDialog() {
        if (chatRoom == null || chatRoom.getMembers().isEmpty()) {
            Toast.makeText(this, "참여자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference friendsRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId).child("friends");
        friendsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> friendIds = new ArrayList<>();
                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    friendIds.add(friendSnapshot.getKey());
                }

                List<Participant> participants = new ArrayList<>();
                for (Map.Entry<String, ChatRoom.Member> entry : chatRoom.getMembers().entrySet()) {
                    participants.add(new Participant(entry.getKey(), entry.getValue().getName()));
                }

                ParticipantAdapter adapter = new ParticipantAdapter(ChatRoomActivity.this, participants, friendIds);

                new AlertDialog.Builder(ChatRoomActivity.this)
                        .setTitle("참여자 목록 (" + participants.size() + "명)")
                        .setAdapter(adapter, null)
                        .setPositiveButton("확인", null)
                        .show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void leaveChatRoom() {
        if (chatRoom == null || currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "채팅방 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("채팅방 나가기")
                .setMessage("정말 채팅방을 나가시겠습니까?")
                .setPositiveButton("나가기", (dialog, which) -> {

                    // Remove member through FirebaseActivityManager
                    FirebaseActivityManager.getInstance().removeParticipant(chatRoom.getActivityId(), currentUserId, new FirebaseActivityManager.OnCompleteListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(ChatRoomActivity.this, "채팅방에서 나갔습니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(ChatRoomActivity.this, "채팅방 나가기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
            .setNegativeButton("취소", null)
            .show();
    }

    private void getCurrentUserInfo() {
        // Get user ID first
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        } else {
            // Try SharedPreferences for social login
            SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
            currentUserId = prefs.getString("user_id", "");
        }

        // Set temporary values while loading from database
        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        currentUserName = prefs.getString("user_name", "Guest");
        currentUserProfileUrl = prefs.getString("profile_image_url", null);

        // Fetch real displayName and profileImageUrl from Firebase Realtime Database
        if (currentUserId != null && !currentUserId.isEmpty()) {
            loadUserInfoFromFirebase(currentUserId);
        }

        Log.d(TAG, "=== Initial getUserInfo result ===");
        Log.d(TAG, "User ID: " + currentUserId);
        Log.d(TAG, "User Name: " + currentUserName);
        Log.d(TAG, "Profile URL: " + currentUserProfileUrl);
    }

    /**
     * Load real user info (displayName and profileImageUrl) from Firebase Realtime Database
     */
    private void loadUserInfoFromFirebase(String userId) {
        Log.d(TAG, "Loading user info from Firebase for user: " + userId);
        com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Get displayName from database
                        String displayName = snapshot.child("displayName").getValue(String.class);
                        if (displayName != null && !displayName.isEmpty()) {
                            currentUserName = displayName;
                            Log.d(TAG, "Loaded displayName from Firebase: " + currentUserName);
                        }

                        // Get profileImageUrl from database
                        String profileUrl = snapshot.child("profileImageUrl").getValue(String.class);
                        if (profileUrl != null && !profileUrl.isEmpty()) {
                            currentUserProfileUrl = profileUrl;
                            Log.d(TAG, "Loaded profileImageUrl from Firebase: " +
                                (profileUrl.length() > 50 ? profileUrl.substring(0, 50) + "..." : profileUrl));
                        }

                        Log.d(TAG, "=== Updated user info from Firebase ===");
                        Log.d(TAG, "User ID: " + currentUserId);
                        Log.d(TAG, "User Name: " + currentUserName);
                        Log.d(TAG, "Profile URL is " + (currentUserProfileUrl != null && !currentUserProfileUrl.isEmpty() ? "VALID" : "NULL/EMPTY"));
                    } else {
                        Log.w(TAG, "User not found in Firebase: " + userId);
                    }
                }

                @Override
                public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                    Log.e(TAG, "Failed to load user info from Firebase", error.toException());
                }
            });
    }

    /**
     * Load profile URL from Firebase Realtime Database
     * Loads Base64 encoded images or Firebase Storage URLs
     */
    private void loadProfileUrlFromFirebase(String userId) {
        Log.d(TAG, "Loading profile URL from Firebase for user: " + userId);
        com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)
            .child("profileImageUrl")
            .get()
            .addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    String profileUrl = dataSnapshot.getValue(String.class);
                    if (profileUrl != null && !profileUrl.isEmpty()) {
                        currentUserProfileUrl = profileUrl;
                        // Save to SharedPreferences for future use
                        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
                        prefs.edit().putString("profile_image_url", profileUrl).apply();

                        String logMsg = profileUrl.startsWith("data:image") ?
                            "Loaded Base64 image from Firebase (length: " + profileUrl.length() + ")" :
                            "Loaded Firebase Storage URL: " + profileUrl;
                        Log.d(TAG, logMsg);
                    } else {
                        Log.d(TAG, "Profile URL exists in Firebase but is null/empty");
                    }
                } else {
                    Log.d(TAG, "No profileImageUrl found in Firebase for user: " + userId);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to load profile URL from Firebase", e);
            });
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_chat_room_title);
        toolbarParticipantCount = findViewById(R.id.toolbar_participant_count);
        messagesRecyclerView = findViewById(R.id.messages_recycler_view);
        emptyState = findViewById(R.id.empty_state);
        messageInput = findViewById(R.id.message_input);
        btnSendMessage = findViewById(R.id.btn_send_message);
        btnUploadPhoto = findViewById(R.id.btn_upload_photo); // 추가
        imagePreviewContainer = findViewById(R.id.image_preview_container); // 추가
        imagePreview = findViewById(R.id.image_preview); // 추가
        filePreview = findViewById(R.id.file_preview); // 추가
        fileNamePreview = findViewById(R.id.file_name_preview); // 추가
        btnRemoveImage = findViewById(R.id.btn_remove_image); // 추가

        // Setup remove image button
        btnRemoveImage.setOnClickListener(v -> clearImageSelection());
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Set custom toolbar title and participant count
        if (toolbarTitle != null) {
            toolbarTitle.setText(chatRoom.getName());
        }
        if (toolbarParticipantCount != null) {
            int memberCount = chatRoom.getMemberCount();
            toolbarParticipantCount.setText(memberCount + "명 참여중");
        }

        // Set navigation icon tint to white using DrawableCompat for compatibility
        Drawable navigationIcon = toolbar.getNavigationIcon();
        if (navigationIcon != null) {
            Drawable wrappedIcon = DrawableCompat.wrap(navigationIcon);
            DrawableCompat.setTint(wrappedIcon, ContextCompat.getColor(this, R.color.white));
            toolbar.setNavigationIcon(wrappedIcon);
        }

        // Set overflow icon tint to white
        Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            Drawable wrappedOverflow = DrawableCompat.wrap(overflowIcon);
            DrawableCompat.setTint(wrappedOverflow, ContextCompat.getColor(this, R.color.white));
            toolbar.setOverflowIcon(wrappedOverflow);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        messageAdapter = new ChatMessageAdapter(messages, currentUserId);

        // Set image click listener
        messageAdapter.setOnImageClickListener(imageUrl -> showImageViewerDialog(imageUrl));

        // Set document click listener
        messageAdapter.setOnDocumentClickListener((fileUrl, fileName, fileType) ->
            downloadDocumentToDownloads(fileUrl, fileName, fileType));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void setupMessageInput() {
        btnSendMessage.setOnClickListener(v -> sendMessage());
        btnUploadPhoto.setOnClickListener(v -> showUploadOptionsDialog());
    }

    // Setup file picker (supports multiple file selection)
    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedFileUris.clear();

                    Intent data = result.getData();

                    // Handle multiple files
                    if (data.getClipData() != null) {
                        android.content.ClipData clipData = data.getClipData();
                        int count = clipData.getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri uri = clipData.getItemAt(i).getUri();
                            selectedFileUris.add(uri);
                        }
                        Log.d(TAG, count + " files selected");
                        Toast.makeText(this, count + "개 파일이 선택되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                    // Handle single file
                    else if (data.getData() != null) {
                        Uri uri = data.getData();
                        selectedFileUris.add(uri);
                        Log.d(TAG, "Single file selected - URI: " + uri);
                        Toast.makeText(this, "파일이 선택되었습니다.", Toast.LENGTH_SHORT).show();
                    }

                    // Show file preview
                    showImagePreview();
                } else {
                    // Clear file selection and hide preview when picker is canceled
                    Log.d(TAG, "File selection cancelled or failed");
                    clearImageSelection();
                }
            }
        );
    }

    // Show file preview (supports multiple files)
    private void showImagePreview() {
        if (selectedFileUris.isEmpty()) {
            imagePreviewContainer.setVisibility(View.GONE);
            return;
        }

        // Show first file as preview
        Uri firstUri = selectedFileUris.get(0);
        String mimeType = getContentResolver().getType(firstUri);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        boolean isImage = mimeType.startsWith("image/") || mimeType.startsWith("video/");

        if (isImage) {
            // Show image/video preview
            imagePreview.setImageURI(firstUri);
            imagePreview.setVisibility(View.VISIBLE);
            filePreview.setVisibility(View.GONE);
        } else {
            // Show document preview with filename
            String fileName = getFileName(firstUri);
            if (fileName == null) {
                fileName = "document_" + System.currentTimeMillis();
            }

            // Show count if multiple files
            if (selectedFileUris.size() > 1) {
                fileName = fileName + " and " + (selectedFileUris.size() - 1) + " more";
            }

            fileNamePreview.setText(fileName);
            imagePreview.setVisibility(View.GONE);
            filePreview.setVisibility(View.VISIBLE);
        }

        imagePreviewContainer.setVisibility(View.VISIBLE);
    }

    // Clear file selection
    private void clearImageSelection() {
        selectedFileUris.clear();
        imagePreview.setImageURI(null);
        imagePreview.setVisibility(View.GONE);
        filePreview.setVisibility(View.GONE);
        imagePreviewContainer.setVisibility(View.GONE);
    }

    // Show upload options dialog
    private void showUploadOptionsDialog() {
        String[] options = {"갤러리", "파일"};

        new AlertDialog.Builder(this)
            .setTitle("사진/파일 선택")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    openGalleryPicker();
                } else {
                    openFilePicker();
                }
            })
            .setNegativeButton("취소", null)
            .show();
    }

    // Open gallery picker (supports multiple selection of photos and videos)
    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Enable multiple selection
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Create chooser to prioritize photo apps (Gallery, Google Photos)
        Intent chooser = Intent.createChooser(intent, "사진/동영상 선택");
        pickImageLauncher.launch(chooser);
    }

    // Open file picker (supports multiple file selection)
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Enable multiple selection
        // Set to open file browser explicitly
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        pickImageLauncher.launch(intent);
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
                markMessagesAsRead();
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
     * Listen for chat room updates (member count, etc.) in real-time
     */
    private void listenForChatRoomUpdates() {
        if (chatRoom == null) return;

        FirebaseChatManager chatManager = FirebaseChatManager.getInstance();
        chatManager.listenToChatRoom(chatRoom.getId(), new FirebaseChatManager.OnCompleteListener<ChatRoom>() {
            @Override
            public void onSuccess(ChatRoom updatedRoom) {
                chatRoom = updatedRoom;
                runOnUiThread(() -> {
                    if (toolbarParticipantCount != null) {
                        int memberCount = chatRoom.getMemberCount();
                        toolbarParticipantCount.setText(memberCount + "명 참여중");
                        Log.d(TAG, "Chat room member count updated: " + memberCount);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error listening to chat room updates", e);
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
        Log.d(TAG, "=== sendMessage() called ===");
        String messageText = (messageInput.getText() != null) ?
            messageInput.getText().toString().trim() : "";

        Log.d(TAG, "Message text: '" + messageText + "'");
        Log.d(TAG, "selectedFileUris count: " + selectedFileUris.size());
        Log.d(TAG, "currentUserId: " + currentUserId);

        if (messageText.isEmpty() && selectedFileUris.isEmpty()) {
            Log.w(TAG, "Both message and files are empty");
            Toast.makeText(this, "메시지를 입력하거나 파일을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId.isEmpty()) {
            Log.e(TAG, "currentUserId is empty!");
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // IMPORTANT: Reload profile URL from SharedPreferences before sending
        // This ensures we have the latest profile photo (including Base64 images)
        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        String latestProfileUrl = prefs.getString("profile_image_url", null);
        if (latestProfileUrl != null && !latestProfileUrl.isEmpty()) {
            currentUserProfileUrl = latestProfileUrl;
            Log.d(TAG, "Refreshed profile URL from SharedPreferences: " +
                (latestProfileUrl.startsWith("data:image") ? "Base64 image" : latestProfileUrl));
        }

        if (!selectedFileUris.isEmpty()) {
            Log.d(TAG, selectedFileUris.size() + " files selected, uploading...");
            uploadMultipleFilesAndSendMessages(messageText); // Upload multiple files
        } else {
            Log.d(TAG, "No image selected, sending text message only");
            // Create message
            ChatMessage message = new ChatMessage(
                chatRoom.getId(),
                currentUserId,
                currentUserName,
                messageText
            );

            // Set profile URL if available
            if (currentUserProfileUrl != null && !currentUserProfileUrl.isEmpty()) {
                message.setSenderProfileUrl(currentUserProfileUrl);
                Log.d(TAG, "Setting profile URL on message: " +
                    (currentUserProfileUrl.startsWith("data:image") ? "Base64 image (length: " + currentUserProfileUrl.length() + ")" : currentUserProfileUrl));
            } else {
                Log.w(TAG, "No profile URL available to set on message - will use default image");
            }

            Log.d(TAG, "=== Sending message ===");
            Log.d(TAG, "Sender ID: " + currentUserId);
            Log.d(TAG, "Sender Name: " + currentUserName);
            Log.d(TAG, "Sender Profile URL: " + message.getSenderProfileUrl());
            Log.d(TAG, "Message Text: " + messageText);

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
    }

    // Upload multiple files and send messages
    private void uploadMultipleFilesAndSendMessages(String messageText) {
        if (selectedFileUris.isEmpty()) {
            Toast.makeText(this, "선택된 파일이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, selectedFileUris.size() + "개 파일을 업로드 중입니다...", Toast.LENGTH_SHORT).show();

        // Upload each file sequentially
        for (int i = 0; i < selectedFileUris.size(); i++) {
            Uri fileUri = selectedFileUris.get(i);
            // Only send text with the first file
            String textToSend = (i == 0 && !messageText.isEmpty()) ? messageText : "";
            uploadSingleFile(fileUri, textToSend);
        }

        // Clear input and selection after starting uploads
        messageInput.setText("");
        clearImageSelection();
    }

    // Upload a single file
    private void uploadSingleFile(Uri fileUri, String messageText) {
        String mimeType = getContentResolver().getType(fileUri);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        boolean isImage = mimeType.startsWith("image/");
        boolean isVideo = mimeType.startsWith("video/");

        if (isImage) {
            processImageFile(fileUri, messageText);
        } else if (isVideo) {
            Toast.makeText(this, "비디오는 현재 지원되지 않습니다.", Toast.LENGTH_SHORT).show();
        } else {
            processDocumentFile(fileUri, messageText, mimeType);
        }
    }

    // Encode file to Base64 and send message (legacy method)
    private void uploadImageAndSendMessage(String messageText) {
        if (selectedFileUris.isEmpty()) {
            Toast.makeText(this, "선택된 파일이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        uploadMultipleFilesAndSendMessages(messageText);
    }

    // Process a single image file
    private void processImageFile(Uri imageUri, String messageText) {
        Log.d(TAG, "Starting image encoding...");
        Log.d(TAG, "Selected URI: " + imageUri);
        Log.d(TAG, "Chat room ID: " + (chatRoom != null ? chatRoom.getId() : "NULL"));

        Toast.makeText(this, "사진을 처리 중입니다...", Toast.LENGTH_SHORT).show();

        // Convert image to Base64 in background thread
        new Thread(() -> {
            try {
                // Read image from URI
                java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "이미지를 읽을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Compress and resize image
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                if (bitmap == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "이미지 처리 실패", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Resize image to max 800x800 to reduce size
                int maxSize = 800;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                float scale = Math.min(((float) maxSize / width), ((float) maxSize / height));

                if (scale < 1.0f) {
                    int newWidth = Math.round(width * scale);
                    int newHeight = Math.round(height * scale);
                    bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                }

                // Convert to Base64
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = "data:image/jpeg;base64," + android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

                Log.d(TAG, "Image encoded successfully. Size: " + base64Image.length() + " chars");

                // Send message on UI thread
                runOnUiThread(() -> {
                    ChatMessage message = new ChatMessage(
                        chatRoom.getId(),
                        currentUserId,
                        currentUserName,
                        messageText.isEmpty() ? null : messageText
                    );
                    message.setImageUrl(base64Image); // Set Base64 image

                    if (currentUserProfileUrl != null && !currentUserProfileUrl.isEmpty()) {
                        message.setSenderProfileUrl(currentUserProfileUrl);
                    }

                    Log.d(TAG, "Sending message with Base64 image...");
                    FirebaseChatManager.getInstance().sendMessage(message, new FirebaseChatManager.OnCompleteListener<ChatMessage>() {
                        @Override
                        public void onSuccess(ChatMessage result) {
                            Log.d(TAG, "Image message sent successfully");
                            runOnUiThread(() -> {
                                messageInput.setText("");
                                clearImageSelection();
                                Toast.makeText(ChatRoomActivity.this, "사진 메시지가 전송되었습니다.", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Failed to send image message", e);
                            runOnUiThread(() -> {
                                Toast.makeText(ChatRoomActivity.this, "사진 메시지 전송 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to encode image", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "이미지 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // Process document files (PDF, docs, txt, etc.)
    private void processDocumentFile(Uri fileUri, String messageText, String mimeType) {
        new Thread(() -> {
            try {
                // Get filename
                String fileName = getFileName(fileUri);
                if (fileName == null) {
                    fileName = "document_" + System.currentTimeMillis();
                }

                // Read file data
                java.io.InputStream inputStream = getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "파일을 읽을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Convert to Base64
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) > -1) {
                    baos.write(buffer, 0, len);
                }
                baos.flush();
                inputStream.close();

                byte[] fileBytes = baos.toByteArray();
                String base64File = android.util.Base64.encodeToString(fileBytes, android.util.Base64.DEFAULT);

                Log.d(TAG, "Document encoded successfully. Size: " + base64File.length() + " chars, File: " + fileName);

                final String finalFileName = fileName;

                // Send message on UI thread
                runOnUiThread(() -> {
                    ChatMessage message = new ChatMessage(
                        chatRoom.getId(),
                        currentUserId,
                        currentUserName,
                        messageText.isEmpty() ? null : messageText
                    );
                    message.setFileUrl(base64File);
                    message.setFileName(finalFileName);
                    message.setFileType(mimeType);
                    message.setMessageType(ChatMessage.TYPE_DOCUMENT);

                    if (currentUserProfileUrl != null && !currentUserProfileUrl.isEmpty()) {
                        message.setSenderProfileUrl(currentUserProfileUrl);
                    }

                    Log.d(TAG, "Sending message with document...");
                    FirebaseChatManager.getInstance().sendMessage(message, new FirebaseChatManager.OnCompleteListener<ChatMessage>() {
                        @Override
                        public void onSuccess(ChatMessage result) {
                            Log.d(TAG, "Document message sent successfully");
                            runOnUiThread(() -> {
                                messageInput.setText("");
                                clearImageSelection();
                                Toast.makeText(ChatRoomActivity.this, "파일이 전송되었습니다.", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Failed to send document message", e);
                            runOnUiThread(() -> {
                                Toast.makeText(ChatRoomActivity.this, "파일 전송 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to encode document", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "파일 처리 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // Get filename from URI
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    // Show image viewer dialog
    private void showImageViewerDialog(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        // Create dialog
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_image_viewer);

        ImageView fullImage = dialog.findViewById(R.id.full_image);
        ImageButton btnClose = dialog.findViewById(R.id.btn_close);
        com.google.android.material.floatingactionbutton.FloatingActionButton btnDownload = dialog.findViewById(R.id.btn_download);

        // Load image
        com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .into(fullImage);

        // Close button
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Download button
        btnDownload.setOnClickListener(v -> {
            downloadImageToGallery(imageUrl);
        });

        dialog.show();
    }

    // Download image to gallery
    private void downloadImageToGallery(String imageUrl) {
        Toast.makeText(this, "이미지를 다운로드 중입니다...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // Decode Base64 image
                String base64Image = imageUrl;
                if (base64Image.contains(",")) {
                    base64Image = base64Image.split(",")[1];
                }

                byte[] imageBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                if (bitmap == null) {
                    runOnUiThread(() -> Toast.makeText(this, "이미지 다운로드 실패", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Save to gallery
                String fileName = "ConnectMate_" + System.currentTimeMillis() + ".jpg";

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // API 29+ (Android 10+): Use MediaStore with RELATIVE_PATH
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/ConnectMate");

                    android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                    if (uri != null) {
                        try (java.io.OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outputStream);
                            runOnUiThread(() -> Toast.makeText(this, "갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "이미지 저장 실패", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // API < 29: Use legacy method
                    java.io.File picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES);
                    java.io.File connectMateDir = new java.io.File(picturesDir, "ConnectMate");
                    if (!connectMateDir.exists()) {
                        connectMateDir.mkdirs();
                    }
                    java.io.File imageFile = new java.io.File(connectMateDir, fileName);

                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile)) {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, fos);

                        // Notify media scanner
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put(android.provider.MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
                        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                        getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                        runOnUiThread(() -> Toast.makeText(this, "갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show());
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to download image", e);
                runOnUiThread(() -> Toast.makeText(this, "이미지 다운로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Download document to Downloads folder
    private void downloadDocumentToDownloads(String fileUrl, String fileName, String fileType) {
        if (fileUrl == null || fileName == null) {
            Toast.makeText(this, "파일 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "파일을 다운로드 중입니다...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                // Decode Base64 file
                byte[] fileBytes = android.util.Base64.decode(fileUrl, android.util.Base64.DEFAULT);

                if (fileBytes == null || fileBytes.length == 0) {
                    runOnUiThread(() -> Toast.makeText(this, "파일 다운로드 실패", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Save to Downloads folder
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // API 29+ (Android 10+): Use MediaStore.Downloads
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
                    if (fileType != null && !fileType.isEmpty()) {
                        values.put(android.provider.MediaStore.Downloads.MIME_TYPE, fileType);
                    }
                    values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

                    android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                    if (uri != null) {
                        try (java.io.OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                            outputStream.write(fileBytes);
                            outputStream.flush();
                            runOnUiThread(() -> Toast.makeText(this, "다운로드 폴더에 저장되었습니다: " + fileName, Toast.LENGTH_LONG).show());
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "파일 저장 실패", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // API < 29: Use legacy method
                    java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                    java.io.File file = new java.io.File(downloadsDir, fileName);

                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                        fos.write(fileBytes);
                        fos.flush();
                        runOnUiThread(() -> Toast.makeText(this, "다운로드 폴더에 저장되었습니다: " + fileName, Toast.LENGTH_LONG).show());
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to download document", e);
                runOnUiThread(() -> Toast.makeText(this, "파일 다운로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
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
