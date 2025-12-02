package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.NotificationAdapter;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.models.NotificationItem;
import com.example.connectmate.utils.FirebaseChatManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatListFragment extends Fragment {

    private static final String TAG = "ChatListFragment";

    // UI Components
    private ImageButton btnMyFriends;
    private ImageButton btnNotifications;
    private ImageButton btnSearch;
    private ImageButton btnFilter;
    private TextInputLayout searchLayout;
    private TextInputEditText searchInput;
    private View filterScrollView;
    private ChipGroup filterChips;
    private View notificationBadge;

    private RecyclerView chatRecyclerView;
    private LinearLayout emptyState;
    private MaterialButton btnStartChat;

    // Adapters
    private ChatRoomAdapter chatRoomAdapter;

    // Data
    private List<ChatRoom> allChatRooms;
    private List<ChatRoom> filteredChatRooms;

    // Firebase
    private DatabaseReference dbRef;
    private ValueEventListener notificationListener;

    public ChatListFragment() {
        super(R.layout.fragment_chat);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbRef = FirebaseDatabase.getInstance().getReference();
        initializeViews(view);
        setupRecyclerViews();
        setupClickListeners();
        loadChatRoomsFromFirebase();
        loadUnreadNotificationCount();
        updateUI();
    }

    private void initializeViews(View view) {
        // Header buttons
        btnMyFriends = view.findViewById(R.id.btn_my_friends);
        btnNotifications = view.findViewById(R.id.btn_notifications);
        btnSearch = view.findViewById(R.id.btn_search);
        btnFilter = view.findViewById(R.id.btn_filter);
        notificationBadge = view.findViewById(R.id.notification_badge);

        // Search
        searchLayout = view.findViewById(R.id.search_layout);
        searchInput = view.findViewById(R.id.search_input);

        // Filter chips
        filterScrollView = view.findViewById(R.id.filter_scroll_view);
        filterChips = view.findViewById(R.id.filter_chips);

        // Chat list
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        emptyState = view.findViewById(R.id.empty_state);
    }

    private void setupRecyclerViews() {
        // Initialize data lists
        allChatRooms = new ArrayList<>();
        filteredChatRooms = new ArrayList<>();

        // Get current user ID
        String currentUserId = getCurrentUserId();

        // Setup main chat RecyclerView
        chatRoomAdapter = new ChatRoomAdapter(filteredChatRooms, this::onChatRoomClick, currentUserId);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatRoomAdapter);
    }

    private void setupClickListeners() {
        // My Friends button
        btnMyFriends.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), FriendsActivity.class);
            startActivity(intent);
        });

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                showNotificationsDialog();
            });
        }

        // Search button - toggle search input visibility
        btnSearch.setOnClickListener(v -> toggleSearch());

        // Filter button - toggle filter chips
        btnFilter.setOnClickListener(v -> toggleFilter());

        // Filter chips
        filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterChatsByCategory();
        });

        // Start chat button (in empty state) - only set listener if button exists
        if (btnStartChat != null) {
            btnStartChat.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Start new chat", Toast.LENGTH_SHORT).show();
                // TODO: Implement start chat functionality
            });
        }

        // Search input text watcher
        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterChats(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void showNotificationsDialog() {
        // Get user ID (works for both Firebase Auth and social logins)
        String userId = getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notifications, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.notifications_recycler_view);
        TextView emptyText = dialogView.findViewById(R.id.notifications_empty_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<NotificationItem> notifications = new ArrayList<>();
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setTitle("알림")
            .setView(dialogView)
            .setPositiveButton("닫기", null)
            .create();

        // Use array to make adapter effectively final for lambda
        final NotificationAdapter[] adapterHolder = new NotificationAdapter[1];

        adapterHolder[0] = new NotificationAdapter(notifications, item -> {
            // Handle notification click
            if ("참여자 평가 요청".equals(item.getTitle())) {
                dialog.dismiss();
                // Pass activityId to filter reviews for specific activity
                openPendingReviewsFragment(item.getActivityId());
            }

            // Delete notification from Firebase and remove from list
            deleteNotification(userId, item, notifications, adapterHolder[0], emptyText);
        });
        recyclerView.setAdapter(adapterHolder[0]);

        dialog.show();

        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
            .getReference("userNotifications")
            .child(userId);

        notificationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notifications.clear();
                Log.d(TAG, "Loading notifications, total count: " + snapshot.getChildrenCount());
                for (DataSnapshot child : snapshot.getChildren()) {
                    NotificationItem item = child.getValue(NotificationItem.class);
                    if (item != null) {
                        Log.d(TAG, "Notification loaded - Type: " + item.getType() + ", Title: " + item.getTitle() + ", Message: " + item.getMessage());
                        notifications.add(item);
                    }
                }
                // Sort oldest first (ascending order by timestamp)
                Collections.sort(notifications, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                adapterHolder[0].notifyDataSetChanged();
                emptyText.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                Log.d(TAG, "Total notifications displayed: " + notifications.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load notifications", error.toException());
                Toast.makeText(requireContext(), "알림을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Delete notification from Firebase and remove from list
     */
    private void deleteNotification(String userId, NotificationItem item,
                                    List<NotificationItem> notifications,
                                    NotificationAdapter adapter,
                                    TextView emptyText) {
        if (item.getId() == null || item.getId().isEmpty()) {
            Log.w(TAG, "Cannot delete notification: ID is null or empty");
            return;
        }

        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
            .getReference("userNotifications")
            .child(userId)
            .child(item.getId());

        notificationRef.removeValue()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Notification deleted successfully: " + item.getId());
                // Remove from local list
                int position = notifications.indexOf(item);
                if (position != -1) {
                    notifications.remove(position);
                    adapter.notifyItemRemoved(position);
                }
                // Update empty state
                if (emptyText != null) {
                    emptyText.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to delete notification: " + item.getId(), e);
                Toast.makeText(requireContext(), "알림 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
            });
    }

    private void openPendingReviewsFragment(String activityId) {
        PendingReviewsFragment fragment = PendingReviewsFragment.newInstance(activityId);
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.main_container, fragment)
            .addToBackStack("PendingReviews")
            .commit();
    }

    private void toggleSearch() {
        if (searchLayout.getVisibility() == View.VISIBLE) {
            searchLayout.setVisibility(View.GONE);
            searchInput.setText("");
            filterChats("");
        } else {
            searchLayout.setVisibility(View.VISIBLE);
            searchInput.requestFocus();
        }
    }

    private void toggleFilter() {
        if (filterScrollView.getVisibility() == View.VISIBLE) {
            filterScrollView.setVisibility(View.GONE);
        } else {
            filterScrollView.setVisibility(View.VISIBLE);
        }
    }

    private void filterChatsByCategory() {
        applyFiltersAndSearch();
    }

    private void filterChats(String query) {
        applyFiltersAndSearch();
    }

    private void applyFiltersAndSearch() {
        filteredChatRooms.clear();

        // Get current search query
        String searchQuery = "";
        if (searchInput != null && searchInput.getText() != null) {
            searchQuery = searchInput.getText().toString().toLowerCase();
        }

        // Get selected categories from chips
        List<String> selectedCategories = getSelectedCategories();

        // Apply both filters
        for (ChatRoom chatRoom : allChatRooms) {
            // Check category filter
            boolean matchesCategory = false;
            if (selectedCategories.isEmpty()) {
                // No category filter or "All" selected - show all categories
                matchesCategory = true;
            } else {
                // Handle comma-separated categories (e.g., "운동,스터디")
                String roomCategory = chatRoom.getCategory();
                if (roomCategory != null && !roomCategory.isEmpty()) {
                    // Split by comma and check if any category matches
                    String[] categories = roomCategory.split(",");
                    for (String cat : categories) {
                        if (selectedCategories.contains(cat.trim())) {
                            matchesCategory = true;
                            break;
                        }
                    }
                }
            }

            // Check search filter
            boolean matchesSearch = false;
            if (searchQuery.isEmpty()) {
                matchesSearch = true;
            } else {
                String name = chatRoom.getName() != null ? chatRoom.getName().toLowerCase() : "";
                String lastMsg = chatRoom.getLastMessage() != null ? chatRoom.getLastMessage().toLowerCase() : "";
                matchesSearch = name.contains(searchQuery) || lastMsg.contains(searchQuery);
            }

            // Add chat room if it matches both filters
            if (matchesCategory && matchesSearch) {
                filteredChatRooms.add(chatRoom);
            }
        }

        chatRoomAdapter.notifyDataSetChanged();
        updateUI();
    }

    private List<String> getSelectedCategories() {
        List<String> selectedCategories = new ArrayList<>();

        if (filterChips == null) {
            return selectedCategories;
        }

        List<Integer> checkedChipIds = filterChips.getCheckedChipIds();

        // If "All" is checked or no chips are checked, return empty list (show all)
        if (checkedChipIds.contains(R.id.chip_all) || checkedChipIds.isEmpty()) {
            return selectedCategories;
        }

        // Map chip IDs to category names
        for (Integer chipId : checkedChipIds) {
            String category = getCategoryFromChipId(chipId);
            if (category != null) {
                selectedCategories.add(category);
            }
        }

        return selectedCategories;
    }

    private String getCategoryFromChipId(int chipId) {
        if (chipId == R.id.chip_sports) return "운동";
        if (chipId == R.id.chip_outdoor) return "야외활동";
        if (chipId == R.id.chip_study) return "스터디";
        if (chipId == R.id.chip_culture) return "문화";
        if (chipId == R.id.chip_social) return "소셜";
        if (chipId == R.id.chip_food) return "맛집";
        if (chipId == R.id.chip_travel) return "여행";
        if (chipId == R.id.chip_game) return "게임";
        if (chipId == R.id.chip_hobby) return "취미";
        if (chipId == R.id.chip_volunteer) return "봉사";
        if (chipId == R.id.chip_other) return "기타";

        return null;
    }

    private void updateUI() {
        // Show/hide empty state
        if (filteredChatRooms.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            chatRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            chatRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Load chat rooms from Firebase with real-time updates
     */
    private void loadChatRoomsFromFirebase() {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.w(TAG, "Cannot load chat rooms - no current user ID");
            // Optionally, show a login prompt or handle the logged-out state
            return;
        }

        FirebaseChatManager.getInstance().getJoinedChatRooms(currentUserId, new FirebaseChatManager.ChatRoomListListener() {
            @Override
            public void onChatRoomsLoaded(List<ChatRoom> chatRooms) {
                allChatRooms.clear();
                allChatRooms.addAll(chatRooms);
                for (ChatRoom chatRoom : allChatRooms) {
                    loadUnreadCountForChatRoom(chatRoom);
                }
                applyFiltersAndSearch();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading chat rooms", e);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "채팅방 로드 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /**
     * Load unread count for a specific chat room for the current user
     */
    private void loadUnreadCountForChatRoom(ChatRoom chatRoom) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.w(TAG, "Cannot load unread count - no current user ID");
            return;
        }

        FirebaseChatManager.getInstance().getUnreadCount(
            chatRoom.getId(),
            currentUserId,
            new FirebaseChatManager.OnCompleteListener<Integer>() {
                @Override
                public void onSuccess(Integer unreadCount) {
                    // Update the chat room's unread count
                    chatRoom.setUnreadCount(unreadCount);

                    // Notify adapter to update the UI
                    if (chatRoomAdapter != null) {
                        chatRoomAdapter.notifyDataSetChanged();
                    }

                    // Re-apply filters and search to update the filtered list
                    applyFiltersAndSearch();

                    Log.d(TAG, "Unread count for " + chatRoom.getName() + ": " + unreadCount);
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error loading unread count for chat room: " + chatRoom.getName(), e);
                }
            }
        );
    }

    /**
     * Get current user ID from Firebase Auth or SharedPreferences
     */
    private String getCurrentUserId() {
        // Try Firebase Auth first
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            return firebaseUser.getUid();
        }

        // Fall back to SharedPreferences for social login
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
            return prefs.getString("user_id", "");
        }

        return "";
    }

    private void onChatRoomClick(ChatRoom chatRoom) {
        // Open chat room activity
        Intent intent = new Intent(requireContext(), ChatRoomActivity.class);
        intent.putExtra("chat_room_id", chatRoom.getId());
        intent.putExtra("chat_room", chatRoom);
        startActivity(intent);
    }

    /**
     * Load unread notification count and show/hide badge
     */
    private void loadUnreadNotificationCount() {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (notificationBadge != null) {
                notificationBadge.setVisibility(View.GONE);
            }
            return;
        }

        // Clear previous listener if exists
        if (notificationListener != null) {
            dbRef.child("userNotifications").child(userId).removeEventListener(notificationListener);
        }

        DatabaseReference notificationsRef = dbRef.child("userNotifications").child(userId);
        Log.d(TAG, "Setting up notification badge listener for user: " + userId);
        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int unreadCount = 0;

                Log.d(TAG, "Badge listener: checking " + dataSnapshot.getChildrenCount() + " notifications");
                // Count unread notifications
                for (DataSnapshot notificationSnapshot : dataSnapshot.getChildren()) {
                    NotificationItem notification =
                        notificationSnapshot.getValue(NotificationItem.class);
                    if (notification != null && !notification.isRead()) {
                        unreadCount++;
                        Log.d(TAG, "Badge listener: found unread notification - Type: " + notification.getType() + ", Title: " + notification.getTitle());
                    }
                }

                Log.d(TAG, "Badge listener: total unread count = " + unreadCount);
                // Show/hide badge based on unread count
                if (notificationBadge != null) {
                    if (unreadCount > 0) {
                        notificationBadge.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Badge listener: showing yellow badge");
                    } else {
                        notificationBadge.setVisibility(View.GONE);
                        Log.d(TAG, "Badge listener: hiding badge");
                    }
                }

                Log.d(TAG, "Unread notifications: " + unreadCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load notifications: " + error.getMessage());
                if (notificationBadge != null) {
                    notificationBadge.setVisibility(View.GONE);
                }
            }
        };

        notificationsRef.addValueEventListener(notificationListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload unread counts when fragment resumes (e.g., after returning from chat room)
        for (ChatRoom chatRoom : allChatRooms) {
            loadUnreadCountForChatRoom(chatRoom);
        }
        // Refresh notification badge
        loadUnreadNotificationCount();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up Firebase listeners
        FirebaseChatManager.getInstance().removeAllListeners();

        // Clean up notification listener
        if (notificationListener != null) {
            String userId = getCurrentUserId();
            if (userId != null) {
                dbRef.child("userNotifications").child(userId).removeEventListener(notificationListener);
            }
            notificationListener = null;
        }
    }
}
