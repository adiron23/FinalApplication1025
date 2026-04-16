package com.example.finalapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TasksActivity extends BaseActivity {

    private FloatingActionButton fabAddTask;
    private RecyclerView rvTasks;
    private LinearLayout filterBar;
    private Spinner spinnerFilter;
    private FirebaseFirestore db;
    private String userFamilyCode, userRole, currentUid;
    private TaskAdapter adapter;
    private final List<Task> taskList     = new ArrayList<>();
    private final List<Task> filteredList = new ArrayList<>();
    private final List<String> filterNames = new ArrayList<>();  // "הכל", child1, …
    private final List<String> filterUids  = new ArrayList<>();  // "",    uid1, …
    private final SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault());

    private static final String EVERYONE       = "";
    private static final String EVERYONE_LABEL = "כולם";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewAndBind(R.layout.activity_tasks);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fabAddTask    = findViewById(R.id.fabAddTask);
        rvTasks       = findViewById(R.id.rvTasks);
        filterBar     = findViewById(R.id.filterBar);
        spinnerFilter = findViewById(R.id.spinnerFilter);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(filteredList);
        rvTasks.setAdapter(adapter);

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        checkPermissionsAndLoadData();
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void checkPermissionsAndLoadData() {
        db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            userRole       = doc.getString("role");
            userFamilyCode = doc.getString("familyCode");
            if ("הורה".equals(userRole)) {
                fabAddTask.setVisibility(View.VISIBLE);
                loadChildrenForFilter();
            }
            loadTasksFromFirestore();
        });
    }

    /** Populates the parent's filter spinner with child names. */
    private void loadChildrenForFilter() {
        db.collection("users")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("role", "ילד/ה")
                .get()
                .addOnSuccessListener(snap -> {
                    filterNames.clear();
                    filterUids.clear();
                    filterNames.add("הכל");
                    filterUids.add("");
                    for (QueryDocumentSnapshot doc : snap) {
                        String name = doc.getString("name");
                        if (name != null) {
                            filterNames.add(name);
                            filterUids.add(doc.getId());
                        }
                    }
                    if (filterNames.size() > 1) {
                        ArrayAdapter<String> fa = new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_item, filterNames);
                        fa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerFilter.setAdapter(fa);
                        spinnerFilter.setOnItemSelectedListener(
                                new android.widget.AdapterView.OnItemSelectedListener() {
                                    @Override public void onItemSelected(
                                            android.widget.AdapterView<?> p, View v, int pos, long id) {
                                        applyFilter(filterUids.get(pos));
                                    }
                                    @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
                                });
                        filterBar.setVisibility(View.VISIBLE);
                    }
                });
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    /**
     * Both parents and children query ALL family tasks.
     * Filtering is done client-side so "everyone" tasks reach all children.
     */
    private void loadTasksFromFirestore() {
        db.collection("tasks")
                .whereEqualTo("familyCode", userFamilyCode)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    taskList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Task task = doc.toObject(Task.class);
                        task.setTaskId(doc.getId());
                        taskList.add(task);
                    }
                    // Undone tasks first, then sort by date ascending
                    taskList.sort((a, b) -> {
                        if (a.isDone() != b.isDone()) return a.isDone() ? 1 : -1;
                        try {
                            String da = a.getDateTime(), db2 = b.getDateTime();
                            if (da == null || da.isEmpty()) return 1;
                            if (db2 == null || db2.isEmpty()) return -1;
                            return sdf.parse(da).compareTo(sdf.parse(db2));
                        } catch (Exception e) { return 0; }
                    });
                    int selPos = spinnerFilter.getSelectedItemPosition();
                    String selectedUid = (filterUids.isEmpty() || selPos < 0 || selPos >= filterUids.size()) ? ""
                            : filterUids.get(selPos);
                    applyFilter(selectedUid);
                });
    }

    /**
     * Filters taskList → filteredList based on role and selected filter.
     *
     * Parent:  show tasks matching the filter spinner selection (empty = show all)
     * Child:   show tasks assigned to them personally OR assigned to "כולם" (uid = "")
     */
    private void applyFilter(String filterUid) {
        filteredList.clear();
        for (Task t : taskList) {
            String assignedUid = t.getAssignedToUid() != null ? t.getAssignedToUid() : "";
            if ("הורה".equals(userRole)) {
                // Parent filter: empty = all; otherwise match specific child uid
                if (filterUid.isEmpty() || filterUid.equals(assignedUid)) {
                    filteredList.add(t);
                }
            } else {
                // Child: see tasks assigned personally OR tasks assigned to everyone
                if (assignedUid.equals(currentUid) || assignedUid.equals(EVERYONE)) {
                    filteredList.add(t);
                }
            }
        }
        adapter.notifyDataSetChanged();
        scheduleAlarmsForMyTasks();
    }

    // ── Alarm scheduling ──────────────────────────────────────────────────────

    private void scheduleAlarmsForMyTasks() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;
        for (Task task : taskList) {
            if (task.isDone()) continue;
            String assignedUid = task.getAssignedToUid() != null ? task.getAssignedToUid() : "";
            // Only schedule alarms for tasks that belong to the current user or "כולם"
            if (!assignedUid.isEmpty() && !assignedUid.equals(currentUid)) continue;
            if (task.getDateTime() == null || task.getDateTime().isEmpty()) continue;
            try {
                Date taskDate = sdf.parse(task.getDateTime());
                if (taskDate == null || !taskDate.after(new Date())) continue;

                Intent intent = new Intent(this, TaskAlarmReceiver.class);
                intent.putExtra("taskName",     task.getTaskName());
                intent.putExtra("taskId",       task.getTaskId());
                intent.putExtra("assignedName", task.getAssignedToName());

                PendingIntent pi = PendingIntent.getBroadcast(this,
                        Math.abs(task.getTaskId().hashCode()),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, taskDate.getTime(), pi);
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, taskDate.getTime(), pi);
                }
            } catch (Exception ignored) {}
        }
    }

    // ── Add task dialog ───────────────────────────────────────────────────────

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        TextInputEditText etTaskName    = dialogView.findViewById(R.id.etTaskName);
        Spinner spinnerChildren         = dialogView.findViewById(R.id.spinnerChildren);
        Button btnSetDate               = dialogView.findViewById(R.id.btnSetDate);
        TextView tvSelectedDateTime     = dialogView.findViewById(R.id.tvSelectedDateTime);

        // "כולם" is always the first option; children are appended after
        ArrayList<String> assigneeNames = new ArrayList<>();
        ArrayList<String> assigneeUids  = new ArrayList<>();
        assigneeNames.add(EVERYONE_LABEL);
        assigneeUids.add(EVERYONE);

        final String[] finalDateTime = {""};
        btnSetDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = day + "/" + (month + 1) + "/" + year;
                new TimePickerDialog(this, (view1, hour, minute) -> {
                    finalDateTime[0] = date + " " + String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    tvSelectedDateTime.setText("זמן נבחר: " + finalDateTime[0]);
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        builder.setPositiveButton("שמור", (dialog, which) -> {
            String title = etTaskName.getText() != null ? etTaskName.getText().toString().trim() : "";
            int idx      = spinnerChildren.getSelectedItemPosition();
            if (title.isEmpty() || idx < 0 || idx >= assigneeUids.size() || finalDateTime[0].isEmpty()) {
                Toast.makeText(this, "נא למלא שם משימה ותאריך", Toast.LENGTH_SHORT).show();
                return;
            }
            saveTask(title, assigneeUids.get(idx), assigneeNames.get(idx), finalDateTime[0]);
        });
        builder.setNegativeButton("ביטול", null);

        // Load children, append after "כולם", then show dialog
        db.collection("users")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("role", "ילד/ה")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        if (name != null) {
                            assigneeNames.add(name);
                            assigneeUids.add(doc.getId());
                        }
                    }
                    ArrayAdapter<String> sAdapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, assigneeNames);
                    sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerChildren.setAdapter(sAdapter);
                    builder.show();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "שגיאה בטעינת ילדים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ── Firestore writes ──────────────────────────────────────────────────────

    private void saveTask(String title, String cUid, String cName, String time) {
        Map<String, Object> task = new HashMap<>();
        task.put("taskName",       title);
        task.put("assignedToUid",  cUid);
        task.put("assignedToName", cName);
        task.put("dateTime",       time);
        task.put("familyCode",     userFamilyCode);
        task.put("isDone",         false);
        db.collection("tasks").add(task)
                .addOnFailureListener(e -> Toast.makeText(this,
                        "שגיאה בשמירת משימה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void markTaskDone(String taskId) {
        db.collection("tasks").document(taskId)
                .update("isDone", true)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "כל הכבוד! המשימה הושלמה.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteTask(String taskId) {
        db.collection("tasks").document(taskId).delete()
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "המשימה נמחקה", Toast.LENGTH_SHORT).show());
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
        private final List<Task> list;
        TaskAdapter(List<Task> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.task_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Task task = list.get(position);

            // ── Text ──────────────────────────────────────────────────────────
            holder.tvTitle.setText(task.getTaskName());
            String assigneeName = task.getAssignedToName();
            holder.tvAssignee.setText("מיועד ל: " +
                    (assigneeName != null && !assigneeName.isEmpty() ? assigneeName : EVERYONE_LABEL));
            holder.tvTime.setText(task.getDateTime() != null ? task.getDateTime() : "");

            // ── Overdue stripe ────────────────────────────────────────────────
            boolean overdue = false;
            if (!task.isDone() && task.getDateTime() != null && !task.getDateTime().isEmpty()) {
                try { overdue = sdf.parse(task.getDateTime()).before(new java.util.Date()); }
                catch (Exception ignored) {}
            }
            holder.priorityStripe.setBackgroundColor(overdue ? 0xFFE53935 : 0xFF6D4C41);
            holder.tvPriorityLabel.setVisibility(View.GONE);

            // ── Done visual ───────────────────────────────────────────────────
            if (task.isDone()) {
                holder.tvTitle.setPaintFlags(
                        holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvTitle.setTextColor(Color.parseColor("#AAAAAA"));
                holder.itemView.setAlpha(0.55f);
            } else {
                holder.tvTitle.setPaintFlags(
                        holder.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvTitle.setTextColor(Color.parseColor("#3E2723"));
                holder.itemView.setAlpha(1f);
            }

            // ── Checkbox — tap to mark done ───────────────────────────────────
            // Determine if this user is allowed to complete this task
            String assignedUid = task.getAssignedToUid() != null ? task.getAssignedToUid() : "";
            boolean isMyTask   = assignedUid.equals(currentUid);
            boolean isEveryone = assignedUid.equals(EVERYONE);
            boolean canDone    = "הורה".equals(userRole) || isMyTask || isEveryone;

            // Suppress listener while programmatically setting state
            holder.cbTaskDone.setOnCheckedChangeListener(null);
            holder.cbTaskDone.setChecked(task.isDone());
            holder.cbTaskDone.setEnabled(canDone && !task.isDone());

            if (canDone && !task.isDone()) {
                holder.cbTaskDone.setOnCheckedChangeListener((btn, isChecked) -> {
                    if (isChecked) markTaskDone(task.getTaskId());
                });
            }

            // ── Long-press: parent deletes ────────────────────────────────────
            if ("הורה".equals(userRole)) {
                holder.itemView.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(TasksActivity.this)
                            .setTitle("מחיקת משימה")
                            .setMessage("למחוק את \"" + task.getTaskName() + "\"?")
                            .setPositiveButton("מחק", (d, w) -> deleteTask(task.getTaskId()))
                            .setNegativeButton("ביטול", null)
                            .show();
                    return true;
                });
            } else {
                holder.itemView.setOnLongClickListener(null);
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView  tvTitle, tvAssignee, tvTime, tvPriorityLabel;
            View      priorityStripe;
            CheckBox  cbTaskDone;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle         = itemView.findViewById(R.id.tvTaskTitle);
                tvAssignee      = itemView.findViewById(R.id.tvTaskAssignee);
                tvTime          = itemView.findViewById(R.id.tvTaskTime);
                tvPriorityLabel = itemView.findViewById(R.id.tvPriorityLabel);
                priorityStripe  = itemView.findViewById(R.id.priorityStripe);
                cbTaskDone      = itemView.findViewById(R.id.cbTaskDone);
            }
        }
    }
}
