package com.example.finalapplication;

// מחלקת מודל המייצגת משימה משפחתית – משמשת לסריאליזציה/דה-סריאליזציה מ-Firestore
public class Task {
    private String taskId;
    private String taskName;
    private String assignedToName;
    private String assignedToUid;
    private String dateTime;
    private String familyCode;
    private boolean isDone;
    private String priority; // "דחוף", "רגיל", "נמוך"

    // קונסטרקטור ריק – נדרש על ידי Firebase כדי ליצור אובייקט מהמסמך
    public Task() {}

    // מחזיר את מזהה המשימה
    public String getTaskId() { return taskId; }
    // קובע את מזהה המשימה
    public void setTaskId(String taskId) { this.taskId = taskId; }

    // מחזיר את שם המשימה
    public String getTaskName() { return taskName; }
    // קובע את שם המשימה
    public void setTaskName(String taskName) { this.taskName = taskName; }

    // מחזיר את שם מי שהמשימה מוקצית אליו
    public String getAssignedToName() { return assignedToName; }
    // קובע את שם מי שהמשימה מוקצית אליו
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    // מחזיר את ה-UID של מי שהמשימה מוקצית אליו
    public String getAssignedToUid() { return assignedToUid; }
    // קובע את ה-UID של מי שהמשימה מוקצית אליו
    public void setAssignedToUid(String assignedToUid) { this.assignedToUid = assignedToUid; }

    // מחזיר את תאריך ושעת המשימה
    public String getDateTime() { return dateTime; }
    // קובע את תאריך ושעת המשימה
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    // מחזיר את קוד המשפחה המשויך למשימה
    public String getFamilyCode() { return familyCode; }
    // קובע את קוד המשפחה
    public void setFamilyCode(String familyCode) { this.familyCode = familyCode; }

    // מחזיר האם המשימה הושלמה – שם השדה ב-Firestore הוא "isDone" (מניעת קיצור אוטומטי של Java)
    @com.google.firebase.firestore.PropertyName("isDone")
    public boolean isDone() { return isDone; }

    // קובע האם המשימה הושלמה
    @com.google.firebase.firestore.PropertyName("isDone")
    public void setDone(boolean done) { isDone = done; }

    // מחזיר את רמת העדיפות של המשימה
    public String getPriority() { return priority; }
    // קובע את רמת העדיפות של המשימה
    public void setPriority(String priority) { this.priority = priority; }
}