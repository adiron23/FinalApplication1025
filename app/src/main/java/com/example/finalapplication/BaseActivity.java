package com.example.finalapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Toolbar toolbar;

    @Override
    public void setContentView(int layoutResID) {
        // 1. ניפוח השלד (Base Layout)
        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_base, null);
        FrameLayout contentFrame = drawerLayout.findViewById(R.id.content_frame);

        // 2. הזרקת התוכן של המסך הספציפי (כמו MainActivity)
        getLayoutInflater().inflate(layoutResID, contentFrame, true);

        // 3. הצגת השלד המלא על המסך
        super.setContentView(drawerLayout);

        // 4. הפעלת הגדרות התפריט
        setupDrawer();
    }

    private void setupDrawer() {
        toolbar = findViewById(R.id.my_toolbar);
        navigationView = findViewById(R.id.nav_view);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        // הגדרת ה-Toggle (הפסים)
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        // צביעת הפסים בלבן כדי שיראו אותם היטב
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // האזנה ללחיצות בתפריט
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_shopping_list) {
                startActivity(new Intent(this, ShoppingListActivity.class));
            } else if (id == R.id.log_out) {
                logoutUser();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    protected void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LogInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}