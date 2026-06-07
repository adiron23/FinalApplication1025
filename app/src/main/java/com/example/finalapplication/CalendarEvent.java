package com.example.finalapplication;

import com.google.firebase.firestore.Exclude;

// מחלקת מודל המייצגת אירוע ביומן – משמשת לסריאליזציה/דה-סריאליזציה מ-Firestore
public class CalendarEvent {

    private String eventId;
    private String title;
    private String notes;
    private String date;        // "yyyy-MM-dd"
    private String familyCode;
    private String authorUid;
    private String authorName;
    private long   timestamp;
    private String time;        // "HH:mm", optional

    // קונסטרקטור ריק – נדרש על ידי Firestore כדי ליצור אובייקט מהמסמך
    public CalendarEvent() {}

    // מחזיר את מזהה האירוע (לא נשמר ב-Firestore, רק בזיכרון)
    @Exclude
    public String getEventId()               { return eventId; }
    // קובע את מזהה האירוע
    public void   setEventId(String id)      { this.eventId = id; }

    // מחזיר את כותרת האירוע
    public String getTitle()                 { return title; }
    // קובע את כותרת האירוע
    public void   setTitle(String title)     { this.title = title; }

    // מחזיר את ההערות של האירוע
    public String getNotes()                 { return notes; }
    // קובע את ההערות של האירוע
    public void   setNotes(String notes)     { this.notes = notes; }

    // מחזיר את תאריך האירוע בפורמט yyyy-MM-dd
    public String getDate()                  { return date; }
    // קובע את תאריך האירוע
    public void   setDate(String date)       { this.date = date; }

    // מחזיר את קוד המשפחה המשויך לאירוע
    public String getFamilyCode()            { return familyCode; }
    // קובע את קוד המשפחה
    public void   setFamilyCode(String fc)   { this.familyCode = fc; }

    // מחזיר את ה-UID של מי שיצר את האירוע
    public String getAuthorUid()             { return authorUid; }
    // קובע את ה-UID של יוצר האירוע
    public void   setAuthorUid(String uid)   { this.authorUid = uid; }

    // מחזיר את שם יוצר האירוע
    public String getAuthorName()            { return authorName; }
    // קובע את שם יוצר האירוע
    public void   setAuthorName(String name) { this.authorName = name; }

    // מחזיר את חותמת הזמן של יצירת האירוע (במילישניות)
    public long   getTimestamp()             { return timestamp; }
    // קובע את חותמת הזמן
    public void   setTimestamp(long ts)      { this.timestamp = ts; }

    // מחזיר את שעת האירוע בפורמט HH:mm (אופציונלי)
    public String getTime()                  { return time; }
    // קובע את שעת האירוע
    public void   setTime(String time)       { this.time = time; }
}