package com.example.connectmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.PendingReviewItem;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingReviewsFragment extends Fragment {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private View emptyState;
    private ProgressBar progressBar;

    private PendingReviewsAdapter adapter;
    private final List<PendingReviewItem> pendingItems = new ArrayList<>();
    private final Map<String, User> userCache = new HashMap<>();

    private DatabaseReference pendingReviewsRef;
    private DatabaseReference usersRef;
    private ValueEventListener pendingListener;
    private FirebaseAuth auth;

    public PendingReviewsFragment() {
        super(R.layout.fragment_pending_reviews);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        auth = FirebaseAuth.getInstance();
        pendingReviewsRef = FirebaseDatabase.getInstance().getReference("pendingReviews");
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        initializeViews(view);
        setupToolbarNavigation();
        setupRecycler();
        observePendingReviews();
    }

    private void initializeViews(View view) {
        toolbar = view.findViewById(R.id.review_toolbar);
        recyclerView = view.findViewById(R.id.pending_reviews_recycler);
        emptyState = view.findViewById(R.id.pending_reviews_empty);
        progressBar = view.findViewById(R.id.pending_reviews_progress);
    }

    private void setupToolbarNavigation() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
    }

    private void setupRecycler() {
        adapter = new PendingReviewsAdapter(pendingItems, this::openSubmitReview);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void observePendingReviews() {
        String userId = getCurrentUserId();
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            updateEmptyState();
            return;
        }

        showLoading(true);
        pendingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingItems.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    PendingReviewItem item = child.getValue(PendingReviewItem.class);
                    if (item != null) {
                        item.setId(child.getKey());
                        pendingItems.add(item);
                        fetchTargetUser(item.getTargetUserId());
                    }
                }
                Collections.sort(pendingItems, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                adapter.notifyDataSetChanged();
                showLoading(false);
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(requireContext(), "평가 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        };

        pendingReviewsRef.child(userId).addValueEventListener(pendingListener);
    }

    private void fetchTargetUser(String userId) {
        if (TextUtils.isEmpty(userId) || userCache.containsKey(userId)) {
            updateItemsWithUser(userId);
            return;
        }

        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User target = snapshot.getValue(User.class);
                if (target != null) {
                    userCache.put(userId, target);
                    updateItemsWithUser(userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore
            }
        });
    }

    private void updateItemsWithUser(String userId) {
        if (TextUtils.isEmpty(userId) || !userCache.containsKey(userId)) {
            return;
        }
        User target = userCache.get(userId);
        for (int i = 0; i < pendingItems.size(); i++) {
            PendingReviewItem item = pendingItems.get(i);
            if (userId.equals(item.getTargetUserId())) {
                item.setTargetDisplayName(target.displayName);
                item.setTargetProfileImageUrl(target.profileImageUrl);
                adapter.notifyItemChanged(i);
            }
        }
    }

    private void updateEmptyState() {
        if (emptyState != null) {
            emptyState.setVisibility(pendingItems.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void openSubmitReview(PendingReviewItem item) {
        SubmitReviewFragment fragment = SubmitReviewFragment.newInstance(item);
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.main_container, fragment)
            .addToBackStack("SubmitReview")
            .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        String userId = getCurrentUserId();
        if (pendingListener != null && !TextUtils.isEmpty(userId)) {
            pendingReviewsRef.child(userId).removeEventListener(pendingListener);
        }
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
}
