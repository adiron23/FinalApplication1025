package com.example.finalapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewAndBind(R.layout.activity_shopping_list);

        db = FirebaseFirestore.getInstance();

        rvShopping   = findViewById(R.id.rvShopping);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        rvShopping.setLayoutManager(new LinearLayoutManager(this));

        etNewItem     = findViewById(R.id.etNewItem);
        addRow        = findViewById(R.id.addRow);
        btnClearBought = findViewById(R.id.btnClearBought);
        tvEmpty       = findViewById(R.id.tvEmpty);

        adapter = new ShoppingAdapter(new ArrayList<>(), false, new ShoppingAdapter.ShoppingListener() {
            @Override
            public void onCheckedChanged(ShoppingItem item, boolean checked) {
                if (item.getId() != null) {
                    db.collection("shopping_lists").document(item.getId())
                            .update("checked", checked);
                }
            }

            @Override
            public void onDeleteRequested(ShoppingItem item) {
                new AlertDialog.Builder(ShoppingListActivity.this)
                        .setTitle("מחיקת פריט")
                        .setMessage("למחוק את \"" + item.getName() + "\"?")
                        .setPositiveButton("מחק", (d, w) -> deleteItem(item))
                        .setNegativeButton("ביטול", null)
                        .show();
            }
        });
        rvShopping.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(android.R.color.holo_blue_bright);
        swipeRefresh.setOnRefreshListener(() -> {
            if (familyCode != null) listenToShoppingList();
            else swipeRefresh.setRefreshing(false);
        });

        loadUserDataAndListen();

        findViewById(R.id.btnAddItem).setOnClickListener(v -> addItem());
        btnClearBought.setOnClickListener(v -> clearBoughtItems());
    }

    private void loadUserDataAndListen() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                familyCode = doc.getString("familyCode");
                String name = doc.getString("name");
                if (name != null) userName = name;

                boolean isParent = "הורה".equals(doc.getString("role"));
                adapter.setIsParent(isParent);

                if (isParent) {
                    addRow.setVisibility(View.VISIBLE);
                    btnClearBought.setVisibility(View.VISIBLE);
                }

                if (familyCode != null) {
                    loadFamilyMembers();
                    listenToShoppingList();
                }
            }
        });
    }

    private void listenToShoppingList() {
        db.collection("shopping_lists")
                .whereEqualTo("familyCode", familyCode)
                .addSnapshotListener((value, error) -> {
                    swipeRefresh.setRefreshing(false);
                    if (error != null || value == null) return;
                    List<ShoppingItem> newItems = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ShoppingItem item = doc.toObject(ShoppingItem.class);
                        if (item != null) newItems.add(item);
                    }
                    adapter.updateItems(newItems);
                    tvEmpty.setVisibility(newItems.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void loadFamilyMembers() {
        db.collection("users")
                .whereEqualTo("familyCode", familyCode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
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
                });
    }

    private void addItem() {
        String name = etNewItem.getText().toString().trim();
        if (name.isEmpty()) return;
        if (familyCode == null) {
            Toast.makeText(this, "מזהה משפחה חסר, נסה שוב", Toast.LENGTH_SHORT).show();
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
                .setPositiveButton("הוסף", (d, w) -> {
                    int idx = spinner.getSelectedItemPosition();
                    String assignedToUid  = memberUids.get(idx);
                    String assignedToName = "כולם".equals(memberNames.get(idx)) ? "" : memberNames.get(idx);
                    String id = UUID.randomUUID().toString();
                    ShoppingItem item = new ShoppingItem(id, name, familyCode, userName, assignedToUid, assignedToName);
                    db.collection("shopping_lists").document(id).set(item)
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    etNewItem.setText("");
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void deleteItem(ShoppingItem item) {
        db.collection("shopping_lists").document(item.getId()).delete()
                .addOnFailureListener(e -> Toast.makeText(this,
                        "שגיאה במחיקה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void clearBoughtItems() {
        if (familyCode == null) return;
        db.collection("shopping_lists")
                .whereEqualTo("familyCode", familyCode)
                .whereEqualTo("checked", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "אין פריטים שנקנו", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
