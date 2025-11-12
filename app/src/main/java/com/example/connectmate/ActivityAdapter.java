package com.example.connectmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connectmate.models.Activity;
import com.example.connectmate.models.ChatRoom;
import com.example.connectmate.utils.ChatManager;
import com.example.connectmate.utils.FirebaseActivityManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private final List<Activity> activities;
    private final OnActivityClickListener listener;

    public interface OnActivityClickListener {
        void onActivityClick(Activity activity);
        void onEditActivity(Activity activity);
    }

    public ActivityAdapter(List<Activity> activities, OnActivityClickListener listener) {
        this.activities = activities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        Activity activity = activities.get(position);
        holder.bind(activity, listener, holder.itemView.getContext());
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        private final TextView activityTitle;
        private final Chip categoryChip;
        private final TextView activityLocation;
        private final TextView activityTime;
        private final TextView activityCreator;
        private final TextView activityDescription;
        private final TextView participantsCount;
        private final MaterialButton btnEditActivity;
        private final MaterialButton btnViewDetails;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            activityTitle = itemView.findViewById(R.id.activity_title);
            categoryChip = itemView.findViewById(R.id.category_chip);
            activityLocation = itemView.findViewById(R.id.activity_location);
            activityTime = itemView.findViewById(R.id.activity_time);
            activityCreator = itemView.findViewById(R.id.activity_creator);
            activityDescription = itemView.findViewById(R.id.activity_description);
            participantsCount = itemView.findViewById(R.id.participants_count);
            btnEditActivity = itemView.findViewById(R.id.btn_edit_activity);
            btnViewDetails = itemView.findViewById(R.id.btn_view_details);
        }

        public void bind(Activity activity, OnActivityClickListener listener, Context context) {
            activityTitle.setText(activity.getTitle());
            activityLocation.setText(activity.getLocation() != null ? activity.getLocation() : "");

            // Format date and time
            String dateTime = activity.getDateTime();
            if (dateTime.isEmpty()) {
                dateTime = activity.getTime() != null ? activity.getTime() : "";
            }
            activityTime.setText(dateTime);

            activityDescription.setText(activity.getDescription() != null ? activity.getDescription() : "");

            // Listen to participant count changes in realtime
            FirebaseActivityManager activityManager = FirebaseActivityManager.getInstance();
            activityManager.listenToParticipantCount(activity.getId(),
                new FirebaseActivityManager.ParticipantCountListener() {
                    @Override
                    public void onCountChanged(int count) {
                        // Update UI on main thread
                        participantsCount.post(() -> {
                            if (activity.getMaxParticipants() > 0) {
                                participantsCount.setText(count + "/" + activity.getMaxParticipants());
                            } else {
                                participantsCount.setText(count + "명");
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        // Fallback to showing current value from activity model
                        participantsCount.post(() -> {
                            int currentCount = activity.getCurrentParticipants();
                            if (activity.getMaxParticipants() > 0) {
                                participantsCount.setText(currentCount + "/" + activity.getMaxParticipants());
                            } else {
                                participantsCount.setText(currentCount + "명");
                            }
                        });
                    }
                });

            // Display creator name
            if (activity.getCreatorName() != null && !activity.getCreatorName().isEmpty()) {
                activityCreator.setText("개설자: " + activity.getCreatorName());
            } else {
                activityCreator.setText("개설자: 알 수 없음");
            }

            // Check if current user is the creator
            String currentUserId = getCurrentUserId(context);
            boolean isCreator = currentUserId != null &&
                activity.getCreatorId() != null &&
                currentUserId.equals(activity.getCreatorId());

            // Show/hide edit button based on creator status
            btnEditActivity.setVisibility(isCreator ? View.VISIBLE : View.GONE);

            // Set category chip
            if (activity.getCategory() != null) {
                categoryChip.setText(activity.getCategory());
                setCategoryColor(categoryChip, activity.getCategory());
            }

            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActivityClick(activity);
                }
            });

            btnViewDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActivityClick(activity);
                }
            });

            btnEditActivity.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditActivity(activity);
                }
            });
        }

        /**
         * Get current user ID from Firebase or SharedPreferences
         */
        private String getCurrentUserId(Context context) {
            // Try Firebase Auth first
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                return firebaseUser.getUid();
            }

            // Fallback to SharedPreferences for social login
            SharedPreferences prefs = context.getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
            return prefs.getString("user_id", null);
        }

        private void setCategoryColor(Chip chip, String category) {
            int color;
            switch (category) {
                case "운동":
                case "Sports":
                    color = Color.parseColor("#FF6B6B"); // Red
                    break;
                case "야외활동":
                    color = Color.parseColor("#FF8C42"); // Orange
                    break;
                case "스터디":
                case "Study":
                    color = Color.parseColor("#4ECDC4"); // Teal
                    break;
                case "문화":
                    color = Color.parseColor("#A8E6CF"); // Light green
                    break;
                case "소셜":
                case "Social":
                    color = Color.parseColor("#FFD93D"); // Yellow
                    break;
                case "맛집":
                    color = Color.parseColor("#FF6B9D"); // Pink
                    break;
                case "여행":
                    color = Color.parseColor("#95E1D3"); // Aqua
                    break;
                case "게임":
                    color = Color.parseColor("#AA96DA"); // Light purple
                    break;
                case "취미":
                    color = Color.parseColor("#FCBAD3"); // Light pink
                    break;
                case "봉사":
                    color = Color.parseColor("#A8D8EA"); // Light blue
                    break;
                case "기타":
                default:
                    color = Color.parseColor("#6C5CE7"); // Purple
                    break;
            }
            chip.setChipBackgroundColorResource(android.R.color.transparent);
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color));
            chip.setTextColor(Color.WHITE);
        }
    }
}
