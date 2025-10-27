package com.example.finalapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LogInActivity extends AppCompatActivity {

    EditText eTEmail, eTPass;
    TextView tVMsg, tVGoToRegister;
    Button createUser;
    CheckBox cBStayConnected;

    FirebaseAuth refAuth;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        tVMsg = findViewById(R.id.tVMsg);
        createUser = findViewById(R.id.createUser);
        cBStayConnected = findViewById(R.id.cBStayConnected);
        tVGoToRegister = findViewById(R.id.tVGoToRegister);

        refAuth = FirebaseAuth.getInstance();
        sharedPref = getSharedPreferences("MyPref", MODE_PRIVATE);
        editor = sharedPref.edit();

        tVGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LogInActivity.this, RegisterActivity.class))
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean isChecked = sharedPref.getBoolean("stayConnect", false);

        if (refAuth.getCurrentUser() != null && isChecked) {
            startActivity(new Intent(LogInActivity.this, MainActivity.class));
            finish();
        }
    }

    public void createUser(View view) {
        String email = eTEmail.getText().toString();
        String pass = eTPass.getText().toString();

        if (email.isEmpty() || pass.isEmpty()) {
            tVMsg.setText("Please fill all fields");
        } else {
            ProgressDialog pd = new ProgressDialog(this);
            pd.setTitle("Connecting");
            pd.setMessage("Signing in...");
            pd.show();

            refAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        pd.dismiss();
                        if (task.isSuccessful()) {
                            FirebaseUser user = refAuth.getCurrentUser();
                            String userEmail = user.getEmail();

                            editor.putBoolean("stayConnect", cBStayConnected.isChecked());
                            editor.apply();

                            Intent si = new Intent(LogInActivity.this, MainActivity.class);
                            si.putExtra("email", userEmail);
                            startActivity(si);
                            finish();
                        } else {
                            Exception exp = task.getException();
                            if (exp instanceof FirebaseAuthInvalidUserException) {
                                tVMsg.setText("Invalid email address.");
                            } else if (exp instanceof FirebaseAuthInvalidCredentialsException) {
                                tVMsg.setText("Email or password incorrect.");
                            } else if (exp instanceof FirebaseNetworkException) {
                                tVMsg.setText("Network error. Please check your connection.");
                            } else {
                                tVMsg.setText("An error occurred. Please try again later.");
                            }
                        }
                    });
        }
    }
}
