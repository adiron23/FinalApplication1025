package com.example.finalapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends BaseActivity { // יורש מ-BaseActivity

    private TextView tVWelcome;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // מפעיל את התפריט שהגדרנו ב-BaseActivity
        setupDrawer();

        tVWelcome = findViewById(R.id.tVWelcome);
        btnLogout = findViewById(R.id.btnLogout);

        // הצגת שם המשתמש
        String name = getIntent().getStringExtra("name");
        tVWelcome.setText((name != null && !name.isEmpty()) ? "Welcome, " + name : "Welcome");

        // כפתור הלוגאאוט משתמש בפונקציה שנמצאת ב-BaseActivity
        btnLogout.setOnClickListener(v -> logoutUser());
    }
}