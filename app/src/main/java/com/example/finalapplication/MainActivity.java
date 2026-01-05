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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        tVWelcome = findViewById(R.id.tVWelcome);

        setupDrawer();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadUserData(currentUser.getUid());
        }
    }

    private void loadUserData(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String role = document.getString("role");
                        String familyCode = document.getString("familyCode");

                        if (role == null || role.isEmpty()) {
                            tVWelcome.setText("ברוך הבא, " + name);
                        } else {
                            // אם יש לו תפקיד, ננסה להביא גם את שם המשפחה
                            fetchFamilyName(familyCode, name, role);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("MainActivity", "Error", e));
    }

    private void fetchFamilyName(String familyCode, String userName, String role) {
        if (familyCode == null || familyCode.isEmpty()) {
            tVWelcome.setText("שלום " + userName + "\nתפקיד: " + role);
            return;
        }

        db.collection("families").document(familyCode).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String familyName = doc.getString("familyName");
                        tVWelcome.setText("משפחת " + familyName + "\nשלום " + userName + " (" + role + ")");
                    } else {
                        tVWelcome.setText("שלום " + userName + " (" + role + ")");
                    }
                });
    }
}