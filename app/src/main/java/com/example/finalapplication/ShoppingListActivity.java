package com.example.finalapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ShoppingListActivity extends BaseActivity {

    private EditText etNewItem;
    private LinearLayout addRow;
    private Button btnClearBought;
    private RecyclerView rvShopping;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private ShoppingAdapter adapter;
    private FirebaseFirestore db;
    private String familyCode;
    private String userName = "בן משפחה";
    private final List<String> memberNames = new ArrayList<>();
    private final List<String> memberUids  = new ArrayList<>();

    // מאתחל את המסך: מחבר views, מגדיר אדפטר עם האזנה לסימון ומחיקה, וטוען נתוני משתמש
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewAndBind(R.layout.activity_shopping_list);

        db = FirebaseFirestore.getInstance();

        rvShopping   = findViewById(R.id.rvShopping);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvShopping.setLayoutManager(new LinearLayoutManager(this));

        etNewItem      = findViewById(R.id.etNewItem);
        addRow         = findViewById(R.id.addRow);
        btnClearBought = findViewById(R.id.btnClearBought);
        tvEmpty        = findViewById(R.id.tvEmpty);

        adapter = new ShoppingAdapter(new ArrayList<>(), false, new ShoppingAdapter.ShoppingListener() {
            // מעדכן מצב סימון הפריט ב-Firestore
            @Override
            public void onCheckedChanged(ShoppingItem item, boolean checked) {
                if (item.getId() != null) {
                    db.collection("shopping_lists").document(item.getId())
                            .update("checked", checked);
                }
            }

            // מציג דיאלוג אישור מחיקת פריט בלחיצה ארוכה
            @Override
            public void onDeleteRequested(ShoppingItem item) {
                new AlertDialog.Builder(ShoppingListActivity.this)
                        .setTitle("מחיקת פריט")
                        .setMessage("למחוק את \"" + item.getName() + "\"?")
                        .setPositiveButton("מחק", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                ShoppingListActivity.this.deleteItem(item);
                            }
                        })
                        .setNegativeButton("ביטול", null)
                        .show();
            }
        });
        rvShopping.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(android.R.color.holo_blue_bright);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (familyCode != null) ShoppingListActivity.this.listenToShoppingList();
                else swipeRefresh.setRefreshing(false);
            }
        });

        loadUserDataAndListen();

        findViewById(R.id.btnAddItem).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShoppingListActivity.this.addItem();
            }
        });
        btnClearBought.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShoppingListActivity.this.clearBoughtItems();
            }
        });
    }

    // טוען את פרטי המשתמש מ-Firestore, קובע הרשאות תצוגה לפי תפקיד/גיל ומתחיל האזנה לרשימה
    private void loadUserDataAndListen() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
                        if (doc.exists()) {
                            familyCode = doc.getString("familyCode");
                            String name = doc.getString("name");
                            if (name != null) userName = name;

                            boolean isParent = "הורה".equals(doc.getString("role"));
                            adapter.setIsParent(isParent);

                            if (isParent) {
                                addRow.setVisibility(View.VISIBLE);
                                btnClearBought.setVisibility(View.VISIBLE);
                            } else if (ShoppingListActivity.this.ageFromBirthDate(doc.getString("birthDate")) >= 12) {
                                addRow.setVisibility(View.VISIBLE);
                            }

                            if (familyCode != null) {
                                ShoppingListActivity.this.loadFamilyMembers();
                                ShoppingListActivity.this.listenToShoppingList();
                            }
                        }
                    }
                });
    }

    // מאזין בזמן אמת לשינויים ברשימת הקניות של המשפחה ומעדכן את האדפטר
    private void listenToShoppingList() {
        db.collection("shopping_lists")
                .whereEqualTo("familyCode", familyCode)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot value, FirebaseFirestoreException error) {
                        swipeRefresh.setRefreshing(false);
                        if (error != null || value == null) return;
                        List<ShoppingItem> newItems = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            ShoppingItem item = doc.toObject(ShoppingItem.class);
                            if (item != null) newItems.add(item);
                        }
                        adapter.updateItems(newItems);
                        tvEmpty.setVisibility(newItems.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    // טוען את רשימת חברי המשפחה מ-Firestore לצורך הקצאת פריטים
    private void loadFamilyMembers() {
        db.collection("users")
                .whereEqualTo("familyCode", familyCode)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        memberNames.clear();
                        memberUids.clear();
                        memberNames.add("כולם");
                        memberUids.add("");
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String n = doc.getString("name");
                            if (n != null) {
                                memberNames.add(n);
                                memberUids.add(doc.getId());
                            }
                        }
                    }
                });
    }

    // פותח דיאלוג לבחירת נמען ומוסיף את הפריט החדש ל-Firestore
    private void addItem() {
        String name = etNewItem.getText().toString().trim();
        if (name.isEmpty()) return;
        if (familyCode == null) {
            Toast.makeText(this, "מזהה משפחה חסר, נסה שוב", Toast.LENGTH_SHORT).show();
            return;
        }
        if (memberUids.isEmpty()) {
            Toast.makeText(this, "טוען חברי משפחה, נסה שנית", Toast.LENGTH_SHORT).show();
            return;
        }

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, memberNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(this)
                .setTitle("הוספת פריט")
                .setMessage("מיועד למי?")
                .setView(spinner)
                .setPositiveButton("הוסף", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        int idx = spinner.getSelectedItemPosition();
                        String assignedToUid  = memberUids.get(idx);
                        String assignedToName = "כולם".equals(memberNames.get(idx)) ? "" : memberNames.get(idx);
                        String id = UUID.randomUUID().toString();
                        ShoppingItem item = new ShoppingItem(id, name, familyCode, userName, assignedToUid, assignedToName);
                        db.collection("shopping_lists").document(id).set(item)
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(ShoppingListActivity.this,
                                                "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                        etNewItem.setText("");
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // מוחק פריט בודד מ-Firestore
    private void deleteItem(ShoppingItem item) {
        db.collection("shopping_lists").document(item.getId()).delete()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ShoppingListActivity.this,
                                "שגיאה במחיקה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // מחשב את גיל המשתמש מתאריך הלידה שלו
    private int ageFromBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isEmpty()) return -1;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
            Calendar birth = Calendar.getInstance();
            birth.setTime(sdf.parse(birthDate));
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--;
            return age;
        } catch (Exception e) {
            return -1;
        }
    }

    // מוחק מ-Firestore את כל הפריטים שסומנו כנקנו
    private void clearBoughtItems() {
        if (familyCode == null) return;
        db.collection("shopping_lists")
                .whereEqualTo("familyCode", familyCode)
                .whereEqualTo("checked", true)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            doc.getReference().delete();
                        }
                        if (querySnapshot.isEmpty()) {
                            Toast.makeText(ShoppingListActivity.this, "אין פריטים שנקנו", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}