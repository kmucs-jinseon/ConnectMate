package com.example.connectmate;

/**
 * Model class representing a search result from Kakao Local API
 */
public class SearchResult {
    private String placeName;
    private String addressName;
    private String roadAddressName;
    private String categoryName;
    private double latitude;
    private double longitude;
    private String phone;
    private String placeUrl;

    public SearchResult(String placeName, String addressName, String roadAddressName,
                       String categoryName, double latitude, double longitude,
                       String phone, String placeUrl) {
        this.placeName = placeName;
        this.addressName = addressName;
        this.roadAddressName = roadAddressName;
        this.categoryName = categoryName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.placeUrl = placeUrl;
    }

    public String getPlaceName() {
        return placeName;
    }

    public String getAddressName() {
        return addressName;
    }

    public String getRoadAddressName() {
        return roadAddressName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getPhone() {
        return phone;
    }

    public String getPlaceUrl() {
        return placeUrl;
    }

    /**
     * Get display address (prefer road address, fall back to address)
     */
    public String getDisplayAddress() {
        if (roadAddressName != null && !roadAddressName.isEmpty()) {
            return roadAddressName;
        }
        return addressName;
    }
}
