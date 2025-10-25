package com.example.connectmate;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // XML 화면 연결 (res/layout/activity_sign_up.xml)
        setContentView(R.layout.activity_signup);

        // TextView 연결
        TextView loginTextView = findViewById(R.id.loginTextView);

        // "로그인"만 클릭 가능하게 만들기
        String fullText = "이미 회원이신가요? 로그인";
        SpannableString spannable = new SpannableString(fullText);

        int startIndex = fullText.indexOf("로그인");
        int endIndex = startIndex + "로그인".length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // 로그인 화면으로 이동
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false); // 밑줄 제거
                ds.setColor(ContextCompat.getColor(SignUpActivity.this, R.color.black)); // 파란색
            }
        };

        spannable.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        loginTextView.setText(spannable);
        loginTextView.setMovementMethod(LinkMovementMethod.getInstance());
        loginTextView.setHighlightColor(Color.TRANSPARENT);
    }
}
