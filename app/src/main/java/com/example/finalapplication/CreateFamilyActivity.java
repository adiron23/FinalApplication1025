package com.example.finalapplication;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
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

    // מאתחל את המסך: מחבר views, מגדיר כפתורים ומוסיף שדה ילד ראשון ריק
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_family);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        childrenFieldsContainer = findViewById(R.id.childrenFieldsContainer);
        eTFamilyName = findViewById(R.id.eTFamilyName);
        eTParent1    = findViewById(R.id.eTParent1);
        eTParent2    = findViewById(R.id.eTParent2);

        findViewById(R.id.btnAddChild).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreateFamilyActivity.this.addChildField();
            }
        });
        findViewById(R.id.btnFinishCreate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreateFamilyActivity.this.saveFamily();
            }
        });

        addChildField();
    }

    // מוסיף שדה טקסט חדש להזנת שם ילד נוסף
    private void addChildField() {
        TextInputLayout childLayout = (TextInputLayout) getLayoutInflater()
                .inflate(R.layout.item_child_field, childrenFieldsContainer, false);
        EditText editText = childLayout.getEditText();
        childrenFieldsContainer.addView(childLayout);
        if (editText != null) childrenEdits.add(editText);
    }

    // אוסף את פרטי המשפחה מהטופס, יוצר קוד משפחה ייחודי ושומר ב-Firestore
    private void saveFamily() {
        String famName = eTFamilyName.getText().toString().trim();
        String p1      = eTParent1.getText().toString().trim();
        String p2      = eTParent2.getText().toString().trim();

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
        familyData.put("familyName",     famName);
        familyData.put("familyCode",     code);
        familyData.put("availableRoles", roles);

        final String finalCode      = code;
        final List<String> finalRoles = roles;

        db.collection("families").document(code).set(familyData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        CreateFamilyActivity.this.identifyAndSetUserRole(finalCode, finalRoles);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(CreateFamilyActivity.this, "שגיאה ביצירת משפחה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // מזהה את תפקיד המשתמש הנוכחי לפי שמו ברשימת התפקידים ומעדכן את פרטיו ב-Firestore
    private void identifyAndSetUserRole(String code, List<String> roles) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
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

                        final String finalRole = myRole;
                        db.collection("users").document(uid).update(
                                "familyCode", code,
                                "role", finalRole
                        ).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void v) {
                                CreateFamilyActivity.this.showShareDialog(code);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(CreateFamilyActivity.this, "שגיאה בשמירת תפקיד: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
    }

    // מציג דיאלוג עם קישור הצטרפות למשפחה – מאפשר שיתוף, העתקה או המשך בלי שיתוף
    private void showShareDialog(String code) {
        String link = "familyapp://join?code=" + code;

        new AlertDialog.Builder(this)
                .setTitle("המשפחה נוצרה!")
                .setMessage("שתף את הקישור הבא עם בני המשפחה כדי שיצטרפו:\n\n" + link + "\n\nקוד ידני: " + code)
                .setCancelable(false)
                .setPositiveButton("שתף קישור", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT,
                                "הצטרף למשפחה שלנו באפליקציה! לחץ על הקישור: " + link);
                        startActivity(Intent.createChooser(shareIntent, "שתף קישור הצטרפות"));
                        startActivity(new Intent(CreateFamilyActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .setNeutralButton("העתק קישור", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText("family_link", link));
                        Toast.makeText(CreateFamilyActivity.this, "הקישור הועתק", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(CreateFamilyActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .setNegativeButton("המשך בלי לשתף", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        startActivity(new Intent(CreateFamilyActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .show();
    }
}