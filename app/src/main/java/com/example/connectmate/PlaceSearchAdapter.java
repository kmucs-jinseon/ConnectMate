package com.example.connectmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.PlaceSearchResult;
import com.example.connectmate.utils.CategoryMapper;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying place search results
 */
public class PlaceSearchAdapter extends RecyclerView.Adapter<PlaceSearchAdapter.ViewHolder> {

    private List<PlaceSearchResult> searchResults;
    private OnPlaceClickListener listener;

    public interface OnPlaceClickListener {
        void onPlaceClick(PlaceSearchResult place);
    }

    public PlaceSearchAdapter(List<PlaceSearchResult> searchResults, OnPlaceClickListener listener) {
        this.searchResults = searchResults;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_map_location, parent, false);
        return new ViewHolder(view, listener, searchResults);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlaceSearchResult place = searchResults.get(position);
        holder.bind(place);
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public void updateResults(List<PlaceSearchResult> newResults) {
        this.searchResults = newResults;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView iconContainer;
        ImageView locationIcon;
        TextView locationName;
        TextView locationCategory;
        TextView distanceText;

        ViewHolder(View itemView, OnPlaceClickListener listener, List<PlaceSearchResult> searchResults) {
            super(itemView);

            iconContainer = itemView.findViewById(R.id.icon_container);
            locationIcon = itemView.findViewById(R.id.location_icon);
            locationName = itemView.findViewById(R.id.location_name);
            locationCategory = itemView.findViewById(R.id.location_category);
            distanceText = itemView.findViewById(R.id.distance_text);

            // Lambda captures listener and searchResults directly - no need to store as fields
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPlaceClick(searchResults.get(position));
                }
            });
        }

        void bind(PlaceSearchResult place) {
            locationName.setText(place.getPlaceName());

            String address = place.getRoadAddressName() != null && !place.getRoadAddressName().isEmpty()
                ? place.getRoadAddressName()
                : place.getAddressName();

            locationCategory.setText(address);

            // Set icon and color based on category
            setCategoryIconAndColor(place.getCategoryName());

            // Show distance if available
            if (place.getDistance() > 0) {
                distanceText.setVisibility(android.view.View.VISIBLE);
                if (place.getDistance() < 1000) {
                    distanceText.setText(String.format(Locale.getDefault(), "%dm", place.getDistance()));
                } else {
                    distanceText.setText(String.format(Locale.getDefault(), "%.1f km", place.getDistance() / 1000.0));
                }
            } else {
                distanceText.setVisibility(android.view.View.GONE);
            }
        }

        /**
         * Set icon and background color based on place category
         * Uses CategoryMapper to map Kakao categories to activity categories
         */
        private void setCategoryIconAndColor(String kakaoCategory) {
            // Map Kakao category to our activity category
            String mappedCategory = CategoryMapper.mapKakaoCategoryToActivity(kakaoCategory);

            // Get icon and color for the mapped category
            int iconRes = CategoryMapper.getCategoryIcon(mappedCategory);
            int colorRes = CategoryMapper.getCategoryColor(mappedCategory);

            // Set icon and background color
            locationIcon.setImageResource(iconRes);
            iconContainer.setCardBackgroundColor(
                itemView.getContext().getResources().getColor(colorRes, null)
            );
        }
    }
}
