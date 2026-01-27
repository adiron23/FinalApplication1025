package com.example.finalapplication;

public class Task {
    private String taskId;
    private String taskName;
    private String assignedToName;
    private String assignedToUid;
    private String dateTime;
    private String familyCode;
    private boolean isDone; // השדה שגרם לשגיאה

    // קונסטרקטור ריק חובה עבור פיירבייס
    public Task() {}

    // --- Getters & Setters ---
    // אלו הפונקציות שמאפשרות ל-TasksActivity "להבין" את הנתונים

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public String getAssignedToUid() { return assignedToUid; }
    public void setAssignedToUid(String assignedToUid) { this.assignedToUid = assignedToUid; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getFamilyCode() { return familyCode; }
    public void setFamilyCode(String familyCode) { this.familyCode = familyCode; }

    // זו הפונקציה שחסרה לך!
    public boolean isDone() { return isDone; }
    public void setDone(boolean done) { isDone = done; }
}