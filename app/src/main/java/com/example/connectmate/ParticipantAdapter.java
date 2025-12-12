package com.example.connectmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.connectmate.models.NotificationItem;
import com.example.connectmate.utils.NotificationHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParticipantAdapter extends ArrayAdapter<Participant> {

    private Context context;
    private List<Participant> participants;
    private List<String> friendIds;
    private String currentUserId;
    private String hostId;
    private OnParticipantActionListener actionListener;

    public interface OnParticipantActionListener {
        void onKickParticipant(Participant participant);
        void onGrantAdmin(Participant participant);
        void onViewProfile(Participant participant);
    }

    public ParticipantAdapter(@NonNull Context context, List<Participant> participants, List<String> friendIds, String currentUserId, String hostId, OnParticipantActionListener actionListener) {
        super(context, 0, participants);
        this.context = context;
        this.participants = participants;
        this.friendIds = friendIds;
        this.currentUserId = currentUserId;
        this.hostId = hostId;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_participant, parent, false);
        }

        Participant participant = participants.get(position);
        String participantId = participant.getId();
        String participantName = participant.getName();
        String profileImageUrl = participant.getProfileImageUrl();

        ImageView profileImageView = convertView.findViewById(R.id.participant_profile_image);
        TextView participantNameTextView = convertView.findViewById(R.id.participant_name);
        ImageButton addFriendButton = convertView.findViewById(R.id.add_friend_button);
        ImageButton moreOptionsButton = convertView.findViewById(R.id.more_options_button);
        ImageView hostBadge = convertView.findViewById(R.id.host_badge);
        ImageView friendBadge = convertView.findViewById(R.id.friend_badge);

        // Load profile image
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(context)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.circle_logo)
                    .error(R.drawable.circle_logo)
                    .circleCrop()
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.circle_logo);
        }

        boolean isFriend = friendIds != null && friendIds.contains(participantId);
        boolean isCurrentUser = participantId != null && participantId.equals(currentUserId);

        // Show friend badge if this participant is a friend (but not the current user)
        if (isFriend && !isCurrentUser) {
            friendBadge.setVisibility(View.VISIBLE);
        } else {
            friendBadge.setVisibility(View.GONE);
        }

        // Set participant name
        participantNameTextView.setText(participantName);

        // Show host badge if the participant is the host
        if (participantId != null && participantId.equals(hostId)) {
            hostBadge.setVisibility(View.VISIBLE);
        } else {
            hostBadge.setVisibility(View.GONE);
        }

        // Handle 'Add Friend' button visibility
        if (isCurrentUser || isFriend) {
            addFriendButton.setVisibility(View.GONE);
        } else {
            addFriendButton.setVisibility(View.VISIBLE);
            addFriendButton.setOnClickListener(v -> {
                sendFriendRequest(participantId);
                addFriendButton.setVisibility(View.GONE);
                Toast.makeText(context, "친구 추가를 요청했습니다", Toast.LENGTH_SHORT).show();
            });
        }

        // Handle 'More Options' button
        moreOptionsButton.setOnClickListener(v -> showPopupMenu(v, participant));

        return convertView;
    }

    private void showPopupMenu(View view, Participant participant) {
        android.view.ContextThemeWrapper wrapper = new android.view.ContextThemeWrapper(context, R.style.ParticipantPopupMenu);
        PopupMenu popup = new PopupMenu(wrapper, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.participant_options_menu, popup.getMenu());

        // Use popup menu text color which adapts to theme automatically
        int textColor = context.getResources().getColor(R.color.popup_menu_text_color);

        // Apply text color to each menu item
        android.view.Menu menu = popup.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            android.view.MenuItem item = menu.getItem(i);
            android.text.SpannableString spannableString = new android.text.SpannableString(item.getTitle());
            spannableString.setSpan(new android.text.style.ForegroundColorSpan(textColor), 0, spannableString.length(), 0);
            item.setTitle(spannableString);
        }

        // Conditionally show/hide "Kick" and "Grant Admin" options
        MenuItem kickItem = popup.getMenu().findItem(R.id.action_kick);
        MenuItem grantAdminItem = popup.getMenu().findItem(R.id.action_grant_admin);

        boolean isHost = currentUserId != null && currentUserId.equals(hostId);
        boolean isSelf = participant.getId() != null && participant.getId().equals(currentUserId);

        kickItem.setVisible(isHost && !isSelf);
        grantAdminItem.setVisible(isHost && !isSelf);

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_view_profile) {
                if (actionListener != null) {
                    actionListener.onViewProfile(participant);
                }
                return true;
            } else if (itemId == R.id.action_kick) {
                if (actionListener != null) {
                    actionListener.onKickParticipant(participant);
                }
                return true;
            } else if (itemId == R.id.action_grant_admin) {
                if (actionListener != null) {
                    actionListener.onGrantAdmin(participant);
                }
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void sendFriendRequest(String friendId) {
        DatabaseReference friendRequestRef = FirebaseDatabase.getInstance().getReference("users").child(friendId).child("friendRequests").child(currentUserId);
        friendRequestRef.setValue(true);

        android.util.Log.d("ParticipantAdapter", "Sending friend request to: " + friendId);

        // Fetch current user's information to create notification
        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String senderName = dataSnapshot.child("displayName").getValue(String.class);
                    String senderProfileUrl = dataSnapshot.child("profileImageUrl").getValue(String.class);

                    android.util.Log.d("ParticipantAdapter", "Creating notification for friend request from: " + senderName);

                    // Create friend request notification
                    DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                            .getReference("userNotifications")
                            .child(friendId);

                    String notificationId = notificationsRef.push().getKey();
                    if (notificationId != null) {
                        Map<String, Object> notificationData = new HashMap<>();
                        notificationData.put("id", notificationId);
                        notificationData.put("type", "FRIEND_REQUEST");
                        String title = "친구 요청";
                        String message = senderName + "님이 친구 요청을 보냈습니다";
                        notificationData.put("title", title);
                        notificationData.put("message", message);
                        notificationData.put("senderId", currentUserId);
                        notificationData.put("senderName", senderName);
                        notificationData.put("senderProfileUrl", senderProfileUrl != null ? senderProfileUrl : "");
                        notificationData.put("timestamp", System.currentTimeMillis());
                        notificationData.put("isRead", false);

                        notificationsRef.child(notificationId).setValue(notificationData)
                                .addOnSuccessListener(aVoid -> {
                                    android.util.Log.d("ParticipantAdapter", "Friend request notification created successfully at: userNotifications/" + friendId + "/" + notificationId);
                                    // Show OS-level notification
                                    NotificationHelper notificationHelper = new NotificationHelper(context);
                                    notificationHelper.showNotification(
                                        "FRIEND_REQUEST",
                                        title,
                                        message,
                                        null,
                                        currentUserId,
                                        senderName,
                                        senderProfileUrl
                                    );
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("ParticipantAdapter", "Failed to create friend request notification", e);
                                });
                    } else {
                        android.util.Log.e("ParticipantAdapter", "Failed to generate notification ID");
                    }
                } else {
                    android.util.Log.e("ParticipantAdapter", "Current user data not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                android.util.Log.e("ParticipantAdapter", "Failed to fetch current user data", databaseError.toException());
            }
        });
    }
}
