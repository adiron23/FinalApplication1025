package com.example.finalapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    TextView tVWelcome;
    Button btnLogout;
    FirebaseAuth refAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        tVWelcome = findViewById(R.id.tVWelcome);
        btnLogout = findViewById(R.id.btnLogout);
        refAuth = FirebaseAuth.getInstance();

        // הצגת Welcome עם האימייל
        String email = getIntent().getStringExtra("email");
        tVWelcome.setText("Welcome, " + email);

        // לחצן Logout
        btnLogout.setOnClickListener(v -> {
            refAuth.signOut();
            startActivity(new Intent(MainActivity.this, LogInActivity.class));
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
