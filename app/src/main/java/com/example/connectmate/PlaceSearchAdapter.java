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
         * Kakao categories format: "음식점 > 카페", "문화,예술 > 공원" etc.
         */
        private void setCategoryIconAndColor(String category) {
            int iconRes = R.drawable.ic_map_pin;
            int colorRes = R.color.blue_500;

            if (category != null && !category.isEmpty()) {
                String lowerCategory = category.toLowerCase();

                // Food & Dining
                if (lowerCategory.contains("음식점") || lowerCategory.contains("restaurant")) {
                    iconRes = R.drawable.ic_activity;
                    colorRes = R.color.orange_500;
                } else if (lowerCategory.contains("카페") || lowerCategory.contains("cafe") || lowerCategory.contains("디저트")) {
                    iconRes = R.drawable.ic_activity;
                    colorRes = R.color.brown_500;
                }
                // Shopping
                else if (lowerCategory.contains("편의점") || lowerCategory.contains("마트") ||
                         lowerCategory.contains("쇼핑") || lowerCategory.contains("상점")) {
                    iconRes = R.drawable.ic_activity;
                    colorRes = R.color.purple_500;
                }
                // Health & Medical
                else if (lowerCategory.contains("병원") || lowerCategory.contains("약국") ||
                         lowerCategory.contains("의료") || lowerCategory.contains("health")) {
                    iconRes = R.drawable.ic_plus;
                    colorRes = R.color.red_500;
                }
                // Education
                else if (lowerCategory.contains("학교") || lowerCategory.contains("교육") ||
                         lowerCategory.contains("학원") || lowerCategory.contains("도서관")) {
                    iconRes = R.drawable.ic_activity;
                    colorRes = R.color.indigo_500;
                }
                // Culture & Entertainment
                else if (lowerCategory.contains("문화") || lowerCategory.contains("예술") ||
                         lowerCategory.contains("영화") || lowerCategory.contains("공연")) {
                    iconRes = R.drawable.ic_activity;
                    colorRes = R.color.pink_500;
                }
                // Sports & Fitness
                else if (lowerCategory.contains("운동") || lowerCategory.contains("체육") ||
                         lowerCategory.contains("헬스") || lowerCategory.contains("fitness")) {
                    iconRes = R.drawable.ic_activity;
                    colorRes = R.color.green_500;
                }
                // Parks & Nature
                else if (lowerCategory.contains("공원") || lowerCategory.contains("park") ||
                         lowerCategory.contains("자연")) {
                    iconRes = R.drawable.ic_map_pin;
                    colorRes = R.color.teal_500;
                }
                // Transportation
                else if (lowerCategory.contains("교통") || lowerCategory.contains("주차") ||
                         lowerCategory.contains("정류장") || lowerCategory.contains("역")) {
                    iconRes = R.drawable.ic_map_pin;
                    colorRes = R.color.gray_600;
                }
                // Accommodation
                else if (lowerCategory.contains("숙박") || lowerCategory.contains("호텔") ||
                         lowerCategory.contains("모텔")) {
                    iconRes = R.drawable.ic_activity;
                    colorRes = R.color.cyan_500;
                }
                // Social & Community
                else if (lowerCategory.contains("커뮤니티") || lowerCategory.contains("모임") ||
                         lowerCategory.contains("community")) {
                    iconRes = R.drawable.ic_people;
                    colorRes = R.color.amber_500;
                }
            }

            // Set icon and background color
            locationIcon.setImageResource(iconRes);
            iconContainer.setCardBackgroundColor(
                itemView.getContext().getResources().getColor(colorRes, null)
            );
        }
    }
}
