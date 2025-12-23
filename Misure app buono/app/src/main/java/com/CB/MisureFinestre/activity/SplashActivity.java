package com.CB.MisureFinestre.activity;

import com.bugfender.sdk.Bugfender;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.CB.MisureFinestre.R;
import com.CB.MisureFinestre.utils.PreferenceManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bugfender.d("LIFECYCLE", "SplashActivity - onCreate() - App starting");
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            PreferenceManager pref = new PreferenceManager(SplashActivity.this);

            if (pref.isFirstLogin()) {
                // First time app open → Go to Login
                Bugfender.d("LIFECYCLE", "First time app open - going to LoginActivity");
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            } else {
                // Not first time → check login state
                if (pref.isLoggedIn()) {
                    // User already logged in → Go Home
                    Bugfender.d("LIFECYCLE", "User already logged in - going to HomeActivity");
                    startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                } else {
                    // User not logged in → Go Login
                    Bugfender.d("LIFECYCLE", "User not logged in - going to LoginActivity");
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
            }

            finish();
        }, 2000); // Splash delay 2 seconds
    }
}