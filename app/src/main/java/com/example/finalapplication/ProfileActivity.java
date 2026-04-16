package com.example.finalapplication;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Calendar;

public class ProfileActivity extends BaseActivity {

    private LinearLayout familyContainer;
    private TextView tvUserName, tvUserEmail, tvUserBirthDate, tvUserRole, tvFamilyNameTitle;
    private Button btnInviteMember;
    private FirebaseFirestore db;

    // Cached values used by the edit dialog and name propagation
    private String currentUid = "";
    private String currentUserName = "";
    private String currentEmail = "";
    private String currentBirthDate = "";
    private String userFamilyCode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewAndBind(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        initViews();

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();
            loadUserProfile(currentUid);
        }
    }

    private void initViews() {
        tvUserName       = findViewById(R.id.tvUserName);
        tvUserEmail      = findViewById(R.id.tvUserEmail);
        tvUserBirthDate  = findViewById(R.id.tvUserBirthDate);
        tvUserRole       = findViewById(R.id.tvUserRole);
        tvFamilyNameTitle = findViewById(R.id.tvFamilyNameTitle);
        familyContainer  = findViewById(R.id.familyContainer);
        btnInviteMember  = findViewById(R.id.btnInviteMember);

        Button btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> logoutUser());

        Button btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
    }

    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            currentUserName  = doc.getString("name")      != null ? doc.getString("name")      : "";
            currentEmail     = doc.getString("email")     != null ? doc.getString("email")     : "";
            currentBirthDate = doc.getString("birthDate") != null ? doc.getString("birthDate") : "";
            String role      = doc.getString("role")      != null ? doc.getString("role")      : "";
            userFamilyCode   = doc.getString("familyCode")!= null ? doc.getString("familyCode"): "";

            tvUserName.setText(currentUserName);
            tvUserEmail.setText(currentEmail);
            tvUserBirthDate.setText(currentBirthDate.isEmpty() ? "—" : currentBirthDate);
            tvUserRole.setText(role.isEmpty() ? "—" : role);

            if (!userFamilyCode.isEmpty()) {
                isCurrentUserParent = "הורה".equals(role);
                loadFamilyData(userFamilyCode, uid);
                if ("הורה".equals(role)) {
                    btnInviteMember.setVisibility(View.VISIBLE);
                    btnInviteMember.setOnClickListener(v -> showInviteDialog(userFamilyCode));
                }
            }
        });
    }

    private void loadFamilyData(String familyCode, String uid) {
        db.collection("families").document(familyCode).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvFamilyNameTitle.setText("משפחת " + doc.getString("familyName") + ":");
            }
        });

        db.collection("users").whereEqualTo("familyCode", familyCode).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (familyContainer == null) return;
                    familyContainer.removeAllViews();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        addFamilyRow(document.getString("name"), document.getString("role"),
                                document.getId(), uid);
                    }
                });
    }

    // ── Edit profile ──────────────────────────────────────────────────────────

    private void showEditProfileDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);

        EditText etName     = view.findViewById(R.id.etEditName);
        EditText etBirth    = view.findViewById(R.id.etEditBirth);
        EditText etEmail    = view.findViewById(R.id.etEditEmail);
        EditText etPassword = view.findViewById(R.id.etEditPassword);

        etName.setText(currentUserName);
        etBirth.setText(currentBirthDate);
        etEmail.setText(currentEmail);

        etBirth.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                    (dp, year, month, day) -> etBirth.setText(day + "/" + (month + 1) + "/" + year),
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("עריכת פרופיל")
                .setView(view)
                .setPositiveButton("שמור", (d, w) -> {
                    String newName     = etName.getText().toString().trim();
                    String newBirth    = etBirth.getText().toString().trim();
                    String newEmail    = etEmail.getText().toString().trim();
                    String newPassword = etPassword.getText().toString().trim();

                    if (newName.isEmpty() || newEmail.isEmpty()) {
                        Toast.makeText(this, "שם ואימייל הם שדות חובה", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPassword.isEmpty() && newPassword.length() < 6) {
                        Toast.makeText(this, "הסיסמה חייבת להכיל לפחות 6 תווים", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveProfileChanges(newName, newBirth, newEmail, newPassword);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void saveProfileChanges(String newName, String newBirth, String newEmail, String newPassword) {
        boolean nameChanged = !newName.equals(currentUserName);

        // 1. Update Firestore user document
        db.collection("users").document(currentUid)
                .update("name", newName, "birthDate", newBirth, "email", newEmail)
                .addOnSuccessListener(v -> {
                    // 2. Propagate name change across tasks and shopping list
                    if (nameChanged && !userFamilyCode.isEmpty()) {
                        propagateNameChange(currentUserName, newName);
                    }

                    // Refresh display
                    currentUserName  = newName;
                    currentEmail     = newEmail;
                    currentBirthDate = newBirth;
                    tvUserName.setText(newName);
                    tvUserEmail.setText(newEmail);
                    tvUserBirthDate.setText(newBirth.isEmpty() ? "—" : newBirth);

                    // Reload family list so other members see updated name
                    if (!userFamilyCode.isEmpty()) loadFamilyData(userFamilyCode, currentUid);

                    Toast.makeText(this, "הפרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        // 3. Update Firebase Auth email if changed
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && !newEmail.equals(currentEmail)) {
            firebaseUser.updateEmail(newEmail)
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "שגיאה בעדכון אימייל: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }

        // 4. Update password if provided
        if (!newPassword.isEmpty() && firebaseUser != null) {
            firebaseUser.updatePassword(newPassword)
                    .addOnSuccessListener(v -> Toast.makeText(this, "הסיסמה עודכנה", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "שגיאה בעדכון סיסמה: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void propagateNameChange(String oldName, String newName) {
        // Tasks assigned to this user
        db.collection("tasks")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("assignedToUid", currentUid)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap)
                        doc.getReference().update("assignedToName", newName);
                });

        // Shopping items assigned to this user
        db.collection("shopping_lists")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("assignedToUid", currentUid)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap)
                        doc.getReference().update("assignedToName", newName);
                });

        // Shopping items created by this user (stored by name, not UID)
        db.collection("shopping_lists")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("createdBy", oldName)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap)
                        doc.getReference().update("createdBy", newName);
                });
    }

    // ── Invite ────────────────────────────────────────────────────────────────

    private void showInviteDialog(String familyCode) {
        String link = "familyapp://join?code=" + familyCode;

        new AlertDialog.Builder(this)
                .setTitle("הוסף חבר משפחה")
                .setMessage("שתף את הקישור הבא:\n\n" + link + "\n\nקוד ידני: " + familyCode)
                .setPositiveButton("שתף קישור", (d, w) -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT,
                            "הצטרף למשפחה שלנו באפליקציה! לחץ על הקישור: " + link);
                    startActivity(Intent.createChooser(shareIntent, "שתף קישור הצטרפות"));
                })
                .setNeutralButton("העתק קישור", (d, w) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("family_link", link));
                    Toast.makeText(this, "הקישור הועתק", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("סגור", null)
                .show();
    }

    // ── Family list ───────────────────────────────────────────────────────────

    private boolean isCurrentUserParent = false;

    private void addFamilyRow(String name, String role, String memberId, String uid) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams rowParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 4, 0, 4);
        row.setLayoutParams(rowParams);

        TextView textView = new TextView(this);
        android.widget.LinearLayout.LayoutParams tvParams =
                new android.widget.LinearLayout.LayoutParams(0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textView.setLayoutParams(tvParams);
        textView.setTextSize(19);
        textView.setPadding(0, 15, 0, 15);
        textView.setTextColor(Color.BLACK);
        String text = (name != null ? name : "") + " (" + (role != null ? role : "") + ")";
        textView.setText(memberId.equals(uid) ? text + " - אני" : text);
        row.addView(textView);

        // Remove button — visible only to parents, and not for themselves
        if (!memberId.equals(uid)) {
            android.widget.Button btnRemove = new android.widget.Button(this);
            android.widget.LinearLayout.LayoutParams btnParams =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            btnRemove.setLayoutParams(btnParams);
            btnRemove.setText("הסר");
            btnRemove.setTextSize(15);
            btnRemove.setTextColor(Color.WHITE);
            btnRemove.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#B71C1C")));
            btnRemove.setAllCaps(false);
            btnRemove.setVisibility(isCurrentUserParent ? android.view.View.VISIBLE : android.view.View.GONE);
            btnRemove.setOnClickListener(v -> confirmRemoveMember(memberId, name != null ? name : ""));
            row.addView(btnRemove);
        }

        familyContainer.addView(row);
    }

    private void confirmRemoveMember(String memberId, String memberName) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("הסרת חבר משפחה")
                .setMessage("להסיר את " + memberName + " מהמשפחה?")
                .setPositiveButton("הסר", (d, w) -> removeMember(memberId))
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void removeMember(String memberId) {
        db.collection("users").document(memberId)
                .update("familyCode", "", "role", "")
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "החבר הוסר מהמשפחה", Toast.LENGTH_SHORT).show();
                    loadFamilyData(userFamilyCode, currentUid);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
