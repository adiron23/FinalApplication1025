package com.example.finalapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity {

    protected Toolbar toolbar;
    protected BottomNavigationView bottomNavigationView;

    @Override
    public void setContentView(int layoutResID) {
        RelativeLayout baseLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.activity_base, null);
        FrameLayout contentFrame = baseLayout.findViewById(R.id.content_frame);
        getLayoutInflater().inflate(layoutResID, contentFrame, true);
        super.setContentView(baseLayout);

        setupNavigation();
    }

    private void setupNavigation() {
        toolbar = findViewById(R.id.my_toolbar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_main && !(this instanceof MainActivity)) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.nav_shopping_list && !(this instanceof ShoppingListActivity)) {
                startActivity(new Intent(this, ShoppingListActivity.class));
            } else if (id == R.id.nav_tasks && !(this instanceof TasksActivity)) {
                startActivity(new Intent(this, TasksActivity.class));
            } else if (id == R.id.nav_profile && !(this instanceof ProfileActivity)) {
                startActivity(new Intent(this, ProfileActivity.class));
            }
            return true;
        });
    }

    // פונקציה חדשה שכל עמוד יקרא לה כדי לסמן את האייקון הנכון
    protected void markSelectedMenuItem(int menuItemId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(menuItemId);
        }
    }

    protected void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LogInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}