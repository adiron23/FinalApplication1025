package com.example.finalapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

// מסך פתיחה (Splash) – מוצג בעת הפעלת האפליקציה ומנתב למסך המתאים אחרי 2.5 שניות
public class SplashActivity extends AppCompatActivity {

    // מאתחל את מסך הפתיחה וממתין 2.5 שניות לפני מעבר למסך הראשי או מסך ההתחברות
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean loggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
                Class<?> dest = loggedIn ? MainActivity.class : LogInActivity.class;
                SplashActivity.this.startActivity(new Intent(SplashActivity.this, dest));
                SplashActivity.this.finish();
            }
        }, 2500);
    }
}