package com.example.connectmate.models;

/**
 * Data class representing a place from Kakao Local Search API.
 */
public class Place {
    private final String name;
    private final String address;
    private final double longitude; // 경도 (x)
    private final double latitude;  // 위도 (y)

    public Place(String name, String address, double longitude, double latitude) {
        this.name = name;
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    @Override
    public String toString() {
        return "Place{" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                '}';
    }
}
