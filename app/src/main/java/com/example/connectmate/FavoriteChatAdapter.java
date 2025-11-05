package com.example.connectmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class FavoriteChatAdapter extends RecyclerView.Adapter<FavoriteChatAdapter.FavoriteViewHolder> {

    private final List<ChatListFragment.FavoriteChat> favorites;
    private final OnFavoriteClickListener listener;

    public interface OnFavoriteClickListener {
        void onFavoriteClick(ChatListFragment.FavoriteChat favorite);
    }

    public FavoriteChatAdapter(List<ChatListFragment.FavoriteChat> favorites, OnFavoriteClickListener listener) {
        this.favorites = favorites;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_favorite, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        ChatListFragment.FavoriteChat favorite = favorites.get(position);
        holder.bind(favorite, listener);
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView profileImage;
        private final TextView name;

        public FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.favorite_profile_image);
            name = itemView.findViewById(R.id.favorite_name);
        }

        public void bind(ChatListFragment.FavoriteChat favorite, OnFavoriteClickListener listener) {
            name.setText(favorite.getName());

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFavoriteClick(favorite);
                }
            });

            // Set profile image: use user's profile image if available, otherwise use circle_logo
            if (favorite.getProfileImageUrl() != null && !favorite.getProfileImageUrl().isEmpty()) {
                // TODO: Load user's profile image using Glide/Picasso
                // For now, use default circle_logo
                profileImage.setImageResource(R.drawable.circle_logo);
            } else {
                // Use default circle_logo
                profileImage.setImageResource(R.drawable.circle_logo);
            }
        }
    }
}
