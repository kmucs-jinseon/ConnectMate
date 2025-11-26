package com.example.connectmate;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (savedInstanceState == null) {
            String userId = getIntent().getStringExtra("USER_ID");
            boolean showButtons = getIntent().getBooleanExtra("SHOW_BUTTONS", false);

            ProfileFragment profileFragment = ProfileFragment.newInstance(userId, showButtons);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.profile_fragment_container, profileFragment);
            transaction.commit();
        }
    }
}
