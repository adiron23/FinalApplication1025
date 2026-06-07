package com.example.finalapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

// מחלקת עזר לניהול התראות – מגדירה את ערוץ ההתראות של האפליקציה
public class NotificationHelper {
    public static final String CHANNEL_ID = "tasks_channel";

    // יוצר ערוץ התראות למשימות משפחתיות (נדרש באנדרואיד 8 ומעלה)
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "מטלות משפחתיות",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}