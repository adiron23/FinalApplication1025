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
    private FirebaseFirestore db;
    private String userFamilyCode, userRole, currentUid;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault());

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
        markSelectedMenuItem(R.id.nav_tasks);
    }

    private void checkPermissionsAndLoadData() {
        db.collection("users").document(currentUid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                userRole = doc.getString("role");
                userFamilyCode = doc.getString("familyCode");

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
                long currentTime = System.currentTimeMillis();
                long oneDayInMs = 24 * 60 * 60 * 1000;

                for (QueryDocumentSnapshot doc : value) {
                    Task task = doc.toObject(Task.class);
                    task.setTaskId(doc.getId());

                    try {
                        Date taskDate = dateTimeFormatter.parse(task.getDateTime());
                        if (taskDate != null) {
                            long taskTimeMillis = taskDate.getTime();

                            // 1. מחיקה אוטומטית אם עבר יום מהיעד
                            if (currentTime - taskTimeMillis > oneDayInMs) {
                                db.collection("tasks").document(doc.getId()).delete();
                                continue;
                            }

                            // 2. התראה לילד אם הגיעה השעה והוא לא סיים
                            if (!"הורה".equals(userRole) && currentTime >= taskTimeMillis && !task.isDone()) {
                                Toast.makeText(this, "היי! עבר הזמן לביצוע: " + task.getTaskName(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }

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
                    ArrayAdapter<String> sAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, childNames);
                    sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerChildren.setAdapter(sAdapter);
                });

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

        db.collection("tasks").add(task);
    }

    private void deleteTask(String taskId) {
        db.collection("tasks").document(taskId).delete().addOnSuccessListener(aVoid ->
                Toast.makeText(this, "כל הכבוד! המשימה הושלמה.", Toast.LENGTH_SHORT).show());
    }

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
            holder.tvAssignee.setText("מיועד ל: " + task.getAssignedToName());
            holder.tvTime.setText(task.getDateTime());

            holder.itemView.setOnLongClickListener(v -> {
                if (task.getAssignedToUid() != null && task.getAssignedToUid().equals(currentUid)) {
                    new AlertDialog.Builder(TasksActivity.this)
                            .setTitle("סיום משימה")
                            .setMessage("ביצעת את המשימה?")
                            .setPositiveButton("כן, סיימתי!", (dialog, which) -> deleteTask(task.getTaskId()))
                            .setNegativeButton("עוד לא", null).show();
                } else {
                    Toast.makeText(TasksActivity.this, "רק " + task.getAssignedToName() + " יכול/ה לאשר זאת", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvAssignee, tvTime;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTaskTitle);
                tvAssignee = itemView.findViewById(R.id.tvTaskAssignee);
                tvTime = itemView.findViewById(R.id.tvTaskTime);
            }
        }
    }
}