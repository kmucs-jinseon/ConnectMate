package com.example.connectmate;

/**
 * ActivityMarker - Data class for storing activity information for map markers
 */
public class ActivityMarker {
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
