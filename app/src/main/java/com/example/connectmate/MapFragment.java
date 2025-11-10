package com.example.connectmate;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectmate.models.Place;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;
import com.kakao.vectormap.label.LabelLayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// Import BuildConfig to access API key
import com.example.connectmate.BuildConfig;

public class MapFragment extends Fragment implements MapSearchAdapter.OnPlaceClickListener {

    private static final String TAG = "MapFragment";

    // UI Components
    private MapView mapView;
    private ProgressBar loadingIndicator;
    private EditText searchEditText;
    private ImageButton searchIconButton, clearSearchButton;
    private RecyclerView searchResultsRecycler;
    private CardView searchResultsCard;

    // Map components
    private KakaoMap kakaoMap;
    private LocationManager locationManager;

    // Search
    private MapSearchAdapter searchAdapter;
    private List<Place> searchResults = new ArrayList<>();
    private OkHttpClient httpClient = new OkHttpClient();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupSearch();
        setupMap();
    }

    private void initializeViews(View view) {
        mapView = view.findViewById(R.id.map_view);
        loadingIndicator = view.findViewById(R.id.loading_indicator);
        searchEditText = view.findViewById(R.id.search_edit_text);
        searchIconButton = view.findViewById(R.id.search_icon_button);
        clearSearchButton = view.findViewById(R.id.clear_search_button);
        searchResultsRecycler = view.findViewById(R.id.search_results_recycler);
        searchResultsCard = view.findViewById(R.id.search_results_card);
    }

    private void setupMap() {
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {}

            @Override
            public void onMapError(Exception error) {
                Log.e(TAG, "Map Error: ", error);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Map Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap map) {
                kakaoMap = map;
                moveToCurrentLocation();
            }
        });
    }

    private void setupSearch() {
        searchAdapter = new MapSearchAdapter(searchResults, this);
        searchResultsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        searchResultsRecycler.setAdapter(searchAdapter);

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchPlaces(v.getText().toString());
                return true;
            }
            return false;
        });

        searchIconButton.setOnClickListener(v -> searchPlaces(searchEditText.getText().toString()));
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchResults.clear();
            searchAdapter.notifyDataSetChanged();
            searchResultsCard.setVisibility(View.GONE);
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchPlaces(String query) {
        if (query.trim().isEmpty()) {
            Toast.makeText(getContext(), "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        hideKeyboard();
        loadingIndicator.setVisibility(View.VISIBLE);

        String url = "https://dapi.kakao.com/v2/local/search/keyword.json?query=" + query;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "KakaoAK " + BuildConfig.KAKAO_REST_API_KEY)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Search failed", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadingIndicator.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "검색에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() != null && response.isSuccessful()) {
                    final String responseData = response.body().string();
                    getActivity().runOnUiThread(() -> {
                        parsePlaces(responseData);
                        loadingIndicator.setVisibility(View.GONE);
                    });
                } else {
                    if (getActivity() != null) {
                         getActivity().runOnUiThread(() -> {
                            loadingIndicator.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        });
    }

    private void parsePlaces(String jsonData) {
        searchResults.clear();
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            JSONArray documents = jsonObject.getJSONArray("documents");

            for (int i = 0; i < documents.length(); i++) {
                JSONObject doc = documents.getJSONObject(i);
                String name = doc.getString("place_name");
                String address = doc.getString("address_name");
                double longitude = Double.parseDouble(doc.getString("x"));
                double latitude = Double.parseDouble(doc.getString("y"));
                searchResults.add(new Place(name, address, longitude, latitude));
            }
        } catch (Exception e) {
            Log.e(TAG, "JSON parsing failed", e);
        }

        searchAdapter.notifyDataSetChanged();
        searchResultsCard.setVisibility(searchResults.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onPlaceClick(Place place) {
        hideKeyboard();
        searchResultsCard.setVisibility(View.GONE);
        navigateToLocation(place.getLatitude(), place.getLongitude(), place.getName());
    }
    
    // Methods for MainActivity
    public void navigateToLocation(double latitude, double longitude, String title) {
        if (kakaoMap == null) {
            if (mapView != null) {
                mapView.post(() -> navigateToLocation(latitude, longitude, title));
            }
            return;
        }
        LatLng position = LatLng.from(latitude, longitude);
        kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(position, 16));

        LabelLayer layer = kakaoMap.getLabelManager().getLayer();
        if (layer != null) {
            layer.removeAll();
            LabelStyles styles = LabelStyles.from(LabelStyle.from(R.drawable.ic_map_pin).setAnchorPoint(0.5f, 1.0f));
            LabelOptions options = LabelOptions.from(title, position).setStyles(styles);
            layer.addLabel(options);
        }
    }

    public void showRouteToLocation(double latitude, double longitude, String title) {
        navigateToLocation(latitude, longitude, title);
        if (getContext() != null) {
            Toast.makeText(getContext(), "Route to " + title + " (Route drawing coming soon)", Toast.LENGTH_LONG).show();
        }
    }

    public void moveToCurrent() {
        moveToCurrentLocation();
    }

    public void zoomIn() {
        if (kakaoMap != null) {
            kakaoMap.moveCamera(CameraUpdateFactory.zoomIn());
        }
    }

    public void zoomOut() {
        if (kakaoMap != null) {
            kakaoMap.moveCamera(CameraUpdateFactory.zoomOut());
        }
    }

    public void toggleMapType() {
        if (kakaoMap != null && getContext() != null) {
            Toast.makeText(getContext(), "Map type toggle not implemented yet", Toast.LENGTH_SHORT).show();
        }
    }
    
    // Private helper methods
    private void moveToCurrentLocation() {
        if (kakaoMap != null && hasLocationPermission()) {
            try {
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (location != null) {
                    LatLng currentPosition = LatLng.from(location.getLatitude(), location.getLongitude());
                    kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(currentPosition, 15));
                } else {
                    moveToDefaultLocation();
                }
            } catch (SecurityException e) {
                moveToDefaultLocation();
            }
        } else {
            moveToDefaultLocation();
        }
    }

    private void moveToDefaultLocation() {
        if (kakaoMap != null) {
            LatLng defaultPosition = LatLng.from(37.5665, 126.9780); // Seoul City Hall
            kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(defaultPosition, 15));
        }
    }

    private boolean hasLocationPermission() {
        return getContext() != null && (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.pause();
    }
}
