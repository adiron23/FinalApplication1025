package com.example.finalapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

public class LogInActivity extends AppCompatActivity {

    private EditText eTEmail, eTPass;
    private TextView tVMsg, tVGoToRegister;
    private Button btnLogin; // שיניתי את השם ל-btnLogin שיהיה ברור
    private FirebaseAuth refAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // אתחול רכיבים
        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        tVMsg = findViewById(R.id.tVMsg);
        btnLogin = findViewById(R.id.createUser); // ה-ID מה-XML שלך
        tVGoToRegister = findViewById(R.id.tVGoToRegister);

        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tVGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LogInActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String email = eTEmail.getText().toString().trim();
        String pass = eTPass.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            tVMsg.setText("אנא מלא את כל השדות");
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מתחבר...");
        pd.show();

        refAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkUserFamilyStatus(pd);
                    } else {
                        pd.dismiss();
                        tVMsg.setText("שגיאה: " + task.getException().getMessage());
                    }
                });
    }

    private void checkUserFamilyStatus(ProgressDialog pd) {
        String uid = refAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            pd.dismiss();
            if (documentSnapshot.exists()) {
                String familyCode = documentSnapshot.getString("familyCode");

                Intent intent;
                // אם כבר יש למשתמש קוד משפחתי, הוא לא צריך לעבור ב-Gateway
                if (familyCode != null && !familyCode.isEmpty()) {
                    intent = new Intent(LogInActivity.this, MainActivity.class);
                } else {
                    intent = new Intent(LogInActivity.this, FamilyGatewayActivity.class);
                }

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }).addOnFailureListener(e -> {
            pd.dismiss();
            tVMsg.setText("שגיאה במשיכת נתונים");
        });
    }

    // הוספת בדיקה: אם המשתמש כבר מחובר כשהוא פותח את האפליקציה
    @Override
    protected void onStart() {
        super.onStart();
        if (refAuth.getCurrentUser() != null) {
            // אפשר להוסיף כאן את checkUserFamilyStatus אם רוצים דילוג אוטומטי
        }
    }
}