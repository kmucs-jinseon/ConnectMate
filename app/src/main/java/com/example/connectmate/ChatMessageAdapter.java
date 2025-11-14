package com.example.connectmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connectmate.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 100;
    private static final int VIEW_TYPE_RECEIVED = 101;
    private static final int VIEW_TYPE_SYSTEM = 1;

    private final List<ChatMessage> messages;
    private final String currentUserId;

    public ChatMessageAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);

        // System messages
        if (message.isSystemMessage()) {
            return VIEW_TYPE_SYSTEM;
        }

        // Check if message is sent by current user
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_SYSTEM) {
            View view = inflater.inflate(R.layout.item_chat_message_system, parent, false);
            return new SystemMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_chat_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);

        if (holder instanceof SystemMessageViewHolder) {
            ((SystemMessageViewHolder) holder).bind(message);
        } else if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ViewHolder for system messages
    static class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView systemMessage;

        public SystemMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            systemMessage = itemView.findViewById(R.id.system_message);
        }

        public void bind(ChatMessage message) {
            systemMessage.setText(message.getMessage());
        }
    }

    // ViewHolder for sent messages (current user)
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView profileImage;
        private final TextView senderName;
        private final TextView messageText;
        private final TextView messageTime;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            senderName = itemView.findViewById(R.id.sender_name);
            messageText = itemView.findViewById(R.id.message_text);
            messageTime = itemView.findViewById(R.id.message_time);
        }

        public void bind(ChatMessage message) {
            senderName.setText(message.getSenderName());
            messageText.setText(message.getMessage());

            // Format timestamp in Korean format
            SimpleDateFormat timeFormat = new SimpleDateFormat("a h:mm", Locale.KOREAN);
            String time = timeFormat.format(new Date(message.getTimestamp()));
            messageTime.setText(time);

            // Load profile image with Glide
            String profileUrl = message.getSenderProfileUrl();
            android.util.Log.d("ChatMessageAdapter", "SENT message - Sender: " + message.getSenderName() + ", Profile URL: " + profileUrl);

            if (profileUrl != null && !profileUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(profileUrl)
                        .placeholder(R.drawable.circle_logo)
                        .error(R.drawable.circle_logo)
                        .into(profileImage);
            } else {
                android.util.Log.w("ChatMessageAdapter", "No profile URL for sent message, using default");
                profileImage.setImageResource(R.drawable.circle_logo);
            }
        }
    }

    // ViewHolder for received messages (other users)
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView profileImage;
        private final TextView senderName;
        private final TextView messageText;
        private final TextView messageTime;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            senderName = itemView.findViewById(R.id.sender_name);
            messageText = itemView.findViewById(R.id.message_text);
            messageTime = itemView.findViewById(R.id.message_time);
        }

        public void bind(ChatMessage message) {
            senderName.setText(message.getSenderName());
            messageText.setText(message.getMessage());

            // Format timestamp in Korean format
            SimpleDateFormat timeFormat = new SimpleDateFormat("a h:mm", Locale.KOREAN);
            String time = timeFormat.format(new Date(message.getTimestamp()));
            messageTime.setText(time);

            // Load profile image with Glide
            String profileUrl = message.getSenderProfileUrl();
            android.util.Log.d("ChatMessageAdapter", "RECEIVED message - Sender: " + message.getSenderName() + ", Profile URL: " + profileUrl);

            if (profileUrl != null && !profileUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(profileUrl)
                        .placeholder(R.drawable.circle_logo)
                        .error(R.drawable.circle_logo)
                        .into(profileImage);
            } else {
                android.util.Log.w("ChatMessageAdapter", "No profile URL for received message, using default");
                profileImage.setImageResource(R.drawable.circle_logo);
            }
        }
    }
}
