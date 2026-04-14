package com.example.finalapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LogInActivity extends AppCompatActivity {

    private EditText eTEmail, eTPass;
    private TextView tVMsg, tVGoToRegister, tvForgotPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        eTEmail         = findViewById(R.id.eTEmail);
        eTPass          = findViewById(R.id.eTPass);
        tVMsg           = findViewById(R.id.tVMsg);
        btnLogin        = findViewById(R.id.createUser);
        tVGoToRegister  = findViewById(R.id.tVGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> loginUser());

        tVGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> sendPasswordReset());

        // Real-time email format validation
        eTEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String email = s.toString().trim();
                if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    tVMsg.setText("כתובת אימייל לא תקינה");
                } else {
                    tVMsg.setText("");
                }
            }
        });
    }

    private void loginUser() {
        String email = eTEmail.getText().toString().trim();
        String pass  = eTPass.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            tVMsg.setText("נא למלא את כל השדות");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tVMsg.setText("כתובת אימייל לא תקינה");
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מתחבר...");
        pd.show();

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    pd.dismiss();
                    if (task.isSuccessful()) {
                        String pendingCode = getSharedPreferences("family_prefs", MODE_PRIVATE)
                                .getString("pending_family_code", null);
                        Class<?> destination = (pendingCode != null) ? FamilyGatewayActivity.class : MainActivity.class;
                        Intent intent = new Intent(this, destination);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Exception e = task.getException();
                        tVMsg.setText("שגיאה: " + (e != null ? e.getMessage() : "לא ידוע"));
                    }
                });
    }

    private void sendPasswordReset() {
        String email = eTEmail.getText().toString().trim();
        if (email.isEmpty()) {
            tVMsg.setText("הכנס אימייל ואז לחץ שכחתי סיסמה");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tVMsg.setText("כתובת אימייל לא תקינה");
            return;
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "קישור לאיפוס סיסמה נשלח לאימייל", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        tVMsg.setText("שגיאה: " + e.getMessage()));
    }
}
