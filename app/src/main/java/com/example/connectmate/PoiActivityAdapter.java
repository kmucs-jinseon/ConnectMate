package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.Activity;
import com.example.connectmate.utils.CategoryMapper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class PoiActivityAdapter extends RecyclerView.Adapter<PoiActivityAdapter.ViewHolder> {

    private final Context context;
    private final List<Activity> activities;

    public PoiActivityAdapter(Context context, List<Activity> activities) {
        this.context = context;
        this.activities = activities;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_poi_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Activity activity = activities.get(position);

        // Set activity name
        holder.activityName.setText(activity.getTitle());

        // Set categories (support multiple categories separated by comma or semicolon)
        holder.categoryGroup.removeAllViews();
        String categories = activity.getCategory();
        if (categories != null && !categories.isEmpty()) {
            // Split by comma or semicolon
            String[] categoryArray = categories.split("[,;]");

            for (String category : categoryArray) {
                category = category.trim();
                if (!category.isEmpty()) {
                    // Create chip with CategoryChipStyle
                    Chip chip = new Chip(new android.view.ContextThemeWrapper(context, R.style.CategoryChipStyle));
                    chip.setText(category);
                    chip.setClickable(false);

                    // Set category-specific pastel color for display
                    int colorRes = com.example.connectmate.utils.CategoryMapper.getCategoryColor(category);
                    int color = androidx.core.content.ContextCompat.getColor(context, colorRes);
                    chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color));

                    // Set dark gray text and stroke for pastel background (hardcoded to ensure dark in both themes)
                    int textColor = android.graphics.Color.parseColor("#4B5563");  // Dark gray
                    chip.setTextColor(textColor);
                    chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(textColor));
                    chip.setChipStrokeWidth(2 * context.getResources().getDisplayMetrics().density);

                    holder.categoryGroup.addView(chip);
                }
            }

            holder.categoryGroup.setVisibility(View.VISIBLE);
        } else {
            holder.categoryGroup.setVisibility(View.GONE);
        }

        // Set description
        String description = activity.getDescription();
        if (description != null && !description.isEmpty()) {
            holder.activityDescription.setText(description);
            holder.activityDescription.setVisibility(View.VISIBLE);
        } else {
            holder.activityDescription.setVisibility(View.GONE);
        }

        // Hide divider for the last item
        if (position == activities.size() - 1) {
            holder.divider.setVisibility(View.GONE);
        } else {
            holder.divider.setVisibility(View.VISIBLE);
        }

        // Click listener to open activity detail
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ActivityDetailActivity.class);
            intent.putExtra("activity_id", activity.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView activityName;
        ChipGroup categoryGroup;
        TextView activityDescription;
        View divider;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            activityName = itemView.findViewById(R.id.poi_activity_name);
            categoryGroup = itemView.findViewById(R.id.poi_activity_category_group);
            activityDescription = itemView.findViewById(R.id.poi_activity_description);
            divider = itemView.findViewById(R.id.poi_activity_divider);
        }
    }
}
