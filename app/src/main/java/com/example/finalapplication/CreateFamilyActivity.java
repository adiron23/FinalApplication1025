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
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        childrenFieldsContainer = findViewById(R.id.childrenFieldsContainer);
        eTFamilyName = findViewById(R.id.eTFamilyName);
        eTParent1 = findViewById(R.id.eTParent1);
        eTParent2 = findViewById(R.id.eTParent2);

        findViewById(R.id.btnAddChild).setOnClickListener(v -> addChildField());
        findViewById(R.id.btnFinishCreate).setOnClickListener(v -> saveFamily());

        addChildField();
    }

    private void addChildField() {
        // 1. "ניפוח" (Inflation) - הפיכת קובץ ה-XML לאובייקט Java חי
        TextInputLayout childLayout = (TextInputLayout) getLayoutInflater()
                .inflate(R.layout.item_child_field, childrenFieldsContainer, false);

        // 2. שליפת תיבת הטקסט הפנימית (ה-EditText) כדי שנוכל לקרוא את השם אחר כך
        EditText editText = childLayout.getEditText();

        // 3. הוספת השדה המעוצב לתוך ה-LinearLayout הראשי במסך
        childrenFieldsContainer.addView(childLayout);

        // 4. שמירת ה-EditText ברשימה (List) כדי שנוכל לעבור עליה בלחיצה על "סיום"
        if (editText != null) childrenEdits.add(editText);
    }

    private void saveFamily() {
        String famName = eTFamilyName.getText().toString().trim();
        String p1 = eTParent1.getText().toString().trim();
        String p2 = eTParent2.getText().toString().trim();

        if (famName.isEmpty() || p1.isEmpty()) {
            Toast.makeText(this, "נא למלא שם משפחה והורה 1", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> roles = new ArrayList<>();
        roles.add(p1 + ":הורה");
        if (!p2.isEmpty()) roles.add(p2 + ":הורה");

        for (EditText et : childrenEdits) {
            String name = et.getText().toString().trim();
            if (!name.isEmpty()) roles.add(name + ":ילד/ה");
        }

        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Map<String, Object> familyData = new HashMap<>();
        familyData.put("familyName", famName);
        familyData.put("familyCode", code);
        familyData.put("availableRoles", roles);

        db.collection("families").document(code).set(familyData)
                .addOnSuccessListener(aVoid -> identifyAndSetUserRole(code, roles))
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה ביצירת משפחה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void identifyAndSetUserRole(String code, List<String> roles) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String myName = doc.getString("name");
            String myRole = "בן משפחה";

            if (myName != null) {
                for (String roleEntry : roles) {
                    String[] parts = roleEntry.split(":");
                    if (parts.length >= 2 && parts[0].equalsIgnoreCase(myName)) {
                        myRole = parts[1];
                        break;
                    }
                }
            }

            db.collection("users").document(uid).update(
                    "familyCode", code,
                    "role", myRole
            ).addOnSuccessListener(v -> showShareDialog(code))
             .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בשמירת תפקיד: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void showShareDialog(String code) {
        String link = "familyapp://join?code=" + code;

        new AlertDialog.Builder(this)
                .setTitle("המשפחה נוצרה!")
                .setMessage("שתף את הקישור הבא עם בני המשפחה כדי שיצטרפו:\n\n" + link + "\n\nקוד ידני: " + code)
                .setCancelable(false)
                .setPositiveButton("שתף קישור", (d, w) -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT,
                            "הצטרף למשפחה שלנו באפליקציה! לחץ על הקישור: " + link);
                    startActivity(Intent.createChooser(shareIntent, "שתף קישור הצטרפות"));
                    // continue to MainActivity after sharing
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNeutralButton("העתק קישור", (d, w) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("family_link", link));
                    Toast.makeText(this, "הקישור הועתק", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .setNegativeButton("המשך בלי לשתף", (d, w) -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }).show();
    }
}