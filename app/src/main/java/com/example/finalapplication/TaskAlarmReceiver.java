package com.example.finalapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;

// מקלט שידור שמופעל כשמגיע תזכורת למשימה – יוצר ומציג התראה למשתמש
public class TaskAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "family_tasks";

    // מקבל את פרטי המשימה מה-Intent, יוצר ערוץ התראות ומציג התראה עם שם המשימה והנמען
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskName     = intent.getStringExtra("taskName");
        String taskId       = intent.getStringExtra("taskId");
        String assignedName = intent.getStringExtra("assignedName");

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // יוצר ערוץ התראות (נדרש באנדרואיד 8+; בטוח לקרוא מספר פעמים)
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "משימות משפחה", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("תזכורות למשימות משפחתיות");
        channel.enableVibration(true);
        nm.createNotificationChannel(channel);

        // לחיצה על ההתראה תפתח את מסך המשימות
        Intent openIntent = new Intent(context, TasksActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(context, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String body = taskName != null ? taskName : "";
        if (assignedName != null && !assignedName.isEmpty()) {
            body += "\nמיועד ל: " + assignedName;
        }

        // בונה ומציג את ההתראה עם כותרת, תוכן מורחב וצליל/רטט
        android.app.Notification notification =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_tasks)
                        .setContentTitle("⏰ הגיע הזמן למשימה!")
                        .setContentText(taskName != null ? taskName : "")
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setContentIntent(openPi)
                        .build();

        // מזהה ייחודי להתראה לפי מזהה המשימה (או זמן נוכחי כגיבוי)
        int notifId = taskId != null ? Math.abs(taskId.hashCode()) : (int) System.currentTimeMillis();
        nm.notify(notifId, notification);
    }
}