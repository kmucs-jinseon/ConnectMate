package com.example.connectmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;

public class NotificationSettingsFragment extends Fragment {

    private SharedPreferences prefs;

    // SharedPreferences Keys
    private static final String PREF_NEW_MESSAGE_NOTIFICATIONS = "pref_new_message_notifications";
    private static final String PREF_MENTION_NOTIFICATIONS = "pref_mention_notifications";
    private static final String PREF_ACTIVITY_UPDATE_NOTIFICATIONS = "pref_activity_update_notifications";
    private static final String PREF_NEW_PARTICIPANT_NOTIFICATIONS = "pref_new_participant_notifications";
    private static final String PREF_ACTIVITY_REMINDER_NOTIFICATIONS = "pref_activity_reminder_notifications";
    private static final String PREF_RECOMMENDED_ACTIVITY_NOTIFICATIONS = "pref_recommended_activity_notifications";
    private static final String PREF_FRIEND_REQUEST_NOTIFICATIONS = "pref_friend_request_notifications";
    private static final String PREF_FRIEND_ACCEPTED_NOTIFICATIONS = "pref_friend_accepted_notifications";

    public NotificationSettingsFragment() {
        super(R.layout.fragment_notification_settings);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize SharedPreferences
        prefs = requireActivity().getSharedPreferences("NotificationSettings", Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            Toolbar toolbar = view.findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setNavigationOnClickListener(v -> {
                    // Navigate back to the previous fragment (SettingsFragment)
                    getParentFragmentManager().popBackStack();
                });
            }
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find all switches
        MaterialSwitch newChatMessageSwitch = view.findViewById(R.id.switch_new_message_notifications);
        MaterialSwitch mentionSwitch = view.findViewById(R.id.switch_mention_notifications);
        MaterialSwitch activityUpdateSwitch = view.findViewById(R.id.switch_activity_update_notifications);
        MaterialSwitch newParticipantSwitch = view.findViewById(R.id.switch_new_participant_notifications);
        MaterialSwitch activityReminderSwitch = view.findViewById(R.id.switch_activity_reminder_notifications);
        MaterialSwitch recommendedActivitySwitch = view.findViewById(R.id.switch_recommended_activity_notifications);
        MaterialSwitch friendRequestSwitch = view.findViewById(R.id.switch_friend_request_notifications);
        MaterialSwitch friendAcceptedSwitch = view.findViewById(R.id.switch_friend_accepted_notifications);

        // Load saved states or set default to true
        newChatMessageSwitch.setChecked(prefs.getBoolean(PREF_NEW_MESSAGE_NOTIFICATIONS, true));
        mentionSwitch.setChecked(prefs.getBoolean(PREF_MENTION_NOTIFICATIONS, true));
        activityUpdateSwitch.setChecked(prefs.getBoolean(PREF_ACTIVITY_UPDATE_NOTIFICATIONS, true));
        newParticipantSwitch.setChecked(prefs.getBoolean(PREF_NEW_PARTICIPANT_NOTIFICATIONS, true));
        activityReminderSwitch.setChecked(prefs.getBoolean(PREF_ACTIVITY_REMINDER_NOTIFICATIONS, true));
        recommendedActivitySwitch.setChecked(prefs.getBoolean(PREF_RECOMMENDED_ACTIVITY_NOTIFICATIONS, true));
        friendRequestSwitch.setChecked(prefs.getBoolean(PREF_FRIEND_REQUEST_NOTIFICATIONS, true));
        friendAcceptedSwitch.setChecked(prefs.getBoolean(PREF_FRIEND_ACCEPTED_NOTIFICATIONS, true));

        // Set listeners to save state on change
        newChatMessageSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            prefs.edit().putBoolean(PREF_NEW_MESSAGE_NOTIFICATIONS, isChecked).apply());
        mentionSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            prefs.edit().putBoolean(PREF_MENTION_NOTIFICATIONS, isChecked).apply());
        activityUpdateSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            prefs.edit().putBoolean(PREF_ACTIVITY_UPDATE_NOTIFICATIONS, isChecked).apply());
        newParticipantSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            prefs.edit().putBoolean(PREF_NEW_PARTICIPANT_NOTIFICATIONS, isChecked).apply());
        activityReminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            prefs.edit().putBoolean(PREF_ACTIVITY_REMINDER_NOTIFICATIONS, isChecked).apply());
        recommendedActivitySwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            prefs.edit().putBoolean(PREF_RECOMMENDED_ACTIVITY_NOTIFICATIONS, isChecked).apply());
        friendRequestSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            prefs.edit().putBoolean(PREF_FRIEND_REQUEST_NOTIFICATIONS, isChecked).apply());
        friendAcceptedSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            prefs.edit().putBoolean(PREF_FRIEND_ACCEPTED_NOTIFICATIONS, isChecked).apply());
    }
}
