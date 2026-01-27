package com.example.finalapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ProfileActivity extends BaseActivity {

    private LinearLayout familyContainer;
    private TextView tvUserName, tvUserEmail, tvUserBirthDate, tvUserRole, tvFamilyNameTitle;
    private ImageView imgProfile;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // טעינת ה-Layout שמתאים ל-IDs האלו
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadUserProfile(currentUser.getUid());
        }

        // סימון האייקון הנכון בתפריט התחתון
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

                // ברירת מחדל לתמונה
                imgProfile.setImageResource(R.drawable.ic_person);

                String familyCode = doc.getString("familyCode");
                if (familyCode != null) {
                    loadFamilyData(familyCode, uid);
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "שגיאה בטעינת הנתונים", Toast.LENGTH_SHORT).show());
    }

    private void loadFamilyData(String familyCode, String currentUid) {
        // משיכת שם המשפחה
        db.collection("families")
                .whereEqualTo("familyCode", familyCode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String fName = querySnapshot.getDocuments().get(0).getString("familyName");
                        if (tvFamilyNameTitle != null) {
                            tvFamilyNameTitle.setText("משפחת " + fName + ":");
                        }
                    }
                });

        // משיכת חברי המשפחה
        db.collection("users")
                .whereEqualTo("familyCode", familyCode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (familyContainer != null) {
                        familyContainer.removeAllViews();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            String name = document.getString("name");
                            String role = document.getString("role");
                            String memberId = document.getId();
                            addFamilyRow(name, role, memberId, currentUid);
                        }
                    }
                });
    }

    private void addFamilyRow(String name, String role, String memberId, String currentUid) {
        TextView textView = new TextView(this);
        textView.setTextSize(16);
        textView.setPadding(30, 15, 30, 15);
        textView.setTextColor(Color.parseColor("#1A237E"));

        String text = name + " - " + role;
        if (memberId.equals(currentUid)) {
            textView.setText(text + " (אני)");
            textView.setAlpha(0.6f); // קצת שקוף לציון "זה אני"
        } else {
            textView.setText(text);
        }

        familyContainer.addView(textView);
    }


}