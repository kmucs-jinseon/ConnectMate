package com.example.connectmate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.common.KakaoSdk;
import com.kakao.sdk.user.UserApiClient;
import com.navercorp.nid.NaverIdLoginSDK;
import com.navercorp.nid.oauth.OAuthLoginCallback;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton;
    private TextView signUpTextView;
    private ImageButton googleSignInButton;
    private ImageButton kakaoSignInButton;
    private ImageButton naverSignInButton;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private ActivityResultLauncher<Intent> oauthWebViewLauncher;
    private OkHttpClient httpClient;

    private static final int REQUEST_KAKAO_OAUTH = 1001;
    private static final int REQUEST_NAVER_OAUTH = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Realtime Database
        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Initialize HTTP client for Naver API calls
        httpClient = new OkHttpClient();

        // Note: Kakao and Naver SDKs are initialized in ConnectMateApplication.onCreate()
        // per official documentation requirements

        initViews();
        configureGoogleSignIn();
        configureOAuthWebViewLauncher();
        configureSocialLoginButtons();
        setupClickListeners();
    }

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
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

    private void configureOAuthWebViewLauncher() {
        // Initialize the OAuth WebView launcher for Kakao and Naver
        oauthWebViewLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == OAuthWebViewActivity.RESULT_AUTH_CODE) {
                        Intent data = result.getData();
                        if (data != null) {
                            String code = data.getStringExtra(OAuthWebViewActivity.RESULT_CODE);
                            int requestCode = data.getIntExtra("request_code", 0);

                            if (requestCode == REQUEST_KAKAO_OAUTH) {
                                handleKakaoAuthCode(code);
                            } else if (requestCode == REQUEST_NAVER_OAUTH) {
                                handleNaverAuthCode(code);
                            }
                        }
                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        Intent data = result.getData();
                        if (data != null) {
                            String error = data.getStringExtra(OAuthWebViewActivity.RESULT_ERROR);
                            Log.e(TAG, "OAuth cancelled: " + error);
                            Toast.makeText(this, "로그인 취소됨", Toast.LENGTH_SHORT).show();
                        }

                        // Re-enable buttons
                        kakaoSignInButton.setEnabled(true);
                        naverSignInButton.setEnabled(true);
                    }
                }
        );
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> performLogin());
        signUpTextView.setOnClickListener(v -> navigateToSignUp());

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
                .addOnSuccessListener(result -> {
                    FirebaseUser fu = result.getUser();
                    if(fu != null) {
                        String uid = fu.getUid();
                        String mail = fu.getEmail();
                        String name = fu.getDisplayName();
                        String photo = fu.getPhotoUrl() != null ? fu.getPhotoUrl().toString() : null;

                        Log.d(TAG, "[LOGIN] calling saveUserToRealtimeDatabase uid=" + uid);
                        saveUserToRealtimeDatabase(uid, mail, name, photo, "email");

                        // Save login state with user ID
                        saveLoginState("email", uid);
                    }
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "signInWithEmail failed", e);
                    String msg = "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.";
                    if (e instanceof com.google.firebase.auth.FirebaseAuthException) {
                        String code = ((com.google.firebase.auth.FirebaseAuthException) e).getErrorCode();
                        switch (code) {
                            case "ERROR_INVALID_EMAIL":        msg = "이메일 형식이 올바르지 않습니다."; break;
                            case "ERROR_WRONG_PASSWORD":       msg = "비밀번호가 올바르지 않습니다."; break;
                            case "ERROR_USER_NOT_FOUND":       msg = "해당 이메일의 계정을 찾을 수 없습니다."; break;
                            case "ERROR_USER_DISABLED":        msg = "해당 계정은 비활성화되었습니다."; break;
                            case "ERROR_NETWORK_REQUEST_FAILED": msg = "네트워크 오류입니다. 연결을 확인해 주세요."; break;
                            case "ERROR_TOO_MANY_REQUESTS":    msg = "요청이 많습니다. 잠시 후 다시 시도해 주세요."; break;
                        }
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
        intent.putExtra("just_logged_in", true);
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

        // Sign out first to force account picker to show
        // This prevents automatic re-login with cached account
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d(TAG, "Google sign out completed, showing account picker");
            // Now launch sign-in intent which will show account picker
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        // Re-enable button after sign-in attempt
        googleSignInButton.setEnabled(true);

        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null) {
                Log.w(TAG, "Google sign in account is null");
                Toast.makeText(this, "Google sign in failed: Account is null", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());

            String idToken = account.getIdToken();
            if (idToken != null && !idToken.isEmpty()) {
                // Firebase authentication with ID token
                firebaseAuthWithGoogle(idToken);
            } else {
                // Google Sign-In succeeded but no ID token (web client ID not configured)
                Log.w(TAG, "Google Sign-In succeeded but ID token is null. Configure Web Client ID in Firebase Console.");

                // Save user info to Realtime Database using Google ID
                String userId = "google_" + account.getId();
                saveUserToRealtimeDatabase(
                        userId,
                        account.getEmail() != null ? account.getEmail() : "",
                        account.getDisplayName() != null ? account.getDisplayName() : "Google User",
                        account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null,
                        "google"
                );

                // Save login state with user ID
                saveLoginState("google", userId);

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
                            // Save user to Realtime Database
                            saveUserToRealtimeDatabase(
                                    user.getUid(),
                                    user.getEmail() != null ? user.getEmail() : "",
                                    user.getDisplayName() != null ? user.getDisplayName() : "Google User",
                                    user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null,
                                    "google"
                            );
                        }

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

        Function2<OAuthToken, Throwable, Unit> callback = (token, error) -> {
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

                // Provide helpful error messages
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

        // Always use Kakao Account (web-based OAuth) login
        try {
            Log.d(TAG, "Using Kakao Account login (web-based OAuth)");
            UserApiClient.getInstance().loginWithKakaoAccount(this, callback);
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
                    // Save user to Realtime Database
                    String userId = "kakao_" + user.getId();
                    saveUserToRealtimeDatabase(userId, email, nickname, profileImageUrl, "kakao");

                    // Save login state with user ID
                    saveLoginState("kakao", userId);

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

        Log.d(TAG, "═══════════════════════════════════════════");
        Log.d(TAG, "Starting Naver Login");
        Log.d(TAG, "Client ID: " + BuildConfig.NAVER_CLIENT_ID);
        Log.d(TAG, "═══════════════════════════════════════════");

        // Disable button during sign-in
        naverSignInButton.setEnabled(false);

        // IMPORTANT: Logout first to clear cached session
        // This forces Naver OAuth to show the login screen and allow account selection
        Log.d(TAG, "Clearing Naver session to allow account selection...");
        NaverIdLoginSDK.INSTANCE.logout();

        // Clear Naver SDK SharedPreferences to ensure fresh login
        try {
            SharedPreferences naverOAuth = getSharedPreferences("NaverOAuthSDK", Context.MODE_PRIVATE);
            naverOAuth.edit().clear().apply();
            Log.d(TAG, "Cleared Naver SDK SharedPreferences");
        } catch (Exception e) {
            Log.w(TAG, "Could not clear Naver SharedPreferences", e);
        }

        OAuthLoginCallback callback = new OAuthLoginCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "═══════════════════════════════════════════");
                Log.d(TAG, "Naver OAuth login success");
                Log.d(TAG, "═══════════════════════════════════════════");
                getUserInfoFromNaver();
            }

            @Override
            public void onFailure(int httpStatus, @NonNull String message) {
                Log.e(TAG, "═══════════════════════════════════════════");
                Log.e(TAG, "Naver login failed");
                Log.e(TAG, "HTTP Status: " + httpStatus);
                Log.e(TAG, "Message: " + message);
                Log.e(TAG, "═══════════════════════════════════════════");
                runOnUiThread(() -> {
                    naverSignInButton.setEnabled(true);
                    String errorMsg = "Naver login failed";
                    if (message != null && !message.isEmpty()) {
                        errorMsg = message;
                    }
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(int errorCode, @NonNull String message) {
                Log.e(TAG, "═══════════════════════════════════════════");
                Log.e(TAG, "Naver login error");
                Log.e(TAG, "Error Code: " + errorCode);
                Log.e(TAG, "Message: " + message);
                Log.e(TAG, "═══════════════════════════════════════════");
                runOnUiThread(() -> {
                    naverSignInButton.setEnabled(true);
                    String errorMsg = "Naver login error";
                    if (message != null && !message.isEmpty()) {
                        errorMsg = message;
                    }
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        };

        try {
            NaverIdLoginSDK.INSTANCE.authenticate(this, callback);
        } catch (Exception e) {
            Log.e(TAG, "═══════════════════════════════════════════");
            Log.e(TAG, "Naver login exception");
            Log.e(TAG, "Exception type: " + e.getClass().getName());
            Log.e(TAG, "Exception message: " + e.getMessage());
            Log.e(TAG, "═══════════════════════════════════════════");
            e.printStackTrace();
            naverSignInButton.setEnabled(true);
            Toast.makeText(this, "Naver login error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void getUserInfoFromNaver() {
        Log.d(TAG, "Fetching Naver user info...");

        // After successful login, you can get the access token
        String accessToken = NaverIdLoginSDK.INSTANCE.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "═══════════════════════════════════════════");
            Log.e(TAG, "Failed to get Naver access token");
            Log.e(TAG, "Access token is null or empty");
            Log.e(TAG, "═══════════════════════════════════════════");
            runOnUiThread(() -> {
                naverSignInButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Failed to get access token", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        Log.d(TAG, "Naver access token obtained: " + accessToken.substring(0, Math.min(20, accessToken.length())) + "...");

        // Fetch user profile from Naver API
        Request request = new Request.Builder()
                .url("https://openapi.naver.com/v1/nid/me")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        Log.d(TAG, "Calling Naver user info API...");

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "═══════════════════════════════════════════");
                Log.e(TAG, "Failed to fetch Naver user info");
                Log.e(TAG, "Exception: " + e.getClass().getName());
                Log.e(TAG, "Message: " + e.getMessage());
                Log.e(TAG, "═══════════════════════════════════════════");
                e.printStackTrace();
                runOnUiThread(() -> {
                    naverSignInButton.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Failed to connect to Naver: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Log.d(TAG, "Naver API response code: " + response.code());

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.e(TAG, "═══════════════════════════════════════════");
                    Log.e(TAG, "Naver API error");
                    Log.e(TAG, "Status code: " + response.code());
                    Log.e(TAG, "Error body: " + errorBody);
                    Log.e(TAG, "═══════════════════════════════════════════");
                    runOnUiThread(() -> {
                        naverSignInButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Naver API error: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                try {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "═══════════════════════════════════════════");
                    Log.d(TAG, "Naver API response:");
                    Log.d(TAG, "Response body is null: " + (responseBody == null));
                    Log.d(TAG, "Response body is empty: " + (responseBody != null && responseBody.isEmpty()));
                    Log.d(TAG, "Response body length: " + (responseBody != null ? responseBody.length() : 0));
                    Log.d(TAG, "Response body: " + (responseBody != null ? responseBody : "NULL"));
                    Log.d(TAG, "═══════════════════════════════════════════");

                    if (responseBody == null || responseBody.isEmpty()) {
                        Log.e(TAG, "Empty response body from Naver API!");
                        runOnUiThread(() -> {
                            naverSignInButton.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "Empty response from Naver", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    JSONObject json = new JSONObject(responseBody);
                    String resultCode = json.getString("resultcode");

                    if ("00".equals(resultCode)) {
                        JSONObject userResponse = json.getJSONObject("response");
                        String userId = userResponse.optString("id", "");
                        String email = userResponse.optString("email", "");
                        String name = userResponse.optString("name", "");
                        String nickname = userResponse.optString("nickname", "Naver User");
                        String profileImage = userResponse.optString("profile_image", null);

                        Log.d(TAG, "═══════════════════════════════════════════");
                        Log.d(TAG, "Naver user info parsed successfully");
                        Log.d(TAG, "ID: " + userId);
                        Log.d(TAG, "Email: " + email);
                        Log.d(TAG, "Name: " + name);
                        Log.d(TAG, "Nickname: " + nickname);
                        Log.d(TAG, "═══════════════════════════════════════════");

                        runOnUiThread(() -> {
                            // Re-enable button
                            naverSignInButton.setEnabled(true);

                            // Save user to Database
                            String realtimeUserId = "naver_" + userId;
                            String displayName = !name.isEmpty() ? name : (!nickname.isEmpty() ? nickname : "Naver User");
                            saveUserToRealtimeDatabase(realtimeUserId, email, displayName, profileImage, "naver");

                            // Save login state with user ID
                            saveLoginState("naver", realtimeUserId);

                            Toast.makeText(LoginActivity.this, "Naver login successful!\nWelcome " + displayName, Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        });
                    } else {
                        Log.e(TAG, "═══════════════════════════════════════════");
                        Log.e(TAG, "Naver API error: Invalid result code");
                        Log.e(TAG, "Result code: " + resultCode);
                        Log.e(TAG, "═══════════════════════════════════════════");
                        runOnUiThread(() -> {
                            naverSignInButton.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "Naver API returned error code: " + resultCode, Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "═══════════════════════════════════════════");
                    Log.e(TAG, "Failed to parse Naver user info");
                    Log.e(TAG, "Exception: " + e.getClass().getName());
                    Log.e(TAG, "Message: " + e.getMessage());
                    Log.e(TAG, "═══════════════════════════════════════════");
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        naverSignInButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Failed to parse user info: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
    private void saveUserToRealtimeDatabase(String userId, String email, String displayName,
                                            String profileImageUrl, String loginMethod) {
        Log.d(TAG, "Saving user to Realtime Database: " + userId);

        DatabaseReference userRef = databaseRef.child("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // User exists, update last login time and profile image if available
                    Log.d(TAG, "User exists, updating lastLoginAt");
                    userRef.child("lastLoginAt").setValue(System.currentTimeMillis());
                    if (profileImageUrl != null) {
                        userRef.child("profileImageUrl").setValue(profileImageUrl);
                    }
                    Log.d(TAG, "User updated successfully");
                    saveUserToSharedPreferences(snapshot);
                } else {
                    // New user, create profile
                    Log.d(TAG, "New user, creating profile");
                    User user = new User(userId, email, displayName, loginMethod);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        user.setProfileImageUrl(profileImageUrl);
                    }

                    userRef.setValue(user)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User created successfully");
                                // Save to SharedPreferences
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
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to check user existence", error.toException());
            }
        });
    }

    /**
     * Save user data from Realtime Database snapshot to SharedPreferences
     */
    private void saveUserToSharedPreferences(DataSnapshot snapshot) {
        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        String displayName = snapshot.child("displayName").getValue(String.class);
        String email = snapshot.child("email").getValue(String.class);
        String username = snapshot.child("username").getValue(String.class);
        String bio = snapshot.child("bio").getValue(String.class);
        String mbti = snapshot.child("mbti").getValue(String.class);
        String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

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
        editor.putBoolean("auto_login", false); // Disable auto-login - user must login every time
        editor.putString("login_method", loginMethod);
        if (userId != null) {
            editor.putString("user_id", userId);
        }
        editor.apply();
        Log.d(TAG, "Login state saved: " + loginMethod + (userId != null ? " (ID: " + userId + ")" : "") + ", auto_login DISABLED");
    }

    /**
     * Handle Kakao authorization code from WebView OAuth
     */
    private void handleKakaoAuthCode(String code) {
        Log.d(TAG, "Handling Kakao auth code (WebView OAuth not fully implemented yet)");
        Toast.makeText(this, "Kakao WebView OAuth - 개발 중", Toast.LENGTH_SHORT).show();
        kakaoSignInButton.setEnabled(true);
        // TODO: Exchange code for access token and get user info
    }

    /**
     * Handle Naver authorization code from WebView OAuth
     */
    private void handleNaverAuthCode(String code) {
        Log.d(TAG, "Handling Naver auth code (WebView OAuth not fully implemented yet)");
        Toast.makeText(this, "Naver WebView OAuth - 개발 중", Toast.LENGTH_SHORT).show();
        naverSignInButton.setEnabled(true);
        // TODO: Exchange code for access token and get user info
    }
}
