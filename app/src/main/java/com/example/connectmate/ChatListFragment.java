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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.utils.FirebaseChatManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {

    private static final String TAG = "ChatListFragment";

    // UI Components
    private ImageButton btnSearch;
    private ImageButton btnFilter;
    private TextInputLayout searchLayout;
    private TextInputEditText searchInput;
    private View filterScrollView;
    private ChipGroup filterChips;

    private RecyclerView chatRecyclerView;
    private LinearLayout emptyState;
    private MaterialButton btnStartChat;

    // Adapters
    private ChatRoomAdapter chatRoomAdapter;

    // Data
    private List<ChatRoom> allChatRooms;
    private List<ChatRoom> filteredChatRooms;

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

        initializeViews(view);
        setupRecyclerViews();
        setupClickListeners();
        loadChatRoomsFromFirebase();
        updateUI();
    }

    private void initializeViews(View view) {
        // Header buttons
        btnSearch = view.findViewById(R.id.btn_search);
        btnFilter = view.findViewById(R.id.btn_filter);

        // Search
        searchLayout = view.findViewById(R.id.search_layout);
        searchInput = view.findViewById(R.id.search_input);

        // Filter chips
        filterScrollView = view.findViewById(R.id.filter_scroll_view);
        filterChips = view.findViewById(R.id.filter_chips);

        // Chat list
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        emptyState = view.findViewById(R.id.empty_state);
        btnStartChat = view.findViewById(R.id.btn_start_chat);
    }

    private void setupRecyclerViews() {
        // Initialize data lists
        allChatRooms = new ArrayList<>();
        filteredChatRooms = new ArrayList<>();

        // Setup main chat RecyclerView
        chatRoomAdapter = new ChatRoomAdapter(filteredChatRooms, this::onChatRoomClick);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatRoomAdapter);
    }

    private void setupClickListeners() {
        // Search button - toggle search input visibility
        btnSearch.setOnClickListener(v -> toggleSearch());

        // Filter button - toggle filter chips
        btnFilter.setOnClickListener(v -> toggleFilter());

        // Filter chips
        filterChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterChatsByCategory();
        });

        // Start chat button (in empty state)
        btnStartChat.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Start new chat", Toast.LENGTH_SHORT).show();
            // TODO: Implement start chat functionality
        });

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
                String roomCategory = chatRoom.getCategory();
                matchesCategory = roomCategory != null && selectedCategories.contains(roomCategory);
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
        FirebaseChatManager chatManager = FirebaseChatManager.getInstance();

        // Listen for chat room changes in real-time
        chatManager.listenForChatRoomChanges(new FirebaseChatManager.ChatRoomChangeListener() {
            @Override
            public void onChatRoomAdded(ChatRoom chatRoom) {
                // Check if chat room already exists to avoid duplicates
                boolean exists = false;
                for (ChatRoom room : allChatRooms) {
                    if (room.getId().equals(chatRoom.getId())) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    allChatRooms.add(0, chatRoom); // Add to beginning (newest first)

                    // Load unread count for this chat room
                    loadUnreadCountForChatRoom(chatRoom);

                    Log.d(TAG, "Chat room added: " + chatRoom.getName());
                }
            }

            @Override
            public void onChatRoomChanged(ChatRoom chatRoom) {
                // Update existing chat room
                for (int i = 0; i < allChatRooms.size(); i++) {
                    if (allChatRooms.get(i).getId().equals(chatRoom.getId())) {
                        allChatRooms.set(i, chatRoom);
                        break;
                    }
                }

                // Load unread count for this chat room
                loadUnreadCountForChatRoom(chatRoom);

                Log.d(TAG, "Chat room updated: " + chatRoom.getName());
            }

            @Override
            public void onChatRoomRemoved(ChatRoom chatRoom) {
                // Remove chat room from lists
                allChatRooms.removeIf(room -> room.getId().equals(chatRoom.getId()));

                // Re-apply filters and search to update the filtered list
                applyFiltersAndSearch();

                Log.d(TAG, "Chat room removed: " + chatRoom.getName());
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

    @Override
    public void onResume() {
        super.onResume();
        // Reload unread counts when fragment resumes (e.g., after returning from chat room)
        for (ChatRoom chatRoom : allChatRooms) {
            loadUnreadCountForChatRoom(chatRoom);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up Firebase listeners
        FirebaseChatManager.getInstance().removeAllListeners();
    }
}
