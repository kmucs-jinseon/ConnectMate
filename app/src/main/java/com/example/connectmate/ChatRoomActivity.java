package com.example.connectmate;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.database.Cursor; // 추가
import android.net.Uri; // 추가
import android.os.Bundle;
import android.provider.MediaStore; // 추가
import android.provider.OpenableColumns; // 추가
import android.util.Log;
import android.webkit.MimeTypeMap; // 추가
import android.widget.ImageButton; // 추가
import android.widget.ImageView; // 추가
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
import androidx.cardview.widget.CardView; // 추가
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
    private CardView imagePreviewContainer; // 추가
    private ImageView imagePreview; // 추가
    private View filePreview; // 추가: 문서 미리보기
    private TextView fileNamePreview; // 추가: 파일명 미리보기
    private ImageButton btnRemoveImage; // 추가

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
                    Log.d(TAG, "Image selected - URI: " + selectedImageUri);
                    Log.d(TAG, "URI scheme: " + (selectedImageUri != null ? selectedImageUri.getScheme() : "null"));

                    // Show image preview
                    showImagePreview();
                    Toast.makeText(this, "사진이 선택되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    // Clear image selection and hide preview when picker is canceled
                    Log.d(TAG, "Image selection cancelled or failed");
                    clearImageSelection();
                }
            }
        );
    }

    // 추가: 파일 미리보기 표시 (이미지 또는 문서)
    private void showImagePreview() {
        if (selectedImageUri != null) {
            // Detect file type
            String mimeType = getContentResolver().getType(selectedImageUri);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            boolean isImage = mimeType.startsWith("image/");

            if (isImage) {
                // Show image preview
                imagePreview.setImageURI(selectedImageUri);
                imagePreview.setVisibility(View.VISIBLE);
                filePreview.setVisibility(View.GONE);
            } else {
                // Show document preview with filename
                String fileName = getFileName(selectedImageUri);
                if (fileName == null) {
                    fileName = "document_" + System.currentTimeMillis();
                }
                fileNamePreview.setText(fileName);
                imagePreview.setVisibility(View.GONE);
                filePreview.setVisibility(View.VISIBLE);
            }

            imagePreviewContainer.setVisibility(View.VISIBLE);
        }
    }

    // 추가: 파일 선택 취소
    private void clearImageSelection() {
        selectedImageUri = null;
        imagePreview.setImageURI(null);
        imagePreview.setVisibility(View.GONE);
        filePreview.setVisibility(View.GONE);
        imagePreviewContainer.setVisibility(View.GONE);
        Toast.makeText(this, "파일 선택이 취소되었습니다.", Toast.LENGTH_SHORT).show();
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
        intent.setType("*/*");
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
        Log.d(TAG, "=== sendMessage() called ===");
        String messageText = (messageInput.getText() != null) ?
            messageInput.getText().toString().trim() : "";

        Log.d(TAG, "Message text: '" + messageText + "'");
        Log.d(TAG, "selectedImageUri: " + selectedImageUri);
        Log.d(TAG, "currentUserId: " + currentUserId);

        if (messageText.isEmpty() && selectedImageUri == null) { // 이미지도 없고 텍스트도 없으면 리턴
            Log.w(TAG, "Both message and image are empty");
            Toast.makeText(this, "메시지를 입력하거나 사진을 선택해주세요.", Toast.LENGTH_SHORT).show();
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

        if (selectedImageUri != null) {
            Log.d(TAG, "Image is selected, calling uploadImageAndSendMessage()");
            uploadImageAndSendMessage(messageText); // 이미지 업로드 후 메시지 전송
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

    // 추가: 파일을 Base64로 인코딩하고 메시지를 전송하는 메서드
    private void uploadImageAndSendMessage(String messageText) {
        Log.d(TAG, "=== uploadImageAndSendMessage() called ===");
        if (selectedImageUri == null) {
            Toast.makeText(this, "선택된 파일이 없습니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "selectedImageUri is null in upload method");
            return;
        }

        Log.d(TAG, "Starting file encoding...");
        Log.d(TAG, "Selected URI: " + selectedImageUri);
        Log.d(TAG, "Chat room ID: " + (chatRoom != null ? chatRoom.getId() : "NULL"));

        // Detect file type
        String mimeType = getContentResolver().getType(selectedImageUri);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        Log.d(TAG, "File MIME type: " + mimeType);

        boolean isImage = mimeType.startsWith("image/");

        if (isImage) {
            Toast.makeText(this, "사진을 처리 중입니다...", Toast.LENGTH_SHORT).show();
            processImageFile(messageText);
        } else {
            Toast.makeText(this, "파일을 처리 중입니다...", Toast.LENGTH_SHORT).show();
            processDocumentFile(messageText, mimeType);
        }
    }

    // Process image files
    private void processImageFile(String messageText) {

        // Convert image to Base64 in background thread
        new Thread(() -> {
            try {
                // Read image from URI
                java.io.InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
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
    private void processDocumentFile(String messageText, String mimeType) {
        new Thread(() -> {
            try {
                // Get filename
                String fileName = getFileName(selectedImageUri);
                if (fileName == null) {
                    fileName = "document_" + System.currentTimeMillis();
                }

                // Read file data
                java.io.InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
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
