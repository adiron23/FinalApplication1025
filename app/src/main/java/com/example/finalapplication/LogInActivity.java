package com.example.finalapplication;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LogInActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private EditText eTEmail, eTPass;
    private TextView tVMsg, tVGoToRegister, tvForgotPassword;
    private Button btnLogin;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        eTEmail          = findViewById(R.id.eTEmail);
        eTPass           = findViewById(R.id.eTPass);
        tVMsg            = findViewById(R.id.tVMsg);
        btnLogin         = findViewById(R.id.createUser);
        tVGoToRegister   = findViewById(R.id.tVGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogInActivity.this.loginUser();
            }
        });

        tVGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LogInActivity.this, RegisterActivity.class));
                finish();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogInActivity.this.showPasswordResetDialog();
            }
        });

        eTEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String email = s.toString().trim();
                if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    tVMsg.setText("כתובת אימייל לא תקינה");
                } else {
                    tVMsg.setText("");
                }
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Button btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_GOOGLE_SIGN_IN);
                }
            });
        }
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
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        pd.dismiss();
                        if (task.isSuccessful()) {
                            LogInActivity.this.navigateAfterLogin();
                        } else {
                            Exception e = task.getException();
                            tVMsg.setText("שגיאה: " + (e != null ? e.getMessage() : "לא ידוע"));
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                tVMsg.setText("Google Sign-In נכשל: " + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מתחבר...");
        pd.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        pd.dismiss();
                        if (task.isSuccessful()) {
                            LogInActivity.this.navigateAfterLogin();
                        } else {
                            Exception e = task.getException();
                            tVMsg.setText("שגיאה: " + (e != null ? e.getMessage() : "לא ידוע"));
                        }
                    }
                });
    }

    private void navigateAfterLogin() {
        String pendingCode = getSharedPreferences("family_prefs", MODE_PRIVATE)
                .getString("pending_family_code", null);
        Class<?> destination = (pendingCode != null) ? FamilyGatewayActivity.class : MainActivity.class;
        Intent intent = new Intent(this, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showPasswordResetDialog() {
        String currentEmail = eTEmail.getText().toString().trim();

        EditText etResetEmail = new EditText(this);
        etResetEmail.setHint("כתובת האימייל שלך");
        etResetEmail.setText(currentEmail);
        etResetEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etResetEmail.setPadding(48, 32, 48, 32);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("איפוס סיסמה")
                .setMessage("הזן את כתובת האימייל שלך ונשלח לך קישור לאיפוס.")
                .setView(etResetEmail)
                .setPositiveButton("שלח", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String email = etResetEmail.getText().toString().trim();
                        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            Toast.makeText(LogInActivity.this, "נא להזין אימייל תקין", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void v) {
                                        new androidx.appcompat.app.AlertDialog.Builder(LogInActivity.this)
                                                .setTitle("נשלח!")
                                                .setMessage("קישור לאיפוס סיסמה נשלח ל:\n" + email)
                                                .setPositiveButton("אישור", null)
                                                .show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        tVMsg.setText("שגיאה: " + e.getMessage());
                                    }
                                });
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}