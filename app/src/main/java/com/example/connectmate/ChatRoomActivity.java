package com.example.connectmate;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri; // 추가
import android.os.Bundle;
import android.provider.MediaStore; // 추가
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent; // 추가

import androidx.activity.result.ActivityResultLauncher; // 추가
import androidx.activity.result.contract.ActivityResultContracts; // 추가
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.google.android.material.button.MaterialButton; // 추가
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage; // 추가
import com.google.firebase.storage.StorageReference; // 추가

import java.util.ArrayList;
import java.util.List;
import java.util.UUID; // 추가

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
    private MaterialButton btnUploadPhoto; // 추가

    // Adapter
    private ChatMessageAdapter messageAdapter;
    private List<ChatMessage> messages;

    // Image Upload
    private Uri selectedImageUri; // 추가
    private ActivityResultLauncher<Intent> pickImageLauncher; // 추가

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
        setupImagePicker(); // 추가

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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_leave_room) {
            leaveChatRoom();
            return true;
        } else if (itemId == R.id.action_view_participants) {
            showParticipantsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showParticipantsDialog() {
        if (chatRoom == null || chatRoom.getMemberNames().isEmpty()) {
            Toast.makeText(this, "참여자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> memberNames = chatRoom.getMemberNames();
        String[] items = memberNames.toArray(new String[0]);

        new AlertDialog.Builder(this)
            .setTitle("참여자 목록 (" + memberNames.size() + "명)")
            .setItems(items, null)
            .setPositiveButton("확인", null)
            .show();
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

                    // ✅ FirebaseActivityManager 통해 멤버 제거
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
        // Try Firebase Auth
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
            currentUserName = (firebaseUser.getDisplayName() != null) ?
                firebaseUser.getDisplayName() : firebaseUser.getEmail();
            currentUserProfileUrl = (firebaseUser.getPhotoUrl() != null) ?
                firebaseUser.getPhotoUrl().toString() : null;
            Log.d(TAG, "Firebase Auth - User: " + currentUserName + ", Profile URL: " + currentUserProfileUrl);
        } else {
            // Try SharedPreferences for social login
            SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
            currentUserId = prefs.getString("user_id", "");
            currentUserName = prefs.getString("user_name", "Guest");
            currentUserProfileUrl = prefs.getString("profile_image_url", null);
            Log.d(TAG, "SharedPreferences - User: " + currentUserName + ", Profile URL: " + currentUserProfileUrl);
        }

        // If no profile URL from auth, try to fetch from SharedPreferences as fallback
        if (currentUserProfileUrl == null || currentUserProfileUrl.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
            currentUserProfileUrl = prefs.getString("profile_image_url", null);
            Log.d(TAG, "Fallback SharedPreferences - Profile URL: " + currentUserProfileUrl);

            // Also try to load from Firebase user profile
            if ((currentUserProfileUrl == null || currentUserProfileUrl.isEmpty()) && !currentUserId.isEmpty()) {
                Log.d(TAG, "Attempting to load profile URL from Firebase for user: " + currentUserId);
                loadProfileUrlFromFirebase(currentUserId);
            }
        }

        Log.d(TAG, "=== Final getUserInfo result ===");
        Log.d(TAG, "User ID: " + currentUserId);
        Log.d(TAG, "User Name: " + currentUserName);
        Log.d(TAG, "Profile URL: " + currentUserProfileUrl);
        Log.d(TAG, "Profile URL is " + (currentUserProfileUrl != null && !currentUserProfileUrl.isEmpty() ? "VALID" : "NULL/EMPTY"));
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

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Start from bottom
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void setupMessageInput() {
        btnSendMessage.setOnClickListener(v -> sendMessage());
        btnUploadPhoto.setOnClickListener(v -> showUploadOptionsDialog()); // 추가
    }

    // 추가: 이미지 선택기를 설정하는 메서드
    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Toast.makeText(this, "사진이 선택되었습니다. 메시지 전송 버튼을 눌러주세요.", Toast.LENGTH_SHORT).show();
                    // 여기에 선택된 이미지를 미리 보여주는 UI를 추가할 수 있습니다.
                } else {
                    selectedImageUri = null;
                }
            }
        );
    }

    // 추가: 업로드 옵션 선택 다이얼로그
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

    // 추가: 갤러리를 여는 메서드 (포토 앨범을 열음)
    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    // 추가: 파일 탐색기를 여는 메서드 (파일 브라우저를 열음)
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 파일 브라우저를 명시적으로 열도록 설정
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
        String messageText = (messageInput.getText() != null) ?
            messageInput.getText().toString().trim() : "";

        if (messageText.isEmpty() && selectedImageUri == null) { // 이미지도 없고 텍스트도 없으면 리턴
            Toast.makeText(this, "메시지를 입력하거나 사진을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId.isEmpty()) {
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

        if (selectedImageUri != null) {
            uploadImageAndSendMessage(messageText); // 이미지 업로드 후 메시지 전송
        } else {
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

    // 추가: 이미지를 Firebase Storage에 업로드하고 메시지를 전송하는 메서드
    private void uploadImageAndSendMessage(String messageText) {
        if (selectedImageUri == null) {
            Toast.makeText(this, "선택된 이미지가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "사진을 업로드 중입니다...", Toast.LENGTH_SHORT).show();

        StorageReference storageRef = FirebaseStorage.getInstance().getReference("chat_images")
            .child(chatRoom.getId())
            .child(UUID.randomUUID().toString() + ".jpg");

        storageRef.putFile(selectedImageUri)
            .addOnSuccessListener(taskSnapshot -> {
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    Log.d(TAG, "Image uploaded. Download URL: " + imageUrl);

                    // Create message with image URL
                    ChatMessage message = new ChatMessage(
                        chatRoom.getId(),
                        currentUserId,
                        currentUserName,
                        messageText.isEmpty() ? null : messageText // 텍스트가 비어있으면 null, 아니면 텍스트
                    );
                    message.setImageUrl(imageUrl); // 이미지 URL 설정

                    if (currentUserProfileUrl != null && !currentUserProfileUrl.isEmpty()) {
                        message.setSenderProfileUrl(currentUserProfileUrl);
                    }

                    // Send message to Firebase
                    FirebaseChatManager.getInstance().sendMessage(message, new FirebaseChatManager.OnCompleteListener<ChatMessage>() {
                        @Override
                        public void onSuccess(ChatMessage result) {
                            runOnUiThread(() -> {
                                messageInput.setText("");
                                selectedImageUri = null; // 이미지 전송 후 초기화
                                Toast.makeText(ChatRoomActivity.this, "사진 메시지가 전송되었습니다.", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Failed to send image message", e);
                            runOnUiThread(() -> {
                                Toast.makeText(ChatRoomActivity.this, "사진 메시지 전송 실패", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });

                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get download URL", e);
                    Toast.makeText(ChatRoomActivity.this, "사진 URL 가져오기 실패", Toast.LENGTH_SHORT).show();
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to upload image", e);
                Toast.makeText(ChatRoomActivity.this, "사진 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
