package com.example.finalapplication;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import android.widget.RadioGroup;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends BaseActivity {

    // Preset avatar drawable names (PNG files in res/drawable/)
    private static final String[] AVATAR_NAMES = {
        "duck", "gorilla", "hen", "sloth",
        "penguin", "panda", "monkey", "sealion", "koala"
    };

    private LinearLayout familyContainer;
    private ImageView    ivAvatar;
    private TextView     tvUserName, tvUserEmail, tvUserBirthDate, tvUserRole, tvFamilyNameTitle;
    private Button       btnInviteMember;
    private FirebaseFirestore db;

    // Cached values
    private String currentUid        = "";
    private String currentUserName   = "";
    private String currentEmail      = "";
    private String currentBirthDate  = "";
    private String userFamilyCode    = "";
    private String currentImageUri   = "";   // either a content/file URI string or "avatar_<name>"
    private boolean isCurrentUserParent = false;

    // Temp state while edit-profile dialog is open
    private Uri     pendingImageUri    = null;   // set when gallery or camera result arrives
    private String  pendingAvatarName  = null;   // set when user picks a preset avatar
    private ImageView dialogAvatarPreview = null;

    // Camera photo temp file URI
    private Uri cameraPhotoUri = null;

    // Modern ActivityResult launchers (registered in onCreate)
    private ActivityResultLauncher<Uri>      cameraLauncher;
    private ActivityResultLauncher<String[]> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewAndBind(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();

        // Camera: TakePicture writes directly to cameraPhotoUri
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (Boolean.TRUE.equals(success) && cameraPhotoUri != null) {
                        pendingImageUri   = cameraPhotoUri;
                        pendingAvatarName = null;
                        if (dialogAvatarPreview != null)
                            dialogAvatarPreview.setImageURI(pendingImageUri);
                    }
                });

        // Gallery: OpenDocument returns a persistable Uri
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignored) {}
                        pendingImageUri   = uri;
                        pendingAvatarName = null;
                        if (dialogAvatarPreview != null)
                            dialogAvatarPreview.setImageURI(uri);
                    }
                });

        initViews();

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();
            loadUserProfile(currentUid);
        }
    }

    private void initViews() {
        ivAvatar          = findViewById(R.id.ivAvatar);
        tvUserName        = findViewById(R.id.tvUserName);
        tvUserEmail       = findViewById(R.id.tvUserEmail);
        tvUserBirthDate   = findViewById(R.id.tvUserBirthDate);
        tvUserRole        = findViewById(R.id.tvUserRole);
        tvFamilyNameTitle = findViewById(R.id.tvFamilyNameTitle);
        familyContainer   = findViewById(R.id.familyContainer);
        btnInviteMember   = findViewById(R.id.btnInviteMember);

        Button btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> logoutUser());

        Button btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
    }

    // ── Load profile ──────────────────────────────────────────────────────────

    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            currentUserName  = doc.getString("name")       != null ? doc.getString("name")      : "";
            currentEmail     = doc.getString("email")      != null ? doc.getString("email")     : "";
            currentBirthDate = doc.getString("birthDate")  != null ? doc.getString("birthDate") : "";
            currentImageUri  = doc.getString("imageUri")   != null ? doc.getString("imageUri")  : "";
            String role      = doc.getString("role")       != null ? doc.getString("role")      : "";
            userFamilyCode   = doc.getString("familyCode") != null ? doc.getString("familyCode"): "";

            tvUserName.setText(currentUserName);
            tvUserEmail.setText(currentEmail);
            tvUserBirthDate.setText(currentBirthDate.isEmpty() ? "—" : currentBirthDate);
            tvUserRole.setText(role.isEmpty() ? "—" : role);

            applyAvatarToView(currentImageUri, ivAvatar);

            if (!userFamilyCode.isEmpty()) {
                isCurrentUserParent = "הורה".equals(role);
                loadFamilyData(userFamilyCode, uid);
                if (isCurrentUserParent) {
                    btnInviteMember.setVisibility(View.VISIBLE);
                    btnInviteMember.setOnClickListener(v -> showAddMemberChooser());
                }
            }
        });
    }

    /**
     * Applies a stored image value to an ImageView.
     * URI strings (content://, file://) are loaded via setImageURI.
     * Everything else is treated as a drawable resource name (e.g. "duck").
     */
    private void applyAvatarToView(String value, ImageView target) {
        if (value == null || value.isEmpty()) return;
        if (value.startsWith("content://") || value.startsWith("file://")) {
            try { target.setImageURI(Uri.parse(value)); }
            catch (Exception ignored) {}
        } else {
            // Preset drawable name
            int resId = getResources().getIdentifier(value, "drawable", getPackageName());
            if (resId != 0) {
                target.setPadding(0, 0, 0, 0);
                target.setScaleType(ImageView.ScaleType.CENTER_CROP);
                target.setImageResource(resId);
            }
        }
    }

    // ── Family data — sorted: parents first ──────────────────────────────────

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

                    List<QueryDocumentSnapshot> parents  = new ArrayList<>();
                    List<QueryDocumentSnapshot> children = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        if ("הורה".equals(doc.getString("role"))) parents.add(doc);
                        else children.add(doc);
                    }

                    for (QueryDocumentSnapshot doc : parents)  addFamilyCard(doc, uid);
                    for (QueryDocumentSnapshot doc : children) addFamilyCard(doc, uid);
                });
    }

    private void addFamilyCard(QueryDocumentSnapshot doc, String currentUserId) {
        String memberId    = doc.getId();
        String memberName  = doc.getString("name")     != null ? doc.getString("name")     : "?";
        String memberRole  = doc.getString("role")     != null ? doc.getString("role")     : "";
        String memberImage = doc.getString("imageUri") != null ? doc.getString("imageUri") : "";

        View card = LayoutInflater.from(this).inflate(R.layout.family_member_card, familyContainer, false);

        TextView  tvAvatarLetter = card.findViewById(R.id.tvMemberAvatar);
        ImageView ivAvatarImage  = card.findViewById(R.id.ivMemberAvatar);
        TextView  tvName         = card.findViewById(R.id.tvMemberName);
        TextView  tvRole         = card.findViewById(R.id.tvMemberRole);
        TextView  btnRemove      = card.findViewById(R.id.btnRemoveMember);

        int avatarColor = "הורה".equals(memberRole) ? 0xFF4E342E : 0xFF8D6E63;

        if (!memberImage.isEmpty()) {
            // Show real picture, hide the letter circle
            ivAvatarImage.setVisibility(View.VISIBLE);
            tvAvatarLetter.setVisibility(View.GONE);
            applyAvatarToView(memberImage, ivAvatarImage);
        } else {
            // Fallback: coloured circle with first letter
            tvAvatarLetter.setText(memberName.isEmpty() ? "?" : String.valueOf(memberName.charAt(0)).toUpperCase());
            tvAvatarLetter.getBackground().setTint(avatarColor);
        }

        tvName.setText(memberId.equals(currentUserId) ? memberName + " (אני)" : memberName);
        tvRole.setText(memberRole);

        if (isCurrentUserParent && !memberId.equals(currentUserId)) {
            btnRemove.setVisibility(View.VISIBLE);
            btnRemove.setOnClickListener(v -> confirmRemoveMember(memberId, memberName));
        }

        familyContainer.addView(card);
    }

    // ── Edit profile ──────────────────────────────────────────────────────────

    private void showEditProfileDialog() {
        pendingImageUri   = null;
        pendingAvatarName = null;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);

        dialogAvatarPreview = view.findViewById(R.id.ivEditAvatar);
        Button btnPickImage = view.findViewById(R.id.btnPickProfileImage);
        EditText etName     = view.findViewById(R.id.etEditName);
        EditText etBirth    = view.findViewById(R.id.etEditBirth);
        EditText etEmail    = view.findViewById(R.id.etEditEmail);
        EditText etPassword = view.findViewById(R.id.etEditPassword);

        etName.setText(currentUserName);
        etBirth.setText(currentBirthDate);
        etEmail.setText(currentEmail);
        applyAvatarToView(currentImageUri, dialogAvatarPreview);

        btnPickImage.setOnClickListener(v -> showImageSourceChooser());

        etBirth.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this,
                    (dp, year, month, day) -> etBirth.setText(day + "/" + (month + 1) + "/" + year),
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("עריכת פרופיל")
                .setView(view)
                .setPositiveButton("שמור", null)
                .setNegativeButton("ביטול", null)
                .setNeutralButton("שלח קישור לאיפוס סיסמה", (d, w) -> sendPasswordResetEmail())
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
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
            dialog.dismiss();
        }));

        dialog.show();
    }

    // ── Image source chooser ─────────────────────────────────────────────────

    private void showImageSourceChooser() {
        String[] options = {"צלם תמונה", "בחר מהגלריה", "אווטאר מוכן"};
        new AlertDialog.Builder(this)
                .setTitle("בחר תמונת פרופיל")
                .setItems(options, (d, which) -> {
                    if (which == 0) launchCamera();
                    else if (which == 1) launchGallery();
                    else showAvatarPicker();
                })
                .show();
    }

    private void launchCamera() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File photoFile  = File.createTempFile("PROFILE_" + timeStamp, ".jpg", storageDir);
            cameraPhotoUri  = FileProvider.getUriForFile(this,
                    "com.example.finalapplication.fileprovider", photoFile);
            cameraLauncher.launch(cameraPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "שגיאה ביצירת קובץ תמונה", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        galleryLauncher.launch(new String[]{"image/*"});
    }

    /** Shows a horizontal-scroll grid of preset avatars in an AlertDialog. */
    private void showAvatarPicker() {
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setPadding(16, 24, 16, 16);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(row);

        int sizePx = (int) (getResources().getDisplayMetrics().density * 80);
        int marginPx = (int) (getResources().getDisplayMetrics().density * 8);

        AlertDialog[] dialogHolder = new AlertDialog[1];

        for (String avatarName : AVATAR_NAMES) {
            int resId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
            if (resId == 0) continue;

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cellLp.setMargins(marginPx, 0, marginPx, 0);
            cell.setLayoutParams(cellLp);

            ImageView img = new ImageView(this);
            img.setImageResource(resId);
            img.setLayoutParams(new LinearLayout.LayoutParams(sizePx, sizePx));
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            cell.addView(img);

            final String name = avatarName;
            cell.setOnClickListener(v -> {
                pendingAvatarName = name;
                pendingImageUri   = null;
                if (dialogAvatarPreview != null) dialogAvatarPreview.setImageResource(resId);
                if (dialogHolder[0] != null) dialogHolder[0].dismiss();
            });

            row.addView(cell);
        }

        dialogHolder[0] = new AlertDialog.Builder(this)
                .setTitle("בחר אווטאר")
                .setView(scrollView)
                .setNegativeButton("ביטול", null)
                .create();
        dialogHolder[0].show();
    }

    private void saveProfileChanges(String newName, String newBirth, String newEmail, String newPassword) {
        boolean nameChanged = !newName.equals(currentUserName);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("name",      newName);
        updates.put("birthDate", newBirth);
        updates.put("email",     newEmail);

        if (pendingAvatarName != null) {
            // Preset avatar — store the drawable name
            updates.put("imageUri", pendingAvatarName);
        } else if (pendingImageUri != null) {
            // Camera or gallery URI
            updates.put("imageUri", pendingImageUri.toString());
        }

        db.collection("users").document(currentUid).update(updates)
                .addOnSuccessListener(v -> {
                    if (nameChanged && !userFamilyCode.isEmpty()) {
                        propagateNameChange(currentUserName, newName);
                    }

                    currentUserName  = newName;
                    currentEmail     = newEmail;
                    currentBirthDate = newBirth;
                    tvUserName.setText(newName);
                    tvUserEmail.setText(newEmail);
                    tvUserBirthDate.setText(newBirth.isEmpty() ? "—" : newBirth);

                    if (pendingAvatarName != null) {
                        currentImageUri = pendingAvatarName;
                        applyAvatarToView(pendingAvatarName, ivAvatar);
                        pendingAvatarName = null;
                    } else if (pendingImageUri != null) {
                        currentImageUri = pendingImageUri.toString();
                        ivAvatar.setPadding(0, 0, 0, 0);
                        ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        ivAvatar.setImageURI(pendingImageUri);
                        pendingImageUri = null;
                    }

                    if (!userFamilyCode.isEmpty()) loadFamilyData(userFamilyCode, currentUid);
                    Toast.makeText(this, "הפרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && !newEmail.equals(currentEmail)) {
            firebaseUser.updateEmail(newEmail)
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "שגיאה בעדכון אימייל: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }

        if (!newPassword.isEmpty() && firebaseUser != null) {
            firebaseUser.updatePassword(newPassword)
                    .addOnSuccessListener(v -> Toast.makeText(this, "הסיסמה עודכנה", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "שגיאה בעדכון סיסמה: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    // ── Password reset ────────────────────────────────────────────────────────

    private void sendPasswordResetEmail() {
        if (currentEmail.isEmpty()) {
            Toast.makeText(this, "לא נמצאה כתובת אימייל", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(currentEmail)
                .addOnSuccessListener(v ->
                        new AlertDialog.Builder(this)
                                .setTitle("קישור נשלח")
                                .setMessage("קישור לאיפוס סיסמה נשלח לכתובת:\n" + currentEmail)
                                .setPositiveButton("אישור", null)
                                .show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ── Name propagation ──────────────────────────────────────────────────────

    private void propagateNameChange(String oldName, String newName) {
        db.collection("tasks")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("assignedToUid", currentUid)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap)
                        doc.getReference().update("assignedToName", newName);
                });

        db.collection("shopping_lists")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("assignedToUid", currentUid)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap)
                        doc.getReference().update("assignedToName", newName);
                });

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

    // ── Add member chooser ────────────────────────────────────────────────────

    private void showAddMemberChooser() {
        String[] options = {"שלח הזמנה (קישור / קוד)", "צור חשבון חדש עבורם"};
        new AlertDialog.Builder(this)
                .setTitle("הוסף חבר משפחה")
                .setItems(options, (d, which) -> {
                    if (which == 0) showInviteDialog(userFamilyCode);
                    else            showCreateMemberDialog();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showCreateMemberDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_member, null);

        com.google.android.material.textfield.TextInputEditText etName =
                view.findViewById(R.id.etMemberName);
        com.google.android.material.textfield.TextInputEditText etEmail =
                view.findViewById(R.id.etMemberEmail);
        com.google.android.material.textfield.TextInputEditText etPassword =
                view.findViewById(R.id.etMemberPassword);
        RadioGroup rgRole = view.findViewById(R.id.rgRole);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("יצירת חשבון חבר משפחה")
                .setView(view)
                .setPositiveButton("צור חשבון", null)   // set below to prevent auto-dismiss
                .setNegativeButton("ביטול", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name     = etName.getText()     != null ? etName.getText().toString().trim()     : "";
            String email    = etEmail.getText()    != null ? etEmail.getText().toString().trim()    : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            String role     = rgRole.getCheckedRadioButtonId() == R.id.rbParent ? "הורה" : "ילד/ה";

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "יש למלא את כל השדות", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, "הסיסמה חייבת להכיל לפחות 6 תווים", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            createFamilyMemberAccount(name, email, password, role, dialog);
        }));

        dialog.show();
    }

    private void createFamilyMemberAccount(String name, String email,
                                            String password, String role,
                                            AlertDialog dialog) {
        // Use a secondary FirebaseApp so the parent's auth session is not replaced
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance("member_creation");
        } catch (IllegalStateException e) {
            FirebaseOptions opts = FirebaseApp.getInstance().getOptions();
            secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), opts, "member_creation");
        }

        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    String newUid = result.getUser().getUid();
                    secondaryAuth.signOut();   // sign out of secondary immediately

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name",       name);
                    userData.put("email",      email);
                    userData.put("role",       role);
                    userData.put("familyCode", userFamilyCode);
                    userData.put("birthDate",  "");
                    userData.put("imageUri",   "");

                    db.collection("users").document(newUid).set(userData)
                            .addOnSuccessListener(v -> {
                                dialog.dismiss();
                                loadFamilyData(userFamilyCode, currentUid);
                                Toast.makeText(this,
                                        "החשבון עבור " + name + " נוצר בהצלחה",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                Toast.makeText(this,
                                        "שגיאה בשמירת הנתונים: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    Toast.makeText(this,
                            "שגיאה ביצירת החשבון: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

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

    // ── Remove member ─────────────────────────────────────────────────────────

    private void confirmRemoveMember(String memberId, String memberName) {
        new AlertDialog.Builder(this)
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
