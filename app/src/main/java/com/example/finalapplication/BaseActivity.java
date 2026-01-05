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
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected FirebaseAuth refAuth;

    @Override
    public void setContentView(int layoutResID) {
        // 1. טעינת השלד המרכזי
        drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_base, null);
        FrameLayout contentFrame = drawerLayout.findViewById(R.id.content_frame);

        // 2. הזרקת התוכן
        getLayoutInflater().inflate(layoutResID, contentFrame, true);

        // 3. הצגת השילוב הסופי
        super.setContentView(drawerLayout);

        // 4. הגדרת התפריט
        setupDrawer();
    }

    protected void setupDrawer() {
        refAuth = FirebaseAuth.getInstance();
        navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.my_toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        updateMenuVisibility();

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

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

                // סגירה בטוחה של התפריט
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
    }

    public void updateMenuVisibility() {
        if (navigationView != null) {
            Menu menu = navigationView.getMenu();
            boolean isLoggedIn = (FirebaseAuth.getInstance().getCurrentUser() != null);

            // וודאי שה-ID האלו קיימים בדיוק כך ב-menu_drawer.xml שלך
            if (menu.findItem(R.id.log_out) != null) menu.findItem(R.id.log_out).setVisible(isLoggedIn);
            if (menu.findItem(R.id.log_in) != null) menu.findItem(R.id.log_in).setVisible(!isLoggedIn);
            if (menu.findItem(R.id.sign_in) != null) menu.findItem(R.id.sign_in).setVisible(!isLoggedIn);
        }
    }

    protected void logoutUser() {
        // התנתקות
        FirebaseAuth.getInstance().signOut();

        // יצירת Intent למסך ההתחברות (זה ימנע מהאפליקציה לנסות לטעון נתוני משתמש ב-MainActivity)
        Intent intent = new Intent(this, LogInActivity.class);

        // ניקוי כל המחסנית - המשתמש לא יכול לחזור אחורה למסך הקודם
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish();
    }
}