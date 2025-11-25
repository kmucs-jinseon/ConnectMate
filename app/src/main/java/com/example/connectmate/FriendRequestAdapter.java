package com.example.connectmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            this.currentUserId = currentUser.getUid();
        }
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

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(context).load(user.getProfileImageUrl()).placeholder(R.drawable.ic_profile).into(holder.userProfileImage);
        } else {
            holder.userProfileImage.setImageResource(R.drawable.ic_profile);
        }

        // In FriendRequestAdapter, only "add" button is visible.
        if (holder.chatButton != null) {
            holder.chatButton.setVisibility(View.GONE);
        }
        if (holder.removeFriendButton != null) {
            holder.removeFriendButton.setVisibility(View.GONE);
        }
        if (holder.addFriendButton != null) {
            holder.addFriendButton.setVisibility(View.VISIBLE);
            holder.addFriendButton.setOnClickListener(v -> acceptFriendRequest(user));
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void acceptFriendRequest(User user) {
        if (currentUserId == null) return; // Do nothing if user is not logged in

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
        ImageButton addFriendButton;
        ImageButton chatButton;
        ImageButton removeFriendButton;

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
