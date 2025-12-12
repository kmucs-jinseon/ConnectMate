package com.example.connectmate.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.example.connectmate.ActivityDetailActivity;
import com.example.connectmate.ChatRoomActivity;
import com.example.connectmate.MainActivity;
import com.example.connectmate.ProfileActivity;
import com.example.connectmate.R;

import java.util.concurrent.ExecutionException;

/**
 * NotificationHelper manages all notification-related operations including:
 * - Creating notification channels (Android 8.0+)
 * - Displaying OS-level notifications
 * - Handling notification preferences
 * - Managing notification clicks and actions
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";

    // Notification Channel IDs
    public static final String CHANNEL_CHAT_ID = "chat_notifications";
    public static final String CHANNEL_ACTIVITY_ID = "activity_notifications";
    public static final String CHANNEL_SOCIAL_ID = "social_notifications";

    // Notification Channel Names
    private static final String CHANNEL_CHAT_NAME = "채팅 알림";
    private static final String CHANNEL_ACTIVITY_NAME = "활동 알림";
    private static final String CHANNEL_SOCIAL_NAME = "소셜 알림";

    // Notification Channel Descriptions
    private static final String CHANNEL_CHAT_DESC = "새로운 메시지 및 멘션 알림";
    private static final String CHANNEL_ACTIVITY_DESC = "활동 업데이트, 참가자 및 추천 알림";
    private static final String CHANNEL_SOCIAL_DESC = "친구 요청 및 수락 알림";

    // Notification Types
    public static final String TYPE_ACTIVITY = "ACTIVITY";
    public static final String TYPE_CHAT_JOIN = "CHAT_JOIN";
    public static final String TYPE_FRIEND_REQUEST = "FRIEND_REQUEST";
    public static final String TYPE_CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String TYPE_ACTIVITY_REMINDER = "ACTIVITY_REMINDER";
    public static final String TYPE_FRIEND_ACCEPTED = "FRIEND_ACCEPTED";

    private final Context context;
    private final NotificationManager notificationManager;
    private final SharedPreferences notificationPrefs;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.notificationPrefs = context.getSharedPreferences("NotificationSettings", Context.MODE_PRIVATE);

        // Create notification channels on initialization
        createNotificationChannels();
    }

    /**
     * Creates notification channels for Android 8.0 (Oreo) and above.
     * Channels allow users to control notification settings per category.
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Chat notifications channel
            NotificationChannel chatChannel = new NotificationChannel(
                    CHANNEL_CHAT_ID,
                    CHANNEL_CHAT_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            chatChannel.setDescription(CHANNEL_CHAT_DESC);
            chatChannel.enableVibration(true);
            chatChannel.setShowBadge(true);

            // Activity notifications channel
            NotificationChannel activityChannel = new NotificationChannel(
                    CHANNEL_ACTIVITY_ID,
                    CHANNEL_ACTIVITY_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            activityChannel.setDescription(CHANNEL_ACTIVITY_DESC);
            activityChannel.enableVibration(true);
            activityChannel.setShowBadge(true);

            // Social notifications channel
            NotificationChannel socialChannel = new NotificationChannel(
                    CHANNEL_SOCIAL_ID,
                    CHANNEL_SOCIAL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            socialChannel.setDescription(CHANNEL_SOCIAL_DESC);
            socialChannel.enableVibration(true);
            socialChannel.setShowBadge(true);

            // Register channels with the system
            notificationManager.createNotificationChannel(chatChannel);
            notificationManager.createNotificationChannel(activityChannel);
            notificationManager.createNotificationChannel(socialChannel);

            Log.d(TAG, "Notification channels created successfully");
        }
    }

    /**
     * Displays a notification based on the notification type and data.
     * Checks user preferences before showing the notification.
     */
    public void showNotification(String type, String title, String message,
                                 String activityId, String senderId,
                                 String senderName, String senderProfileUrl) {
        // Check if notifications are enabled for this type
        if (!shouldShowNotification(type)) {
            Log.d(TAG, "Notification disabled by user for type: " + type);
            return;
        }

        String channelId = getChannelIdForType(type);
        int notificationId = generateNotificationId(type, activityId, senderId);

        // Create notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        // Set large icon for sender profile if available
        if (senderProfileUrl != null && !senderProfileUrl.isEmpty()) {
            try {
                Bitmap profileBitmap = Glide.with(context)
                        .asBitmap()
                        .load(senderProfileUrl)
                        .circleCrop()
                        .submit(64, 64)
                        .get();
                builder.setLargeIcon(profileBitmap);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Failed to load profile image for notification", e);
            }
        }

        // Set appropriate intent based on notification type
        PendingIntent pendingIntent = createPendingIntent(type, activityId, senderId);
        builder.setContentIntent(pendingIntent);

        // Show the notification
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Notification shown: " + type + " - " + title);
    }

    /**
     * Checks if notifications should be shown based on user preferences.
     */
    private boolean shouldShowNotification(String type) {
        switch (type) {
            case TYPE_CHAT_MESSAGE:
                return notificationPrefs.getBoolean("pref_new_message_notifications", true);
            case TYPE_CHAT_JOIN:
                return notificationPrefs.getBoolean("pref_new_participant_notifications", true);
            case TYPE_ACTIVITY:
                return notificationPrefs.getBoolean("pref_activity_update_notifications", true);
            case TYPE_ACTIVITY_REMINDER:
                return notificationPrefs.getBoolean("pref_activity_reminder_notifications", true);
            case TYPE_FRIEND_REQUEST:
                return notificationPrefs.getBoolean("pref_friend_request_notifications", true);
            case TYPE_FRIEND_ACCEPTED:
                return notificationPrefs.getBoolean("pref_friend_accepted_notifications", true);
            default:
                return true;
        }
    }

    /**
     * Returns the appropriate notification channel ID for the notification type.
     */
    private String getChannelIdForType(String type) {
        switch (type) {
            case TYPE_CHAT_MESSAGE:
            case TYPE_CHAT_JOIN:
                return CHANNEL_CHAT_ID;
            case TYPE_FRIEND_REQUEST:
            case TYPE_FRIEND_ACCEPTED:
                return CHANNEL_SOCIAL_ID;
            case TYPE_ACTIVITY:
            case TYPE_ACTIVITY_REMINDER:
            default:
                return CHANNEL_ACTIVITY_ID;
        }
    }

    /**
     * Generates a unique notification ID to allow proper notification updates.
     */
    private int generateNotificationId(String type, String activityId, String senderId) {
        String uniqueId = type + (activityId != null ? activityId : "") + (senderId != null ? senderId : "");
        return uniqueId.hashCode();
    }

    /**
     * Creates a PendingIntent that will be triggered when the user taps the notification.
     */
    private PendingIntent createPendingIntent(String type, String activityId, String senderId) {
        Intent intent;

        switch (type) {
            case TYPE_ACTIVITY:
            case TYPE_ACTIVITY_REMINDER:
                // Open activity detail
                if (activityId != null && !activityId.isEmpty()) {
                    intent = new Intent(context, ActivityDetailActivity.class);
                    intent.putExtra("activityId", activityId);
                } else {
                    intent = new Intent(context, MainActivity.class);
                }
                break;

            case TYPE_CHAT_MESSAGE:
            case TYPE_CHAT_JOIN:
                // Open chat room
                if (activityId != null && !activityId.isEmpty()) {
                    intent = new Intent(context, ChatRoomActivity.class);
                    intent.putExtra("activityId", activityId);
                } else {
                    intent = new Intent(context, MainActivity.class);
                    intent.putExtra("openChatList", true);
                }
                break;

            case TYPE_FRIEND_REQUEST:
            case TYPE_FRIEND_ACCEPTED:
                // Open user profile
                if (senderId != null && !senderId.isEmpty()) {
                    intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra("userId", senderId);
                } else {
                    intent = new Intent(context, MainActivity.class);
                }
                break;

            default:
                // Default to main activity
                intent = new Intent(context, MainActivity.class);
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(context, 0, intent, flags);
    }

    /**
     * Cancels a specific notification by ID.
     */
    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }

    /**
     * Cancels all notifications from this app.
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }
}
