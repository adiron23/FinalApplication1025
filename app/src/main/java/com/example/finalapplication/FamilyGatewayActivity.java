package com.example.finalapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView; // ייבוא של CardView
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FamilyGatewayActivity extends AppCompatActivity {

    // שינוי סוג המשתנים מ-Button ל-CardView כדי שיתאימו ל-XML
    private CardView btnCreateFamily, btnJoinFamily;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_gateway);

        db = FirebaseFirestore.getInstance();

        // הגנה למקרה שהמשתמש לא מחובר
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // קישור הרכיבים מה-XML
        btnCreateFamily = findViewById(R.id.btnCreateFamily);
        btnJoinFamily = findViewById(R.id.btnJoinFamily);

        // לחיצה על כרטיס "צור משפחה"
        btnCreateFamily.setOnClickListener(v -> {
            Intent intent = new Intent(FamilyGatewayActivity.this, CreateFamilyActivity.class);
            startActivity(intent);
        });

        // לחיצה על כרטיס "הצטרף למשפחה"
        btnJoinFamily.setOnClickListener(v -> {
            showJoinDialog();
        });
    }

    private void showJoinDialog() {
        // יצירת תיבת טקסט בתוך דיאלוג
        final EditText input = new EditText(this);
        input.setHint("הכנס קוד (6 תווים)");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        // הוספת ריווח פנימי לתיבת הטקסט בתוך הדיאלוג
        input.setPadding(50, 40, 50, 40);

        new AlertDialog.Builder(this)
                .setTitle("הצטרפות למשפחה")
                .setMessage("הזן את הקוד שקיבלת מההורה:")
                .setView(input)
                .setPositiveButton("אישור", (dialog, which) -> {
                    String code = input.getText().toString().toUpperCase().trim();
                    if (!code.isEmpty()) {
                        verifyCode(code);
                    } else {
                        Toast.makeText(this, "נא להזין קוד", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void verifyCode(String code) {
        // חיפוש הקוד ב-Firestore
        db.collection("families").document(code).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // הקוד נמצא! עוברים לבחירת דמות
                Intent intent = new Intent(FamilyGatewayActivity.this, SelectRoleActivity.class);
                intent.putExtra("FAMILY_CODE", code);
                startActivity(intent);
                finish(); // סוגר את המסך הנוכחי
            } else {
                Toast.makeText(this, "קוד לא תקין, נסה שוב", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "שגיאה בחיבור: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}