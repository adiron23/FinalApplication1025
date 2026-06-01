package com.example.finalapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBottomMenuSelection();
    }

    protected void setContentViewAndBind(int layoutResID) {
        super.setContentView(R.layout.activity_base);

        FrameLayout contentFrame = findViewById(R.id.content_frame);
        LayoutInflater.from(this).inflate(layoutResID, contentFrame, true);

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);

            bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int id = item.getItemId();
                    Intent intent = null;

                    if (id == R.id.nav_main && !(BaseActivity.this instanceof MainActivity)) {
                        intent = new Intent(BaseActivity.this, MainActivity.class);
                    } else if (id == R.id.nav_shopping_list && !(BaseActivity.this instanceof ShoppingListActivity)) {
                        intent = new Intent(BaseActivity.this, ShoppingListActivity.class);
                    } else if (id == R.id.nav_tasks && !(BaseActivity.this instanceof TasksActivity)) {
                        intent = new Intent(BaseActivity.this, TasksActivity.class);
                    } else if (id == R.id.nav_calendar && !(BaseActivity.this instanceof CalendarActivity)) {
                        intent = new Intent(BaseActivity.this, CalendarActivity.class);
                    } else if (id == R.id.nav_profile && !(BaseActivity.this instanceof ProfileActivity)) {
                        intent = new Intent(BaseActivity.this, ProfileActivity.class);
                    }

                    if (intent != null) {
                        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        BaseActivity.this.startActivity(intent);
                        BaseActivity.this.overridePendingTransition(0, 0);
                        return true;
                    }
                    return true;
                }
            });
        }
    }

    private void updateBottomMenuSelection() {
        if (bottomNavigationView == null) {
            bottomNavigationView = findViewById(R.id.bottom_navigation);
        }

        if (bottomNavigationView != null) {
            int id = -1;
            if (this instanceof MainActivity) id = R.id.nav_main;
            else if (this instanceof ShoppingListActivity) id = R.id.nav_shopping_list;
            else if (this instanceof TasksActivity) id = R.id.nav_tasks;
            else if (this instanceof CalendarActivity) id = R.id.nav_calendar;
            else if (this instanceof ProfileActivity)  id = R.id.nav_profile;

            if (id != -1) {
                bottomNavigationView.getMenu().findItem(id).setChecked(true);
            }
        }
    }

    protected void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LogInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}