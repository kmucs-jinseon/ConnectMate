package com.example.connectmate;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.utils.CategoryMapper;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import de.hdodenhof.circleimageview.CircleImageView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {

    private final List<ChatRoom> chatRooms;
    private final OnChatRoomClickListener listener;

    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom chatRoom);
    }

    public ChatRoomAdapter(List<ChatRoom> chatRooms, OnChatRoomClickListener listener) {
        this.chatRooms = chatRooms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoom chatRoom = chatRooms.get(position);
        holder.bind(chatRoom, listener);
    }

    @Override
    public int getItemCount() {
        return chatRooms.size();
    }

    static class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView profileImage;
        private final TextView chatName;
        private final Chip categoryChip;
        private final TextView memberCount;
        private final TextView lastMessage;
        private final TextView timestamp;
        private final TextView unreadCount;

        public ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            chatName = itemView.findViewById(R.id.chat_name);
            categoryChip = itemView.findViewById(R.id.category_chip);
            memberCount = itemView.findViewById(R.id.member_count);
            lastMessage = itemView.findViewById(R.id.last_message);
            timestamp = itemView.findViewById(R.id.timestamp);
            unreadCount = itemView.findViewById(R.id.unread_count);
        }

        public void bind(ChatRoom chatRoom, OnChatRoomClickListener listener) {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if ("private".equals(chatRoom.getCategory()) && chatRoom.getMembers() != null) {
                for (Map.Entry<String, ChatRoom.Member> entry : chatRoom.getMembers().entrySet()) {
                    if (!entry.getKey().equals(currentUserId)) {
                        chatName.setText(entry.getValue().getName());
                        break;
                    }
                }
            } else {
                chatName.setText(chatRoom.getName());
            }

            // Category chip is hidden but data is kept for filtering
            // No need to set visibility or styling as it's hidden in XML

            // Display member count
            int count = chatRoom.getMemberCount();
            memberCount.setText(count + "명");

            // Show last message or default text
            if (chatRoom.getLastMessage() != null && !chatRoom.getLastMessage().isEmpty()) {
                lastMessage.setText(chatRoom.getLastMessage());
            } else {
                lastMessage.setText("새로운 채팅방");
            }

            // Format timestamp in Korean local time format (오전/오후 h:mm)
            SimpleDateFormat timeFormat = new SimpleDateFormat("a h:mm", Locale.KOREAN);
            String timeStr = timeFormat.format(new Date(chatRoom.getLastMessageTime()));
            timestamp.setText(timeStr);

            // Show/hide unread badge (perfect circle)
            if (chatRoom.getUnreadCount() > 0) {
                unreadCount.setVisibility(View.VISIBLE);
                unreadCount.setText(String.valueOf(chatRoom.getUnreadCount()));
            } else {
                unreadCount.setVisibility(View.GONE);
            }

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatRoomClick(chatRoom);
                }
            });

            // Load last message sender's profile image
            // Supports both Firebase Storage URLs and Base64 encoded images
            String profileUrl = chatRoom.getLastMessageSenderProfileUrl();
            if (profileUrl != null && !profileUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(profileUrl)  // Glide handles both URLs and Base64 (data:image/...)
                        .placeholder(R.drawable.circle_logo)
                        .error(R.drawable.circle_logo)
                        .circleCrop()
                        .into(profileImage);
            } else {
                // Fallback to main logo
                profileImage.setImageResource(R.drawable.circle_logo);
            }
        }
    }
}
