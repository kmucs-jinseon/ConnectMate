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
import com.google.firebase.auth.GoogleAuthProvider;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.user.UserApiClient;
import com.navercorp.nid.NaverIdLoginSDK;
import com.navercorp.nid.oauth.OAuthLoginCallback;

import java.util.Objects;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;

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
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

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

                // Save login state
                saveLoginState("google");

                Toast.makeText(this, "Google sign up successful!\\nEmail: " + account.getEmail(), Toast.LENGTH_SHORT).show();
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

                // Save login state
                saveLoginState("kakao");

                Toast.makeText(SignUpActivity.this, "Kakao signup successful!", Toast.LENGTH_SHORT).show();
                navigateToMain();
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
        naverSignUpButton.setEnabled(true);

        if (accessToken != null && !accessToken.isEmpty()) {
            Log.d(TAG, "Naver access token: " + accessToken);

            // Save login state
            saveLoginState("naver");

            Toast.makeText(SignUpActivity.this, "Naver signup successful!", Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {
            Log.w(TAG, "Failed to get Naver access token");
            Toast.makeText(SignUpActivity.this, "Failed to get access token", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidApiKey(String apiKey) {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Save login state to SharedPreferences
     * @param loginMethod The login method used (google, kakao, naver, firebase)
     */
    private void saveLoginState(String loginMethod) {
        SharedPreferences prefs = getSharedPreferences("ConnectMate", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_logged_in", true);
        editor.putString("login_method", loginMethod);
        editor.apply();
        Log.d(TAG, "Login state saved: " + loginMethod);
    }
}
