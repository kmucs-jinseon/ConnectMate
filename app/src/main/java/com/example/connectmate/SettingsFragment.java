package com.example.connectmate;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the Account item
        LinearLayout itemAccount = view.findViewById(R.id.itemAccount);

        // Set click listener to navigate to ProfileFragment
        if (itemAccount != null) {
            itemAccount.setOnClickListener(v -> {
                // Create ProfileFragment instance
                ProfileFragment profileFragment = new ProfileFragment();

                // Navigate to ProfileFragment
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.main_container, profileFragment);
                transaction.addToBackStack(null);  // Add to back stack so user can go back
                transaction.commit();
            });
        }
    }
}
