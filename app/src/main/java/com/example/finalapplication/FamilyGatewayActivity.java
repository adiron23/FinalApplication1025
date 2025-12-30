package com.example.finalapplication;

import android.content.DialogInterface;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FamilyGatewayActivity extends AppCompatActivity {

    private CardView cardCreateFamily, cardJoinFamily;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_gateway);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        cardCreateFamily = findViewById(R.id.cardCreateFamily);
        cardJoinFamily = findViewById(R.id.cardJoinFamily);

        cardCreateFamily.setOnClickListener(v -> createNewFamily());
        cardJoinFamily.setOnClickListener(v -> showJoinDialog());
    }

    private void createNewFamily() {
        // יצירת קוד ייחודי קצר (6 תווים ראשונים של UUID)
        String familyCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Map<String, Object> familyData = new HashMap<>();
        familyData.put("adminUid", uid);
        familyData.put("familyCode", familyCode);
        familyData.put("members", new ArrayList<String>() {{ add(uid); }});

        db.collection("families").document(familyCode)
                .set(familyData)
                .addOnSuccessListener(aVoid -> {
                    // עדכון המשתמש שיש לו משפחה
                    db.collection("users").document(uid).update("familyCode", familyCode);

                    // הצגת הקוד להורה
                    new AlertDialog.Builder(this)
                            .setTitle("משפחה נוצרה!")
                            .setMessage("הקוד המשפחתי שלך הוא: " + familyCode + "\nשלח אותו לבני המשפחה כדי שיצטרפו.")
                            .setPositiveButton("מעולה", (d, w) -> {
                                startActivity(new Intent(FamilyGatewayActivity.this, MainActivity.class));
                                finish();
                            }).show();
                });
    }

    private void showJoinDialog() {
        EditText input = new EditText(this);
        input.setHint("הכנס קוד משפחה");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("הצטרפות למשפחה")
                .setView(input)
                .setPositiveButton("הצטרף", (dialog, which) -> {
                    String code = input.getText().toString().toUpperCase().trim();
                    joinFamily(code);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void joinFamily(String code) {
        db.collection("families").document(code).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // הוספת המשתמש לרשימת החברים במשפחה
                ArrayList<String> members = (ArrayList<String>) documentSnapshot.get("members");
                if (!members.contains(uid)) {
                    members.add(uid);
                    db.collection("families").document(code).update("members", members);
                    db.collection("users").document(uid).update("familyCode", code);

                    Toast.makeText(this, "הצטרפת למשפחה בהצלחה!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(FamilyGatewayActivity.this, MainActivity.class));
                    finish();
                }
            } else {
                Toast.makeText(this, "קוד לא תקין", Toast.LENGTH_SHORT).show();
            }
        });
    }
}