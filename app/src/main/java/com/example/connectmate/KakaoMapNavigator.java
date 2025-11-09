package com.example.connectmate;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/**
 * Utility class for navigating to locations using Kakao Map
 */
public class KakaoMapNavigator {
    private static final String TAG = "KakaoMapNavigator";
    private static final String KAKAO_MAP_PACKAGE = "net.daum.android.map";

    /**
     * Navigate to a specific coordinate using Kakao Map
     * @param context The context to use for launching the intent
     * @param latitude The latitude of the destination
     * @param longitude The longitude of the destination
     * @param placeName Optional place name to display (can be null)
     */
    public static void navigateToLocation(Context context, double latitude, double longitude, String placeName) {
        if (isKakaoMapInstalled(context)) {
            // Use app scheme if Kakao Map is installed
            String scheme = String.format("kakaomap://look?p=%f,%f", latitude, longitude);
            openKakaoMapScheme(context, scheme, placeName);
        } else {
            // Fallback to mobile web
            String webUrl = String.format("http://m.map.kakao.com/scheme/look?p=%f,%f", latitude, longitude);
            openKakaoMapWeb(context, webUrl, placeName);
        }
    }

    /**
     * Get directions from current location to destination using Kakao Map
     * @param context The context to use for launching the intent
     * @param fromLat Starting latitude
     * @param fromLng Starting longitude
     * @param toLat Destination latitude
     * @param toLng Destination longitude
     * @param transportMode Transport mode: "car", "publictransit", "foot", or "bicycle"
     */
    public static void getDirections(Context context, double fromLat, double fromLng,
                                     double toLat, double toLng, String transportMode) {
        if (isKakaoMapInstalled(context)) {
            // Use app scheme
            String scheme = String.format("kakaomap://route?sp=%f,%f&ep=%f,%f&by=%s",
                    fromLat, fromLng, toLat, toLng, transportMode);
            openKakaoMapScheme(context, scheme, "길찾기");
        } else {
            // Fallback to mobile web
            String webUrl = String.format("http://m.map.kakao.com/scheme/route?sp=%f,%f&ep=%f,%f&by=%s",
                    fromLat, fromLng, toLat, toLng, transportMode);
            openKakaoMapWeb(context, webUrl, "길찾기");
        }
    }

    /**
     * Search for a place on Kakao Map
     * @param context The context to use for launching the intent
     * @param query Search query
     * @param centerLat Center latitude for search (optional)
     * @param centerLng Center longitude for search (optional)
     */
    public static void searchPlace(Context context, String query, Double centerLat, Double centerLng) {
        if (isKakaoMapInstalled(context)) {
            String scheme;
            if (centerLat != null && centerLng != null) {
                scheme = String.format("kakaomap://search?q=%s&p=%f,%f",
                        Uri.encode(query), centerLat, centerLng);
            } else {
                scheme = String.format("kakaomap://search?q=%s", Uri.encode(query));
            }
            openKakaoMapScheme(context, scheme, "검색");
        } else {
            // Fallback to mobile web
            String webUrl;
            if (centerLat != null && centerLng != null) {
                webUrl = String.format("http://m.map.kakao.com/scheme/search?q=%s&p=%f,%f",
                        Uri.encode(query), centerLat, centerLng);
            } else {
                webUrl = String.format("http://m.map.kakao.com/scheme/search?q=%s", Uri.encode(query));
            }
            openKakaoMapWeb(context, webUrl, "검색");
        }
    }

    /**
     * Check if Kakao Map app is installed
     */
    public static boolean isKakaoMapInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(KAKAO_MAP_PACKAGE, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Open Kakao Map using app scheme
     */
    private static void openKakaoMapScheme(Context context, String scheme, String fallbackMessage) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(scheme));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "Opened Kakao Map with scheme: " + scheme);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Failed to open Kakao Map app", e);
            Toast.makeText(context, "카카오맵을 열 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Open Kakao Map using mobile web fallback
     */
    private static void openKakaoMapWeb(Context context, String url, String fallbackMessage) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "Opened Kakao Map web: " + url);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Failed to open Kakao Map web", e);
            Toast.makeText(context, "카카오맵을 열 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show install prompt for Kakao Map
     */
    public static void showInstallPrompt(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + KAKAO_MAP_PACKAGE));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // If Play Store is not available, open in browser
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + KAKAO_MAP_PACKAGE));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
