package com.example.finalapplication;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private static final String[] AVATAR_NAMES = {
            "duck", "gorilla", "hen", "sloth",
            "penguin", "panda", "monkey", "sealion", "koala"
    };

    private EditText  eTEmail, eTPass, eTName, eTBirth;
    private TextView  tVMsg, btnGoToLogin;
    private Button    createUser, btnSelectImage;
    private ImageView profileImageView;
    private FirebaseAuth      refAuth;
    private FirebaseFirestore db;

    private Uri    selectedImageUri;
    private String selectedAvatarName;
    private Uri    cameraPhotoUri;

    private ActivityResultLauncher<String[]> galleryLauncher;
    private ActivityResultLauncher<Uri>      cameraLauncher;
    private GoogleSignInClient mGoogleSignInClient;

    // בודק בתחילת המסך אם המשתמש כבר מחובר ומנתב אותו בהתאם
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            checkUserStatusAndNavigate(currentUser.getUid());
        }
    }

    // מאתחל את מסך ההרשמה: מגדיר משגרי מצלמה וגלריה, מחבר views, מגדיר כפתורים ובורר תאריך לידה
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                new ActivityResultCallback<Boolean>() {
                    @Override
                    public void onActivityResult(Boolean success) {
                        if (Boolean.TRUE.equals(success) && cameraPhotoUri != null) {
                            selectedImageUri   = cameraPhotoUri;
                            selectedAvatarName = null;
                            profileImageView.setPadding(0, 0, 0, 0);
                            profileImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            profileImageView.setImageURI(selectedImageUri);
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
                                RegisterActivity.this.getContentResolver().takePersistableUriPermission(
                                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (Exception ignored) {}
                            selectedImageUri   = uri;
                            selectedAvatarName = null;
                            profileImageView.setPadding(0, 0, 0, 0);
                            profileImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            profileImageView.setImageURI(uri);
                        }
                    }
                });

        eTEmail          = findViewById(R.id.eTEmail);
        eTPass           = findViewById(R.id.eTPass);
        eTName           = findViewById(R.id.eTName);
        eTBirth          = findViewById(R.id.eTBirth);
        tVMsg            = findViewById(R.id.tVMsg);
        createUser       = findViewById(R.id.createUser);
        btnSelectImage   = findViewById(R.id.btnSelectImage);
        profileImageView = findViewById(R.id.profileImageView);
        btnGoToLogin     = findViewById(R.id.btnGoToLogin);

        refAuth = FirebaseAuth.getInstance();
        db      = FirebaseFirestore.getInstance();

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterActivity.this.showImageSourceChooser();
            }
        });

        createUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterActivity.this.registerUser();
            }
        });

        btnGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LogInActivity.class));
                finish();
            }
        });

        eTBirth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                new DatePickerDialog(RegisterActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int day) {
                                eTBirth.setText(day + "/" + (month + 1) + "/" + year);
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                ).show();
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Button btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_GOOGLE_SIGN_IN);
                }
            });
        }
    }

    // מציג דיאלוג לבחירת מקור תמונת פרופיל: מצלמה, גלריה או אווטאר
    private void showImageSourceChooser() {
        String[] options = {"צלם תמונה", "בחר מהגלריה", "בחר אווטאר"};
        new AlertDialog.Builder(this)
                .setTitle("בחר תמונת פרופיל")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (which == 0)      RegisterActivity.this.launchCamera();
                        else if (which == 1) galleryLauncher.launch(new String[]{"image/*"});
                        else                 RegisterActivity.this.showAvatarPicker();
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

    // מציג גלילה אופקית של אווטארים לבחירה ומעדכן את תמונת הפרופיל בהתאם
    private void showAvatarPicker() {
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setPadding(16, 24, 16, 16);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        scrollView.addView(row);

        int sizePx   = (int) (getResources().getDisplayMetrics().density * 80);
        int marginPx = (int) (getResources().getDisplayMetrics().density * 8);

        AlertDialog[] holder = new AlertDialog[1];

        for (String avatarName : AVATAR_NAMES) {
            int resId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
            if (resId == 0) continue;

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.HORIZONTAL);
            cell.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(marginPx, 0, marginPx, 0);
            cell.setLayoutParams(lp);

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
                    selectedAvatarName = name;
                    selectedImageUri   = null;
                    profileImageView.setPadding(0, 0, 0, 0);
                    profileImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    profileImageView.setImageResource(resId2);
                    if (holder[0] != null) holder[0].dismiss();
                }
            });

            row.addView(cell);
        }

        holder[0] = new AlertDialog.Builder(this)
                .setTitle("בחר אווטאר")
                .setView(scrollView)
                .setNegativeButton("ביטול", null)
                .create();
        holder[0].show();
    }

    // בודק ב-Firestore אם למשתמש קיים קוד משפחה ומנתב למסך הראשי או מסך המשפחה
    private void checkUserStatusAndNavigate(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        if (doc.exists() && doc.getString("familyCode") != null
                                && !doc.getString("familyCode").isEmpty()) {
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        } else {
                            startActivity(new Intent(RegisterActivity.this, FamilyGatewayActivity.class));
                        }
                        finish();
                    }
                });
    }

    // מקבל את תוצאת התחברות Google ומעביר לאימות מול Firebase
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                tVMsg.setText("Google Sign-In נכשל: " + e.getMessage());
            }
        }
    }

    // מבצע אימות Google מול Firebase – אם משתמש חדש שומר ב-Firestore, אחרת מנתב ישירות
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מתחבר...");
        pd.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        pd.dismiss();
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            tVMsg.setText("שגיאה: " + (e != null ? e.getMessage() : "לא ידוע"));
                            return;
                        }
                        FirebaseUser user = task.getResult().getUser();
                        boolean isNew = task.getResult().getAdditionalUserInfo() != null
                                && task.getResult().getAdditionalUserInfo().isNewUser();

                        if (isNew) {
                            String displayName = account.getDisplayName() != null ? account.getDisplayName() : "";
                            String email       = account.getEmail()       != null ? account.getEmail()       : "";
                            RegisterActivity.this.saveGoogleUserToFirestore(user.getUid(), email, displayName);
                        } else {
                            RegisterActivity.this.checkUserStatusAndNavigate(user.getUid());
                        }
                    }
                });
    }

    // שומר משתמש Google חדש ב-Firestore ומעביר למסך המשפחה
    private void saveGoogleUserToFirestore(String uid, String email, String name) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid",        uid);
        userData.put("email",      email);
        userData.put("name",       name);
        userData.put("birthDate",  "");
        userData.put("imageUri",   "");
        userData.put("familyCode", "");
        userData.put("role",       "");

        db.collection("users").document(uid).set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Intent intent = new Intent(RegisterActivity.this, FamilyGatewayActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        tVMsg.setText("שגיאה בשמירת נתונים: " + e.getMessage());
                    }
                });
    }

    // מאמת את שדות הטופס ויוצר משתמש חדש ב-Firebase Auth
    private void registerUser() {
        String email = eTEmail.getText().toString().trim();
        String pass  = eTPass.getText().toString().trim();
        String name  = eTName.getText().toString().trim();
        String birth = eTBirth.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty() || name.isEmpty() || birth.isEmpty()) {
            tVMsg.setText("אנא מלא את כל השדות");
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("יוצר משתמש...");
        pd.show();

        refAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                            RegisterActivity.this.saveUserToFirestore(
                                    task.getResult().getUser().getUid(), email, name, birth, pd);
                        } else {
                            pd.dismiss();
                            Exception e = task.getException();
                            tVMsg.setText("שגיאה: " + (e != null ? e.getMessage() : "לא ידוע"));
                        }
                    }
                });
    }

    // שומר את פרטי המשתמש החדש (כולל תמונה/אווטאר) ב-Firestore ומעביר למסך המשפחה
    private void saveUserToFirestore(String uid, String email, String name,
                                     String birth, ProgressDialog pd) {
        String imageValue = "";
        if (selectedAvatarName != null) {
            imageValue = selectedAvatarName;
        } else if (selectedImageUri != null) {
            imageValue = selectedImageUri.toString();
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid",        uid);
        userData.put("email",      email);
        userData.put("name",       name);
        userData.put("birthDate",  birth);
        userData.put("imageUri",   imageValue);
        userData.put("familyCode", "");
        userData.put("role",       "");

        db.collection("users").document(uid).set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        pd.dismiss();
                        Intent intent = new Intent(RegisterActivity.this, FamilyGatewayActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        tVMsg.setText("שגיאה בשמירת נתונים: " + e.getMessage());
                    }
                });
    }
}