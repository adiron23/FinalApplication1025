package com.example.finalapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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

        // Normal gateway UI
        btnCreateFamily = findViewById(R.id.btnCreateFamily);
        btnJoinFamily   = findViewById(R.id.btnJoinFamily);
        progressBar     = findViewById(R.id.progressBar);

        btnCreateFamily.setOnClickListener(v ->
                startActivity(new Intent(this, CreateFamilyActivity.class)));
        btnJoinFamily.setOnClickListener(v -> showJoinCodeDialog());
    }



    private void showJoinCodeDialog() {
        EditText input = new EditText(this);
        input.setHint("הכנס קוד משפחתי");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setPadding(50, 40, 50, 40);

        new AlertDialog.Builder(this)
                .setTitle("הצטרפות למשפחה")
                .setView(input)
                .setPositiveButton("המשך", (dialog, which) -> {
                    String code = input.getText().toString().toUpperCase().trim();
                    if (!code.isEmpty()) startJoinFlow(code);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }



    /**
     * Step 1 — Validate the code, then compute unclaimed slots and route accordingly.
     */
    private void startJoinFlow(String code) {
        setLoading(true);

        db.collection("families").document(code).get()
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "שגיאת חיבור: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(familyDoc -> {
                    if (!familyDoc.exists()) {
                        setLoading(false);
                        Toast.makeText(this, "קוד משפחה לא קיים — בדוק שוב", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //noinspection unchecked
                    List<String> availableRoles = (List<String>) familyDoc.get("availableRoles");
                    if (availableRoles == null) availableRoles = new ArrayList<>();

                    List<String> finalAvailableRoles = availableRoles;

                    // Step 2 — find already-claimed slots by querying existing family members
                    db.collection("users")
                            .whereEqualTo("familyCode", code)
                            .get()
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            })
                            .addOnSuccessListener(usersSnap -> {
                                // Build a set of already-claimed member names (lower-case, trimmed)
                                // We match by name only — avoids role-format mismatches
                                Set<String> claimedNames = new HashSet<>();
                                for (QueryDocumentSnapshot userDoc : usersSnap) {
                                    String name = userDoc.getString("name");
                                    String role = userDoc.getString("role");
                                    if (name != null && !name.isEmpty()
                                            && role != null && !role.isEmpty()) {
                                        claimedNames.add(name.trim().toLowerCase());
                                    }
                                }

                                // Compute unclaimed slots: entries whose name part is not yet taken
                                List<String> unclaimed = new ArrayList<>();
                                for (String entry : finalAvailableRoles) {
                                    String[] parts = entry.split(":", 2);
                                    String entryName = parts[0].trim().toLowerCase();
                                    if (!claimedNames.contains(entryName)) {
                                        unclaimed.add(entry);
                                    }
                                }

                                // Step 3 — fetch the joining user's registered name and try to auto-match
                                db.collection("users").document(uid).get()
                                        .addOnSuccessListener(myDoc -> {
                                            setLoading(false);
                                            String myName = myDoc.getString("name");
                                            resolveClaimForUser(code, myName, unclaimed);
                                        })
                                        .addOnFailureListener(e -> {
                                            setLoading(false);
                                            Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            });
                });
    }



    /**
     * Step 3 — match user's registered name against unclaimed slots.
     * Auto-match  → show confirmation.
     * No match    → show pick list.
     * List empty  → family is full.
     */
    private void resolveClaimForUser(String code, String myName, List<String> unclaimed) {
        if (unclaimed.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("המשפחה מלאה")
                    .setMessage("כל המקומות במשפחה זו כבר תפוסים. פנה להורה להסיר מקום.")
                    .setPositiveButton("אישור", null)
                    .show();
            return;
        }

        // Try to find an unclaimed slot whose name matches the user's registered name
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
            // Found an auto-match — ask the user to confirm before claiming
            String[] parts = autoMatchEntry.split(":");
            String matchedName = parts[0];
            String matchedRole = parts[1];
            final String finalEntry = autoMatchEntry;

            new AlertDialog.Builder(this)
                    .setTitle("נמצאה התאמה!")
                    .setMessage("זיהינו שאתה " + matchedName + " (" + matchedRole + ") במשפחה זו.\n\nזה נכון?")
                    .setPositiveButton("כן, זה אני!", (d, w) -> claimSlot(code, matchedRole))
                    .setNegativeButton("לא, בחר בעצמי", (d, w) -> showClaimPickerDialog(code, unclaimed))
                    .show();
        } else {
            // No auto-match — let the user pick from the available (unclaimed) members
            showClaimPickerDialog(code, unclaimed);
        }
    }

    /**
     * Show a list of unclaimed family members so the user can pick their slot.
     *
     * NOTE: setMessage() and setItems() are mutually exclusive in AlertDialog —
     * never use both together or the list will not appear.
     */
    private void showClaimPickerDialog(String code, List<String> unclaimed) {
        // Build display labels: "Sarah (ילד/ה)"
        String[] displayLabels = new String[unclaimed.size()];
        for (int i = 0; i < unclaimed.size(); i++) {
            String[] parts = unclaimed.get(i).split(":", 2);
            String name = parts[0].trim();
            String role = parts.length >= 2 ? parts[1].trim() : "";
            displayLabels[i] = name + " (" + role + ")";
        }

        // Title carries the instruction — NO setMessage() here, it hides the list
        new AlertDialog.Builder(this)
                .setTitle("מי אתה במשפחה? בחר את המקום שלך")
                .setItems(displayLabels, (dialog, which) -> {
                    String[] parts = unclaimed.get(which).split(":", 2);
                    String chosenRole = parts.length >= 2 ? parts[1].trim() : "בן משפחה";
                    claimSlot(code, chosenRole);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * Final step — write the claim to Firestore and navigate to the main screen.
     */
    private void claimSlot(String code, String role) {
        setLoading(true);
        db.collection("users").document(uid)
                .update("familyCode", code, "role", role)
                .addOnSuccessListener(v -> {
                    setLoading(false);
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "שגיאה בהצטרפות: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
