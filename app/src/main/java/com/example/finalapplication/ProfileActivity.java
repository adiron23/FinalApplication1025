package com.example.finalapplication;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private static final String[] AVATAR_NAMES = {
            "duck", "gorilla", "hen", "sloth",
            "penguin", "panda", "monkey", "sealion", "koala"
    };

    private LinearLayout familyContainer;
    private ImageView    ivAvatar;
    private TextView     tvUserName, tvUserEmail, tvUserBirthDate, tvUserRole, tvFamilyNameTitle;
    private Button       btnInviteMember;
    private FirebaseFirestore db;

    private String currentUid, currentUserName, currentEmail, currentBirthDate;
    private String userFamilyCode, currentImageUri;
    private boolean isCurrentUserParent = false;

    private Uri       pendingImageUri;
    private String    pendingAvatarName;
    private ImageView dialogAvatarPreview;
    private Uri       cameraPhotoUri;

    private ActivityResultLauncher<Uri>      cameraLauncher;
    private ActivityResultLauncher<String[]> galleryLauncher;

    // מאתחל את המסך: מגדיר משגרי מצלמה וגלריה, מחבר views וטוען פרופיל משתמש
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewAndBind(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean success) {
                        if (Boolean.TRUE.equals(success) && cameraPhotoUri != null) {
                            pendingImageUri   = cameraPhotoUri;
                            pendingAvatarName = null;
                            if (dialogAvatarPreview != null)
                                dialogAvatarPreview.setImageURI(pendingImageUri);
                        }
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            try {
                                ProfileActivity.this.getContentResolver().takePersistableUriPermission(
                                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (Exception ignored) {}
                            pendingImageUri   = uri;
                            pendingAvatarName = null;
                            if (dialogAvatarPreview != null)
                                dialogAvatarPreview.setImageURI(uri);
                        }
                    }
                });

        initViews();

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUid = firebaseUser.getUid();
            loadUserProfile(currentUid);
        }
    }

    // מחבר את כל ה-views למשתנים ומגדיר כפתורי התנתקות ועריכת פרופיל
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
        if (btnLogout != null) btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileActivity.this.logoutUser();
            }
        });

        Button btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileActivity.this.showEditProfileDialog();
            }
        });
    }

    // טוען את פרטי המשתמש מ-Firestore ומציג אותם במסך, כולל תמונה ופרטי משפחה
    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        if (!doc.exists()) return;

                        currentUserName  = doc.getString("name")       != null ? doc.getString("name")       : "";
                        currentEmail     = doc.getString("email")      != null ? doc.getString("email")      : "";
                        currentBirthDate = doc.getString("birthDate")  != null ? doc.getString("birthDate")  : "";
                        currentImageUri  = doc.getString("imageUri")   != null ? doc.getString("imageUri")   : "";
                        String role      = doc.getString("role")       != null ? doc.getString("role")       : "";
                        userFamilyCode   = doc.getString("familyCode") != null ? doc.getString("familyCode") : "";

                        tvUserName.setText(currentUserName);
                        tvUserEmail.setText(currentEmail);
                        tvUserBirthDate.setText(currentBirthDate.isEmpty() ? "—" : currentBirthDate);
                        tvUserRole.setText(role.isEmpty() ? "—" : role);

                        applyAvatarToView(currentImageUri, ivAvatar);

                        if (!userFamilyCode.isEmpty()) {
                            isCurrentUserParent = "הורה".equals(role);
                            ProfileActivity.this.loadFamilyData(userFamilyCode, uid);
                            if (isCurrentUserParent) {
                                btnInviteMember.setVisibility(View.VISIBLE);
                                btnInviteMember.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ProfileActivity.this.showAddMemberChooser();
                                    }
                                });
                            }
                        }
                    }
                });
    }

    // מציג תמונת פרופיל ב-ImageView – תומך ב-URI מהגלריה/מצלמה או שם אווטאר מ-drawable
    private void applyAvatarToView(String value, ImageView target) {
        if (value == null || value.isEmpty()) return;
        if (value.startsWith("content://") || value.startsWith("file://")) {
            try { target.setImageURI(Uri.parse(value)); }
            catch (Exception ignored) {}
        } else {
            int resId = getResources().getIdentifier(value, "drawable", getPackageName());
            if (resId != 0) {
                target.setPadding(0, 0, 0, 0);
                target.setScaleType(ImageView.ScaleType.CENTER_CROP);
                target.setImageResource(resId);
            }
        }
    }

    // טוען את שם המשפחה וכל חברי המשפחה מ-Firestore ומציג אותם בכרטיסיות (הורים קודם)
    private void loadFamilyData(String familyCode, String uid) {
        db.collection("families").document(familyCode).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        if (doc.exists()) {
                            tvFamilyNameTitle.setText("משפחת " + doc.getString("familyName") + ":");
                        }
                    }
                });

        db.collection("users").whereEqualTo("familyCode", familyCode).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        if (familyContainer == null) return;
                        familyContainer.removeAllViews();

                        List<QueryDocumentSnapshot> parents  = new ArrayList<>();
                        List<QueryDocumentSnapshot> children = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            if ("הורה".equals(doc.getString("role"))) parents.add(doc);
                            else children.add(doc);
                        }

                        for (QueryDocumentSnapshot doc : parents)  ProfileActivity.this.addFamilyCard(doc, uid);
                        for (QueryDocumentSnapshot doc : children) ProfileActivity.this.addFamilyCard(doc, uid);
                    }
                });
    }

    // יוצר ומוסיף כרטיסיית חבר משפחה לתצוגה, כולל תמונה/אות, שם, תפקיד וכפתור הסרה (להורה בלבד)
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
            ivAvatarImage.setVisibility(View.VISIBLE);
            tvAvatarLetter.setVisibility(View.GONE);
            applyAvatarToView(memberImage, ivAvatarImage);
        } else {
            tvAvatarLetter.setText(memberName.isEmpty() ? "?" : String.valueOf(memberName.charAt(0)).toUpperCase());
            tvAvatarLetter.getBackground().setTint(avatarColor);
        }

        tvName.setText(memberId.equals(currentUserId) ? memberName + " (אני)" : memberName);
        tvRole.setText(memberRole);

        if (isCurrentUserParent && !memberId.equals(currentUserId)) {
            btnRemove.setVisibility(View.VISIBLE);
            final String finalMemberId   = memberId;
            final String finalMemberName = memberName;
            btnRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ProfileActivity.this.confirmRemoveMember(finalMemberId, finalMemberName);
                }
            });
        }

        familyContainer.addView(card);
    }

    // פותח דיאלוג עריכת פרופיל עם שדות שם, תאריך לידה, אימייל, סיסמה ובחירת תמונה
    private void showEditProfileDialog() {
        pendingImageUri   = null;
        pendingAvatarName = null;

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);

        dialogAvatarPreview = view.findViewById(R.id.ivEditAvatar);
        Button   btnPickImage = view.findViewById(R.id.btnPickProfileImage);
        EditText etName       = view.findViewById(R.id.etEditName);
        EditText etBirth      = view.findViewById(R.id.etEditBirth);
        EditText etEmail      = view.findViewById(R.id.etEditEmail);
        EditText etPassword   = view.findViewById(R.id.etEditPassword);

        etName.setText(currentUserName);
        etBirth.setText(currentBirthDate);
        etEmail.setText(currentEmail);
        applyAvatarToView(currentImageUri, dialogAvatarPreview);

        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileActivity.this.showImageSourceChooser();
            }
        });

        // פותח בורר תאריך בלחיצה על שדה תאריך הלידה
        etBirth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();
                new DatePickerDialog(ProfileActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker dp, int year, int month, int day) {
                                etBirth.setText(day + "/" + (month + 1) + "/" + year);
                            }
                        },
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                ).show();
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("עריכת פרופיל")
                .setView(view)
                .setPositiveButton("שמור", null)
                .setNegativeButton("ביטול", null)
                .setNeutralButton("שלח קישור לאיפוס סיסמה", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        ProfileActivity.this.sendPasswordResetEmail();
                    }
                })
                .create();

        // מאמת קלט ושומר שינויים בלחיצה על "שמור"
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String newName     = etName.getText().toString().trim();
                        String newBirth    = etBirth.getText().toString().trim();
                        String newEmail    = etEmail.getText().toString().trim();
                        String newPassword = etPassword.getText().toString().trim();

                        if (newName.isEmpty() || newEmail.isEmpty()) {
                            Toast.makeText(ProfileActivity.this, "שם ואימייל הם שדות חובה", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (!newPassword.isEmpty() && newPassword.length() < 6) {
                            Toast.makeText(ProfileActivity.this, "הסיסמה חייבת להכיל לפחות 6 תווים", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ProfileActivity.this.saveProfileChanges(newName, newBirth, newEmail, newPassword);
                        dialog.dismiss();
                    }
                });
            }
        });

        dialog.show();
    }

    // מציג דיאלוג לבחירת מקור תמונה: מצלמה, גלריה או אווטאר מוכן
    private void showImageSourceChooser() {
        String[] options = {"צלם תמונה", "בחר מהגלריה", "אווטאר מוכן"};
        new AlertDialog.Builder(this)
                .setTitle("בחר תמונת פרופיל")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0)      ProfileActivity.this.launchCamera();
                        else if (which == 1) ProfileActivity.this.launchGallery();
                        else                 ProfileActivity.this.showAvatarPicker();
                    }
                })
                .show();
    }

    // יוצר קובץ תמונה זמני ופותח את מצלמת המכשיר לצילום
    private void launchCamera() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir  = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File photoFile   = File.createTempFile("PROFILE_" + timeStamp, ".jpg", storageDir);
            cameraPhotoUri   = FileProvider.getUriForFile(this,
                    "com.example.finalapplication.fileprovider", photoFile);
            cameraLauncher.launch(cameraPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "שגיאה ביצירת קובץ תמונה", Toast.LENGTH_SHORT).show();
        }
    }

    // פותח בורר תמונות מהגלריה
    private void launchGallery() {
        galleryLauncher.launch(new String[]{"image/*"});
    }

    // מציג גלילה אופקית של אווטארים לבחירה ומעדכן את התצוגה המקדימה בדיאלוג
    private void showAvatarPicker() {
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setPadding(16, 24, 16, 16);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(row);

        int sizePx   = (int) (getResources().getDisplayMetrics().density * 80);
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

            final String name   = avatarName;
            final int    resId2 = resId;
            cell.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pendingAvatarName = name;
                    pendingImageUri   = null;
                    if (dialogAvatarPreview != null) dialogAvatarPreview.setImageResource(resId2);
                    if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                }
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

    // שומר שינויי פרופיל ב-Firestore ומעדכן אימייל/סיסמה ב-Firebase Auth במידת הצורך
    private void saveProfileChanges(String newName, String newBirth, String newEmail, String newPassword) {
        boolean nameChanged = !newName.equals(currentUserName);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name",      newName);
        updates.put("birthDate", newBirth);
        updates.put("email",     newEmail);

        if (pendingAvatarName != null) {
            updates.put("imageUri", pendingAvatarName);
        } else if (pendingImageUri != null) {
            updates.put("imageUri", pendingImageUri.toString());
        }

        final String oldName = currentUserName;

        db.collection("users").document(currentUid).update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        if (nameChanged && !userFamilyCode.isEmpty()) {
                            ProfileActivity.this.propagateNameChange(oldName, newName);
                        }

                        currentUserName  = newName;
                        currentEmail     = newEmail;
                        currentBirthDate = newBirth;
                        tvUserName.setText(newName);
                        tvUserEmail.setText(newEmail);
                        tvUserBirthDate.setText(newBirth.isEmpty() ? "—" : newBirth);

                        if (pendingAvatarName != null) {
                            currentImageUri   = pendingAvatarName;
                            applyAvatarToView(pendingAvatarName, ivAvatar);
                            pendingAvatarName = null;
                        } else if (pendingImageUri != null) {
                            currentImageUri = pendingImageUri.toString();
                            ivAvatar.setPadding(0, 0, 0, 0);
                            ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            ivAvatar.setImageURI(pendingImageUri);
                            pendingImageUri = null;
                        }

                        if (!userFamilyCode.isEmpty()) ProfileActivity.this.loadFamilyData(userFamilyCode, currentUid);
                        Toast.makeText(ProfileActivity.this, "הפרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileActivity.this, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && !newEmail.equals(currentEmail)) {
            firebaseUser.updateEmail(newEmail)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ProfileActivity.this, "שגיאה בעדכון אימייל: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }

        if (!newPassword.isEmpty() && firebaseUser != null) {
            firebaseUser.updatePassword(newPassword)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void v) {
                            Toast.makeText(ProfileActivity.this, "הסיסמה עודכנה", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ProfileActivity.this, "שגיאה בעדכון סיסמה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    // שולח מייל איפוס סיסמה לכתובת האימייל הנוכחית של המשתמש
    private void sendPasswordResetEmail() {
        if (currentEmail.isEmpty()) {
            Toast.makeText(this, "לא נמצאה כתובת אימייל", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseAuth.getInstance()
                .sendPasswordResetEmail(currentEmail)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        new AlertDialog.Builder(ProfileActivity.this)
                                .setTitle("קישור נשלח")
                                .setMessage("קישור לאיפוס סיסמה נשלח לכתובת:\n" + currentEmail)
                                .setPositiveButton("אישור", null)
                                .show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // מעדכן את שם המשתמש בכל המשימות ופריטי הקנייה המשויכים אליו אחרי שינוי שם
    private void propagateNameChange(String oldName, String newName) {
        db.collection("tasks")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("assignedToUid", currentUid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snap) {
                        for (QueryDocumentSnapshot doc : snap)
                            doc.getReference().update("assignedToName", newName);
                    }
                });

        db.collection("shopping_lists")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("assignedToUid", currentUid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snap) {
                        for (QueryDocumentSnapshot doc : snap)
                            doc.getReference().update("assignedToName", newName);
                    }
                });

        db.collection("shopping_lists")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("createdBy", oldName)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot snap) {
                        for (QueryDocumentSnapshot doc : snap)
                            doc.getReference().update("createdBy", newName);
                    }
                });
    }

    // מציג דיאלוג לבחירה בין שליחת הזמנה ליצירת חשבון חדש עבור חבר משפחה
    private void showAddMemberChooser() {
        String[] options = {"שלח הזמנה (קישור / קוד)", "צור חשבון חדש עבורם"};
        new AlertDialog.Builder(this)
                .setTitle("הוסף חבר משפחה")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0) ProfileActivity.this.showInviteDialog(userFamilyCode);
                        else            ProfileActivity.this.showCreateMemberDialog();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // פותח דיאלוג ליצירת חשבון חדש לחבר משפחה עם שם, אימייל, סיסמה ותפקיד
    private void showCreateMemberDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_member, null);

        com.google.android.material.textfield.TextInputEditText etName     = view.findViewById(R.id.etMemberName);
        com.google.android.material.textfield.TextInputEditText etEmail    = view.findViewById(R.id.etMemberEmail);
        com.google.android.material.textfield.TextInputEditText etPassword = view.findViewById(R.id.etMemberPassword);
        RadioGroup rgRole = view.findViewById(R.id.rgRole);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("יצירת חשבון חבר משפחה")
                .setView(view)
                .setPositiveButton("צור חשבון", null)
                .setNegativeButton("ביטול", null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name     = etName.getText()     != null ? etName.getText().toString().trim()     : "";
                        String email    = etEmail.getText()    != null ? etEmail.getText().toString().trim()    : "";
                        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
                        String role     = rgRole.getCheckedRadioButtonId() == R.id.rbParent ? "הורה" : "ילד/ה";

                        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                            Toast.makeText(ProfileActivity.this, "יש למלא את כל השדות", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (password.length() < 6) {
                            Toast.makeText(ProfileActivity.this, "הסיסמה חייבת להכיל לפחות 6 תווים", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        ProfileActivity.this.createFamilyMemberAccount(name, email, password, role, dialog);
                    }
                });
            }
        });

        dialog.show();
    }

    // יוצר חשבון Firebase Auth חדש לחבר משפחה דרך אפליקציה משנית (כדי לא להתנתק מהמשתמש הנוכחי) ושומר את פרטיו ב-Firestore
    private void createFamilyMemberAccount(String name, String email,
                                           String password, String role,
                                           AlertDialog dialog) {
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance("member_creation");
        } catch (IllegalStateException e) {
            FirebaseOptions opts = FirebaseApp.getInstance().getOptions();
            secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), opts, "member_creation");
        }

        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        String newUid = result.getUser().getUid();
                        secondaryAuth.signOut();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name",       name);
                        userData.put("email",      email);
                        userData.put("role",       role);
                        userData.put("familyCode", userFamilyCode);
                        userData.put("birthDate",  "");
                        userData.put("imageUri",   "");

                        db.collection("users").document(newUid).set(userData)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void v) {
                                        dialog.dismiss();
                                        ProfileActivity.this.loadFamilyData(userFamilyCode, currentUid);
                                        Toast.makeText(ProfileActivity.this,
                                                "החשבון עבור " + name + " נוצר בהצלחה",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                        Toast.makeText(ProfileActivity.this,
                                                "שגיאה בשמירת הנתונים: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(ProfileActivity.this,
                                "שגיאה ביצירת החשבון: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // מציג דיאלוג עם קישור הצטרפות למשפחה – מאפשר שיתוף או העתקה ללוח
    private void showInviteDialog(String familyCode) {
        String link = "familyapp://join?code=" + familyCode;

        new AlertDialog.Builder(this)
                .setTitle("הוסף חבר משפחה")
                .setMessage("שתף את הקישור הבא:\n\n" + link + "\n\nקוד ידני: " + familyCode)
                .setPositiveButton("שתף קישור", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT,
                                "הצטרף למשפחה שלנו באפליקציה! לחץ על הקישור: " + link);
                        startActivity(Intent.createChooser(shareIntent, "שתף קישור הצטרפות"));
                    }
                })
                .setNeutralButton("העתק קישור", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText("family_link", link));
                        Toast.makeText(ProfileActivity.this, "הקישור הועתק", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("סגור", null)
                .show();
    }

    // מציג דיאלוג אישור לפני הסרת חבר משפחה
    private void confirmRemoveMember(String memberId, String memberName) {
        new AlertDialog.Builder(this)
                .setTitle("הסרת חבר משפחה")
                .setMessage("להסיר את " + memberName + " מהמשפחה?")
                .setPositiveButton("הסר", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        ProfileActivity.this.removeMember(memberId);
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // מסיר חבר משפחה ב-Firestore על ידי מחיקת קוד המשפחה והתפקיד שלו
    private void removeMember(String memberId) {
        db.collection("users").document(memberId)
                .update("familyCode", "", "role", "")
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        Toast.makeText(ProfileActivity.this, "החבר הוסר מהמשפחה", Toast.LENGTH_SHORT).show();
                        ProfileActivity.this.loadFamilyData(userFamilyCode, currentUid);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}