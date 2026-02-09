package com.example.finalapplication;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShoppingListActivity extends BaseActivity {

    private EditText etNewItem;
    private ImageButton btnAdd;
    private RecyclerView rvShopping;
    private ShoppingAdapter adapter;
    private List<ShoppingItem> itemList;
    private FirebaseFirestore db;
    private String familyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. קריאה ל-setContentView של BaseActivity (שמנפח את ה-Layout)
        setContentView(R.layout.activity_shopping_list);
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // 2. אתחול הרשימה והאדפטר כבר עכשיו (לפני הגעת הנתונים)
        itemList = new ArrayList<>();
        adapter = new ShoppingAdapter(itemList, item -> deleteItem(item));

        etNewItem = findViewById(R.id.etNewItem);
        btnAdd = findViewById(R.id.btnAddItem);
        rvShopping = findViewById(R.id.rvShopping);

        // 3. חיבור האדפטר מיד כדי למנוע את שגיאת ה-"No adapter attached"
        if (rvShopping != null) {
            rvShopping.setLayoutManager(new LinearLayoutManager(this));
            rvShopping.setAdapter(adapter);
        }

        // 4. טעינת הנתונים
        loadUserDataAndListen();

        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> addItem());
        }

        markSelectedMenuItem(R.id.nav_shopping_list);
    }

    private void loadUserDataAndListen() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    familyCode = doc.getString("familyCode");
                    if (familyCode != null && !familyCode.isEmpty()) {
                        listenToShoppingList();
                    } else {
                        Toast.makeText(this, "יש להצטרף למשפחה תחילה", Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "שגיאה בטעינת נתוני משתמש", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void listenToShoppingList() {
        db.collection("shopping_lists")
                .whereEqualTo("familyCode", familyCode)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return; // טיפול בשגיאות חיבור
                    }
                    if (value != null) {
                        itemList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            ShoppingItem item = doc.toObject(ShoppingItem.class);
                            if (item != null) {
                                itemList.add(item);
                            }
                        }
                        // 5. במקום ליצור אדפטר חדש, אנחנו רק מעדכנים את הקיים
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void addItem() {
        String name = etNewItem.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "נא להזין שם מוצר", Toast.LENGTH_SHORT).show();
            return;
        }
        if (familyCode == null) return;

        String id = UUID.randomUUID().toString();
        ShoppingItem item = new ShoppingItem(id, name, familyCode);

        // הוספה ל-Firestore
        db.collection("shopping_lists").document(id).set(item)
                .addOnSuccessListener(aVoid -> etNewItem.setText(""))
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בהוספה", Toast.LENGTH_SHORT).show());
    }

    private void deleteItem(ShoppingItem item) {
        db.collection("shopping_lists").document(item.getId()).delete();
    }
}