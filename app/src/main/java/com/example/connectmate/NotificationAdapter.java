package com.example.connectmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.NotificationItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

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

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(notifications.get(position), listener);
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
}
