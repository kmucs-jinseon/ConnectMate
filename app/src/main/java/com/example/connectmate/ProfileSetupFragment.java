package com.example.connectmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileSetupFragment extends Fragment {

    interface ProfileSetupHost {
        void onProfileSetupCompleted();
    }

    private static final String ARG_USER_ID = "arg_user_id";
    private static final String ARG_EMAIL = "arg_email";
    private static final String ARG_NAME = "arg_name";
    private static final String ARG_LOGIN_METHOD = "arg_login_method";

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private SharedPreferences prefs;
    private ProfileSetupHost host;

    private TextInputEditText nameInput;
    private TextInputEditText usernameInput;
    private TextInputEditText mbtiInput;
    private TextInputEditText bioInput;
    private MaterialButton completeButton;
    private ProgressBar progressBar;

    private String userId;
    private String email;
    private String initialName;
    private String loginMethod;

    public ProfileSetupFragment() {
        super(R.layout.fragment_profile_setup);
    }

    public static ProfileSetupFragment newInstance(String userId,
                                                   String email,
                                                   String name,
                                                   String loginMethod) {
        ProfileSetupFragment fragment = new ProfileSetupFragment();
        Bundle args = new Bundle();
        if (userId != null) args.putString(ARG_USER_ID, userId);
        if (email != null) args.putString(ARG_EMAIL, email);
        if (name != null) args.putString(ARG_NAME, name);
        if (loginMethod != null) args.putString(ARG_LOGIN_METHOD, loginMethod);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ProfileSetupHost) {
            host = (ProfileSetupHost) context;
        } else {
            throw new IllegalStateException("Parent activity must implement ProfileSetupHost");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        prefs = requireContext().getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);

        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
            email = getArguments().getString(ARG_EMAIL);
            initialName = getArguments().getString(ARG_NAME);
            loginMethod = getArguments().getString(ARG_LOGIN_METHOD);
        }

        if (userId == null && mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        if (email == null && mAuth.getCurrentUser() != null) {
            email = mAuth.getCurrentUser().getEmail();
        }

        initializeViews(view);
        setupAutoUsername();
        loadExistingData();
        setupListeners();
    }

    private void initializeViews(View view) {
        nameInput = view.findViewById(R.id.profile_setup_name_input);
        usernameInput = view.findViewById(R.id.profile_setup_username_input);
        mbtiInput = view.findViewById(R.id.profile_setup_mbti_input);
        bioInput = view.findViewById(R.id.profile_setup_bio_input);
        completeButton = view.findViewById(R.id.profile_setup_complete_button);
        progressBar = view.findViewById(R.id.profile_setup_progress);
    }

    private void setupAutoUsername() {
        if (nameInput == null || usernameInput == null) return;
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (usernameInput.isFocused()) return;
                String generated = generateUsername(s != null ? s.toString() : "");
                usernameInput.setText(generated);
                usernameInput.setSelection(generated.length());
            }
        });
    }

    private void setupListeners() {
        if (completeButton != null) {
            completeButton.setOnClickListener(v -> saveProfile());
        }
    }

    private void loadExistingData() {
        if (initialName != null && nameInput != null) {
            nameInput.setText(initialName);
        }

        if (email == null) {
            email = prefs.getString("user_email", null);
        }

        if (userId == null) {
            loadFromPreferences();
            return;
        }

        toggleLoading(true);
        databaseReference.child("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        toggleLoading(false);
                        if (!snapshot.exists()) {
                            loadFromPreferences();
                            return;
                        }

                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            if (nameInput != null && !TextUtils.isEmpty(user.displayName)) {
                                nameInput.setText(user.displayName);
                            }
                            if (usernameInput != null && !TextUtils.isEmpty(user.username)) {
                                usernameInput.setText(user.username);
                            }
                            if (mbtiInput != null && !TextUtils.isEmpty(user.mbti)) {
                                mbtiInput.setText(user.mbti);
                            }
                            if (bioInput != null && !TextUtils.isEmpty(user.bio)) {
                                bioInput.setText(user.bio);
                            }
                        } else {
                            loadFromPreferences();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        toggleLoading(false);
                        loadFromPreferences();
                    }
                });
    }

    private void loadFromPreferences() {
        if (prefs == null) return;
        if (nameInput != null) {
            nameInput.setText(prefs.getString("user_name", ""));
        }
        if (usernameInput != null) {
            String username = prefs.getString("user_username", "");
            if (TextUtils.isEmpty(username) && nameInput != null) {
                username = generateUsername(nameInput.getText() != null ? nameInput.getText().toString() : "");
            }
            usernameInput.setText(username);
        }
        if (mbtiInput != null) {
            mbtiInput.setText(prefs.getString("user_mbti", ""));
        }
        if (bioInput != null) {
            bioInput.setText(prefs.getString("user_bio", ""));
        }
    }

    private void saveProfile() {
        if (nameInput == null || usernameInput == null) return;

        String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
        String username = usernameInput.getText() != null ? usernameInput.getText().toString().trim() : "";
        String mbti = mbtiInput != null && mbtiInput.getText() != null ? mbtiInput.getText().toString().trim().toUpperCase(Locale.US) : "";
        String bio = bioInput != null && bioInput.getText() != null ? bioInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("이름을 입력하세요");
            return;
        }

        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("사용자 이름을 입력하세요");
            return;
        }

        username = sanitizeUsername(username);
        if (TextUtils.isEmpty(username)) {
            usernameInput.setError("사용자 이름에 허용되지 않는 문자가 포함되어 있습니다");
            return;
        }

        if (mbti.length() == 3) {
            mbti = mbti + "P";
        }

        if (userId == null) {
            if (mAuth.getCurrentUser() != null) {
                userId = mAuth.getCurrentUser().getUid();
            } else {
                // Unable to continue without a user id, send user to main screen.
                if (host != null) {
                    host.onProfileSetupCompleted();
                }
                return;
            }
        }

        toggleLoading(true);
        DatabaseReference userRef = databaseReference.child("users").child(userId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", name);
        updates.put("username", username);
        if (email != null) updates.put("email", email);
        if (!TextUtils.isEmpty(mbti)) updates.put("mbti", mbti);
        if (!TextUtils.isEmpty(bio)) updates.put("bio", bio);
        updates.put("loginMethod", loginMethod != null ? loginMethod : "email");
        updates.put("profileCompleted", true);
        updates.put("lastLoginAt", System.currentTimeMillis());

        final String finalName = name;
        final String finalUsername = username;
        final String finalMbti = mbti;
        final String finalBio = bio;

        userRef.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    toggleLoading(false);
                    persistToPreferences(finalName, finalUsername, finalMbti, finalBio);
                    if (host != null) {
                        host.onProfileSetupCompleted();
                    }
                })
                .addOnFailureListener(e -> {
                    toggleLoading(false);
                    Toast.makeText(requireContext(), "프로필 저장에 실패했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void persistToPreferences(String name, String username, String mbti, String bio) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_name", name);
        editor.putString("user_username", username);
        if (email != null) editor.putString("user_email", email);
        editor.putString("user_mbti", mbti);
        editor.putString("user_bio", bio);
        editor.putBoolean("profile_completed", true);
        editor.apply();
    }

    private void toggleLoading(boolean loading) {
        if (completeButton != null) {
            completeButton.setEnabled(!loading);
        }
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private String sanitizeUsername(String input) {
        String lower = input.toLowerCase(Locale.US).replace(" ", "");
        return lower.replaceAll("[^a-z0-9_]", "");
    }

    private String generateUsername(String name) {
        String base = sanitizeUsername(name != null ? name : "");
        if (TextUtils.isEmpty(base)) {
            base = "user" + (System.currentTimeMillis() % 10000);
        }
        return base;
    }
}
