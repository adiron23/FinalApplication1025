package com.example.finalapplication;

import android.os.Bundle;

public class TasksActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks); // ודא שיש לך activity_tasks.xml

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("משימות");
        }

        // מסמן את המשימות בתפריט
        markSelectedMenuItem(R.id.nav_tasks);
    }
}