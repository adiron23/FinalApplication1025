package com.example.finalapplication;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CreateFamilyActivity extends AppCompatActivity {

    private LinearLayout childrenFieldsContainer;
    private EditText eTFamilyName, eTParent1, eTParent2;
    private List<EditText> childrenEdits = new ArrayList<>();
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_family);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        childrenFieldsContainer = findViewById(R.id.childrenFieldsContainer);
        eTFamilyName = findViewById(R.id.eTFamilyName);
        eTParent1 = findViewById(R.id.eTParent1);
        eTParent2 = findViewById(R.id.eTParent2);

        findViewById(R.id.btnAddChild).setOnClickListener(v -> addChildField());
        findViewById(R.id.btnFinishCreate).setOnClickListener(v -> saveFamily());

        // הוספת שדה ילד ראשון אוטומטית
        addChildField();
    }

    private void addChildField() {
        TextInputLayout layout = new TextInputLayout(this, null, com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 16, 0, 0);
        layout.setLayoutParams(params);
        layout.setHint("שם הילד");

        TextInputEditText editText = new TextInputEditText(layout.getContext());
        layout.addView(editText);

        childrenFieldsContainer.addView(layout);
        childrenEdits.add(editText);
    }

    private void saveFamily() {
        String famName = eTFamilyName.getText().toString().trim();
        String p1 = eTParent1.getText().toString().trim();
        String p2 = eTParent2.getText().toString().trim();

        if (famName.isEmpty() || p1.isEmpty()) {
            Toast.makeText(this, "נא למלא שם משפחה והורה 1", Toast.LENGTH_SHORT).show();
            return;
        }

        // בניית רשימת השמות לבחירה (availableRoles)
        List<String> roles = new ArrayList<>();
        roles.add(p1 + " (הורה)");
        if (!p2.isEmpty()) roles.add(p2 + " (הורה)");

        for (EditText et : childrenEdits) {
            String name = et.getText().toString().trim();
            if (!name.isEmpty()) roles.add(name + " (ילד/ה)");
        }

        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Map<String, Object> familyData = new HashMap<>();
        familyData.put("familyName", famName);
        familyData.put("familyCode", code);
        familyData.put("availableRoles", roles);
        familyData.put("adminUid", uid);

        db.collection("families").document(code).set(familyData).addOnSuccessListener(aVoid -> {
            // עדכון המשתמש הנוכחי (ההורה שיצר)
            db.collection("users").document(uid).update(
                    "familyCode", code,
                    "role", p1 + " (הורה)"
            );

            showCodeDialog(code);
        });
    }

    private void showCodeDialog(String code) {
        new AlertDialog.Builder(this)
                .setTitle("המשפחה נוצרה!")
                .setMessage("הקוד שלך הוא: " + code + "\nלחץ להעתקה ושליחה.")
                .setCancelable(false)
                .setPositiveButton("העתק והמשך", (d, w) -> {
                    ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cb.setPrimaryClip(ClipData.newPlainText("Family Code", code));
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }).show();
    }
}