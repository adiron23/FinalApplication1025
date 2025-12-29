package com.example.finalapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
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

    // הפונקציה הזו היא הקסם - היא מזריקה את התוכן של כל מסך לתוך השלד עם התפריט
    @Override
    public void setContentView(int layoutResID) {
        // 1. טעינת השלד המרכזי (activity_base)
        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_base, null);
        FrameLayout contentFrame = drawerLayout.findViewById(R.id.content_frame);

        // 2. הזרקת התוכן של המסך הספציפי לתוך ה-FrameLayout
        getLayoutInflater().inflate(layoutResID, contentFrame, true);

        // 3. הצגת השילוב הסופי על המסך
        super.setContentView(drawerLayout);

        // 4. הגדרת התפריט והסרגל העליון
        setupDrawer();
    }

    protected void setupDrawer() {
        refAuth = FirebaseAuth.getInstance();
        navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.my_toolbar);

        // הגדרת ה-Toolbar כ-ActionBar של המסך
        setSupportActionBar(toolbar);

        // עדכון אילו כפתורים יופיעו בתפריט (התחבר/התנתק)
        updateMenuVisibility();

        // הגדרת כפתור ה"המבורגר" (שלושת הפסים) וסנכרון שלו עם ה-Toolbar
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // הגדרת לחיצות על פריטים בתפריט
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

                // סגירת התפריט לאחר לחיצה
                drawerLayout.closeDrawers();
                return true;
            }
        });
    }

    // פונקציה שבודקת אם המשתמש מחובר ומציגה/מסתירה כפתורים בתפריט בהתאם
    private void updateMenuVisibility() {
        if (navigationView != null) {
            Menu menu = navigationView.getMenu();
            boolean isLoggedIn = (refAuth.getCurrentUser() != null);

            // הצגת "התנתק" רק אם מחובר, והצגת "התחבר/הרשם" רק אם לא מחובר
            menu.findItem(R.id.log_out).setVisible(isLoggedIn);
            menu.findItem(R.id.log_in).setVisible(!isLoggedIn);
            menu.findItem(R.id.sign_in).setVisible(!isLoggedIn);
        }
    }

    protected void logoutUser() {
        if (refAuth != null) {
            refAuth.signOut();
            // חזרה למסך הראשי וניקוי המחסנית
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}