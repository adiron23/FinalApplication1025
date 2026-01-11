package com.example.finalapplication;

import android.os.Bundle;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends BaseActivity {

    private TextView tVWelcome;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // חשוב: קודם setContentView כדי שה-BaseActivity יבנה את השלד
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            tVWelcome = findViewById(R.id.tVWelcome);
            loadInfo();
        }

        // מעדכן את התפריט למטה שהעמוד הנוכחי הוא "בית"
        markSelectedMenuItem(R.id.nav_main);
    }

    private void loadInfo() {
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                String name = userDoc.getString("name");
                String role = userDoc.getString("role");
                String code = userDoc.getString("familyCode");

                if (code != null && !code.isEmpty()) {
                    db.collection("families").document(code).get().addOnSuccessListener(famDoc -> {
                        String famName = famDoc.getString("familyName");
                        if (tVWelcome != null) {
                            tVWelcome.setText("משפחת " + famName + "\nשלום " + name + " (" + role + ")");
                        }
                    });
                } else if (tVWelcome != null) {
                    tVWelcome.setText("ברוך הבא, " + name);
                }
            }
        });
    }
}