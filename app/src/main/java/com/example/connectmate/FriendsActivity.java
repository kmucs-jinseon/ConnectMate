package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.utils.FirebaseChatManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
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

public class FriendsActivity extends AppCompatActivity implements UserAdapter.OnChatClickListener, UserAdapter.OnRemoveFriendClickListener, FriendRequestAdapter.OnFriendRequestAcceptedListener, FriendRequestAdapter.OnFriendRequestRejectedListener {

    private static final String TAG = "FriendsActivity";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private String currentUserId;
    public DatabaseReference usersRef;
    public List<User> friendList = new ArrayList<>();
    public List<User> friendRequestList = new ArrayList<>();
    public UserAdapter userAdapter;
    public FriendRequestAdapter friendRequestAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.my_friends);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Get user ID from SharedPreferences for consistency across all login methods
        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", null);

        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "User ID not found in SharedPreferences. Finishing FriendsActivity.");
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Initialize adapters
        userAdapter = new UserAdapter(this, friendList, this, this);
        friendRequestAdapter = new FriendRequestAdapter(this, friendRequestList, this, currentUserId);
        friendRequestAdapter.setOnFriendRequestRejectedListener(this);

        // Setup ViewPager2 with tabs
        setupViewPager();
        loadFriends();
        loadFriendRequests();
    }

    private void setupViewPager() {
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        FriendsViewPagerAdapter adapter = new FriendsViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("친구");
                    break;
                case 1:
                    tab.setText("친구 요청");
                    break;
            }
        }).attach();
    }

    private void loadFriends() {
        usersRef.child(currentUserId).child("friends").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                friendList.clear();
                 if (!snapshot.exists()) {
                    // This is normal if the user has no friends or the node was just created.
                    Log.d(TAG, "Friends node does not exist or is empty.");
                    userAdapter.notifyDataSetChanged();
                    return;
                }
                for (DataSnapshot friendSnapshot : snapshot.getChildren()) {
                    String friendId = friendSnapshot.getKey();
                    if (friendId != null) {
                        usersRef.child(friendId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                User user = userSnapshot.getValue(User.class);
                                if (user != null) {
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
                if (!snapshot.exists()){
                     friendRequestAdapter.notifyDataSetChanged();
                     return;
                }
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
        if (user == null || user.getUserId() == null || user.getUserId().isEmpty()) {
            Toast.makeText(this, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onChatClick: user or userId is null");
            return;
        }

        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onChatClick: currentUserId is null");
            return;
        }

        Log.d(TAG, "Opening chat with user: " + user.getDisplayName() + " (ID: " + user.getUserId() + ")");

        String chatRoomId = getChatRoomId(currentUserId, user.getUserId());
        String chatRoomName = user.getDisplayName();

        // Simply pass the chat room ID to ChatRoomActivity
        // It will handle loading/creating the room
        Intent intent = new Intent(this, ChatRoomActivity.class);
        intent.putExtra("chat_room_id", chatRoomId);
        intent.putExtra("chat_room_name", chatRoomName);
        intent.putExtra("is_private_chat", true);
        intent.putExtra("other_user_id", user.getUserId());
        intent.putExtra("other_user_name", user.getDisplayName());
        startActivity(intent);
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

    @Override
    public void onFriendRequestRejected(User user) {
        friendRequestList.remove(user);
        friendRequestAdapter.notifyDataSetChanged();
        Toast.makeText(this, user.getDisplayName() + "님의 친구 요청을 거절했습니다.", Toast.LENGTH_SHORT).show();
    }

    // ViewPager Adapter
    private class FriendsViewPagerAdapter extends FragmentStateAdapter {
        public FriendsViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new FriendsListFragment();
                case 1:
                    return new FriendRequestsFragment();
                default:
                    return new FriendsListFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    // Friends List Fragment
    public static class FriendsListFragment extends Fragment {
        private RecyclerView recyclerView;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_friends_list, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            recyclerView = view.findViewById(R.id.friends_recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            if (getActivity() instanceof FriendsActivity) {
                FriendsActivity activity = (FriendsActivity) getActivity();
                recyclerView.setAdapter(activity.userAdapter);
            }
        }
    }

    // Friend Requests Fragment
    public static class FriendRequestsFragment extends Fragment {
        private RecyclerView recyclerView;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_friend_requests, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            recyclerView = view.findViewById(R.id.friend_requests_recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            if (getActivity() instanceof FriendsActivity) {
                FriendsActivity activity = (FriendsActivity) getActivity();
                recyclerView.setAdapter(activity.friendRequestAdapter);
            }
        }
    }
}
