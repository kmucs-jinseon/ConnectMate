package com.example.connectmate;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    private Fragment mapFragment, chatFragment, profileFragment, settingFragment, active;
    private static final String TAG_MAP="TAG_MAP", TAG_CHAT="TAG_CHAT", TAG_PROFILE="TAG_PROFILE", TAG_SETTING="TAG_SETTING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();

        // 모든 프래그먼트를 초기화합니다.
        mapFragment     = new MapFragment();
        chatFragment    = new ChatListFragment();
        profileFragment = new ProfileFragment();
        settingFragment = new SettingsFragment();

        // MapFragment를 기본 화면으로 설정합니다.
        fm.beginTransaction()
                .add(R.id.main_container, mapFragment, TAG_MAP) // mapFragment를 기본으로 보여줍니다.
                .add(R.id.main_container, chatFragment, TAG_CHAT).hide(chatFragment)
                .add(R.id.main_container, profileFragment, TAG_PROFILE).hide(profileFragment)
                .add(R.id.main_container, settingFragment, TAG_SETTING).hide(settingFragment)
                .commitNow();

        active = mapFragment; // 활성화된 프래그먼트를 mapFragment로 설정합니다.

        BottomNavigationView nav = findViewById(R.id.bottomNavigationView);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            Fragment target = null;
            if (id == R.id.nav_map) target = mapFragment;
            else if (id == R.id.nav_chat)  target = chatFragment;
            else if (id == R.id.nav_profile) target = profileFragment;
            else if (id == R.id.nav_settings) target = settingFragment;

            if (target != null && target != active) {
                fm.beginTransaction().hide(active).show(target).commit();
                active = target;
            }
            return true;
        });

        // 기본 선택 탭을 '지도'로 설정합니다.
        nav.setSelectedItemId(R.id.nav_map);

        // Floating Action Button 설정은 그대로 유지합니다.
        FloatingActionButton fab = findViewById(R.id.fabCreateActivity);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateActivityActivity.class);
            startActivity(intent);
        });
    }
}
