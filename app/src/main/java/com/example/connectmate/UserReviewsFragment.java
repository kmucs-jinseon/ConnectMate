package com.example.connectmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.UserReview;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserReviewsFragment extends Fragment {

    private static final String ARG_USER_ID = "arg_user_id";

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private TextView emptyState;
    private ProgressBar progressBar;

    private final List<UserReview> reviewList = new ArrayList<>();
    private UserReviewsAdapter adapter;
    private DatabaseReference reviewsRef;
    private ValueEventListener reviewsListener;
    private FirebaseAuth auth;
    private String userId;

    public UserReviewsFragment() {
        super(R.layout.fragment_user_reviews);
    }

    public static UserReviewsFragment newInstance(@NonNull String userId) {
        UserReviewsFragment fragment = new UserReviewsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        toolbar = view.findViewById(R.id.user_reviews_toolbar);
        recyclerView = view.findViewById(R.id.reviews_recycler_view);
        emptyState = view.findViewById(R.id.reviews_empty_state);
        progressBar = view.findViewById(R.id.reviews_progress_bar);

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        }

        adapter = new UserReviewsAdapter(reviewList);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        String resolvedUserId = resolveUserId();
        if (resolvedUserId == null || resolvedUserId.isEmpty()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        reviewsRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(resolvedUserId)
            .child("reviews");

        loadReviews();
    }

    private void loadReviews() {
        showLoading(true);
        reviewsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reviewList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    UserReview review = child.getValue(UserReview.class);
                    if (review != null) {
                        if (review.getReviewId() == null) {
                            review.setReviewId(child.getKey());
                        }
                        reviewList.add(review);
                    }
                }
                Collections.sort(reviewList, (a, b) ->
                    Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.notifyDataSetChanged();
                showLoading(false);
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(requireContext(), "후기를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        };
        reviewsRef.addValueEventListener(reviewsListener);
    }

    private void updateEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(reviewList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reviewsRef != null && reviewsListener != null) {
            reviewsRef.removeEventListener(reviewsListener);
        }
    }

    private String resolveUserId() {
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            return firebaseUser.getUid();
        }
        SharedPreferences prefs = requireContext()
            .getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        return prefs.getString("user_id", null);
    }
}
