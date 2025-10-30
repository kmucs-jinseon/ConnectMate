package com.example.connectmate;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    // MapFragment 관련 코드를 모두 제거했습니다.
    private Fragment chatFragment, profileFragment, settingFragment, active;
    private static final String TAG_CHAT="TAG_CHAT", TAG_PROFILE="TAG_PROFILE", TAG_SETTING="TAG_SETTING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();

        // MapFragment를 제외한 나머지 프래그먼트를 초기화합니다.
        chatFragment    = new ChatListFragment();
        profileFragment = new ProfileFragment();
        settingFragment = new SettingsFragment();

        // ChatFragment를 기본 화면으로 설정합니다.
        fm.beginTransaction()
                .add(R.id.main_container, chatFragment, TAG_CHAT) // chatFragment를 기본으로 보여줍니다.
                .add(R.id.main_container, profileFragment, TAG_PROFILE).hide(profileFragment)
                .add(R.id.main_container, settingFragment, TAG_SETTING).hide(settingFragment)
                .commitNow();

        active = chatFragment; // 활성화된 프래그먼트를 chatFragment로 설정합니다.

        BottomNavigationView nav = findViewById(R.id.bottomNavigationView);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // '지도' 탭을 누르면 MapActivity를 실행하도록 변경합니다.
            if (id == R.id.nav_map) {
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
                // false를 반환하여, 탭이 선택된 것처럼 보이지 않게 합니다.
                // 액티비티가 실행되고, 사용자가 돌아왔을 때 이전 탭이 그대로 유지됩니다.
                return false;
            }

            Fragment target = null;
            if (id == R.id.nav_chat)  target = chatFragment;
            else if (id == R.id.nav_profile) target = profileFragment;
            else if (id == R.id.nav_settings) target = settingFragment;

            if (target != null && target != active) {
                fm.beginTransaction().hide(active).show(target).commit();
                active = target;
            }
            return true;
        });

        // 기본 선택 탭을 '채팅'으로 변경합니다.
        nav.setSelectedItemId(R.id.nav_chat);

        // Floating Action Button 설정은 그대로 유지합니다.
        FloatingActionButton fab = findViewById(R.id.fabCreateActivity);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreateActivityActivity.class);
            startActivity(intent);
        });
    }
}
