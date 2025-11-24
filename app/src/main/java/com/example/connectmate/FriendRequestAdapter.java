package com.example.connectmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {

    private Context context;
    private List<User> userList;
    private String currentUserId;
    private OnFriendRequestAcceptedListener listener;

    public interface OnFriendRequestAcceptedListener {
        void onFriendRequestAccepted(User user);
    }

    public FriendRequestAdapter(Context context, List<User> userList, OnFriendRequestAcceptedListener listener) {
        this.context = context;
        this.userList = userList;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new FriendRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendRequestViewHolder holder, int position) {
        User user = userList.get(position);
        holder.userName.setText(user.getDisplayName());
        Glide.with(context).load(user.getProfileImageUrl()).into(holder.userProfileImage);

        holder.chatButton.setVisibility(View.GONE);
        holder.removeFriendButton.setVisibility(View.GONE);
        holder.addFriendButton.setVisibility(View.VISIBLE);
        holder.addFriendButton.setText(R.string.accept);
        holder.addFriendButton.setOnClickListener(v -> {
            acceptFriendRequest(user);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void acceptFriendRequest(User user) {
        DatabaseReference currentUserFriendsRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId).child("friends").child(user.getUserId());
        currentUserFriendsRef.setValue(true);

        DatabaseReference otherUserFriendsRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUserId()).child("friends").child(currentUserId);
        otherUserFriendsRef.setValue(true);

        DatabaseReference friendRequestRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId).child("friendRequests").child(user.getUserId());
        friendRequestRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (listener != null) {
                    listener.onFriendRequestAccepted(user);
                }
            }
        });
    }

    public static class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        ImageView userProfileImage;
        TextView userName;
        Button addFriendButton;
        Button chatButton;
        Button removeFriendButton;

        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            userProfileImage = itemView.findViewById(R.id.user_profile_image);
            userName = itemView.findViewById(R.id.user_name);
            addFriendButton = itemView.findViewById(R.id.add_friend_button);
            chatButton = itemView.findViewById(R.id.chat_button);
            removeFriendButton = itemView.findViewById(R.id.remove_friend_button);
        }
    }
}