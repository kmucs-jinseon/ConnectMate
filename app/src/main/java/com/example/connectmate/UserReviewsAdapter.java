package com.example.connectmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        private final TextView ratingText;
        private final TextView commentText;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ratingText = itemView.findViewById(R.id.review_rating_text);
            commentText = itemView.findViewById(R.id.review_comment_text);
        }

        void bind(UserReview review) {
            ratingText.setText(review.getRating() + "점");
            String comment = review.getComment();
            if (comment == null || comment.trim().isEmpty()) {
                comment = "한줄평이 없습니다.";
            }
            commentText.setText(comment);
        }
    }
}
