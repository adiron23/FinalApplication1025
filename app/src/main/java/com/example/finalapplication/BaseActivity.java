package com.example.finalapplication;

import android.content.Intent;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected FirebaseAuth refAuth;

    // פונקציה שכל מסך יקרא לה כדי להפעיל את התפריט שלו
    protected void setupDrawer() {
        refAuth = FirebaseAuth.getInstance();
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.my_toolbar);

        setSupportActionBar(toolbar);

        // הגדרת כפתור ההמבורגר
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // טיפול בלחיצות בתפריט
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.log_in) {
                    startActivity(new Intent(BaseActivity.this, LogInActivity.class));
                } else if (id == R.id.sign_in) {
                    startActivity(new Intent(BaseActivity.this, RegisterActivity.class));
                } else if (id == R.id.log_out) {
                    logoutUser();
                }

                drawerLayout.closeDrawers();
                return true;
            }
        });
    }

    protected void logoutUser() {
        if (refAuth != null) {
            refAuth.signOut();
            Intent intent = new Intent(this, LogInActivity.class);
            // מנקה את היסטוריית המסכים כדי שלא יוכלו לחזור אחורה אחרי התנתקות
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}