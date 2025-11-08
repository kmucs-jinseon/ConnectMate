package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.common.KakaoSdk;
import com.kakao.sdk.user.UserApiClient;
import com.navercorp.nid.NaverIdLoginSDK;
import com.navercorp.nid.oauth.OAuthLoginCallback;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private CheckBox autoLoginCheckbox;
    private Button loginButton;
    private TextView signUpTextView;
    private ImageButton googleSignInButton;
    private ImageButton kakaoSignInButton;
    private ImageButton naverSignInButton;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Realtime Database
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // Initialize HTTP client for Naver API calls
        httpClient = new OkHttpClient();

        // Note: Kakao and Naver SDKs are initialized in ConnectMateApplication.onCreate()
        // per official documentation requirements

        initViews();
        configureGoogleSignIn();
        configureSocialLoginButtons();
        setupClickListeners();
    }

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        autoLoginCheckbox = findViewById(R.id.autoLoginCheckbox);
        loginButton = findViewById(R.id.loginButton);
        signUpTextView = findViewById(R.id.signUpTextView);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        kakaoSignInButton = findViewById(R.id.kakaoSignInButton);
        naverSignInButton = findViewById(R.id.naverSignInButton);

        // Debug: Verify all buttons are found
        Log.d(TAG, "Google button found: " + (googleSignInButton != null));
        Log.d(TAG, "Kakao button found: " + (kakaoSignInButton != null));
        Log.d(TAG, "Naver button found: " + (naverSignInButton != null));
    }

    private void configureSocialLoginButtons() {
        // Debug: Log API key status
        Log.d(TAG, "Kakao key configured: " + isValidApiKey(BuildConfig.KAKAO_APP_KEY));
        Log.d(TAG, "Naver credentials configured: " +
              (isValidApiKey(BuildConfig.NAVER_CLIENT_ID) && isValidApiKey(BuildConfig.NAVER_CLIENT_SECRET)));

        // Disable Kakao button if not configured
        if (!isValidApiKey(BuildConfig.KAKAO_APP_KEY)) {
            kakaoSignInButton.setEnabled(false);
            kakaoSignInButton.setAlpha(0.5f);
            Log.w(TAG, "Kakao button disabled - API key not configured");
        } else {
            Log.d(TAG, "Kakao button enabled");
        }

        // Disable Naver button if not configured
        if (!isValidApiKey(BuildConfig.NAVER_CLIENT_ID) || !isValidApiKey(BuildConfig.NAVER_CLIENT_SECRET)) {
            naverSignInButton.setEnabled(false);
            naverSignInButton.setAlpha(0.5f);
            Log.w(TAG, "Naver button disabled - credentials not configured");
        } else {
            Log.d(TAG, "Naver button enabled");
        }
    }

    private void configureGoogleSignIn() {
        try {
            // Configure Google Sign-In
            GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail();

            // Get the web client ID from strings.xml
            String webClientId = getString(R.string.default_web_client_id);

            // Only request ID token if we have a valid web client ID
            if (webClientId != null && !webClientId.isEmpty() && !webClientId.equals("YOUR_WEB_CLIENT_ID_HERE")) {
                gsoBuilder.requestIdToken(webClientId);
            } else {
                Log.w(TAG, "Google Sign-In: Web Client ID not configured. Update google-services.json and enable Google Sign-In in Firebase Console.");
            }

            GoogleSignInOptions gso = gsoBuilder.build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

            // Initialize the ActivityResultLauncher
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
        loginButton.setOnClickListener(v -> performLogin());
        signUpTextView.setOnClickListener(v -> navigateToSignUp());

        // Hide sign up link when auto-login is checked (user already has account)
        autoLoginCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            signUpTextView.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        googleSignInButton.setOnClickListener(v -> {
            Log.d(TAG, "Google button clicked");
            signInWithGoogle();
        });

        kakaoSignInButton.setOnClickListener(v -> {
            Log.d(TAG, "Kakao button clicked");
            signInWithKakao();
        });

        naverSignInButton.setOnClickListener(v -> {
            Log.d(TAG, "Naver button clicked");
            signInWithNaver();
        });
    }

    private void performLogin() {
        String email = emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
        String password = passwordEditText.getText() != null ? passwordEditText.getText().toString().trim() : "";

        if (!validateInputs(email, password)) return;

        // Disable button to prevent multiple clicks
        loginButton.setEnabled(false);

        // Sign in with Firebase
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    loginButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // Load user data from Realtime Database and sync to SharedPreferences
                            String userId = user.getUid();
                            dbRef.child("users").child(userId)
                                    .get()
                                    .addOnSuccessListener(dataSnapshot -> {
                                        if (dataSnapshot.exists()) {
                                            // User exists in database, sync to SharedPreferences
                                            Log.d(TAG, "User data found in database, syncing to SharedPreferences");
                                            saveUserToSharedPreferences(dataSnapshot);

                                            // Update last login time
                                            dbRef.child("users").child(userId).child("lastLoginAt")
                                                    .setValue(System.currentTimeMillis());
                                        } else {
                                            // User doesn't exist in database, create new user profile
                                            Log.d(TAG, "User not found in database, creating new profile");
                                            String displayName = user.getDisplayName() != null ? user.getDisplayName() :
                                                    (user.getEmail() != null ? user.getEmail().split("@")[0] : "User");
                                            saveUserToFirestore(
                                                    userId,
                                                    user.getEmail() != null ? user.getEmail() : "",
                                                    displayName,
                                                    user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null,
                                                    "firebase"
                                            );
                                        }

                                        // Save login state
                                        saveLoginState("firebase", userId);

                                        // Save auto-login preference
                                        boolean autoLogin = autoLoginCheckbox != null && autoLoginCheckbox.isChecked();
                                        saveAutoLoginPreference(autoLogin);

                                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                        navigateToMain();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to load user data from database", e);
                                        // Even if database fails, still log in with basic info
                                        saveLoginState("firebase", userId);

                                        boolean autoLogin = autoLoginCheckbox != null && autoLoginCheckbox.isChecked();
                                        saveAutoLoginPreference(autoLogin);

                                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                        navigateToMain();
                                    });
                        } else {
                            // Shouldn't happen, but handle gracefully
                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
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

        return true;
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("just_logged_in", true);  // Flag to indicate fresh login
        startActivity(intent);
        finish();
    }

    private void navigateToSignUp() {
        Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
        startActivity(intent);
    }

    // Google Sign-In Methods
    private void signInWithGoogle() {
        // Disable button during sign-in
        googleSignInButton.setEnabled(false);

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        // Re-enable button after sign-in attempt
        googleSignInButton.setEnabled(true);

        Log.d(TAG, "handleGoogleSignInResult called");

        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null) {
                Log.w(TAG, "Google sign in account is null");
                Toast.makeText(this, "Google sign in failed: Account is null", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Google sign in account retrieved: " + account.getEmail());
            Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());

            String idToken = account.getIdToken();
            if (idToken != null && !idToken.isEmpty()) {
                Log.d(TAG, "ID token present, using Firebase auth");
                // Firebase authentication with ID token
                firebaseAuthWithGoogle(idToken);
            } else {
                // Google Sign-In succeeded but no ID token (web client ID not configured)
                Log.w(TAG, "Google Sign-In succeeded but ID token is null. Using fallback method.");

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

                // Save auto-login preference
                boolean autoLogin = autoLoginCheckbox != null && autoLoginCheckbox.isChecked();
                saveAutoLoginPreference(autoLogin);

                Toast.makeText(this, "Google sign in successful!\nEmail: " + account.getEmail(), Toast.LENGTH_SHORT).show();
                navigateToMain();
            }
        } catch (ApiException e) {
            Log.w(TAG, "Google sign in failed", e);
            String errorMessage = "Google sign in failed";
            if (e.getStatusCode() == 12501) {
                errorMessage = "Google sign in cancelled";
            } else if (e.getStatusCode() == 7) {
                errorMessage = "Network error. Please check your connection.";
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during Google sign in", e);
            Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null || idToken.isEmpty()) {
            Log.w(TAG, "Cannot authenticate with Firebase: ID token is null or empty");
            Toast.makeText(this, "Cannot complete Firebase authentication. Please configure Web Client ID.", Toast.LENGTH_LONG).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            Log.d(TAG, "Google Firebase user authenticated: " + user.getEmail());

                            // Save user to Firestore
                            saveUserToFirestore(
                                    user.getUid(),
                                    user.getEmail() != null ? user.getEmail() : "",
                                    user.getDisplayName() != null ? user.getDisplayName() : "Google User",
                                    user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null,
                                    "google"
                            );

                            // Save login state
                            saveLoginState("google", user.getUid());
                        }

                        // Save auto-login preference
                        boolean autoLogin = autoLoginCheckbox != null && autoLoginCheckbox.isChecked();
                        saveAutoLoginPreference(autoLogin);

                        Log.d(TAG, "About to navigate to MainActivity from Google sign-in");
                        Toast.makeText(LoginActivity.this, "Google sign in successful!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + errorMsg,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Kakao Sign-In Methods
    private void signInWithKakao() {
        // Check if Kakao SDK is initialized
        if (!isValidApiKey(BuildConfig.KAKAO_APP_KEY)) {
            Toast.makeText(this, "Kakao Sign-In is not configured. Add KAKAO_APP_KEY to local.properties", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "═══════════════════════════════════════════");
        Log.d(TAG, "Starting Kakao Login");
        Log.d(TAG, "Kakao App Key configured: " + BuildConfig.KAKAO_APP_KEY.substring(0, 8) + "...");
        Log.d(TAG, "═══════════════════════════════════════════");

        // Disable button during sign-in
        kakaoSignInButton.setEnabled(false);

        // Check if KakaoTalk app is installed
        try {
            boolean isTalkAvailable = UserApiClient.getInstance().isKakaoTalkLoginAvailable(this);
            Log.d(TAG, "KakaoTalk app installed: " + isTalkAvailable);

            if (isTalkAvailable) {
                Log.d(TAG, "Using KakaoTalk login");
                UserApiClient.getInstance().loginWithKakaoTalk(this, createKakaoLoginCallback());
            } else {
                Log.d(TAG, "Using Kakao Account login (web)");
                UserApiClient.getInstance().loginWithKakaoAccount(this, createKakaoLoginCallback());
            }
        } catch (Exception e) {
            Log.e(TAG, "═══════════════════════════════════════════");
            Log.e(TAG, "Kakao login exception");
            Log.e(TAG, "Exception type: " + e.getClass().getName());
            Log.e(TAG, "Exception message: " + e.getMessage());
            Log.e(TAG, "═══════════════════════════════════════════");
            e.printStackTrace();
            kakaoSignInButton.setEnabled(true);
            Toast.makeText(this, "Kakao login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Create Kakao login callback with fallback support
     */
    private Function2<OAuthToken, Throwable, Unit> createKakaoLoginCallback() {
        return (token, error) -> {
            // Re-enable button after attempt
            runOnUiThread(() -> kakaoSignInButton.setEnabled(true));

            if (error != null) {
                Log.e(TAG, "═══════════════════════════════════════════");
                Log.e(TAG, "Kakao login failed");
                Log.e(TAG, "Error type: " + error.getClass().getName());
                Log.e(TAG, "Error message: " + error.getMessage());
                Log.e(TAG, "═══════════════════════════════════════════");
                error.printStackTrace();

                String errorMsg = "Kakao login failed: " + error.getMessage();

                // Check if error is because KakaoTalk is not connected to account
                if (error.getMessage() != null && error.getMessage().contains("not connected to Kakao account")) {
                    Log.d(TAG, "KakaoTalk not connected, falling back to web login");
                    runOnUiThread(() -> {
                        // Disable button during fallback attempt
                        kakaoSignInButton.setEnabled(false);
                        Toast.makeText(LoginActivity.this, "KakaoTalk에 로그인되어 있지 않습니다. 웹 로그인으로 전환합니다.", Toast.LENGTH_SHORT).show();
                    });

                    // Fallback to web-based Kakao Account login
                    UserApiClient.getInstance().loginWithKakaoAccount(LoginActivity.this, createKakaoLoginCallback());
                    return Unit.INSTANCE;
                }

                // Provide helpful error messages for other errors
                if (error.getMessage() != null) {
                    if (error.getMessage().contains("KOE320")) {
                        errorMsg = "Kakao login cancelled by user";
                    } else if (error.getMessage().contains("platform")) {
                        errorMsg = "App not registered in Kakao Console. Check package name and key hash.";
                    } else if (error.getMessage().contains("consent")) {
                        errorMsg = "Required permissions not configured in Kakao Console";
                    }
                }

                String finalErrorMsg = errorMsg;
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, finalErrorMsg, Toast.LENGTH_LONG).show());
            } else if (token != null) {
                Log.d(TAG, "═══════════════════════════════════════════");
                Log.d(TAG, "Kakao login success!");
                Log.d(TAG, "Access Token: " + token.getAccessToken().substring(0, 20) + "...");
                Log.d(TAG, "═══════════════════════════════════════════");
                getUserInfoFromKakao();
            }
            return Unit.INSTANCE;
        };
    }

    private void getUserInfoFromKakao() {
        Log.d(TAG, "Fetching Kakao user info...");
        UserApiClient.getInstance().me((user, error) -> {
            if (error != null) {
                Log.e(TAG, "═══════════════════════════════════════════");
                Log.e(TAG, "Failed to get Kakao user info");
                Log.e(TAG, "Error: " + error.getMessage());
                Log.e(TAG, "═══════════════════════════════════════════");
                error.printStackTrace();
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Failed to get user info: " + error.getMessage(), Toast.LENGTH_LONG).show());
            } else if (user != null) {
                Log.d(TAG, "═══════════════════════════════════════════");
                Log.d(TAG, "Kakao user info retrieved successfully");
                Log.d(TAG, "User ID: " + user.getId());

                final String email;
                final String nickname;
                final String profileImageUrl;

                if (user.getKakaoAccount() != null) {
                    if (user.getKakaoAccount().getEmail() != null) {
                        email = user.getKakaoAccount().getEmail();
                        Log.d(TAG, "Email: " + email);
                    } else {
                        email = "";
                    }
                    if (user.getKakaoAccount().getProfile() != null) {
                        if (user.getKakaoAccount().getProfile().getNickname() != null) {
                            nickname = user.getKakaoAccount().getProfile().getNickname();
                            Log.d(TAG, "Nickname: " + nickname);
                        } else {
                            nickname = "Kakao User";
                        }
                        if (user.getKakaoAccount().getProfile().getProfileImageUrl() != null) {
                            profileImageUrl = user.getKakaoAccount().getProfile().getProfileImageUrl();
                            Log.d(TAG, "Profile Image: " + profileImageUrl);
                        } else {
                            profileImageUrl = null;
                        }
                    } else {
                        nickname = "Kakao User";
                        profileImageUrl = null;
                    }
                } else {
                    email = "";
                    nickname = "Kakao User";
                    profileImageUrl = null;
                }

                Log.d(TAG, "═══════════════════════════════════════════");

                runOnUiThread(() -> {
                    // Save user to Firestore
                    String userId = "kakao_" + user.getId();
                    saveUserToFirestore(userId, email, nickname, profileImageUrl, "kakao");

                    // Save login state with user ID
                    saveLoginState("kakao", userId);

                    // Save auto-login preference
                    boolean autoLogin = autoLoginCheckbox != null && autoLoginCheckbox.isChecked();
                    saveAutoLoginPreference(autoLogin);

                    Toast.makeText(LoginActivity.this, "Kakao login successful!\nWelcome " + nickname, Toast.LENGTH_SHORT).show();
                    navigateToMain();
                });
            }
            return Unit.INSTANCE;
        });
    }

    // Naver Sign-In Methods
    private void signInWithNaver() {
        // Check if Naver SDK is initialized
        if (!isValidApiKey(BuildConfig.NAVER_CLIENT_ID) || !isValidApiKey(BuildConfig.NAVER_CLIENT_SECRET)) {
            Toast.makeText(this, "Naver Sign-In is not configured. Add NAVER_CLIENT_ID and NAVER_CLIENT_SECRET to local.properties", Toast.LENGTH_LONG).show();
            return;
        }

        // Disable button during sign-in
        naverSignInButton.setEnabled(false);

        OAuthLoginCallback callback = new OAuthLoginCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Naver login success");
                getUserInfoFromNaver();
            }

            @Override
            public void onFailure(int httpStatus, @NonNull String message) {
                Log.e(TAG, "Naver login failed: " + message);
                naverSignInButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Naver login failed: " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int errorCode, @NonNull String message) {
                Log.e(TAG, "Naver login error: " + message);
                naverSignInButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Naver login error: " + message, Toast.LENGTH_SHORT).show();
            }
        };

        try {
            NaverIdLoginSDK.INSTANCE.authenticate(this, callback);
        } catch (Exception e) {
            Log.e(TAG, "Naver login exception", e);
            naverSignInButton.setEnabled(true);
            Toast.makeText(this, "Naver login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void getUserInfoFromNaver() {
        // After successful login, you can get the access token
        String accessToken = NaverIdLoginSDK.INSTANCE.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.w(TAG, "Failed to get Naver access token");
            naverSignInButton.setEnabled(true);
            Toast.makeText(LoginActivity.this, "Failed to get access token", Toast.LENGTH_SHORT).show();
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
                    naverSignInButton.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Failed to get user info", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Naver API error: " + response.code());
                    runOnUiThread(() -> {
                        naverSignInButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Failed to get user info", Toast.LENGTH_SHORT).show();
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
                        String nickname = userResponse.optString("nickname", "");
                        String profileImage = userResponse.optString("profile_image", null);

                        Log.d(TAG, "========== NAVER API RESPONSE ==========");
                        Log.d(TAG, "Naver user ID: " + userId);
                        Log.d(TAG, "Naver email: " + email);
                        Log.d(TAG, "Naver name: '" + name + "' (empty: " + name.isEmpty() + ")");
                        Log.d(TAG, "Naver nickname: '" + nickname + "' (empty: " + nickname.isEmpty() + ")");
                        Log.d(TAG, "Naver profile_image: " + profileImage);
                        Log.d(TAG, "========================================");

                        runOnUiThread(() -> {
                            // Re-enable button
                            naverSignInButton.setEnabled(true);

                            // Save user to Firestore
                            String firestoreUserId = "naver_" + userId;

                            // Determine display name: prefer name > nickname > default
                            String displayName;
                            if (name != null && !name.isEmpty()) {
                                displayName = name;
                            } else if (nickname != null && !nickname.isEmpty()) {
                                displayName = nickname;
                            } else {
                                displayName = "Naver User";
                            }

                            Log.d(TAG, "Final displayName for Naver: '" + displayName + "'");
                            saveUserToFirestore(firestoreUserId, email, displayName, profileImage, "naver");

                            // Save login state with user ID
                            saveLoginState("naver", firestoreUserId);

                            // Save auto-login preference
                            boolean autoLogin = autoLoginCheckbox != null && autoLoginCheckbox.isChecked();
                            saveAutoLoginPreference(autoLogin);

                            Toast.makeText(LoginActivity.this, "Naver login successful!\nWelcome " + displayName, Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        });
                    } else {
                        Log.e(TAG, "Naver API error: Invalid result code " + resultCode);
                        runOnUiThread(() -> {
                            naverSignInButton.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "Failed to get user info", Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse Naver user info", e);
                    runOnUiThread(() -> {
                        naverSignInButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Failed to parse user info", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * Helper method to validate API keys from BuildConfig.
     * Checks if the key is not null, not empty, and not a placeholder value.
     *
     * @param apiKey The API key to validate
     * @return true if the key is valid, false otherwise
     */
    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Save or update user information in Realtime Database
     * @param userId User ID (Firebase UID or social login ID)
     * @param email User email
     * @param displayName User display name
     * @param profileImageUrl Profile image URL (optional)
     * @param loginMethod Login method used (google, kakao, naver, firebase)
     */
    private void saveUserToFirestore(String userId, String email, String displayName,
                                     String profileImageUrl, String loginMethod) {
        Log.d(TAG, "Saving user to Realtime Database: " + userId);

        // Check if user already exists
        dbRef.child("users").child(userId)
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        // User exists, update user info in case it changed
                        Log.d(TAG, "User exists, updating user info");

                        // Update name, email, profile image and lastLoginAt
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("lastLoginAt", System.currentTimeMillis());

                        // Always update display name and email from login provider
                        if (displayName != null && !displayName.isEmpty()) {
                            updates.put("displayName", displayName);
                        }
                        if (email != null && !email.isEmpty()) {
                            updates.put("email", email);
                        }
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            updates.put("profileImageUrl", profileImageUrl);
                        }

                        dbRef.child("users").child(userId)
                                .updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User info updated - Name: " + displayName + ", Email: " + email);

                                    // Reload user data from database to get updated info
                                    dbRef.child("users").child(userId).get()
                                            .addOnSuccessListener(updatedSnapshot -> {
                                                saveUserToSharedPreferences(updatedSnapshot);
                                            });
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to update user", e));
                    } else {
                        // New user, create profile
                        Log.d(TAG, "New user, creating profile");
                        User user = new User(userId, email, displayName, loginMethod);
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            user.setProfileImageUrl(profileImageUrl);
                        }

                        dbRef.child("users").child(userId)
                                .setValue(user)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User created successfully");
                                    // Save to SharedPreferences
                                    SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString("user_id", userId);
                                    editor.putString("user_name", displayName);
                                    editor.putString("user_email", email);
                                    editor.putString("user_username", user.getUsername());
                                    editor.putString("user_bio", user.getBio());
                                    editor.putString("user_mbti", user.getMbti());
                                    if (profileImageUrl != null) {
                                        editor.putString("profile_image_url", profileImageUrl);
                                    }
                                    editor.apply();
                                    Log.d(TAG, "Saved new user to SharedPreferences - Name: " + displayName + ", Email: " + email);
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to create user", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to check user existence", e));
    }

    /**
     * Save user data from Realtime Database DataSnapshot to SharedPreferences
     */
    private void saveUserToSharedPreferences(DataSnapshot dataSnapshot) {
        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        User user = dataSnapshot.getValue(User.class);
        if (user != null) {
            // Save user ID for future reference
            if (user.getUserId() != null) editor.putString("user_id", user.getUserId());

            if (user.getDisplayName() != null) editor.putString("user_name", user.getDisplayName());
            if (user.getEmail() != null) editor.putString("user_email", user.getEmail());
            if (user.getUsername() != null) editor.putString("user_username", user.getUsername());
            if (user.getBio() != null) editor.putString("user_bio", user.getBio());
            if (user.getMbti() != null) editor.putString("user_mbti", user.getMbti());
            if (user.getProfileImageUrl() != null) editor.putString("profile_image_url", user.getProfileImageUrl());

            Log.d(TAG, "Saved to SharedPreferences - Name: " + user.getDisplayName() + ", Email: " + user.getEmail());
        }

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

    /**
     * Save auto-login preference to SharedPreferences
     * @param autoLogin Whether auto-login should be enabled
     */
    private void saveAutoLoginPreference(boolean autoLogin) {
        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("auto_login", autoLogin);
        editor.apply();
        Log.d(TAG, "Auto-login preference saved: " + autoLogin);
    }
}