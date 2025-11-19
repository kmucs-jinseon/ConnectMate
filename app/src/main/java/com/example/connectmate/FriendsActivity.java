package com.example.connectmate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.utils.FirebaseChatManager;
import com.google.firebase.auth.FirebaseAuth;
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

        friendsRecyclerView = findViewById(R.id.friends_recycler_view);
        friendRequestsRecyclerView = findViewById(R.id.friend_requests_recycler_view);

        friendList = new ArrayList<>();
        friendRequestList = new ArrayList<>();

        userAdapter = new UserAdapter(this, friendList, this, this);
        friendRequestAdapter = new FriendRequestAdapter(this, friendRequestList, this);

        friendsRecyclerView.setAdapter(userAdapter);
        friendRequestsRecyclerView.setAdapter(friendRequestAdapter);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        loadFriends();
        loadFriendRequests();
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
                                if (user != null && user.getFriends().containsKey(currentUserId)) {
                                    friendList.add(user);
                                    userAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Handle error
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
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
                                    friendRequestAdapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Handle error
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
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
                    String chatRoomName = user.getDisplayName() + " and " + FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                    ChatRoom newChatRoom = new ChatRoom(chatRoomName, null);
                    newChatRoom.setId(chatRoomId);
                    newChatRoom.setCategory("private");

                    Map<String, ChatRoom.Member> members = new HashMap<>();
                    members.put(currentUserId, new ChatRoom.Member(FirebaseAuth.getInstance().getCurrentUser().getDisplayName(), 0));
                    members.put(user.getUserId(), new ChatRoom.Member(user.getDisplayName(), 0));
                    newChatRoom.setMembers(members);

                    FirebaseChatManager.getInstance().saveChatRoom(newChatRoom, new FirebaseChatManager.OnCompleteListener<ChatRoom>() {
                        @Override
                        public void onSuccess(ChatRoom result) {
                            openChatRoom(result);
                        }

                        @Override
                        public void onError(Exception e) {
                            // Handle error
                        }
                    });
                } else {
                    openChatRoom(chatRoom);
                }
            }

            @Override
            public void onError(Exception e) {
                // Handle error
            }
        });
    }

    private void openChatRoom(ChatRoom chatRoom) {
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("chat_room", chatRoom);
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
                .setTitle("Remove Friend")
                .setMessage("Are you sure you want to remove " + user.getDisplayName() + " as your friend? This will also delete your 1-on-1 chat room.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    // Remove friend from both users' friend lists
                    usersRef.child(currentUserId).child("friends").child(user.getUserId()).removeValue();
                    usersRef.child(user.getUserId()).child("friends").child(currentUserId).removeValue();

                    // Delete the 1-on-1 chat room
                    String chatRoomId = getChatRoomId(currentUserId, user.getUserId());
                    FirebaseChatManager.getInstance().deleteChatRoom(chatRoomId, new FirebaseChatManager.OnCompleteListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(FriendsActivity.this, "Friend and chat room removed.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            // Handle error, but still update UI
                        }
                    });

                    // Immediately update UI
                    friendList.remove(user);
                    userAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onFriendRequestAccepted(User user) {
        // The ValueEventListeners in loadFriends() and loadFriendRequests() will handle the UI updates.
    }
}
