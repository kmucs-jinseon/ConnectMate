package com.example.connectmate;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.connectmate.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * MyFirebaseMessagingService handles incoming FCM messages and token updates.
 * This service runs in the background and can receive notifications even when
 * the app is not active.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";

    /**
     * Called when a new FCM token is generated.
     * This happens on app install, token refresh, or device restore.
     * We store the token in Firebase to enable sending push notifications to this device.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token generated: " + token);

        // Store the token in Firebase for the current user
        saveTokenToFirebase(token);
    }

    /**
     * Called when a message is received from FCM.
     * This method handles both foreground and background messages.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message notification body: " + remoteMessage.getNotification().getBody());
            handleNotificationMessage(remoteMessage);
        }
    }

    /**
     * Handles data messages sent via FCM.
     * Data messages are always delivered to onMessageReceived regardless of app state.
     */
    private void handleDataMessage(Map<String, String> data) {
        String type = data.get("type");
        String title = data.get("title");
        String message = data.get("message");
        String activityId = data.get("activityId");
        String senderId = data.get("senderId");
        String senderName = data.get("senderName");
        String senderProfileUrl = data.get("senderProfileUrl");

        // Display the notification using NotificationHelper
        NotificationHelper notificationHelper = new NotificationHelper(this);
        notificationHelper.showNotification(
                type != null ? type : NotificationHelper.TYPE_ACTIVITY,
                title != null ? title : "ConnectMate",
                message != null ? message : "",
                activityId,
                senderId,
                senderName,
                senderProfileUrl
        );

        Log.d(TAG, "Data message handled and notification displayed");
    }

    /**
     * Handles notification messages sent via FCM.
     * When the app is in foreground, we manually display the notification.
     * When in background, the system displays it automatically.
     */
    private void handleNotificationMessage(RemoteMessage remoteMessage) {
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        if (notification == null) return;

        String title = notification.getTitle();
        String body = notification.getBody();

        // Extract additional data from the message
        Map<String, String> data = remoteMessage.getData();
        String type = data.get("type");
        String activityId = data.get("activityId");
        String senderId = data.get("senderId");
        String senderName = data.get("senderName");
        String senderProfileUrl = data.get("senderProfileUrl");

        // Display the notification
        NotificationHelper notificationHelper = new NotificationHelper(this);
        notificationHelper.showNotification(
                type != null ? type : NotificationHelper.TYPE_ACTIVITY,
                title != null ? title : "ConnectMate",
                body != null ? body : "",
                activityId,
                senderId,
                senderName,
                senderProfileUrl
        );

        Log.d(TAG, "Notification message handled");
    }

    /**
     * Saves the FCM token to Firebase Realtime Database.
     * This allows the server to send push notifications to this specific device.
     */
    private void saveTokenToFirebase(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference tokenRef = FirebaseDatabase.getInstance()
                    .getReference("userTokens")
                    .child(userId);

            tokenRef.setValue(token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved to Firebase for user: " + userId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token to Firebase", e));
        } else {
            Log.w(TAG, "Cannot save token: User not authenticated");
        }
    }

    /**
     * Called when messages are deleted on the server.
     * This can happen due to message quota exceeded or other server issues.
     */
    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
        Log.w(TAG, "Messages deleted on server");
    }

    /**
     * Called when an error occurs while sending a message to FCM.
     */
    @Override
    public void onMessageSent(@NonNull String msgId) {
        super.onMessageSent(msgId);
        Log.d(TAG, "Message sent successfully: " + msgId);
    }

    /**
     * Called when there's an error sending a message.
     */
    @Override
    public void onSendError(@NonNull String msgId, @NonNull Exception exception) {
        super.onSendError(msgId, exception);
        Log.e(TAG, "Error sending message: " + msgId, exception);
    }
}
