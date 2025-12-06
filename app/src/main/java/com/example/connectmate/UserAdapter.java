package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private List<User> userList;
    private OnChatClickListener chatClickListener;
    private OnRemoveFriendClickListener removeFriendClickListener;

    public interface OnChatClickListener {
        void onChatClick(User user);
    }

    public interface OnRemoveFriendClickListener {
        void onRemoveFriendClick(User user);
    }

    public UserAdapter(Context context, List<User> userList, OnChatClickListener chatClickListener, OnRemoveFriendClickListener removeFriendClickListener) {
        this.context = context;
        this.userList = userList;
        this.chatClickListener = chatClickListener;
        this.removeFriendClickListener = removeFriendClickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
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

        // Show friend badge (all users in this adapter are friends)
        if (holder.friendBadge != null) {
            holder.friendBadge.setVisibility(View.VISIBLE);
        }

        // In UserAdapter, we are dealing with friends, so "add" button is hidden.
        if (holder.addFriendButton != null) {
            holder.addFriendButton.setVisibility(View.GONE);
        }
        if (holder.chatButton != null) {
            holder.chatButton.setVisibility(View.VISIBLE);
            holder.chatButton.setOnClickListener(v -> chatClickListener.onChatClick(user));
        }
        if (holder.moreOptionsButton != null) {
            holder.moreOptionsButton.setVisibility(View.VISIBLE);
            holder.moreOptionsButton.setOnClickListener(v -> showOptionsMenu(v, user));
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void showOptionsMenu(View view, User user) {
        // Use androidx PopupMenu for better dark mode support
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.friend_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_view_profile) {
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("USER_ID", user.getUserId());
                intent.putExtra("SHOW_BUTTONS", false);
                context.startActivity(intent);
                return true;
            } else if (itemId == R.id.action_remove_friend) {
                showRemoveFriendConfirmation(user);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showRemoveFriendConfirmation(User user) {
        new AlertDialog.Builder(context)
                .setTitle("친구 삭제")
                .setMessage("정말로 친구를 삭제하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> {
                    if (removeFriendClickListener != null) {
                        removeFriendClickListener.onRemoveFriendClick(user);
                    }
                })
                .setNegativeButton("아니오", null)
                .show();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView userProfileImage;
        ImageView friendBadge;
        TextView userName;
        ImageButton addFriendButton;
        ImageButton chatButton;
        ImageButton moreOptionsButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userProfileImage = itemView.findViewById(R.id.user_profile_image);
            friendBadge = itemView.findViewById(R.id.friend_badge);
            userName = itemView.findViewById(R.id.user_name);
            addFriendButton = itemView.findViewById(R.id.add_friend_button);
            chatButton = itemView.findViewById(R.id.chat_button);
            moreOptionsButton = itemView.findViewById(R.id.more_options_button);
        }
    }
}
