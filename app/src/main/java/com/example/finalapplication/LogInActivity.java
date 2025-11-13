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
    private Button createUser;
    private CheckBox cBStayConnected;

    private FirebaseAuth refAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        tVMsg = findViewById(R.id.tVMsg);
        createUser = findViewById(R.id.createUser);
        cBStayConnected = findViewById(R.id.cBStayConnected);
        tVGoToRegister = findViewById(R.id.tVGoToRegister);

        refAuth = FirebaseAuth.getInstance();

        tVGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LogInActivity.this, RegisterActivity.class))
        );

        createUser.setOnClickListener(v -> loginUser());
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
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        FirebaseUser user = refAuth.getCurrentUser();
                        String uid = user.getUid();

                        // שליפת שם מה-Firestore
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    String name = documentSnapshot.getString("name");
                                    Intent si = new Intent(LogInActivity.this, MainActivity.class);
                                    si.putExtra("name", name);
                                    startActivity(si);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    // במקרה של שגיאה, נשלח את האימייל
                                    Intent si = new Intent(LogInActivity.this, MainActivity.class);
                                    si.putExtra("name", email);
                                    startActivity(si);
                                    finish();
                                });
                    } else {
                        tVMsg.setText("דוא״ל או סיסמה שגויים");
                    }
                });
    }
}
