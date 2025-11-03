package com.example.connectmate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * MapFragment - Simple empty fragment for Map tab
 * The actual map is now in MainActivity as a background layer
 */
public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    // Inner class for Activity Marker data (used by MainActivity)
    public static class ActivityMarker {
        private final String id;
        private final String title;
        private final String location;
        private final String time;
        private final String description;
        private final int currentParticipants;
        private final int maxParticipants;
        private final double latitude;
        private final double longitude;
        private final String category;

        public ActivityMarker(String id, String title, String location, String time,
                            String description, int currentParticipants, int maxParticipants,
                            double latitude, double longitude, String category) {
            this.id = id;
            this.title = title;
            this.location = location;
            this.time = time;
            this.description = description;
            this.currentParticipants = currentParticipants;
            this.maxParticipants = maxParticipants;
            this.latitude = latitude;
            this.longitude = longitude;
            this.category = category;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getLocation() { return location; }
        public String getTime() { return time; }
        public String getDescription() { return description; }
        public int getCurrentParticipants() { return currentParticipants; }
        public int getMaxParticipants() { return maxParticipants; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getCategory() { return category; }
    }
}
