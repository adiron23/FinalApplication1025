package com.example.finalapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class FamilyGatewayActivity extends AppCompatActivity {

    private CardView btnCreateFamily, btnJoinFamily;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_gateway);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnCreateFamily = findViewById(R.id.btnCreateFamily);
        btnJoinFamily = findViewById(R.id.btnJoinFamily);

        btnCreateFamily.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateFamilyActivity.class));
        });

        btnJoinFamily.setOnClickListener(v -> showJoinDialog());
    }

    private void showJoinDialog() {
        final EditText input = new EditText(this);
        input.setHint("הכנס קוד משפחתי");
        input.setPadding(50, 40, 50, 40);

        new AlertDialog.Builder(this)
                .setTitle("הצטרפות")
                .setView(input)
                .setPositiveButton("אישור", (dialog, which) -> {
                    String code = input.getText().toString().toUpperCase().trim();
                    if (!code.isEmpty()) verifyAndJoin(code);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void verifyAndJoin(String code) {
        db.collection("families").document(code).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> roles = (List<String>) documentSnapshot.get("availableRoles");

                db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
                    String myName = userDoc.getString("name");
                    String myFoundRole = "בן משפחה";

                    if (roles != null) {
                        for (String entry : roles) {
                            String[] parts = entry.split(":");
                            if (parts[0].equalsIgnoreCase(myName)) {
                                myFoundRole = parts[1];
                                break;
                            }
                        }
                    }

                    db.collection("users").document(uid).update(
                            "familyCode", code,
                            "role", myFoundRole
                    ).addOnSuccessListener(v -> {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    });
                });
            } else {
                Toast.makeText(this, "קוד לא נמצא", Toast.LENGTH_SHORT).show();
            }
        });
    }
}