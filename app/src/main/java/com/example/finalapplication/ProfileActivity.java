package com.example.finalapplication;

import android.os.Bundle;
import android.widget.Button;

public class ProfileActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("הפרופיל שלי");
        }

        Button btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logoutUser());
        }

        // מסמן את הפרופיל בתפריט
        markSelectedMenuItem(R.id.nav_profile);
    }
}