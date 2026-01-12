package com.example.finalapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
// אם את משתמשת ב-Glide לטעינת תמונות, הוסיפי את ה-import שלו

public class ProfileActivity extends BaseActivity {

    private LinearLayout familyContainer;
    private TextView tvUserName, tvUserEmail, tvUserBirthDate, tvUserRole, tvFamilyNameTitle;
    private ImageView imgProfile;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadUserProfile(currentUser.getUid());
        }

        markSelectedMenuItem(R.id.nav_profile);
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserBirthDate = findViewById(R.id.tvUserBirthDate);
        tvUserRole = findViewById(R.id.tvUserRole);
        tvFamilyNameTitle = findViewById(R.id.tvFamilyNameTitle);
        imgProfile = findViewById(R.id.imgProfile);
        familyContainer = findViewById(R.id.familyContainer);
        Button btnLogout = findViewById(R.id.btnLogout);

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logoutUser());
        }
    }

    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvUserName.setText(doc.getString("name"));
                tvUserEmail.setText(doc.getString("email"));
                tvUserBirthDate.setText("תאריך לידה: " + doc.getString("birthDate"));
                tvUserRole.setText("תפקיד: " + doc.getString("role"));

                String imageUri = doc.getString("imageUri");
                if (imageUri != null && !imageUri.isEmpty()) {
                    // כאן אפשר להשתמש ב-Glide לטעינה: Glide.with(this).load(imageUri).into(imgProfile);
                } else {
                    imgProfile.setImageResource(R.drawable.ic_person);
                }

                String familyCode = doc.getString("familyCode");
                if (familyCode != null) {
                    loadFamilyData(familyCode, uid);
                }
            }
        });
    }

    private void loadFamilyData(String familyCode, String currentUid) {
        // 1. משיכת שם המשפחה מהקולקציה families
        db.collection("families")
                .whereEqualTo("familyCode", familyCode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String fName = querySnapshot.getDocuments().get(0).getString("familyName");
                        tvFamilyNameTitle.setText("משפחת " + fName + ":");
                    }
                });

        // 2. משיכת חברי המשפחה מהקולקציה users
        db.collection("users")
                .whereEqualTo("familyCode", familyCode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    familyContainer.removeAllViews();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String name = document.getString("name");
                        String role = document.getString("role");
                        String memberId = document.getId();

                        addFamilyRow(name, role, memberId, currentUid);
                    }
                });
    }

    private void addFamilyRow(String name, String role, String memberId, String currentUid) {
        TextView textView = new TextView(this);
        textView.setTextSize(16);
        textView.setPadding(20, 20, 20, 20);

        String text = name + " - " + role;

        if (memberId.equals(currentUid)) {
            textView.setText(text + " (אני)");
            textView.setTextColor(Color.GRAY);
        } else {
            textView.setText(text);
            textView.setTextColor(Color.BLACK);
        }

        familyContainer.addView(textView);
    }
}