package com.example.finalapplication;

import android.os.Bundle;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends BaseActivity { // או BaseActivity אם יש לך

    private TextView tVWelcome;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        tVWelcome = findViewById(R.id.tVWelcome);

        loadInfo();
    }

    private void loadInfo() {
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            String name = userDoc.getString("name");
            String role = userDoc.getString("role");
            String code = userDoc.getString("familyCode");

            if (code != null && !code.isEmpty()) {
                db.collection("families").document(code).get().addOnSuccessListener(famDoc -> {
                    String famName = famDoc.getString("familyName");
                    tVWelcome.setText("שלום " + name + "!\nאת/ה רשומ/ה כ" + role + "\nבמשפחת " + famName);
                });
            } else {
                tVWelcome.setText("ברוך הבא, " + name);
            }
        });
    }
}