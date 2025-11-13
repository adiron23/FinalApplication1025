package com.example.finalapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private TextView tVWelcome;
    private Button btnLogout;
    private FirebaseAuth refAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tVWelcome = findViewById(R.id.tVWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        refAuth = FirebaseAuth.getInstance();

        String name = getIntent().getStringExtra("name");
        if (name != null && !name.isEmpty()) {
            tVWelcome.setText("Welcome, " + name);
        } else {
            tVWelcome.setText("Welcome");
        }

        btnLogout.setOnClickListener(v -> {
            refAuth.signOut();
            startActivity(new Intent(MainActivity.this, LogInActivity.class));
            finish();
        });
    }
}
