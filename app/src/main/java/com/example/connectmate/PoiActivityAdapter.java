package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.Activity;
import com.google.android.material.button.MaterialButton;

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
        holder.activityButton.setText(activity.getTitle());

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
        MaterialButton activityButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            activityButton = itemView.findViewById(R.id.poi_activity_button);
        }
    }
}
