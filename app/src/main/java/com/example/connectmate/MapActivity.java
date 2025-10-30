package com.example.connectmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

public class MapActivity extends AppCompatActivity {

    private MapFragment mapFragment;
    private FloatingActionButton fabCreateActivity;
    private ImageButton searchButton;
    private TextInputLayout searchInputLayout;
    private RecyclerView locationsRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to our restored complex layout
        setContentView(R.layout.activity_map);

        // Initialize UI components
        fabCreateActivity = findViewById(R.id.fab_create_activity);
        searchButton = findViewById(R.id.search_button);
        searchInputLayout = findViewById(R.id.search_input_layout);
        locationsRecyclerView = findViewById(R.id.locations_recycler_view);

        // Set up RecyclerView
        setupRecyclerView();

        // Set up listeners for UI components
        setupFabClickListener();
        setupSearchButtonClickListener();

        // Initialize the bottom sheet behavior
        CardView bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior<CardView> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(200);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Initialize and add MapFragment if not already added
        if (savedInstanceState == null) {
            mapFragment = new MapFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.map_container, mapFragment);
            transaction.commit();
        } else {
            // Retrieve the fragment if it already exists
            mapFragment = (MapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map_container);
        }
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

    private void setupRecyclerView() {
        if (locationsRecyclerView != null) {
            locationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            locationsRecyclerView.setHasFixedSize(true);
            // TODO: Set adapter when location data is available
            // locationsRecyclerView.setAdapter(new LocationAdapter(locationsList));
        }
    }
}
