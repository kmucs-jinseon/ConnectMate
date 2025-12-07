package com.example.connectmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.connectmate.models.PendingReviewItem;
import com.example.connectmate.models.UserReview;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class SubmitReviewFragment extends Fragment {

    private static final String ARG_PENDING_ID = "pending_id";
    private static final String ARG_TARGET_ID = "target_id";
    private static final String ARG_TARGET_NAME = "target_name";
    private static final String ARG_TARGET_AVATAR = "target_avatar";
    private static final String ARG_ACTIVITY_ID = "activity_id";
    private static final String ARG_ACTIVITY_TITLE = "activity_title";

    private String pendingId;
    private String targetUserId;
    private String targetName;
    private String targetAvatar;
    private String activityId;
    private String activityTitle;

    private CircleImageView avatarView;
    private TextView nameView;
    private TextView activityTitleView;
    private RatingBar ratingBar;
    private TextInputLayout commentLayout;
    private TextInputEditText commentInput;
    private MaterialButton submitButton;
    private MaterialButton cancelButton;
    private MaterialToolbar toolbar;

    private DatabaseReference usersRef;
    private DatabaseReference pendingReviewsRef;
    private FirebaseAuth auth;
    private String reviewerName;

    public SubmitReviewFragment() {
        super(R.layout.fragment_submit_review);
    }

    public static SubmitReviewFragment newInstance(PendingReviewItem item) {
        SubmitReviewFragment fragment = new SubmitReviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PENDING_ID, item.getId());
        args.putString(ARG_TARGET_ID, item.getTargetUserId());
        args.putString(ARG_TARGET_NAME, item.getTargetDisplayName());
        args.putString(ARG_TARGET_AVATAR, item.getTargetProfileImageUrl());
        args.putString(ARG_ACTIVITY_ID, item.getActivityId());
        args.putString(ARG_ACTIVITY_TITLE, item.getActivityTitle());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pendingId = getArguments().getString(ARG_PENDING_ID);
            targetUserId = getArguments().getString(ARG_TARGET_ID);
            targetName = getArguments().getString(ARG_TARGET_NAME);
            targetAvatar = getArguments().getString(ARG_TARGET_AVATAR);
            activityId = getArguments().getString(ARG_ACTIVITY_ID);
            activityTitle = getArguments().getString(ARG_ACTIVITY_TITLE);
        }
        auth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        pendingReviewsRef = database.getReference("pendingReviews");
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        bindTargetInfo();
        setupToolbar();
        setupButtons();
        loadReviewerName();
        ensureTargetInfo();
        hideFab();
    }

    private void initializeViews(View view) {
        toolbar = view.findViewById(R.id.submit_review_toolbar);
        avatarView = view.findViewById(R.id.review_target_avatar);
        nameView = view.findViewById(R.id.review_target_name);
        activityTitleView = view.findViewById(R.id.review_activity_title);
        ratingBar = view.findViewById(R.id.review_rating_bar);
        commentLayout = view.findViewById(R.id.review_comment_layout);
        commentInput = view.findViewById(R.id.review_comment_input);
        submitButton = view.findViewById(R.id.btn_submit_review);
        cancelButton = view.findViewById(R.id.btn_cancel_review);
    }

    private void bindTargetInfo() {
        if (!TextUtils.isEmpty(targetName)) {
            nameView.setText(targetName);
        }
        if (!TextUtils.isEmpty(activityTitle)) {
            activityTitleView.setText(activityTitle);
        }
        if (!TextUtils.isEmpty(targetAvatar)) {
            Glide.with(this)
                .load(targetAvatar)
                .placeholder(R.drawable.circle_logo)
                .error(R.drawable.circle_logo)
                .into(avatarView);
        } else {
            avatarView.setImageResource(R.drawable.circle_logo);
        }
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        }
    }

    private void setupButtons() {
        submitButton.setOnClickListener(v -> handleSubmit());
        cancelButton.setOnClickListener(v ->
            requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void loadReviewerName() {
        String reviewerId = getCurrentUserId();
        if (TextUtils.isEmpty(reviewerId)) {
            return;
        }
        usersRef.child(reviewerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && !TextUtils.isEmpty(user.displayName)) {
                    reviewerName = user.displayName;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void ensureTargetInfo() {
        if (!TextUtils.isEmpty(targetUserId) && TextUtils.isEmpty(targetName)) {
            usersRef.child(targetUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        targetName = user.displayName;
                        targetAvatar = user.profileImageUrl;
                        bindTargetInfo();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
    }

    private void handleSubmit() {
        int rating = (int) ratingBar.getRating();
        if (rating <= 0) {
            Toast.makeText(requireContext(), "별점을 평가해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(targetUserId)) {
            Toast.makeText(requireContext(), "대상 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String reviewerId = getCurrentUserId();
        if (TextUtils.isEmpty(reviewerId)) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);

        String comment = commentInput.getText() != null
            ? commentInput.getText().toString().trim() : "";
        long timestamp = System.currentTimeMillis();

        String reviewId = !TextUtils.isEmpty(pendingId) ? pendingId
            : usersRef.child(targetUserId).child("reviews").push().getKey();

        String reviewerDisplay = !TextUtils.isEmpty(reviewerName) ? reviewerName : "익명";
        UserReview review = new UserReview(
            reviewId,
            reviewerId,
            reviewerDisplay,
            activityId,
            activityTitle,
            rating,
            comment,
            timestamp
        );

        usersRef.child(targetUserId)
            .child("reviews")
            .child(reviewId)
            .setValue(review)
            .addOnSuccessListener(aVoid -> {
                updateUserRatingStats(targetUserId, rating);
                removePendingEntry(reviewerId);
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                Toast.makeText(requireContext(), "평가를 저장하지 못했습니다.", Toast.LENGTH_SHORT).show();
            });
    }

    private void removePendingEntry(String reviewerId) {
        if (TextUtils.isEmpty(pendingId)) {
            finishSubmission();
            return;
        }

        pendingReviewsRef.child(reviewerId)
            .child(pendingId)
            .removeValue()
            .addOnCompleteListener(task -> {
                // Check if there are any more pending reviews for this activity
                checkAndDeleteNotificationIfAllReviewsComplete(reviewerId, activityId);
                finishSubmission();
            });
    }

    /**
     * Check if all reviews for this activity are complete, and if so, delete the notification
     */
    private void checkAndDeleteNotificationIfAllReviewsComplete(String userId, String activityId) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(activityId)) {
            return;
        }

        // Check if there are any remaining pending reviews for this activity
        pendingReviewsRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasMoreReviewsForActivity = false;
                for (DataSnapshot child : snapshot.getChildren()) {
                    PendingReviewItem item = child.getValue(PendingReviewItem.class);
                    if (item != null && activityId.equals(item.getActivityId())) {
                        hasMoreReviewsForActivity = true;
                        break;
                    }
                }

                // If no more pending reviews for this activity, delete the notification
                if (!hasMoreReviewsForActivity) {
                    deleteReviewNotificationForActivity(userId, activityId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SubmitReviewFragment", "Error checking pending reviews", error.toException());
            }
        });
    }

    /**
     * Delete the review notification and activity end notification for a specific activity
     */
    private void deleteReviewNotificationForActivity(String userId, String activityId) {
        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(activityId)) {
            return;
        }

        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
            .getReference("userNotifications")
            .child(userId);

        notificationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int deletedCount = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    com.example.connectmate.models.NotificationItem notification =
                        child.getValue(com.example.connectmate.models.NotificationItem.class);

                    if (notification != null && activityId.equals(notification.getActivityId())) {
                        // Delete both review request and activity end notifications
                        if ("참여자 평가 요청".equals(notification.getTitle()) ||
                            "활동 종료".equals(notification.getTitle())) {
                            final String notifTitle = notification.getTitle();
                            child.getRef().removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("SubmitReviewFragment", notifTitle + " notification deleted for activity: " + activityId);
                                })
                                .addOnFailureListener(e ->
                                    Log.e("SubmitReviewFragment", "Failed to delete " + notifTitle + " notification", e));
                            deletedCount++;
                        }
                    }
                }
                if (deletedCount > 0) {
                    Log.d("SubmitReviewFragment", "Deleted " + deletedCount + " notifications for activity: " + activityId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SubmitReviewFragment", "Error loading notifications", error.toException());
            }
        });
    }

    private void finishSubmission() {
        Toast.makeText(requireContext(), "평가가 저장되었습니다.", Toast.LENGTH_SHORT).show();
        setLoading(false);
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void updateUserRatingStats(String userId, int rating) {
        usersRef.child(userId).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Double sum = currentData.child("ratingSum").getValue(Double.class);
                Long count = currentData.child("reviewCount").getValue(Long.class);
                if (sum == null) sum = 0.0;
                if (count == null) count = 0L;
                sum += rating;
                count += 1;
                currentData.child("ratingSum").setValue(sum);
                currentData.child("reviewCount").setValue(count);
                double average = count > 0 ? sum / count : 0.0;
                currentData.child("rating").setValue(average);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                                   @Nullable DataSnapshot currentData) {
                // No-op
            }
        });
    }

    private void setLoading(boolean loading) {
        submitButton.setEnabled(!loading);
        cancelButton.setEnabled(!loading);
        submitButton.setText(loading ? "저장 중..." : "제출하기");
    }

    private String getCurrentUserId() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            return firebaseUser.getUid();
        }
        SharedPreferences prefs = requireContext()
            .getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        return prefs.getString("user_id", "");
    }

    private void hideFab() {
        View fab = requireActivity().findViewById(R.id.fabCreateActivity);
        if (fab != null) {
            fab.setVisibility(View.GONE);
        }
    }

    private void showFab() {
        View fab = requireActivity().findViewById(R.id.fabCreateActivity);
        if (fab != null) {
            fab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showFab();
    }
}
