package com.example.finalapplication;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private TextView tVWelcome, tvFamilyName, tvTasksWidgetTitle, tvTasksEmpty, tvShoppingEmpty, tvTasksCount, tvEventsEmpty;
    private LinearLayout tasksWidgetContainer, shoppingWidgetContainer, eventsWidgetContainer;
    private FirebaseFirestore db;
    private String uid;
    private String userFamilyCode = "";
    private boolean isParent = false;
    private ConnectionReceiver connectionReceiver;

    private final SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentViewAndBind(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        tVWelcome               = findViewById(R.id.tVWelcome);
        tvFamilyName            = findViewById(R.id.tvFamilyName);
        tvTasksWidgetTitle      = findViewById(R.id.tvTasksWidgetTitle);
        tvTasksEmpty            = findViewById(R.id.tvTasksEmpty);
        tvShoppingEmpty         = findViewById(R.id.tvShoppingEmpty);
        tvTasksCount            = findViewById(R.id.tvTasksCount);
        tasksWidgetContainer    = findViewById(R.id.tasksWidgetContainer);
        shoppingWidgetContainer = findViewById(R.id.shoppingWidgetContainer);
        eventsWidgetContainer   = findViewById(R.id.eventsWidgetContainer);
        tvEventsEmpty           = findViewById(R.id.tvEventsEmpty);

        findViewById(R.id.tvTasksSeeAll).setOnClickListener(v ->
                startActivity(new Intent(this, TasksActivity.class)));
        findViewById(R.id.tvShoppingSeeAll).setOnClickListener(v ->
                startActivity(new Intent(this, ShoppingListActivity.class)));
        findViewById(R.id.tvEventsSeeAll).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            uid = currentUser.getUid();
            loadInfo();
        }

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        connectionReceiver = new ConnectionReceiver();
        registerReceiver(connectionReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (uid != null && !userFamilyCode.isEmpty()) {
            loadTasksWidget();
            loadTodayEventsWidget();
            loadShoppingWidget();
        }
    }

    // Returns a time-based greeting in Hebrew
    private String getTimeGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12)  return "בוקר טוב";
        if (hour >= 12 && hour < 17) return "צהריים טובים";
        if (hour >= 17 && hour < 21) return "ערב טוב";
        return "לילה טוב";
    }

    private void loadInfo() {
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            if (!userDoc.exists()) return;

            String name = userDoc.getString("name");
            String code = userDoc.getString("familyCode");
            String role = userDoc.getString("role");

            isParent = "הורה".equals(role);
            tvTasksWidgetTitle.setText(isParent ? "המשימות הדחופות" : "המשימות שלי");

            String greeting = getTimeGreeting();

            if (code != null && !code.isEmpty()) {
                userFamilyCode = code;
                db.collection("families").document(code).get().addOnSuccessListener(famDoc -> {
                    if (famDoc.exists()) {
                        String famName = famDoc.getString("familyName");
                        tVWelcome.setText(greeting + ", " + (name != null ? name : ""));
                        if (famName != null && !famName.isEmpty()) {
                            tvFamilyName.setText("משפחת " + famName);
                            tvFamilyName.setVisibility(android.view.View.VISIBLE);
                        }
                    }
                });
                loadTasksWidget();
                loadShoppingWidget();
                loadTodayEventsWidget();
            } else {
                tVWelcome.setText(greeting + ", " + (name != null ? name : ""));
                tvFamilyName.setVisibility(android.view.View.GONE);
            }
        });
    }

    // ── Tasks widget ──────────────────────────────────────────────────────────

    private void loadTasksWidget() {
        Query query = isParent
                ? db.collection("tasks").whereEqualTo("familyCode", userFamilyCode)
                : db.collection("tasks").whereEqualTo("familyCode", userFamilyCode)
                                        .whereEqualTo("assignedToUid", uid);

        query.get().addOnSuccessListener(querySnapshot -> {
            List<Task> tasks = new ArrayList<>();
            for (QueryDocumentSnapshot doc : querySnapshot) {
                Task task = doc.toObject(Task.class);
                task.setTaskId(doc.getId());
                if (!task.isDone()) tasks.add(task);
            }

            // Sort ascending by date — most urgent first
            tasks.sort((a, b) -> {
                try {
                    Date da  = a.getDateTime() != null ? sdf.parse(a.getDateTime()) : null;
                    Date db2 = b.getDateTime() != null ? sdf.parse(b.getDateTime()) : null;
                    if (da == null) return 1;
                    if (db2 == null) return -1;
                    return da.compareTo(db2);
                } catch (Exception e) { return 0; }
            });

            // Update task count badge
            int count = tasks.size();
            if (count > 0) {
                tvTasksCount.setVisibility(View.VISIBLE);
                tvTasksCount.setText(String.valueOf(count));
            } else {
                tvTasksCount.setVisibility(View.GONE);
            }

            tasksWidgetContainer.removeAllViews();
            if (tasks.isEmpty()) {
                tvTasksEmpty.setVisibility(View.VISIBLE);
                tvTasksEmpty.setText("כל המשימות הושלמו!");
                tvTasksEmpty.setTextColor(Color.parseColor("#43A047"));
            } else {
                tvTasksEmpty.setVisibility(View.GONE);
                int show = Math.min(3, tasks.size());
                for (int i = 0; i < show; i++) {
                    addTaskRow(tasks.get(i), i < show - 1);
                }
            }
        });
    }

    private void addTaskRow(Task task, boolean showDivider) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(4, 10, 4, 10);

        // Task name
        TextView tvName = new TextView(this);
        tvName.setText(task.getTaskName());
        tvName.setTextSize(18);
        tvName.setTypeface(null, Typeface.BOLD);

        // Colour name red if overdue
        boolean overdue = false;
        try {
            Date taskDate = task.getDateTime() != null ? sdf.parse(task.getDateTime()) : null;
            overdue = taskDate != null && taskDate.before(new Date());
        } catch (Exception ignored) {}
        tvName.setTextColor(overdue ? Color.parseColor("#C62828") : Color.parseColor("#3E2723"));

        row.addView(tvName);

        // Priority tag (if not "רגיל")
        if (task.getPriority() != null && "דחוף".equals(task.getPriority())) {
            TextView tvPriority = new TextView(this);
            tvPriority.setText("דחוף");
            tvPriority.setTextSize(14);
            tvPriority.setTextColor(Color.WHITE);
            tvPriority.setBackgroundColor(Color.parseColor("#C62828"));
            tvPriority.setPadding(10, 2, 10, 2);
            row.addView(tvPriority);
        }

        // Detail line: assignee (parents) + date
        TextView tvDetail = new TextView(this);
        StringBuilder detail = new StringBuilder();
        if (isParent && task.getAssignedToName() != null && !task.getAssignedToName().isEmpty()) {
            detail.append("מיועד ל: ").append(task.getAssignedToName()).append("   ");
        }
        if (task.getDateTime() != null) detail.append(task.getDateTime());
        tvDetail.setText(detail.toString());
        tvDetail.setTextSize(15);
        tvDetail.setTextColor(Color.parseColor("#8D6E63"));
        row.addView(tvDetail);

        tasksWidgetContainer.addView(row);

        if (showDivider) {
            View divider = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            lp.setMargins(0, 4, 0, 4);
            divider.setLayoutParams(lp);
            divider.setBackgroundColor(Color.parseColor("#E8DDD3"));
            tasksWidgetContainer.addView(divider);
        }
    }

    // ── Today's events widget ─────────────────────────────────────────────────

    private void loadTodayEventsWidget() {
        Calendar c = Calendar.getInstance();
        String today = String.format(java.util.Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH));

        db.collection("events")
                .whereEqualTo("familyCode", userFamilyCode)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(snap -> {
                    eventsWidgetContainer.removeAllViews();
                    if (snap.isEmpty()) {
                        tvEventsEmpty.setVisibility(View.VISIBLE);
                        return;
                    }
                    tvEventsEmpty.setVisibility(View.GONE);
                    // Sort by timestamp
                    List<com.google.firebase.firestore.DocumentSnapshot> docs =
                            new ArrayList<>(snap.getDocuments());
                    docs.sort((a, b) -> {
                        Long ta = a.getLong("timestamp");
                        Long tb = b.getLong("timestamp");
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return Long.compare(ta, tb);
                    });
                    int showCount = Math.min(3, docs.size());
                    for (int i = 0; i < showCount; i++) {
                        String title  = docs.get(i).getString("title");
                        String author = docs.get(i).getString("authorName");
                        String time   = docs.get(i).getString("time");
                        addEventRow(title  != null ? title  : "",
                                    author != null ? author : "",
                                    time   != null ? time   : "",
                                    i < showCount - 1);
                    }
                });
    }

    private void addEventRow(String title, String author, String time, boolean showDivider) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(4, 10, 4, 10);

        // Turquoise left-side accent strip
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(android.view.Gravity.CENTER_VERTICAL);

        View strip = new View(this);
        LinearLayout.LayoutParams stripLp = new LinearLayout.LayoutParams(4,
                LinearLayout.LayoutParams.MATCH_PARENT);
        stripLp.setMarginEnd(12);
        strip.setLayoutParams(stripLp);
        strip.setBackgroundColor(Color.parseColor("#40E0D0"));
        strip.setMinimumHeight((int) (getResources().getDisplayMetrics().density * 36));
        inner.addView(strip);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#3E2723"));
        textCol.addView(tvTitle);

        String subtitle = (!time.isEmpty() ? "🕐 " + time + "  " : "")
                + (!author.isEmpty() ? "נוסף ע\"י: " + author : "");
        if (!subtitle.isEmpty()) {
            TextView tvAuthor = new TextView(this);
            tvAuthor.setText(subtitle);
            tvAuthor.setTextSize(15);
            tvAuthor.setTextColor(Color.parseColor("#8D6E63"));
            tvAuthor.setPadding(0, 2, 0, 0);
            textCol.addView(tvAuthor);
        }

        inner.addView(textCol);
        row.addView(inner);
        eventsWidgetContainer.addView(row);

        if (showDivider) {
            View divider = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            lp.setMargins(0, 6, 0, 6);
            divider.setLayoutParams(lp);
            divider.setBackgroundColor(Color.parseColor("#E8DDD3"));
            eventsWidgetContainer.addView(divider);
        }
    }

    // ── Shopping widget ───────────────────────────────────────────────────────

    private void loadShoppingWidget() {
        db.collection("shopping_lists")
                .whereEqualTo("familyCode", userFamilyCode)
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ShoppingItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ShoppingItem item = doc.toObject(ShoppingItem.class);
                        if (item != null && !item.isChecked()) {
                            items.add(item);
                            if (items.size() == 3) break;
                        }
                    }

                    shoppingWidgetContainer.removeAllViews();
                    if (items.isEmpty()) {
                        tvShoppingEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvShoppingEmpty.setVisibility(View.GONE);
                        for (int i = 0; i < items.size(); i++) {
                            addShoppingRow(items.get(i), i < items.size() - 1);
                        }
                    }
                });
    }

    private void addShoppingRow(ShoppingItem item, boolean showDivider) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(4, 10, 4, 10);

        TextView tvName = new TextView(this);
        tvName.setText("• " + item.getName());
        tvName.setTextSize(18);
        tvName.setTextColor(Color.parseColor("#3E2723"));
        row.addView(tvName);

        if (item.getAssignedToName() != null && !item.getAssignedToName().isEmpty()) {
            TextView tvAssignee = new TextView(this);
            tvAssignee.setText("מיועד ל: " + item.getAssignedToName());
            tvAssignee.setTextSize(15);
            tvAssignee.setTextColor(Color.parseColor("#8D6E63"));
            row.addView(tvAssignee);
        }

        shoppingWidgetContainer.addView(row);

        if (showDivider) {
            View divider = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            lp.setMargins(0, 4, 0, 4);
            divider.setLayoutParams(lp);
            divider.setBackgroundColor(Color.parseColor("#E8DDD3"));
            shoppingWidgetContainer.addView(divider);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectionReceiver != null) unregisterReceiver(connectionReceiver);
    }
}
