package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connectmate.models.NotificationItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ACTIVITY = 1;
    private static final int VIEW_TYPE_FRIEND_REQUEST = 2;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem item);
    }

    private final List<NotificationItem> notifications;
    private final OnNotificationClickListener listener;
    private final SimpleDateFormat dateFormat =
        new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault());

    public NotificationAdapter(List<NotificationItem> notifications,
                               OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        NotificationItem item = notifications.get(position);
        if ("FRIEND_REQUEST".equals(item.getType())) {
            return VIEW_TYPE_FRIEND_REQUEST;
        } else {
            return VIEW_TYPE_ACTIVITY;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_FRIEND_REQUEST) {
            View view = inflater.inflate(R.layout.item_notification_friend_request, parent, false);
            return new FriendRequestViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);

        if (holder instanceof FriendRequestViewHolder) {
            ((FriendRequestViewHolder) holder).bind(item, listener);
        } else if (holder instanceof NotificationViewHolder) {
            ((NotificationViewHolder) holder).bind(item, listener);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView message;
        private final TextView timestamp;
        private final SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault());

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            message = itemView.findViewById(R.id.notification_message);
            timestamp = itemView.findViewById(R.id.notification_timestamp);
        }

        void bind(NotificationItem item, OnNotificationClickListener listener) {
            title.setText(item.getTitle());
            message.setText(item.getMessage());
            timestamp.setText(formatTime(item.getTimestamp()));
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(item);
                }
            });
        }

        private String formatTime(long ts) {
            if (ts <= 0) {
                return "";
            }
            return formatter.format(new Date(ts));
        }
    }

    // ViewHolder for friend request notifications
    static class FriendRequestViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView senderProfileImage;
        private final TextView senderName;
        private final TextView friendRequestMessage;
        private final TextView timestamp;
        private final ImageView viewProfileButton;
        private final SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault());

        FriendRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            senderProfileImage = itemView.findViewById(R.id.sender_profile_image);
            senderName = itemView.findViewById(R.id.sender_name);
            friendRequestMessage = itemView.findViewById(R.id.friend_request_message);
            timestamp = itemView.findViewById(R.id.notification_timestamp);
            viewProfileButton = itemView.findViewById(R.id.view_profile_button);
        }

        void bind(NotificationItem item, OnNotificationClickListener listener) {
            // Set sender name
            if (item.getSenderName() != null && !item.getSenderName().isEmpty()) {
                senderName.setText(item.getSenderName());
            }

            // Set friend request message with sender name
            String message = item.getSenderName() + "님이 친구 요청을 보냈습니다";
            friendRequestMessage.setText(message);

            // Set timestamp
            timestamp.setText(formatTime(item.getTimestamp()));

            // Load profile image with Glide
            String profileUrl = item.getSenderProfileUrl();
            if (profileUrl != null && !profileUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(profileUrl)
                        .placeholder(R.drawable.circle_logo)
                        .error(R.drawable.circle_logo)
                        .circleCrop()
                        .into(senderProfileImage);
            } else {
                senderProfileImage.setImageResource(R.drawable.circle_logo);
            }

            // Set view profile button click listener
            viewProfileButton.setOnClickListener(v -> {
                Context context = itemView.getContext();
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("USER_ID", item.getSenderId());
                intent.putExtra("SHOW_BUTTONS", false);
                context.startActivity(intent);
            });

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(item);
                }
            });
        }

        private String formatTime(long ts) {
            if (ts <= 0) {
                return "";
            }
            return formatter.format(new Date(ts));
        }
    }
}
