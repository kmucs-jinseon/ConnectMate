package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder> {

    private Context context;
    private List<User> userList;
    private String currentUserId;
    private OnFriendRequestAcceptedListener listener;
    private OnFriendRequestRejectedListener rejectListener;

    public interface OnFriendRequestAcceptedListener {
        void onFriendRequestAccepted(User user);
    }

    public interface OnFriendRequestRejectedListener {
        void onFriendRequestRejected(User user);
    }

    public FriendRequestAdapter(Context context, List<User> userList, OnFriendRequestAcceptedListener listener, String currentUserId) {
        this.context = context;
        this.userList = userList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setOnFriendRequestRejectedListener(OnFriendRequestRejectedListener listener) {
        this.rejectListener = listener;
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
            Glide.with(context)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.circle_logo)
                    .error(R.drawable.circle_logo)
                    .into(holder.userProfileImage);
        } else {
            holder.userProfileImage.setImageResource(R.drawable.circle_logo);
        }

        // In FriendRequestAdapter, "add", "reject", and "more options" buttons are visible.
        if (holder.chatButton != null) {
            holder.chatButton.setVisibility(View.GONE);
        }
        if (holder.moreOptionsButton != null) {
            holder.moreOptionsButton.setVisibility(View.VISIBLE);
            holder.moreOptionsButton.setOnClickListener(v -> showPopupMenu(v, user));
        }
        if (holder.addFriendButton != null) {
            holder.addFriendButton.setVisibility(View.VISIBLE);
            holder.addFriendButton.setOnClickListener(v -> acceptFriendRequest(user));
        }
        if (holder.rejectFriendButton != null) {
            holder.rejectFriendButton.setVisibility(View.VISIBLE);
            holder.rejectFriendButton.setOnClickListener(v -> rejectFriendRequest(user));
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void showPopupMenu(View view, User user) {
        PopupMenu popup = new PopupMenu(context, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.friend_request_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_view_profile) {
                openUserProfile(user);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void openUserProfile(User user) {
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.putExtra("USER_ID", user.getUserId());
        intent.putExtra("SHOW_BUTTONS", false); // Not current user, so don't show edit buttons
        context.startActivity(intent);
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

    private void rejectFriendRequest(User user) {
        if (currentUserId == null) return; // Do nothing if user is not logged in

        // Simply remove the friend request without adding to friends
        DatabaseReference friendRequestRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId).child("friendRequests").child(user.getUserId());
        friendRequestRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (rejectListener != null) {
                    rejectListener.onFriendRequestRejected(user);
                }
            }
        });
    }

    public static class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        ImageView userProfileImage;
        TextView userName;
        ImageButton addFriendButton;
        ImageButton rejectFriendButton;
        ImageButton chatButton;
        ImageButton moreOptionsButton;

        public FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            userProfileImage = itemView.findViewById(R.id.user_profile_image);
            userName = itemView.findViewById(R.id.user_name);
            addFriendButton = itemView.findViewById(R.id.add_friend_button);
            rejectFriendButton = itemView.findViewById(R.id.reject_friend_button);
            chatButton = itemView.findViewById(R.id.chat_button);
            moreOptionsButton = itemView.findViewById(R.id.more_options_button);
        }
    }
}
