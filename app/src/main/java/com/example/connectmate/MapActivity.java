package com.example.connectmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelStyles;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private FloatingActionButton fabCreateActivity;
    private ImageButton searchButton;
    private TextInputLayout searchInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to our restored complex layout
        setContentView(R.layout.activity_map);

        // Initialize other UI components from the original layout
        fabCreateActivity = findViewById(R.id.fab_create_activity);
        searchButton = findViewById(R.id.search_button);
        searchInputLayout = findViewById(R.id.search_input_layout);

        // Set up listeners for the original UI components
        setupFabClickListener();
        setupSearchButtonClickListener();

        // Initialize the bottom sheet behavior from the original layout
        CardView bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior<CardView> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(200);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Dynamically create and add the MapView
        mapView = new MapView(this);
        LinearLayout mapContainer = findViewById(R.id.map_container);
        mapContainer.addView(mapView);

        // Start the map with its lifecycle and ready callbacks
        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                // Map Destroy
            }

            @Override
            public void onMapError(Exception error) {
                // Map Error
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap kakaoMap) {
                // API Authentication is complete, and the map is ready.
                kakaoMap.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(37.5665, 126.9780)));
                LabelStyles styles = LabelStyles.from(MapActivity.this, R.drawable.main_logo);
                LabelOptions options = LabelOptions.from(LatLng.from(37.5665, 126.9780))
                                                   .setStyles(styles)
                                                   .setTexts("서울");
                kakaoMap.getLabelManager().getLayer().addLabel(options);
            }
        });
    }

    private void setupFabClickListener() {
        fabCreateActivity.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, CreateActivityActivity.class);
            startActivity(intent);
        });
    }

    private void setupSearchButtonClickListener() {
        searchButton.setOnClickListener(v -> {
            if (searchInputLayout.getVisibility() == View.VISIBLE) {
                searchInputLayout.setVisibility(View.GONE);
            } else {
                searchInputLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.pause();
        }
    }
}
