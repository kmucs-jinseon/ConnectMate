package com.example.connectmate;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.Activity;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.utils.ActivityManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.label.Label;
import com.kakao.vectormap.label.LabelLayer;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapFragment extends Fragment {

    // ... (Constants and other variables remain the same)
    private static final String TAG = "MapFragment";
    private static final String CHAT_ROOM_LABEL_PREFIX = "chatroom_";
    private MapView mapView;
    private KakaoMap kakaoMap;
    private boolean isMapViewReady = false;
    private EditText searchEditText;
    private ImageButton searchIconButton;
    private ImageButton clearSearchButton;
    private CardView searchResultsCard;
    private RecyclerView searchResultsRecycler;
    private SearchResultsAdapter searchResultsAdapter;
    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<Activity> activities;
    private LocationManager locationManager;
    private Map<String, ChatRoom> displayedChatRooms = new HashMap<>();
    private String currentUserId;
    private DatabaseReference chatRoomsRef;
    private ValueEventListener chatRoomsListener;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        initializeViews(view);
        getCurrentUserId();
        setupFirebase();
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        activities = new ArrayList<>();
        setupSearchBar();
        mapView.start(mapLifeCycleCallback, kakaoMapReadyCallback);
        return view;
    }

    private void createNewChatRoom(String roomName, LatLng latLng) {
        String roomId = chatRoomsRef.push().getKey();
        if (roomId == null) return;

        long timestamp = System.currentTimeMillis();
        ChatRoom newRoom = new ChatRoom(roomId, roomName, currentUserId, "Chat room created!", timestamp, latLng.latitude, latLng.longitude);

        chatRoomsRef.child(roomId).setValue(newRoom)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Chat room created!", Toast.LENGTH_SHORT).show();
                // Navigate to the newly created chat room
                Intent intent = new Intent(getContext(), ChatRoomActivity.class);
                intent.putExtra("chat_room", newRoom);
                startActivity(intent);
            })
            .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ... (All other methods remain unchanged)
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
        mapView = view.findViewById(R.id.map_view);
        searchEditText = view.findViewById(R.id.search_edit_text);
        searchIconButton = view.findViewById(R.id.search_icon_button);
        clearSearchButton = view.findViewById(R.id.clear_search_button);
        searchResultsCard = view.findViewById(R.id.search_results_card);
        searchResultsRecycler = view.findViewById(R.id.search_results_recycler);
    }
    private void setupFirebase() {
        chatRoomsRef = FirebaseDatabase.getInstance().getReference("chat_rooms");
        chatRoomsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                displayedChatRooms.clear();
                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    ChatRoom chatRoom = roomSnapshot.getValue(ChatRoom.class);
                    if (chatRoom != null && chatRoom.getLatitude() != 0 && chatRoom.getLongitude() != 0) {
                        displayedChatRooms.put(chatRoom.getId(), chatRoom);
                    }
                }
                addChatRoomMarkers();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load chat rooms.", error.toException());
            }
        };
    }
    private final MapLifeCycleCallback mapLifeCycleCallback = new MapLifeCycleCallback() {
        @Override public void onMapDestroy() { isMapViewReady = false; }
        @Override public void onMapError(Exception error) { Log.e(TAG, "Map Error", error); }
    };
    private final KakaoMapReadyCallback kakaoMapReadyCallback = new KakaoMapReadyCallback() {
        @Override
        public void onMapReady(@NonNull KakaoMap map) {
            kakaoMap = map;
            isMapViewReady = true;
            setupMap();
        }
    };
    private void setupMap() {
        moveToCurrentLocation();
        addSampleActivityMarkers();
        setupMapEventListeners();
        addChatRoomMarkers();
    }
    private void setupMapEventListeners() {
        if (kakaoMap == null) return;
        kakaoMap.setOnMapLongClickListener((kakaoMap, latLng) -> showCreateChatRoomDialog(latLng));
        kakaoMap.setOnLabelClickListener((kakaoMapInstance, layer, label) -> {
            String labelId = label.getLabelId();
            if (labelId.startsWith(CHAT_ROOM_LABEL_PREFIX)) {
                String chatRoomId = labelId.substring(CHAT_ROOM_LABEL_PREFIX.length());
                ChatRoom clickedRoom = displayedChatRooms.get(chatRoomId);
                if (clickedRoom != null) {
                    Intent intent = new Intent(getContext(), ChatRoomActivity.class);
                    intent.putExtra("chat_room", clickedRoom);
                    startActivity(intent);
                }
            } else {
                Activity clickedActivity = findActivityById(labelId);
                if (clickedActivity != null) {
                    Intent intent = new Intent(getContext(), ActivityDetailActivity.class);
                    intent.putExtra("activity", clickedActivity);
                    startActivity(intent);
                }
            }
            return true;
        });
    }
    private void addChatRoomMarkers() {
        if (kakaoMap == null || kakaoMap.getLabelManager() == null) return;
        LabelLayer layer = kakaoMap.getLabelManager().getLayer();
        if (layer == null) return;
        layer.removeLabels(layer.getLabels(label -> label.getLabelId().startsWith(CHAT_ROOM_LABEL_PREFIX)));
        LabelStyles chatMarkerStyle = kakaoMap.getLabelManager().addLabelStyles(LabelStyles.from(LabelStyle.from(R.drawable.ic_chat)));
        for (ChatRoom room : displayedChatRooms.values()) {
            LabelOptions options = LabelOptions.from(CHAT_ROOM_LABEL_PREFIX + room.getId(), LatLng.from(room.getLatitude(), room.getLongitude()))
                .setStyles(chatMarkerStyle);
            layer.addLabel(options);
        }
    }
    private void addSampleActivityMarkers() { /* Omitted */ }
    private Activity findActivityById(String id) { /* Omitted */ return null; }
    private void moveToCurrentLocation() { /* Omitted */ }
    private void moveToDefaultLocation() { /* Omitted */ }
    private boolean hasLocationPermission() { /* Omitted */ return false; }
    private void setupSearchBar() {
        searchResultsAdapter = new SearchResultsAdapter(this::onSearchResultSelected);
        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsRecycler.setAdapter(searchResultsAdapter);
        searchIconButton.setOnClickListener(v -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchEditText.getText().toString().trim());
                return true;
            }
            return false;
        });
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchResultsCard.setVisibility(View.GONE);
        });
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (s.length() == 0) searchResultsCard.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
    private void performSearch(String query) {
        if (query.trim().isEmpty()) return;
        hideKeyboard();
        searchExecutor.execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                URL url = new URL("https://dapi.kakao.com/v2/local/search/keyword.json?query=" + encodedQuery);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "KakaoAK " + BuildConfig.KAKAO_APP_KEY);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) { /* Omitted */ }
            } catch (Exception e) { /* Omitted */ }
        });
    }
    private void onSearchResultSelected(SearchResult result) { /* Omitted */ }
    private List<SearchResult> parseSearchResults(String json) { /* Omitted */ return new ArrayList<>(); }
    private void showCreateChatRoomDialog(LatLng latLng) {
        if (currentUserId == null || currentUserId.equals("anonymous")) {
            Toast.makeText(getContext(), "You must be logged in to create a chat room.", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create Chat Room");
        final TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint("Enter chat room name");
        builder.setView(input);
        builder.setPositiveButton("Create", (dialog, which) -> {
            String roomName = input.getText().toString().trim();
            if (!roomName.isEmpty()) {
                createNewChatRoom(roomName, latLng);
            } else {
                Toast.makeText(getContext(), "Name cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && requireActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), 0);
        }
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
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.resume();
    }
    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.pause();
    }
}
