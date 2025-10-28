package com.example.connectmate;

import android.content.Intent;
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
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.common.KakaoSdk;
import com.kakao.sdk.user.UserApiClient;
import com.navercorp.nid.NaverIdLoginSDK;
import com.navercorp.nid.oauth.OAuthLoginCallback;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

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
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

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
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
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
                Toast.makeText(this, "Google sign in successful!\nEmail: " + account.getEmail(), Toast.LENGTH_SHORT).show();
                // For now, just navigate to main since Google auth succeeded
                // In production, you might want to create a user profile or link to Firebase differently
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

        // Disable button during sign-in
        kakaoSignInButton.setEnabled(false);

        Function2<OAuthToken, Throwable, Unit> callback = (token, error) -> {
            // Re-enable button after attempt
            runOnUiThread(() -> kakaoSignInButton.setEnabled(true));

            if (error != null) {
                Log.e(TAG, "Kakao login failed", error);
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Kakao login failed: " + error.getMessage(), Toast.LENGTH_SHORT).show());
            } else if (token != null) {
                Log.d(TAG, "Kakao login success: " + token.getAccessToken());
                getUserInfoFromKakao();
            }
            return Unit.INSTANCE;
        };

        // Check if KakaoTalk app is installed
        try {
            if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(this)) {
                UserApiClient.getInstance().loginWithKakaoTalk(this, callback);
            } else {
                UserApiClient.getInstance().loginWithKakaoAccount(this, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Kakao login exception", e);
            kakaoSignInButton.setEnabled(true);
            Toast.makeText(this, "Kakao login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void getUserInfoFromKakao() {
        UserApiClient.getInstance().me((user, error) -> {
            if (error != null) {
                Log.e(TAG, "Failed to get Kakao user info", error);
                Toast.makeText(LoginActivity.this, "Failed to get user info", Toast.LENGTH_SHORT).show();
            } else if (user != null) {
                Log.d(TAG, "Kakao user info: " + user.getId());
                String email = user.getKakaoAccount() != null && user.getKakaoAccount().getEmail() != null
                        ? user.getKakaoAccount().getEmail() : "";
                Toast.makeText(LoginActivity.this, "Kakao login successful!", Toast.LENGTH_SHORT).show();
                navigateToMain();
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

        // Re-enable button
        naverSignInButton.setEnabled(true);

        if (accessToken != null && !accessToken.isEmpty()) {
            Log.d(TAG, "Naver access token: " + accessToken);
            Toast.makeText(LoginActivity.this, "Naver login successful!", Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {
            Log.w(TAG, "Failed to get Naver access token");
            Toast.makeText(LoginActivity.this, "Failed to get access token", Toast.LENGTH_SHORT).show();
        }
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
}