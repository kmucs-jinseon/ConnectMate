package com.example.connectmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.UserReview;

import java.util.List;

public class UserReviewsAdapter extends RecyclerView.Adapter<UserReviewsAdapter.ReviewViewHolder> {

    private final List<UserReview> reviews;

    public UserReviewsAdapter(List<UserReview> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_user_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        holder.bind(reviews.get(position));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final TextView activityTitleText;
        private final TextView ratingText;
        private final TextView commentText;
        private final LinearLayout starsContainer;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            activityTitleText = itemView.findViewById(R.id.review_activity_title);
            ratingText = itemView.findViewById(R.id.review_rating_text);
            commentText = itemView.findViewById(R.id.review_comment_text);
            starsContainer = itemView.findViewById(R.id.review_stars_container);
        }

        void bind(UserReview review) {
            // Display activity title
            String activityTitle = review.getActivityTitle();
            if (activityTitle != null && !activityTitle.trim().isEmpty()) {
                activityTitleText.setText(activityTitle);
                activityTitleText.setVisibility(View.VISIBLE);
            } else {
                activityTitleText.setText("활동 정보 없음");
                activityTitleText.setVisibility(View.VISIBLE);
            }

            ratingText.setText(review.getRating() + "점");

            // Display stars based on rating
            starsContainer.removeAllViews();
            int ratingValue = review.getRating();
            int starCount = ratingValue > 0 ? ratingValue : 1;
            float density = itemView.getContext().getResources().getDisplayMetrics().density;

            for (int j = 0; j < starCount; j++) {
                ImageView star = new ImageView(itemView.getContext());
                LinearLayout.LayoutParams starParams = new LinearLayout.LayoutParams(
                    (int) (20 * density),
                    (int) (20 * density)
                );
                if (j > 0) {
                    starParams.setMarginStart((int) (2 * density));
                }
                star.setLayoutParams(starParams);
                star.setImageResource(R.drawable.ic_star_filled);
                star.setColorFilter(itemView.getContext().getResources().getColor(
                    ratingValue > 0 ? R.color.yellow_500 : R.color.gray_100, null
                ));
                starsContainer.addView(star);
            }

            String comment = review.getComment();
            if (comment == null || comment.trim().isEmpty()) {
                comment = "한 줄 평이 없습니다.";
            }
            commentText.setText(comment);
        }
    }
}
