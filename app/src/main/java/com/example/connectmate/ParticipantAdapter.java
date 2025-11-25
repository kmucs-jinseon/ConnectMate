package com.example.connectmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class ParticipantAdapter extends ArrayAdapter<Participant> {

    private Context context;
    private List<Participant> participants;
    private List<String> friendIds;
    private String currentUserId;

    public ParticipantAdapter(@NonNull Context context, List<Participant> participants, List<String> friendIds) {
        super(context, 0, participants);
        this.context = context;
        this.participants = participants;
        this.friendIds = friendIds;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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

        TextView participantNameTextView = convertView.findViewById(R.id.participant_name);
        Button addFriendButton = convertView.findViewById(R.id.add_friend_button);

        boolean isFriend = friendIds.contains(participantId);
        boolean isCurrentUser = participantId.equals(currentUserId);

        if (isFriend) {
            participantNameTextView.setText(participantName + " - 친구");
        } else {
            participantNameTextView.setText(participantName);
        }

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

        return convertView;
    }

    private void sendFriendRequest(String friendId) {
        DatabaseReference friendRequestRef = FirebaseDatabase.getInstance().getReference("users").child(friendId).child("friendRequests").child(currentUserId);
        friendRequestRef.setValue(true);
    }
}