package com.example.connectmate;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

public class MapActivity extends AppCompatActivity {

    private FloatingActionButton fabCreateActivity;
    private ImageButton searchButton;
    private TextInputLayout searchInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        fabCreateActivity = findViewById(R.id.fab_create_activity);
        searchButton = findViewById(R.id.search_button);
        searchInputLayout = findViewById(R.id.search_input_layout);

        setupFabClickListener();
        setupSearchButtonClickListener();

        CardView bottomSheet = findViewById(R.id.bottom_sheet);
        BottomSheetBehavior<CardView> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheetBehavior.setPeekHeight(150); // Bottom Sheet가 접혔을 때 보일 높이
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); // 초기 상태
    }

    private void setupFabClickListener() {
        fabCreateActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Create Activity screen
                Intent intent = new Intent(MapActivity.this, CreateActivityActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupSearchButtonClickListener() {
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchInputLayout.getVisibility() == View.VISIBLE) {
                    searchInputLayout.setVisibility(View.GONE);
                } else {
                    searchInputLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
