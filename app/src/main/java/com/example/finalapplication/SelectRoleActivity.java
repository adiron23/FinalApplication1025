package com.example.finalapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

public class SelectRoleActivity extends AppCompatActivity {

    private LinearLayout rolesListContainer;
    private FirebaseFirestore db;
    private String uid, familyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_role);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        familyCode = getIntent().getStringExtra("FAMILY_CODE");

        rolesListContainer = findViewById(R.id.rolesListContainer);

        loadAvailableRoles();
    }

    private void loadAvailableRoles() {
        db.collection("families").document(familyCode).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                List<String> roles = (List<String>) doc.get("availableRoles");
                if (roles != null) {
                    for (String roleName : roles) {
                        addRoleButton(roleName);
                    }
                }
            }
        });
    }

    private void addRoleButton(String roleName) {
        MaterialButton btn = new MaterialButton(this, null, com.google.android.material.R.style.Widget_Material3_Button_OutlinedButton);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 150);
        params.setMargins(0, 24, 0, 0);
        btn.setLayoutParams(params);
        btn.setText(roleName);
        btn.setCornerRadius(75); // עיצוב עגול ומודרני

        btn.setOnClickListener(v -> {
            // שמירת הבחירה ב-Firebase
            Map<String, Object> update = new HashMap<>();
            update.put("members." + uid, roleName);

            db.collection("families").document(familyCode).update(update);
            db.collection("users").document(uid).update("role", roleName, "familyCode", familyCode);

            startActivity(new Intent(SelectRoleActivity.this, MainActivity.class));
            finish();
        });

        rolesListContainer.addView(btn);
    }
}