package com.example.finalapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JournalActivity extends BaseActivity {

    private RecyclerView rvJournal;
    private FloatingActionButton fabAddEntry;
    private FirebaseFirestore db;
    private String currentUid, currentName, userFamilyCode, userRole;
    private JournalAdapter adapter;
    private final List<JournalEntry> entries = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy · HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewAndBind(R.layout.activity_journal);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rvJournal   = findViewById(R.id.rvJournal);
        fabAddEntry = findViewById(R.id.fabAddEntry);

        rvJournal.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JournalAdapter(entries);
        rvJournal.setAdapter(adapter);

        loadUserThenEntries();
        fabAddEntry.setOnClickListener(v -> showAddEntryDialog());
    }

    // ── Bootstrap ─────────────────────────────────────────────────────────────

    private void loadUserThenEntries() {
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    currentName    = doc.getString("name");
                    userFamilyCode = doc.getString("familyCode");
                    userRole       = doc.getString("role");
                    listenToEntries();
                });
    }

    // ── Firestore listener ────────────────────────────────────────────────────

    private void listenToEntries() {
        db.collection("journal")
                .whereEqualTo("familyCode", userFamilyCode)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    entries.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        JournalEntry e = doc.toObject(JournalEntry.class);
                        e.setEntryId(doc.getId());
                        entries.add(e);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // ── Add entry dialog ──────────────────────────────────────────────────────

    private void showAddEntryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_journal_entry, null);
        TextInputEditText etText = dialogView.findViewById(R.id.etJournalText);

        new AlertDialog.Builder(this)
                .setTitle("רשומה חדשה")
                .setView(dialogView)
                .setPositiveButton("פרסם", (d, w) -> {
                    String text = etText.getText() != null ? etText.getText().toString().trim() : "";
                    if (text.isEmpty()) {
                        Toast.makeText(this, "נא לכתוב משהו", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveEntry(text);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // ── Firestore writes ──────────────────────────────────────────────────────

    private void saveEntry(String text) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("text",       text);
        entry.put("authorUid",  currentUid);
        entry.put("authorName", currentName != null ? currentName : "");
        entry.put("familyCode", userFamilyCode);
        entry.put("timestamp",  System.currentTimeMillis());
        db.collection("journal").add(entry)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteEntry(String entryId) {
        db.collection("journal").document(entryId).delete()
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "הרשומה נמחקה", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private class JournalAdapter extends RecyclerView.Adapter<JournalAdapter.ViewHolder> {
        private final List<JournalEntry> list;
        JournalAdapter(List<JournalEntry> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.journal_entry_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JournalEntry entry = list.get(position);

            // Avatar: first letter of author name
            String name = entry.getAuthorName() != null && !entry.getAuthorName().isEmpty()
                    ? entry.getAuthorName() : "?";
            holder.tvAvatar.setText(String.valueOf(name.charAt(0)));
            holder.tvAuthor.setText(name);

            // Date
            String dateStr = entry.getTimestamp() > 0
                    ? sdf.format(new Date(entry.getTimestamp())) : "";
            holder.tvDate.setText(dateStr);

            // Text
            holder.tvText.setText(entry.getText());

            // Delete button: own entries always; parents can delete any
            boolean canDelete = currentUid.equals(entry.getAuthorUid())
                    || "הורה".equals(userRole);
            holder.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
            holder.btnDelete.setOnClickListener(v ->
                    new AlertDialog.Builder(JournalActivity.this)
                            .setTitle("מחיקת רשומה")
                            .setMessage("למחוק את הרשומה הזו?")
                            .setPositiveButton("מחק", (d, w) -> deleteEntry(entry.getEntryId()))
                            .setNegativeButton("ביטול", null)
                            .show());
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView  tvAvatar, tvAuthor, tvDate, tvText;
            ImageView btnDelete;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvAvatar  = itemView.findViewById(R.id.tvAuthorAvatar);
                tvAuthor  = itemView.findViewById(R.id.tvAuthorName);
                tvDate    = itemView.findViewById(R.id.tvEntryDate);
                tvText    = itemView.findViewById(R.id.tvEntryText);
                btnDelete = itemView.findViewById(R.id.btnDeleteEntry);
            }
        }
    }
}
