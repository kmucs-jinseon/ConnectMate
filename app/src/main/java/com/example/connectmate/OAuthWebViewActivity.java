package com.example.connectmate;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * WebView-based OAuth login activity
 * Shows OAuth login page in a WebView instead of external browser
 */
public class OAuthWebViewActivity extends AppCompatActivity {
    private static final String TAG = "OAuthWebView";

    public static final String EXTRA_AUTH_URL = "auth_url";
    public static final String EXTRA_REDIRECT_URI = "redirect_uri";
    public static final String EXTRA_PROVIDER = "provider";

    public static final int RESULT_AUTH_CODE = 100;
    public static final String RESULT_CODE = "code";
    public static final String RESULT_ERROR = "error";

    private WebView webView;
    private ProgressBar progressBar;
    private String redirectUri;
    private String provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oauth_webview);

        // Get intent extras
        String authUrl = getIntent().getStringExtra(EXTRA_AUTH_URL);
        redirectUri = getIntent().getStringExtra(EXTRA_REDIRECT_URI);
        provider = getIntent().getStringExtra(EXTRA_PROVIDER);

        if (authUrl == null || redirectUri == null) {
            Log.e(TAG, "Missing required parameters");
            Toast.makeText(this, "OAuth 설정 오류", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Starting OAuth for " + provider);
        Log.d(TAG, "Auth URL: " + authUrl);
        Log.d(TAG, "Redirect URI: " + redirectUri);

        // Initialize views
        webView = findViewById(R.id.oauth_webview);
        progressBar = findViewById(R.id.oauth_progress);

        // Configure WebView
        setupWebView();

        // Load OAuth URL
        webView.loadUrl(authUrl);
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString(
            webView.getSettings().getUserAgentString() + " ConnectMate/1.0"
        );

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);

                Log.d(TAG, "Page started: " + url);

                // Check if this is the redirect URL
                if (url.startsWith(redirectUri)) {
                    Log.d(TAG, "Redirect URL detected: " + url);
                    handleRedirect(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "Page finished: " + url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "Should override URL: " + url);

                // Check if this is the redirect URL
                if (url.startsWith(redirectUri)) {
                    handleRedirect(url);
                    return true;
                }

                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Log.e(TAG, "WebView error: " + error.getDescription());

                if (request.isForMainFrame()) {
                    Toast.makeText(OAuthWebViewActivity.this,
                        "로딩 오류: " + error.getDescription(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void handleRedirect(String url) {
        Log.d(TAG, "Handling redirect: " + url);

        try {
            Uri uri = Uri.parse(url);

            // Extract authorization code
            String code = uri.getQueryParameter("code");
            String error = uri.getQueryParameter("error");

            if (code != null) {
                Log.d(TAG, "Authorization code received: " + code.substring(0, Math.min(10, code.length())) + "...");

                // Return success
                Intent resultIntent = new Intent();
                resultIntent.putExtra(RESULT_CODE, code);
                setResult(RESULT_AUTH_CODE, resultIntent);
                finish();

            } else if (error != null) {
                Log.e(TAG, "OAuth error: " + error);
                String errorDescription = uri.getQueryParameter("error_description");

                // Return error
                Intent resultIntent = new Intent();
                resultIntent.putExtra(RESULT_ERROR, errorDescription != null ? errorDescription : error);
                setResult(Activity.RESULT_CANCELED, resultIntent);
                finish();

            } else {
                Log.w(TAG, "No code or error in redirect URL");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing redirect URL", e);
            Toast.makeText(this, "OAuth 처리 오류", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
