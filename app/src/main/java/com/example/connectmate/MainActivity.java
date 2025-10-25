package com.example.connectmate;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private Fragment mapFragment, chatFragment, profileFragment, settingFragment, active;
    private static final String TAG_MAP="TAG_MAP", TAG_CHAT="TAG_CHAT", TAG_PROFILE="TAG_PROFILE", TAG_SETTING="TAG_SETTING";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();

        mapFragment     = new MapFragment();
        chatFragment    = new ChatListFragment();
        profileFragment = new ProfileFragment();
        settingFragment = new SettingsFragment();

        fm.beginTransaction()
                .add(R.id.main_container, mapFragment, TAG_MAP)
                .add(R.id.main_container, chatFragment, TAG_CHAT).hide(chatFragment)
                .add(R.id.main_container, profileFragment, TAG_PROFILE).hide(profileFragment)
                .add(R.id.main_container, settingFragment, TAG_SETTING).hide(settingFragment)
                .commitNow();
        active = mapFragment;

        BottomNavigationView nav = findViewById(R.id.bottomNavigationView);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment target = null;
            if (id == R.id.nav_map)       target = mapFragment;
            else if (id == R.id.nav_chat) target = chatFragment;
            else if (id == R.id.nav_profile) target = profileFragment;
            else if (id == R.id.nav_settings) target = settingFragment;

            if (target != null && target != active) {
                fm.beginTransaction().hide(active).show(target).commit();
                active = target;
            }
            return true;
        });

        nav.setSelectedItemId(R.id.nav_map);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_main.xml), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}