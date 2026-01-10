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
        setContentView(R.layout.activity_shopping_list);
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        itemList = new ArrayList<>();

        etNewItem = findViewById(R.id.etNewItem);
        btnAdd = findViewById(R.id.btnAddItem);
        rvShopping = findViewById(R.id.rvShopping);
        rvShopping.setLayoutManager(new LinearLayoutManager(this));

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                familyCode = doc.getString("familyCode");
                if (familyCode != null && !familyCode.isEmpty()) {
                    listenToShoppingList();
                } else {
                    Toast.makeText(this, "יש להצטרף למשפחה תחילה", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnAdd.setOnClickListener(v -> addItem());
    }

    private void listenToShoppingList() {
        db.collection("shopping_lists")
                .whereEqualTo("familyCode", familyCode)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        itemList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            itemList.add(doc.toObject(ShoppingItem.class));
                        }
                        adapter = new ShoppingAdapter(itemList, item -> deleteItem(item));
                        rvShopping.setAdapter(adapter);
                    }
                });
    }

    private void addItem() {
        String name = etNewItem.getText().toString().trim();
        if (name.isEmpty() || familyCode == null) return;

        String id = UUID.randomUUID().toString();
        ShoppingItem item = new ShoppingItem(id, name, familyCode);
        db.collection("shopping_lists").document(id).set(item);
        etNewItem.setText("");
    }

    private void deleteItem(ShoppingItem item) {
        db.collection("shopping_lists").document(item.getId()).delete();
    }
}