package com.example.finalapplication;

public class Task {
    private String taskId;
    private String taskName;
    private String assignedToName;
    private String assignedToUid;
    private String dateTime;
    private String familyCode;
    private boolean isDone;
    private String priority; // "דחוף", "רגיל", "נמוך"

    // קונסטרקטור ריק חובה עבור פיירבייס
    public Task() {}

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

    // @PropertyName tells Firestore to use "isDone" as the field name,
    // overriding the JavaBean convention that would strip the "is" prefix.
    @com.google.firebase.firestore.PropertyName("isDone")
    public boolean isDone() { return isDone; }

    @com.google.firebase.firestore.PropertyName("isDone")
    public void setDone(boolean done) { isDone = done; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}