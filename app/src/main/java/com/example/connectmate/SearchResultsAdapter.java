package com.example.connectmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying search results in a RecyclerView
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private List<SearchResult> searchResults = new ArrayList<>();
    private OnResultClickListener clickListener;

    public interface OnResultClickListener {
        void onResultClick(SearchResult result);
    }

    public SearchResultsAdapter(OnResultClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = searchResults.get(position);
        holder.bind(result);
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public void setResults(List<SearchResult> results) {
        this.searchResults = results;
        notifyDataSetChanged();
    }

    public void clearResults() {
        this.searchResults.clear();
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView placeName;
        private TextView placeAddress;
        private TextView placeCategory;

        ViewHolder(View itemView) {
            super(itemView);
            placeName = itemView.findViewById(R.id.place_name);
            placeAddress = itemView.findViewById(R.id.place_address);
            placeCategory = itemView.findViewById(R.id.place_category);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onResultClick(searchResults.get(position));
                }
            });
        }

        void bind(SearchResult result) {
            placeName.setText(result.getPlaceName());
            placeAddress.setText(result.getDisplayAddress());

            // Format category name (remove > separators for cleaner display)
            String category = result.getCategoryName();
            if (category != null && !category.isEmpty()) {
                String[] parts = category.split(" > ");
                placeCategory.setText(parts[parts.length - 1]); // Show last category
                placeCategory.setVisibility(View.VISIBLE);
            } else {
                placeCategory.setVisibility(View.GONE);
            }
        }
    }
}
