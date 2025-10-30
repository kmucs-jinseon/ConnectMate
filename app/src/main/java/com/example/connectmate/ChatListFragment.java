package com.example.connectmate;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;

public class ChatListFragment extends Fragment {

    // UI Components
    private ImageButton btnSearch;
    private ImageButton btnAddChat;
    private ImageButton btnMoreOptions;
    private TextInputLayout searchLayout;
    private TextInputEditText searchInput;

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
        loadSampleData();
        updateUI();
    }

    private void initializeViews(View view) {
        // Header buttons
        btnSearch = view.findViewById(R.id.btn_search);
        btnAddChat = view.findViewById(R.id.btn_add_chat);
        btnMoreOptions = view.findViewById(R.id.btn_more_options);

        // Search
        searchLayout = view.findViewById(R.id.search_layout);
        searchInput = view.findViewById(R.id.search_input);

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

        // Add chat button
        btnAddChat.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Add new chat", Toast.LENGTH_SHORT).show();
            // TODO: Implement add chat functionality
        });

        // More options button
        btnMoreOptions.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "More options", Toast.LENGTH_SHORT).show();
            // TODO: Implement options menu
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

    private void filterChats(String query) {
        filteredChatRooms.clear();

        if (query.isEmpty()) {
            filteredChatRooms.addAll(allChatRooms);
        } else {
            String lowerQuery = query.toLowerCase();
            for (ChatRoom room : allChatRooms) {
                if (room.getName().toLowerCase().contains(lowerQuery) ||
                    room.getLastMessage().toLowerCase().contains(lowerQuery)) {
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

    private void loadSampleData() {
        // Sample chat rooms
        allChatRooms.add(new ChatRoom("1", "John Doe", "Hey, how are you doing today?", "오후 2:30", 3));
        allChatRooms.add(new ChatRoom("2", "Jane Smith", "See you tomorrow!", "오전 11:20", 0));
        allChatRooms.add(new ChatRoom("3", "Study Group", "Anyone finished the assignment?", "어제", 5));
        allChatRooms.add(new ChatRoom("4", "Family", "Don't forget dinner tonight", "일요일", 0));

        // Sample favorites
        favoriteChatList.add(new FavoriteChat("1", "John", null));
        favoriteChatList.add(new FavoriteChat("2", "Jane", null));
        favoriteChatList.add(new FavoriteChat("5", "Mom", null));

        filteredChatRooms.addAll(allChatRooms);

        chatRoomAdapter.notifyDataSetChanged();
        favoriteChatAdapter.notifyDataSetChanged();
    }

    private void onChatRoomClick(ChatRoom chatRoom) {
        Toast.makeText(requireContext(), "Clicked: " + chatRoom.getName(), Toast.LENGTH_SHORT).show();
        // TODO: Open chat detail activity
    }

    private void onFavoriteChatClick(FavoriteChat favorite) {
        Toast.makeText(requireContext(), "Clicked favorite: " + favorite.getName(), Toast.LENGTH_SHORT).show();
        // TODO: Open chat with favorite
    }

    // Inner class for ChatRoom data
    public static class ChatRoom {
        private final String id;
        private final String name;
        private final String lastMessage;
        private final String timestamp;
        private final int unreadCount;

        public ChatRoom(String id, String name, String lastMessage, String timestamp, int unreadCount) {
            this.id = id;
            this.name = name;
            this.lastMessage = lastMessage;
            this.timestamp = timestamp;
            this.unreadCount = unreadCount;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getLastMessage() { return lastMessage; }
        public String getTimestamp() { return timestamp; }
        public int getUnreadCount() { return unreadCount; }
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
