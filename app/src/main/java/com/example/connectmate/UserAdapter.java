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
        Glide.with(context).load(user.getProfileImageUrl()).into(holder.userProfileImage);

        holder.addFriendButton.setVisibility(View.GONE);
        holder.chatButton.setVisibility(View.VISIBLE);
        holder.removeFriendButton.setVisibility(View.VISIBLE);

        holder.chatButton.setOnClickListener(v -> chatClickListener.onChatClick(user));
        holder.removeFriendButton.setOnClickListener(v -> removeFriendClickListener.onRemoveFriendClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView userProfileImage;
        TextView userName;
        Button addFriendButton;
        Button chatButton;
        Button removeFriendButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userProfileImage = itemView.findViewById(R.id.user_profile_image);
            userName = itemView.findViewById(R.id.user_name);
            addFriendButton = itemView.findViewById(R.id.add_friend_button);
            chatButton = itemView.findViewById(R.id.chat_button);
            removeFriendButton = itemView.findViewById(R.id.remove_friend_button);
        }
    }
}
