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
import com.example.connectmate.utils.ChatManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {

    // UI Components
    private ImageButton btnSearch;
    private ImageButton btnFilter;
    private TextInputLayout searchLayout;
    private TextInputEditText searchInput;
    private View filterScrollView;
    private ChipGroup filterChips;

    private LinearLayout favoritesSection;
    private View favoritesDivider;
    private RecyclerView favoritesRecyclerView;
    private RecyclerView chatRecyclerView;
    private LinearLayout emptyState;
    private MaterialButton btnStartChat;

    // Adapters
    private ChatRoomAdapter chatRoomAdapter;
    private FavoriteChatAdapter favoriteChatAdapter;

    // Data
    private List<ChatRoom> allChatRooms;
    private List<ChatRoom> filteredChatRooms;
    private List<FavoriteChat> favoriteChatList;
    private String currentUserId;

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

        getCurrentUserId();
        initializeViews(view);
        setupRecyclerViews();
        setupClickListeners();
    }

    private void getCurrentUserId() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        } else {
            SharedPreferences prefs = requireActivity().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
            currentUserId = prefs.getString("user_id", "");
        }
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

        // Favorites section
        favoritesSection = view.findViewById(R.id.favorites_section);
        favoritesDivider = view.findViewById(R.id.favorites_divider);
        favoritesRecyclerView = view.findViewById(R.id.favorites_recycler_view);

        // Chat list
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        emptyState = view.findViewById(R.id.empty_state);
        btnStartChat = view.findViewById(R.id.btn_start_chat);
    }

    private void setupRecyclerViews() {
        // Initialize data lists
        allChatRooms = new ArrayList<>();
        filteredChatRooms = new ArrayList<>();
        favoriteChatList = new ArrayList<>();

        // Setup main chat RecyclerView
        chatRoomAdapter = new ChatRoomAdapter(filteredChatRooms, this::onChatRoomClick);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatRoomAdapter);

        // Setup favorites RecyclerView (horizontal)
        favoriteChatAdapter = new FavoriteChatAdapter(favoriteChatList, this::onFavoriteChatClick);
        favoritesRecyclerView.setLayoutManager(
            new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        favoritesRecyclerView.setAdapter(favoriteChatAdapter);
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
        // Get selected categories from chips
        if (filterChips.findViewById(R.id.chip_all).isSelected() ||
            filterChips.getCheckedChipIds().isEmpty()) {
            // Show all
            filteredChatRooms.clear();
            filteredChatRooms.addAll(allChatRooms);
        } else {
            filteredChatRooms.clear();
            // Filter by selected categories
            // TODO: Implement actual filtering logic based on chat room categories
            filteredChatRooms.addAll(allChatRooms);
        }

        chatRoomAdapter.notifyDataSetChanged();
        updateUI();
    }

    private void filterChats(String query) {
        filteredChatRooms.clear();

        if (query.isEmpty()) {
            filteredChatRooms.addAll(allChatRooms);
        } else {
            String lowerQuery = query.toLowerCase();
            for (ChatRoom room : allChatRooms) {
                if (room.getName().toLowerCase().contains(lowerQuery) ||
                    (room.getLastMessage() != null && room.getLastMessage().toLowerCase().contains(lowerQuery))) {
                    filteredChatRooms.add(room);
                }
            }
        }

        chatRoomAdapter.notifyDataSetChanged();
        updateUI();
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

        // Show/hide favorites section
        if (favoriteChatList.isEmpty()) {
            favoritesSection.setVisibility(View.GONE);
            favoritesDivider.setVisibility(View.GONE);
        } else {
            favoritesSection.setVisibility(View.VISIBLE);
            favoritesDivider.setVisibility(View.VISIBLE);
        }
    }

    private void loadData() {
        // Load chat rooms from ChatManager for the current user
        ChatManager chatManager = ChatManager.getInstance(requireContext());
        allChatRooms.clear();
        allChatRooms.addAll(chatManager.getChatRoomsForUser(currentUserId));

        // Clear and reload favorites to prevent duplicates
        favoriteChatList.clear();
        // Sample favorites - using circle_logo as default profile image
        favoriteChatList.add(new FavoriteChat("1", "Jane", null));
        favoriteChatList.add(new FavoriteChat("2", "Mom", null));
        // Note: Users can customize these with their own profile images

        filteredChatRooms.clear(); // Also clear filtered list before adding
        filteredChatRooms.addAll(allChatRooms);

        chatRoomAdapter.notifyDataSetChanged();
        favoriteChatAdapter.notifyDataSetChanged();
    }

    private void onChatRoomClick(ChatRoom chatRoom) {
        // Open chat room activity
        Intent intent = new Intent(requireContext(), ChatRoomActivity.class);
        intent.putExtra("chat_room_id", chatRoom.getId());
        intent.putExtra("chat_room", chatRoom);
        startActivity(intent);
    }

    private void onFavoriteChatClick(FavoriteChat favorite) {
        Toast.makeText(requireContext(), "Clicked favorite: " + favorite.getName(), Toast.LENGTH_SHORT).show();
        // TODO: Open chat with favorite
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload chat rooms when fragment becomes visible
        loadData();
        updateUI();
    }

    // Inner class for FavoriteChat data
    public static class FavoriteChat {
        private final String id;
        private final String name;
        private final String profileImageUrl;

        public FavoriteChat(String id, String name, String profileImageUrl) {
            this.id = id;
            this.name = name;
            this.profileImageUrl = profileImageUrl;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getProfileImageUrl() { return profileImageUrl; }
    }
}
