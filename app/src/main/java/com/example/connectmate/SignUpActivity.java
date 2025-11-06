package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.user.UserApiClient;
import com.navercorp.nid.NaverIdLoginSDK;
import com.navercorp.nid.oauth.OAuthLoginCallback;

import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private com.google.android.material.button.MaterialButton signUpButton;
    private TextView loginTextView;
    private ImageButton googleSignUpButton;
    private ImageButton kakaoSignUpButton;
    private ImageButton naverSignUpButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize HTTP client for Naver API calls
        httpClient = new OkHttpClient();

        initViews();
        configureGoogleSignIn();
        configureSocialLoginButtons();
        setupClickListeners();
    }

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        signUpButton = findViewById(R.id.signUpButton);
        loginTextView = findViewById(R.id.loginTextView);
        googleSignUpButton = findViewById(R.id.googleSignUpButton);
        kakaoSignUpButton = findViewById(R.id.kakaoSignUpButton);
        naverSignUpButton = findViewById(R.id.naverSignUpButton);

        // Debug: Verify all buttons are found
        Log.d(TAG, "Google button found: " + (googleSignUpButton != null));
        Log.d(TAG, "Kakao button found: " + (kakaoSignUpButton != null));
        Log.d(TAG, "Naver button found: " + (naverSignUpButton != null));
    }

    private void configureSocialLoginButtons() {
        // Debug: Log API key status
        Log.d(TAG, "Kakao key configured: " + isValidApiKey(BuildConfig.KAKAO_APP_KEY));
        Log.d(TAG, "Naver credentials configured: " +
              (isValidApiKey(BuildConfig.NAVER_CLIENT_ID) && isValidApiKey(BuildConfig.NAVER_CLIENT_SECRET)));

        // Disable Kakao button if not configured
        if (!isValidApiKey(BuildConfig.KAKAO_APP_KEY)) {
            kakaoSignUpButton.setEnabled(false);
            kakaoSignUpButton.setAlpha(0.5f);
            Log.w(TAG, "Kakao button disabled - API key not configured");
        } else {
            Log.d(TAG, "Kakao button enabled");
        }

        // Disable Naver button if not configured
        if (!isValidApiKey(BuildConfig.NAVER_CLIENT_ID) || !isValidApiKey(BuildConfig.NAVER_CLIENT_SECRET)) {
            naverSignUpButton.setEnabled(false);
            naverSignUpButton.setAlpha(0.5f);
            Log.w(TAG, "Naver button disabled - credentials not configured");
        } else {
            Log.d(TAG, "Naver button enabled");
        }
    }

    private void configureGoogleSignIn() {
        try {
            GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail();

            String webClientId = getString(R.string.default_web_client_id);

            if (!webClientId.isEmpty() && !webClientId.equals("YOUR_WEB_CLIENT_ID_HERE")) {
                gsoBuilder.requestIdToken(webClientId);
            } else {
                Log.w(TAG, "Google Sign-In: Web Client ID not configured");
            }

            GoogleSignInOptions gso = gsoBuilder.build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

            googleSignInLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                            handleGoogleSignInResult(task);
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error configuring Google Sign-In", e);
        }
    }

    private void setupClickListeners() {
        signUpButton.setOnClickListener(v -> performSignUp());
        loginTextView.setOnClickListener(v -> navigateToLogin());

        googleSignUpButton.setOnClickListener(v -> {
            Log.d(TAG, "Google button clicked");
            signUpWithGoogle();
        });

        kakaoSignUpButton.setOnClickListener(v -> {
            Log.d(TAG, "Kakao button clicked");
            signUpWithKakao();
        });

        naverSignUpButton.setOnClickListener(v -> {
            Log.d(TAG, "Naver button clicked");
            signUpWithNaver();
        });
    }

    private void performSignUp() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString().trim() : "";
        String confirmPassword = confirmPasswordEditText.getText() != null ? confirmPasswordEditText.getText().toString().trim() : "";

        if (!validateInputs(email, password, confirmPassword)) return;

        // Disable button to prevent multiple clicks
        signUpButton.setEnabled(false);

        // Create user with Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    signUpButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        Toast.makeText(SignUpActivity.this, "Sign up successful!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignUpActivity.this, "Sign up failed: " + Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Please enter your email");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Please enter your password");
            return false;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordEditText.setError("Please confirm your password");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    private void navigateToMain() {
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // Google Sign-Up Methods (uses same flow as login)
    private void signUpWithGoogle() {
        googleSignUpButton.setEnabled(false);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        googleSignUpButton.setEnabled(true);

        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null) {
                Log.w(TAG, "Google sign in account is null");
                Toast.makeText(this, "Google sign up failed: Account is null", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());

            String idToken = account.getIdToken();
            if (idToken != null && !idToken.isEmpty()) {
                firebaseAuthWithGoogle(idToken);
            } else {
                Log.w(TAG, "Google Sign-In succeeded but ID token is null");

                // Save user info to Firestore using Google ID
                String userId = "google_" + account.getId();
                saveUserToFirestore(
                        userId,
                        account.getEmail() != null ? account.getEmail() : "",
                        account.getDisplayName() != null ? account.getDisplayName() : "Google User",
                        account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null,
                        "google"
                );

                // Save login state with user ID
                saveLoginState("google", userId);

                Toast.makeText(this, "Google sign up successful!\nEmail: " + account.getEmail(), Toast.LENGTH_SHORT).show();
                navigateToMain();
            }
        } catch (ApiException e) {
            Log.w(TAG, "Google sign in failed", e);
            String errorMessage = "Google sign up failed";
            if (e.getStatusCode() == 12501) {
                errorMessage = "Google sign up cancelled";
            } else if (e.getStatusCode() == 7) {
                errorMessage = "Network error. Please check your connection.";
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during Google sign up", e);
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            Log.w(TAG, "Cannot authenticate with Firebase: ID token is null or empty");
            Toast.makeText(this, "Cannot complete Firebase authentication", Toast.LENGTH_LONG).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            saveUserToFirestore(
                                    user.getUid(),
                                    user.getEmail() != null ? user.getEmail() : "",
                                    user.getDisplayName() != null ? user.getDisplayName() : "Google User",
                                    user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null,
                                    "google"
                            );
                        }

                        Toast.makeText(SignUpActivity.this, "Google sign up successful!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(SignUpActivity.this, "Authentication failed: " + errorMsg,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Kakao Sign-Up Methods (uses same SDK as login)
    private void signUpWithKakao() {
        if (!isValidApiKey(BuildConfig.KAKAO_APP_KEY)) {
            Toast.makeText(this, "Kakao Sign-Up is not configured", Toast.LENGTH_LONG).show();
            return;
        }

        kakaoSignUpButton.setEnabled(false);

        Function2<OAuthToken, Throwable, Unit> callback = (token, error) -> {
            runOnUiThread(() -> kakaoSignUpButton.setEnabled(true));

            if (error != null) {
                Log.e(TAG, "Kakao signup failed", error);
                runOnUiThread(() -> Toast.makeText(SignUpActivity.this, "Kakao signup failed: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            } else if (token != null) {
                Log.d(TAG, "Kakao signup success: " + token.getAccessToken());
                getUserInfoFromKakao();
            }
            return Unit.INSTANCE;
        };

        try {
            if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(this)) {
                UserApiClient.getInstance().loginWithKakaoTalk(this, callback);
            } else {
                UserApiClient.getInstance().loginWithKakaoAccount(this, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Kakao signup exception", e);
            kakaoSignUpButton.setEnabled(true);
            Toast.makeText(this, "Kakao signup error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void getUserInfoFromKakao() {
        UserApiClient.getInstance().me((user, error) -> {
            if (error != null) {
                Log.e(TAG, "Failed to get Kakao user info", error);
                Toast.makeText(SignUpActivity.this, "Failed to get user info", Toast.LENGTH_SHORT).show();
            } else if (user != null) {
                Log.d(TAG, "Kakao user info: " + user.getId());

                final String email;
                final String nickname;
                final String profileImageUrl;

                if (user.getKakaoAccount() != null) {
                    email = user.getKakaoAccount().getEmail() != null ? user.getKakaoAccount().getEmail() : "";
                    if (user.getKakaoAccount().getProfile() != null) {
                        nickname = user.getKakaoAccount().getProfile().getNickname() != null ?
                                user.getKakaoAccount().getProfile().getNickname() : "Kakao User";
                        profileImageUrl = user.getKakaoAccount().getProfile().getProfileImageUrl();
                    } else {
                        nickname = "Kakao User";
                        profileImageUrl = null;
                    }
                } else {
                    email = "";
                    nickname = "Kakao User";
                    profileImageUrl = null;
                }

                runOnUiThread(() -> {
                    // Save user to Firestore
                    String userId = "kakao_" + user.getId();
                    saveUserToFirestore(userId, email, nickname, profileImageUrl, "kakao");

                    // Save login state with user ID
                    saveLoginState("kakao", userId);

                    Toast.makeText(SignUpActivity.this, "Kakao signup successful!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });
            }
            return Unit.INSTANCE;
        });
    }

    // Naver Sign-Up Methods (uses same SDK as login)
    private void signUpWithNaver() {
        if (!isValidApiKey(BuildConfig.NAVER_CLIENT_ID) || !isValidApiKey(BuildConfig.NAVER_CLIENT_SECRET)) {
            Toast.makeText(this, "Naver Sign-Up is not configured", Toast.LENGTH_LONG).show();
            return;
        }

        naverSignUpButton.setEnabled(false);

        OAuthLoginCallback callback = new OAuthLoginCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Naver signup success");
                getUserInfoFromNaver();
            }

            @Override
            public void onFailure(int httpStatus, @NonNull String message) {
                Log.e(TAG, "Naver signup failed: " + message);
                naverSignUpButton.setEnabled(true);
                Toast.makeText(SignUpActivity.this, "Naver signup failed: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int errorCode, @NonNull String message) {
                Log.e(TAG, "Naver signup error: " + message);
                naverSignUpButton.setEnabled(true);
                Toast.makeText(SignUpActivity.this, "Naver signup error: " + message, Toast.LENGTH_SHORT).show();
            }
        };

        try {
            NaverIdLoginSDK.INSTANCE.authenticate(this, callback);
        } catch (Exception e) {
            Log.e(TAG, "Naver signup exception", e);
            naverSignUpButton.setEnabled(true);
            Toast.makeText(this, "Naver signup error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void getUserInfoFromNaver() {
        String accessToken = NaverIdLoginSDK.INSTANCE.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.w(TAG, "Failed to get Naver access token");
            naverSignUpButton.setEnabled(true);
            Toast.makeText(SignUpActivity.this, "Failed to get access token", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Naver access token: " + accessToken.substring(0, 20) + "...");

        // Fetch user profile from Naver API
        Request request = new Request.Builder()
                .url("https://openapi.naver.com/v1/nid/me")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to fetch Naver user info", e);
                runOnUiThread(() -> {
                    naverSignUpButton.setEnabled(true);
                    Toast.makeText(SignUpActivity.this, "Failed to get user info", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Naver API error: " + response.code());
                    runOnUiThread(() -> {
                        naverSignUpButton.setEnabled(true);
                        Toast.makeText(SignUpActivity.this, "Failed to get user info", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Naver API response: " + responseBody);

                    JSONObject json = new JSONObject(responseBody);
                    String resultCode = json.getString("resultcode");

                    if ("00".equals(resultCode)) {
                        JSONObject userResponse = json.getJSONObject("response");
                        String userId = userResponse.optString("id", "");
                        String email = userResponse.optString("email", "");
                        String name = userResponse.optString("name", "");
                        String nickname = userResponse.optString("nickname", "Naver User");
                        String profileImage = userResponse.optString("profile_image", null);

                        Log.d(TAG, "Naver user info - ID: " + userId + ", Email: " + email + ", Name: " + name);

                        runOnUiThread(() -> {
                            // Re-enable button
                            naverSignUpButton.setEnabled(true);

                            // Save user to Firestore
                            String firestoreUserId = "naver_" + userId;
                            String displayName = !name.isEmpty() ? name : (!nickname.isEmpty() ? nickname : "Naver User");
                            saveUserToFirestore(firestoreUserId, email, displayName, profileImage, "naver");

                            // Save login state with user ID
                            saveLoginState("naver", firestoreUserId);

                            Toast.makeText(SignUpActivity.this, "Naver signup successful!\nWelcome " + displayName, Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        });
                    } else {
                        Log.e(TAG, "Naver API error: Invalid result code " + resultCode);
                        runOnUiThread(() -> {
                            naverSignUpButton.setEnabled(true);
                            Toast.makeText(SignUpActivity.this, "Failed to get user info", Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse Naver user info", e);
                    runOnUiThread(() -> {
                        naverSignUpButton.setEnabled(true);
                        Toast.makeText(SignUpActivity.this, "Failed to parse user info", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Save or update user information in Firestore
     */
    private void saveUserToFirestore(String userId, String email, String displayName,
                                     String profileImageUrl, String loginMethod) {
        Log.d(TAG, "Saving user to Firestore: " + userId);

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "User exists, updating lastLoginAt");
                        db.collection("users").document(userId)
                                .update("lastLoginAt", System.currentTimeMillis(),
                                        "profileImageUrl", profileImageUrl != null ? profileImageUrl : documentSnapshot.getString("profileImageUrl"))
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User updated successfully");
                                    saveUserToSharedPreferences(documentSnapshot);
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update user", e));
                    } else {
                        Log.d(TAG, "New user, creating profile");
                        User user = new User(userId, email, displayName, loginMethod);
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            user.setProfileImageUrl(profileImageUrl);
                        }

                        db.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User created successfully");
                                    SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString("user_name", displayName);
                                    editor.putString("user_email", email);
                                    editor.putString("user_username", user.getUsername());
                                    editor.putString("user_bio", user.getBio());
                                    editor.putString("user_mbti", user.getMbti());
                                    if (profileImageUrl != null) {
                                        editor.putString("profile_image_url", profileImageUrl);
                                    }
                                    editor.apply();
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to create user", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to check user existence", e));
    }

    private void saveUserToSharedPreferences(com.google.firebase.firestore.DocumentSnapshot doc) {
        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String displayName = doc.getString("displayName");
        String email = doc.getString("email");
        String username = doc.getString("username");
        String bio = doc.getString("bio");
        String mbti = doc.getString("mbti");
        String profileImageUrl = doc.getString("profileImageUrl");

        if (displayName != null) editor.putString("user_name", displayName);
        if (email != null) editor.putString("user_email", email);
        if (username != null) editor.putString("user_username", username);
        if (bio != null) editor.putString("user_bio", bio);
        if (mbti != null) editor.putString("user_mbti", mbti);
        if (profileImageUrl != null) editor.putString("profile_image_url", profileImageUrl);

        editor.apply();
    }

    /**
     * Save login state to SharedPreferences
     * @param loginMethod The login method used (google, kakao, naver, firebase)
     */
    private void saveLoginState(String loginMethod) {
        saveLoginState(loginMethod, null);
    }

    /**
     * Save login state to SharedPreferences with user ID
     * @param loginMethod The login method used (google, kakao, naver, firebase)
     * @param userId The user ID to save (optional)
     */
    private void saveLoginState(String loginMethod, String userId) {
        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_logged_in", true);
        editor.putString("login_method", loginMethod);
        if (userId != null) {
            editor.putString("user_id", userId);
        }
        editor.apply();
        Log.d(TAG, "Login state saved: " + loginMethod + (userId != null ? " (ID: " + userId + ")" : ""));
    }
}
