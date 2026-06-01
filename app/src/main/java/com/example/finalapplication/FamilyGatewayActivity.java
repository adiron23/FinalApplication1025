package com.example.finalapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FamilyGatewayActivity extends AppCompatActivity {

    private CardView btnCreateFamily, btnJoinFamily;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String uid;

    private static final String PREFS_NAME       = "family_prefs";
    private static final String KEY_PENDING_CODE = "pending_family_code";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_gateway);

        db = FirebaseFirestore.getInstance();

        String codeFromLink = extractCodeFromIntent(getIntent());

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            if (codeFromLink != null) {
                savePendingCode(codeFromLink);
                Toast.makeText(this, "יש להתחבר או להירשם כדי להצטרף למשפחה", Toast.LENGTH_LONG).show();
            }
            startActivity(new Intent(this, LogInActivity.class));
            finish();
            return;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (codeFromLink != null) {
            clearPendingCode();
            startJoinFlow(codeFromLink);
            return;
        }

        String pendingCode = loadPendingCode();
        if (pendingCode != null) {
            clearPendingCode();
            startJoinFlow(pendingCode);
            return;
        }

        btnCreateFamily = findViewById(R.id.btnCreateFamily);
        btnJoinFamily   = findViewById(R.id.btnJoinFamily);
        progressBar     = findViewById(R.id.progressBar);

        btnCreateFamily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FamilyGatewayActivity.this, CreateFamilyActivity.class));
            }
        });
        btnJoinFamily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FamilyGatewayActivity.this.showJoinCodeDialog();
            }
        });
    }

    private void showJoinCodeDialog() {
        EditText input = new EditText(this);
        input.setHint("הכנס קוד משפחתי");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setPadding(50, 40, 50, 40);

        new AlertDialog.Builder(this)
                .setTitle("הצטרפות למשפחה")
                .setView(input)
                .setPositiveButton("המשך", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String code = input.getText().toString().toUpperCase().trim();
                        if (!code.isEmpty()) FamilyGatewayActivity.this.startJoinFlow(code);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void startJoinFlow(String code) {
        setLoading(true);

        db.collection("families").document(code).get()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setLoading(false);
                        Toast.makeText(FamilyGatewayActivity.this, "שגיאת חיבור: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot familyDoc) {
                        if (!familyDoc.exists()) {
                            setLoading(false);
                            Toast.makeText(FamilyGatewayActivity.this, "קוד משפחה לא קיים — בדוק שוב", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<String> availableRoles = (List<String>) familyDoc.get("availableRoles");
                        if (availableRoles == null) availableRoles = new ArrayList<>();
                        final List<String> finalAvailableRoles = availableRoles;

                        db.collection("users")
                                .whereEqualTo("familyCode", code)
                                .get()
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        setLoading(false);
                                        Toast.makeText(FamilyGatewayActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot usersSnap) {
                                        Set<String> claimedNames = new HashSet<>();
                                        for (QueryDocumentSnapshot userDoc : usersSnap) {
                                            String name = userDoc.getString("name");
                                            String role = userDoc.getString("role");
                                            if (name != null && !name.isEmpty()
                                                    && role != null && !role.isEmpty()) {
                                                claimedNames.add(name.trim().toLowerCase());
                                            }
                                        }

                                        List<String> unclaimed = new ArrayList<>();
                                        for (String entry : finalAvailableRoles) {
                                            String[] parts = entry.split(":", 2);
                                            String entryName = parts[0].trim().toLowerCase();
                                            if (!claimedNames.contains(entryName)) {
                                                unclaimed.add(entry);
                                            }
                                        }

                                        db.collection("users").document(uid).get()
                                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onSuccess(DocumentSnapshot myDoc) {
                                                        setLoading(false);
                                                        String myName = myDoc.getString("name");
                                                        FamilyGatewayActivity.this.resolveClaimForUser(code, myName, unclaimed);
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        setLoading(false);
                                                        Toast.makeText(FamilyGatewayActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    private void resolveClaimForUser(String code, String myName, List<String> unclaimed) {
        if (unclaimed.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("המשפחה מלאה")
                    .setMessage("כל המקומות במשפחה זו כבר תפוסים. פנה להורה להסיר מקום.")
                    .setPositiveButton("אישור", null)
                    .show();
            return;
        }

        String autoMatchEntry = null;
        if (myName != null) {
            String myNameLower = myName.trim().toLowerCase();
            for (String entry : unclaimed) {
                String[] parts = entry.split(":", 2);
                if (parts[0].trim().toLowerCase().equals(myNameLower)) {
                    autoMatchEntry = entry;
                    break;
                }
            }
        }

        if (autoMatchEntry != null) {
            String[] parts = autoMatchEntry.split(":");
            String matchedName = parts[0];
            final String matchedRole = parts[1];

            new AlertDialog.Builder(this)
                    .setTitle("נמצאה התאמה!")
                    .setMessage("זיהינו שאתה " + matchedName + " (" + matchedRole + ") במשפחה זו.\n\nזה נכון?")
                    .setPositiveButton("כן, זה אני!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            FamilyGatewayActivity.this.claimSlot(code, matchedRole);
                        }
                    })
                    .setNegativeButton("לא, בחר בעצמי", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            FamilyGatewayActivity.this.showClaimPickerDialog(code, unclaimed);
                        }
                    })
                    .show();
        } else {
            showClaimPickerDialog(code, unclaimed);
        }
    }

    private void showClaimPickerDialog(String code, List<String> unclaimed) {
        String[] displayLabels = new String[unclaimed.size()];
        for (int i = 0; i < unclaimed.size(); i++) {
            String[] parts = unclaimed.get(i).split(":", 2);
            String name = parts[0].trim();
            String role = parts.length >= 2 ? parts[1].trim() : "";
            displayLabels[i] = name + " (" + role + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("מי אתה במשפחה? בחר את המקום שלך")
                .setItems(displayLabels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String[] parts = unclaimed.get(which).split(":", 2);
                        String chosenRole = parts.length >= 2 ? parts[1].trim() : "בן משפחה";
                        FamilyGatewayActivity.this.claimSlot(code, chosenRole);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void claimSlot(String code, String role) {
        setLoading(true);
        db.collection("users").document(uid)
                .update("familyCode", code, "role", role)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        setLoading(false);
                        Intent intent = new Intent(FamilyGatewayActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setLoading(false);
                        Toast.makeText(FamilyGatewayActivity.this, "שגיאה בהצטרפות: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (btnCreateFamily != null) btnCreateFamily.setEnabled(!loading);
        if (btnJoinFamily   != null) btnJoinFamily.setEnabled(!loading);
    }

    private String extractCodeFromIntent(Intent intent) {
        Uri data = intent.getData();
        if (data != null
                && "familyapp".equals(data.getScheme())
                && "join".equals(data.getHost())) {
            String code = data.getQueryParameter("code");
            if (code != null && !code.isEmpty()) return code.toUpperCase();
        }
        return null;
    }

    private void savePendingCode(String code) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putString(KEY_PENDING_CODE, code).apply();
    }

    private String loadPendingCode() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_PENDING_CODE, null);
    }

    private void clearPendingCode() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().remove(KEY_PENDING_CODE).apply();
    }
}