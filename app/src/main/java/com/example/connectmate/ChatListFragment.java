package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.ChatRoom;
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

    // ... (UI Components and other variables remain the same)
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
    private ChatRoomAdapter chatRoomAdapter;
    private FavoriteChatAdapter favoriteChatAdapter;
    private List<ChatRoom> allChatRooms;
    private List<ChatRoom> filteredChatRooms;
    private List<FavoriteChat> favoriteChatList;
    private DatabaseReference chatRoomsRef;
    private ValueEventListener chatRoomsListener;
    private String currentUserId;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        getCurrentUserId();
        setupRecyclerViews();
        setupClickListeners();
        setupFirebase();
        loadFavoriteChats();
    }

    private void getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        } else {
            SharedPreferences prefs = requireActivity().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
            currentUserId = prefs.getString("user_id", "anonymous");
        }
    }
    
    private void initializeViews(View view) {
        btnSearch = view.findViewById(R.id.btn_search);
        btnFilter = view.findViewById(R.id.btn_filter);
        searchLayout = view.findViewById(R.id.search_layout);
        searchInput = view.findViewById(R.id.search_input);
        filterScrollView = view.findViewById(R.id.filter_scroll_view);
        filterChips = view.findViewById(R.id.filter_chips);
        favoritesSection = view.findViewById(R.id.favorites_section);
        favoritesDivider = view.findViewById(R.id.favorites_divider);
        favoritesRecyclerView = view.findViewById(R.id.favorites_recycler_view);
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        emptyState = view.findViewById(R.id.empty_state);
        btnStartChat = view.findViewById(R.id.btn_start_chat);
    }

    private void setupRecyclerViews() {
        allChatRooms = new ArrayList<>();
        filteredChatRooms = new ArrayList<>();
        favoriteChatList = new ArrayList<>();

        chatRoomAdapter = new ChatRoomAdapter(filteredChatRooms, this::onChatRoomClick);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatRoomAdapter);

        favoriteChatAdapter = new FavoriteChatAdapter(favoriteChatList, this::onFavoriteChatClick);
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        favoritesRecyclerView.setAdapter(favoriteChatAdapter);
    }

    private void setupClickListeners() {
        btnSearch.setOnClickListener(v -> toggleSearch());
        btnFilter.setOnClickListener(v -> toggleFilter());
        btnStartChat.setOnClickListener(v -> showCreateChatRoomDialog());

        if (searchInput != null) {
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterChats(s.toString()); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupFirebase() {
        chatRoomsRef = FirebaseDatabase.getInstance().getReference("chat_rooms");
        chatRoomsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allChatRooms.clear();
                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    ChatRoom chatRoom = roomSnapshot.getValue(ChatRoom.class);
                    // For this list, we only show rooms without a location (or handle them differently)
                    if (chatRoom != null && chatRoom.getLatitude() == 0 && chatRoom.getLongitude() == 0) {
                        allChatRooms.add(chatRoom);
                    }
                }
                Collections.sort(allChatRooms, (o1, o2) -> Long.compare(o2.getLastMessageTimestamp(), o1.getLastMessageTimestamp()));
                filterChats(searchInput.getText().toString());
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load chat rooms.", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void showCreateChatRoomDialog() {
        if (currentUserId == null || currentUserId.equals("anonymous")) {
            Toast.makeText(getContext(), "You must be logged in to create a chat room.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create New Chat Room");
        final TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint("Enter chat room name");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String roomName = input.getText().toString().trim();
            if (!roomName.isEmpty()) {
                createNewChatRoom(roomName);
            } else {
                Toast.makeText(requireContext(), "Chat room name cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void createNewChatRoom(String roomName) {
        String roomId = chatRoomsRef.push().getKey();
        if (roomId == null) return;

        long timestamp = System.currentTimeMillis();
        // Create a non-location-based chat room
        ChatRoom newRoom = new ChatRoom(roomId, roomName, currentUserId, "New chat room created.", timestamp, 0, 0);

        chatRoomsRef.child(roomId).setValue(newRoom)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Chat room created!", Toast.LENGTH_SHORT).show();
                onChatRoomClick(newRoom);
            })
            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    
    // ... (Other methods like loadFavoriteChats, toggleSearch, filterChats, updateUI, onChatRoomClick, etc. remain the same)
    private void loadFavoriteChats() {
        favoriteChatList.clear();
        favoriteChatList.add(new FavoriteChat("1", "Jane", null));
        favoriteChatList.add(new FavoriteChat("2", "Mom", null));
        favoriteChatAdapter.notifyDataSetChanged();
        updateUI();
    }
    private void toggleSearch() {
        searchLayout.setVisibility(searchLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        if (searchLayout.getVisibility() == View.VISIBLE) searchInput.requestFocus();
        else searchInput.setText("");
    }
    private void toggleFilter() {
        filterScrollView.setVisibility(filterScrollView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }
    private void filterChats(String query) {
        filteredChatRooms.clear();
        if (query.isEmpty()) {
            filteredChatRooms.addAll(allChatRooms);
        } else {
            String lowerQuery = query.toLowerCase();
            for (ChatRoom room : allChatRooms) {
                if (room.getName().toLowerCase().contains(lowerQuery)) {
                    filteredChatRooms.add(room);
                }
            }
        }
        chatRoomAdapter.notifyDataSetChanged();
        updateUI();
    }
    private void updateUI() {
        emptyState.setVisibility(filteredChatRooms.isEmpty() ? View.VISIBLE : View.GONE);
        chatRecyclerView.setVisibility(filteredChatRooms.isEmpty() ? View.GONE : View.VISIBLE);
        boolean hasFavorites = !favoriteChatList.isEmpty();
        favoritesSection.setVisibility(hasFavorites ? View.VISIBLE : View.GONE);
        favoritesDivider.setVisibility(hasFavorites ? View.VISIBLE : View.GONE);
    }
    private void onChatRoomClick(ChatRoom chatRoom) {
        Intent intent = new Intent(requireContext(), ChatRoomActivity.class);
        intent.putExtra("chat_room", chatRoom);
        startActivity(intent);
    }
    private void onFavoriteChatClick(FavoriteChat favorite) {
        Toast.makeText(requireContext(), "Clicked favorite: " + favorite.getName(), Toast.LENGTH_SHORT).show();
    }
    public static class FavoriteChat {
        private final String id; private final String name; private final String profileImageUrl;
        public FavoriteChat(String id, String name, String profileImageUrl) { this.id = id; this.name = name; this.profileImageUrl = profileImageUrl; }
        public String getId() { return id; } public String getName() { return name; } public String getProfileImageUrl() { return profileImageUrl; }
    }


    @Override
    public void onStart() {
        super.onStart();
        chatRoomsRef.addValueEventListener(chatRoomsListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        chatRoomsRef.removeEventListener(chatRoomsListener);
    }
}
