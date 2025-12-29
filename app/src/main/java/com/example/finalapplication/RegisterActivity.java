package com.example.finalapplication;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
    private TextView tVMsg;
    private TextView btnGoToLogin; // שונה ל-TextView כדי להתאים ל-XML החדש
    private Button createUser, btnSelectImage;
    private ImageView profileImageView;
    private FirebaseAuth refAuth;
    private FirebaseFirestore db;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // אתחול רכיבים - וודאי שה-ID תואמים ל-XML החדש ששלחתי
        eTEmail = findViewById(R.id.eTEmail);
        eTPass = findViewById(R.id.eTPass);
        eTName = findViewById(R.id.eTName);
        eTBirth = findViewById(R.id.eTBirth);
        tVMsg = findViewById(R.id.tVMsg);
        createUser = findViewById(R.id.createUser);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        profileImageView = findViewById(R.id.profileImageView);
        btnGoToLogin = findViewById(R.id.btnGoToLogin); // עכשיו זה מוצא TextView

        refAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnSelectImage.setOnClickListener(v -> openImageChooser());
        createUser.setOnClickListener(v -> registerUser());

        btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LogInActivity.class));
            finish();
        });

        // בחירת תאריך לידה
        eTBirth.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(RegisterActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        eTBirth.setText(date);
                    }, year, month, day);
            datePickerDialog.show();
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

        if (pass.length() < 6) {
            tVMsg.setText("הסיסמה חייבת להכיל לפחות 6 תווים");
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("יוצר משתמש ושומר נתונים...");
        pd.setCancelable(false);
        pd.show();

        refAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = refAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), email, name, birth, pd);
                        }
                    } else {
                        pd.dismiss();
                        String error = task.getException() != null ? task.getException().getMessage() : "שגיאה לא ידועה";
                        tVMsg.setText("שגיאה ברישום: " + error);
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

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    pd.dismiss();
                    Toast.makeText(this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();

                    // מעבר למסך הראשי וניקוי המחסנית
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    Log.e("FirestoreError", "Error saving user: " + e.getMessage());
                    tVMsg.setText("שגיאה בשמירה: " + e.getLocalizedMessage());
                });
    }
}