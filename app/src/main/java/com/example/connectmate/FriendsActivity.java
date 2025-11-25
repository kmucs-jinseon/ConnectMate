package com.example.connectmate;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.utils.FirebaseChatManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class FriendsActivity extends AppCompatActivity implements UserAdapter.OnChatClickListener, UserAdapter.OnRemoveFriendClickListener, FriendRequestAdapter.OnFriendRequestAcceptedListener {

    private static final String TAG = "FriendsActivity";

    private RecyclerView friendsRecyclerView;
    private RecyclerView friendRequestsRecyclerView;
    private UserAdapter userAdapter;
    private FriendRequestAdapter friendRequestAdapter;
    private List<User> friendList;
    private List<User> friendRequestList;
    private DatabaseReference usersRef;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.my_friends); // Set title
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User is not logged in. Finishing FriendsActivity.");
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();
        
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        initializeViews();
        setupAdapters();
        loadFriends();
        loadFriendRequests();
    }

    private void initializeViews() {
        friendsRecyclerView = findViewById(R.id.friends_recycler_view);
        friendRequestsRecyclerView = findViewById(R.id.friend_requests_recycler_view);

        // Set LayoutManagers
        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        friendRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupAdapters() {
        friendList = new ArrayList<>();
        friendRequestList = new ArrayList<>();

        userAdapter = new UserAdapter(this, friendList, this, this);
        friendRequestAdapter = new FriendRequestAdapter(this, friendRequestList, this);

        friendsRecyclerView.setAdapter(userAdapter);
        friendRequestsRecyclerView.setAdapter(friendRequestAdapter);
    }

    private void loadFriends() {
        usersRef.child(currentUserId).child("friends").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendList.clear();
                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    String friendId = friendSnapshot.getKey();
                    if (friendId != null) {
                        usersRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                User user = userSnapshot.getValue(User.class);
                                if (user != null && user.getFriends() != null && user.getFriends().containsKey(currentUserId)) {
                                    friendList.add(user);
                                }
                                userAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.w(TAG, "loadFriends:onCancelled", error.toException());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadFriends:onCancelled", error.toException());
            }
        });
    }

    private void loadFriendRequests() {
        usersRef.child(currentUserId).child("friendRequests").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendRequestList.clear();
                for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                    String requesterId = requestSnapshot.getKey();
                    if (requesterId != null) {
                        usersRef.child(requesterId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                User user = userSnapshot.getValue(User.class);
                                if (user != null) {
                                    friendRequestList.add(user);
                                }
                                friendRequestAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.w(TAG, "loadFriendRequests:onCancelled", error.toException());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadFriendRequests:onCancelled", error.toException());
            }
        });
    }

    @Override
    public void onChatClick(User user) {
        String chatRoomId = getChatRoomId(currentUserId, user.getUserId());
        FirebaseChatManager.getInstance().getChatRoomById(chatRoomId, new FirebaseChatManager.OnCompleteListener<ChatRoom>() {
            @Override
            public void onSuccess(ChatRoom chatRoom) {
                if (chatRoom == null) {
                    // Create new chat room
                    String chatRoomName = user.getDisplayName();
                    ChatRoom newChatRoom = new ChatRoom(chatRoomName, null);
                    newChatRoom.setId(chatRoomId);
                    newChatRoom.setCategory("private");

                    Map<String, ChatRoom.Member> members = new HashMap<>();
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser == null) return;
                    
                    members.put(currentUserId, new ChatRoom.Member(currentUser.getDisplayName(), 0));
                    members.put(user.getUserId(), new ChatRoom.Member(user.getDisplayName(), 0));
                    newChatRoom.setMembers(members);

                    FirebaseChatManager.getInstance().saveChatRoom(newChatRoom, new FirebaseChatManager.OnCompleteListener<ChatRoom>() {
                        @Override
                        public void onSuccess(ChatRoom result) {
                            openChatRoom(result, chatRoomName);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error creating chat room", e);
                            Toast.makeText(FriendsActivity.this, "채팅방 생성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    openChatRoom(chatRoom, user.getDisplayName());
                }
            }

            @Override
            public void onError(Exception e) {
                 Log.e(TAG, "Error getting chat room", e);
                 Toast.makeText(FriendsActivity.this, "채팅방 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openChatRoom(ChatRoom chatRoom, String chatRoomName) {
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("chat_room", chatRoom);
        intent.putExtra("chat_room_name", chatRoomName);
        startActivity(intent);
    }

    private String getChatRoomId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) > 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    @Override
    public void onRemoveFriendClick(User user) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_friend_title)
                .setMessage(getString(R.string.remove_friend_message, user.getDisplayName()))
                .setPositiveButton(R.string.remove, (dialog, which) -> {
                    usersRef.child(currentUserId).child("friends").child(user.getUserId()).removeValue();
                    usersRef.child(user.getUserId()).child("friends").child(currentUserId).removeValue();

                    String chatRoomId = getChatRoomId(currentUserId, user.getUserId());
                    FirebaseChatManager.getInstance().deleteChatRoom(chatRoomId, new FirebaseChatManager.OnCompleteListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(FriendsActivity.this, user.getDisplayName() + "님과의 연결이 정리되었습니다.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error deleting chat room", e);
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onFriendRequestAccepted(User user) {
        friendRequestList.remove(user);
        friendRequestAdapter.notifyDataSetChanged();
    }
}
