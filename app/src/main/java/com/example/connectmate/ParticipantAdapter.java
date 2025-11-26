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
import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

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

        boolean isFriend = friendIds.contains(participantId);
        boolean isCurrentUser = participantId.equals(currentUserId);

        // Set participant name
        participantNameTextView.setText(participantName);

        // Show host badge if the participant is the host
        if (participantId.equals(hostId)) {
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
        PopupMenu popup = new PopupMenu(context, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.participant_options_menu, popup.getMenu());

        // Conditionally show/hide "Kick" and "Grant Admin" options
        MenuItem kickItem = popup.getMenu().findItem(R.id.action_kick);
        MenuItem grantAdminItem = popup.getMenu().findItem(R.id.action_grant_admin);

        boolean isHost = currentUserId.equals(hostId);
        boolean isSelf = participant.getId().equals(currentUserId);

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
    }
}
