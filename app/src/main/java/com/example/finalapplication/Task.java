package com.example.finalapplication;

public class Task {
    private String taskId;
    private String taskName;
    private String assignedToName;
    private String assignedToUid;
    private String dateTime;
    private String familyCode;
    private boolean isDone;

    public Task() {} //  עבור Firestore

    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getTaskName() { return taskName; }
    public String getAssignedToName() { return assignedToName; }
    public String getAssignedToUid() { return assignedToUid; }
    public String getDateTime() { return dateTime; }
}