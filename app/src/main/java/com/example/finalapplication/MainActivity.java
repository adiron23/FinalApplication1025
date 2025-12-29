package com.example.finalapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends BaseActivity {

    private TextView tVWelcome;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // אתחול רכיבים
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        tVWelcome = findViewById(R.id.tVWelcome);

        // הפעלת התפריט מה-BaseActivity
        setupDrawer();

        // בדיקה אם יש משתמש מחובר
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();

            // משיכת נתוני המשתמש מ-Firestore לפי ה-UID שלו
            db.collection("users").document(uid).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                // שליפת השם מהשדה "name" ששמרנו ב-RegisterActivity
                                String name = document.getString("name");
                                tVWelcome.setText("Welcome, " + name);
                            } else {
                                tVWelcome.setText("Welcome");
                            }
                        } else {
                            Log.e("MainActivity", "Error getting user data", task.getException());
                            tVWelcome.setText("Welcome");
                        }
                    });
        } else {
            // אם במקרה הגענו לכאן בלי משתמש מחובר
            tVWelcome.setText("Welcome");
        }
    }
}