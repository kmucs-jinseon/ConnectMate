package com.example.connectmate;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connectmate.models.PendingReviewItem;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PendingReviewsAdapter extends RecyclerView.Adapter<PendingReviewsAdapter.PendingReviewViewHolder> {

    public interface OnPendingReviewClickListener {
        void onPendingReviewClick(PendingReviewItem item);
    }

    private final List<PendingReviewItem> pendingReviews;
    private final OnPendingReviewClickListener listener;
    private final SimpleDateFormat dateFormat =
        new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault());

    public PendingReviewsAdapter(List<PendingReviewItem> pendingReviews,
                                 OnPendingReviewClickListener listener) {
        this.pendingReviews = pendingReviews;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PendingReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_pending_review, parent, false);
        return new PendingReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingReviewViewHolder holder, int position) {
        holder.bind(pendingReviews.get(position));
    }

    @Override
    public int getItemCount() {
        return pendingReviews.size();
    }

    class PendingReviewViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView avatar;
        private final TextView name;
        private final TextView activityTitle;
        private final TextView timestamp;
        private final MaterialButton reviewButton;

        PendingReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.review_target_avatar);
            name = itemView.findViewById(R.id.review_target_name);
            activityTitle = itemView.findViewById(R.id.review_activity_title);
            timestamp = itemView.findViewById(R.id.review_timestamp);
            reviewButton = itemView.findViewById(R.id.btn_start_review);
        }

        void bind(PendingReviewItem item) {
            String displayName = !TextUtils.isEmpty(item.getTargetDisplayName())
                ? item.getTargetDisplayName() : "알 수 없는 사용자";
            name.setText(displayName);

            // Get position to check if we need to show activity title as header
            int position = getAdapterPosition();
            boolean showActivityTitle = false;

            if (position >= 0 && position < pendingReviews.size()) {
                // Show activity title if it's the first item or different from previous item
                if (position == 0) {
                    showActivityTitle = true;
                } else {
                    PendingReviewItem prevItem = pendingReviews.get(position - 1);
                    String currentActivity = item.getActivityTitle();
                    String prevActivity = prevItem.getActivityTitle();

                    // Check if activity changed
                    if (!TextUtils.equals(currentActivity, prevActivity)) {
                        showActivityTitle = true;
                    }
                }
            }

            if (showActivityTitle) {
                activityTitle.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(item.getActivityTitle())) {
                    activityTitle.setText("📍 " + item.getActivityTitle());
                } else {
                    activityTitle.setText("📍 활동 정보 없음");
                }
            } else {
                activityTitle.setVisibility(View.GONE);
            }

            timestamp.setText(formatTimestamp(item.getTimestamp()));

            if (!TextUtils.isEmpty(item.getTargetProfileImageUrl())) {
                Glide.with(avatar.getContext())
                    .load(item.getTargetProfileImageUrl())
                    .placeholder(R.drawable.circle_logo)
                    .error(R.drawable.circle_logo)
                    .into(avatar);
            } else {
                avatar.setImageResource(R.drawable.circle_logo);
            }

            View.OnClickListener clickListener = v -> {
                if (listener != null) {
                    listener.onPendingReviewClick(item);
                }
            };

            itemView.setOnClickListener(clickListener);
            reviewButton.setOnClickListener(clickListener);
        }

        private String formatTimestamp(long ts) {
            if (ts <= 0) {
                return "최근 요청";
            }
            return dateFormat.format(new Date(ts));
        }
    }
}
