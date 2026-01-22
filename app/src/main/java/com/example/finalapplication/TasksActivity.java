package com.example.finalapplication;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.CheckBox;

public class TasksActivity extends BaseActivity {

    private FloatingActionButton fabAddTask;
    private RecyclerView rvTasks;
    private FirebaseFirestore db;
    private String userFamilyCode, userRole, currentUid;
    private TaskAdapter adapter;
    private List<Task> taskList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fabAddTask = findViewById(R.id.fabAddTask);
        rvTasks = findViewById(R.id.rvTasks);

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskList = new ArrayList<>();
        adapter = new TaskAdapter(taskList);
        rvTasks.setAdapter(adapter);

        checkPermissionsAndLoadData();

        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("מטלות הבית");
        }

        markSelectedMenuItem(R.id.nav_tasks);
    }

    private void checkPermissionsAndLoadData() {
        db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                userRole = doc.getString("role");
                userFamilyCode = doc.getString("familyCode");

                // הצגת כפתור הוספה רק להורה
                if ("הורה".equals(userRole)) {
                    fabAddTask.setVisibility(View.VISIBLE);
                }

                loadTasksFromFirestore();
            }
        });
    }

    private void loadTasksFromFirestore() {
        Query query;
        if ("הורה".equals(userRole)) {
            query = db.collection("tasks").whereEqualTo("familyCode", userFamilyCode);
        } else {
            query = db.collection("tasks")
                    .whereEqualTo("familyCode", userFamilyCode)
                    .whereEqualTo("assignedToUid", currentUid);
        }

        query.addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                taskList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Task task = doc.toObject(Task.class);
                    task.setTaskId(doc.getId()); // שומרים את ה-ID כדי שנוכל למחוק/לעדכן
                    taskList.add(task);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        builder.setView(dialogView);

        TextInputEditText etTaskName = dialogView.findViewById(R.id.etTaskName);
        Spinner spinnerChildren = dialogView.findViewById(R.id.spinnerChildren);
        Button btnSetDate = dialogView.findViewById(R.id.btnSetDate);
        TextView tvSelectedDateTime = dialogView.findViewById(R.id.tvSelectedDateTime);

        ArrayList<String> childNames = new ArrayList<>();
        ArrayList<String> childIds = new ArrayList<>();

        db.collection("users")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("role", "ילד/ה")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        childNames.add(doc.getString("name"));
                        childIds.add(doc.getId());
                    }
                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, childNames);
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerChildren.setAdapter(spinnerAdapter);
                });

        final String[] finalDateTime = {""};
        btnSetDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = day + "/" + (month + 1) + "/" + year;
                new TimePickerDialog(this, (view1, hour, minute) -> {
                    finalDateTime[0] = date + " " + String.format("%02d:%02d", hour, minute);
                    tvSelectedDateTime.setText("זמן נבחר: " + finalDateTime[0]);
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        builder.setPositiveButton("שמור", (dialog, which) -> {
            String title = etTaskName.getText().toString();
            int index = spinnerChildren.getSelectedItemPosition();
            if (!title.isEmpty() && index != -1 && !finalDateTime[0].isEmpty()) {
                saveTask(title, childIds.get(index), childNames.get(index), finalDateTime[0]);
            } else {
                Toast.makeText(this, "נא למלא את כל הפרטים", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private void saveTask(String title, String cUid, String cName, String time) {
        Map<String, Object> task = new HashMap<>();
        task.put("taskName", title);
        task.put("assignedToUid", cUid);
        task.put("assignedToName", cName);
        task.put("dateTime", time);
        task.put("familyCode", userFamilyCode);
        task.put("isDone", false);

        db.collection("tasks").add(task).addOnSuccessListener(ref -> {
            Toast.makeText(this, "משימה הוקצתה ל" + cName, Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteTask(String taskId) {
        db.collection("tasks").document(taskId).delete();
    }

    // --- Adapter ---
// --- Adapter המעודכן עם חסימת מחיקה למי שלא בעל המשימה ---
    private class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
        private List<Task> list;
        public TaskAdapter(List<Task> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Task task = list.get(position);
            holder.tvTitle.setText(task.getTaskName());
            holder.tvAssignee.setText("למי: " + task.getAssignedToName());
            holder.tvTime.setText(task.getDateTime());

            // --- לוגיקה לפי תפקיד משתמש ---

            if ("הורה".equals(userRole)) {
                // תצוגת הורה: מסתירים את הצ'קבוקס לחלוטין
                holder.cbDone.setVisibility(View.GONE);

                // לחיצה ארוכה להורה = מחיקת משימה (כי נעשתה טעות או ביטול)
                holder.itemView.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(TasksActivity.this)
                            .setTitle("מחיקת משימה")
                            .setMessage("האם למחוק את המשימה הזו מהרשימה?")
                            .setPositiveButton("מחק", (dialog, which) -> deleteTask(task.getTaskId()))
                            .setNegativeButton("ביטול", null)
                            .show();
                    return true;
                });

            } else {
                // תצוגת ילד: מראים צ'קבוקס
                holder.cbDone.setVisibility(View.VISIBLE);
                holder.cbDone.setOnCheckedChangeListener(null);
                holder.cbDone.setChecked(false);

                // לחיצה על הצ'קבוקס לילד = אישור ביצוע
                holder.cbDone.setOnClickListener(v -> {
                    new AlertDialog.Builder(TasksActivity.this)
                            .setTitle("סיום משימה")
                            .setMessage("ביצעת את המשימה?")
                            .setPositiveButton("כן, סיימתי!", (dialog, which) -> deleteTask(task.getTaskId()))
                            .setNegativeButton("ביטול", (dialog, which) -> holder.cbDone.setChecked(false))
                            .show();
                });

                // הילד לא יכול למחוק בלחיצה ארוכה, רק לסמן V
                holder.itemView.setOnLongClickListener(null);
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvAssignee, tvTime;
            CheckBox cbDone;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskTitle);
                tvAssignee = itemView.findViewById(R.id.tvTaskAssignee);
                tvTime = itemView.findViewById(R.id.tvTaskTime);
                cbDone = itemView.findViewById(R.id.cbTaskDone);
            }
        }
    }
}