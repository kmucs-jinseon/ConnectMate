package com.example.connectmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connectmate.models.ChatRoom;
import de.hdodenhof.circleimageview.CircleImageView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        private final TextView memberCount;
        private final TextView lastMessage;
        private final TextView timestamp;
        private final CardView unreadBadge;
        private final TextView unreadCount;

        public ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            chatName = itemView.findViewById(R.id.chat_name);
            memberCount = itemView.findViewById(R.id.member_count);
            lastMessage = itemView.findViewById(R.id.last_message);
            timestamp = itemView.findViewById(R.id.timestamp);
            unreadBadge = itemView.findViewById(R.id.unread_badge);
            unreadCount = itemView.findViewById(R.id.unread_count);
        }

        public void bind(ChatRoom chatRoom, OnChatRoomClickListener listener) {
            chatName.setText(chatRoom.getName());

            // Display member count
            int count = chatRoom.getMemberCount();
            memberCount.setText(count + "명");

            // Show last message or default text
            if (chatRoom.getLastMessage() != null && !chatRoom.getLastMessage().isEmpty()) {
                lastMessage.setText(chatRoom.getLastMessage());
            } else {
                lastMessage.setText("새로운 채팅방");
            }

            // Format timestamp
            SimpleDateFormat timeFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            String timeStr = timeFormat.format(new Date(chatRoom.getLastMessageTime()));
            timestamp.setText(timeStr);

            // Show/hide unread badge
            if (chatRoom.getUnreadCount() > 0) {
                unreadBadge.setVisibility(View.VISIBLE);
                unreadCount.setText(String.valueOf(chatRoom.getUnreadCount()));
            } else {
                unreadBadge.setVisibility(View.GONE);
            }

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatRoomClick(chatRoom);
                }
            });

            // Set default profile image (you can later add image loading with Glide/Picasso)
            profileImage.setImageResource(R.drawable.circle_logo);
        }
    }
}
