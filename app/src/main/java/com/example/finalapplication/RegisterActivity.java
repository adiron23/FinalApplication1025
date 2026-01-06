package com.example.finalapplication;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText eTEmail, eTPass, eTName, eTBirth;
    private TextView tVMsg, btnGoToLogin;
    private Button createUser, btnSelectImage;
    private ImageView profileImageView;
    private FirebaseAuth refAuth;
    private FirebaseFirestore db;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onStart() {
        super.onStart();
        // בדיקה אם המשתמש כבר מחובר - אם כן, נשלח אותו לבדיקת סטטוס משפחה
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            checkUserStatusAndNavigate(currentUser.getUid());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        eTName = findViewById(R.id.eTName);
        eTBirth = findViewById(R.id.eTBirth);
        tVMsg = findViewById(R.id.tVMsg);
        createUser = findViewById(R.id.createUser);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        profileImageView = findViewById(R.id.profileImageView);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);

        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnSelectImage.setOnClickListener(v -> openImageChooser());
        createUser.setOnClickListener(v -> registerUser());
        btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LogInActivity.class));
        });

        eTBirth.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, day) -> eTBirth.setText(day + "/" + (month + 1) + "/" + year),
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
    }

    private void checkUserStatusAndNavigate(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("familyCode") != null && !doc.getString("familyCode").isEmpty()) {
                        startActivity(new Intent(this, MainActivity.class));
                    } else {
                        startActivity(new Intent(this, FamilyGatewayActivity.class));
                    }
                    finish();
                });
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
        }
    }

    private void registerUser() {
        String email = eTEmail.getText().toString().trim();
        String pass = eTPass.getText().toString().trim();
        String name = eTName.getText().toString().trim();
        String birth = eTBirth.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty() || name.isEmpty() || birth.isEmpty()) {
            tVMsg.setText("אנא מלא את כל השדות");
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("יוצר משתמש...");
        pd.show();

        refAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                saveUserToFirestore(refAuth.getCurrentUser().getUid(), email, name, birth, pd);
            } else {
                pd.dismiss();
                tVMsg.setText("שגיאה: " + task.getException().getMessage());
            }
        });
    }

    private void saveUserToFirestore(String uid, String email, String name, String birth, ProgressDialog pd) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("email", email);
        userData.put("name", name);
        userData.put("birthDate", birth);
        userData.put("imageUri", imageUri != null ? imageUri.toString() : "");
        userData.put("familyCode", "");
        userData.put("role", "");

        db.collection("users").document(uid).set(userData).addOnSuccessListener(aVoid -> {
            pd.dismiss();
            Intent intent = new Intent(this, FamilyGatewayActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}