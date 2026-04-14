package com.example.finalapplication;

public class ShoppingItem {
    private String id;
    private String name;
    private String familyCode;
    private String createdBy;
    private String assignedToUid;
    private String assignedToName;
    private boolean isChecked;

    public ShoppingItem() {} // חובה עבור Firebase

    public ShoppingItem(String id, String name, String familyCode, String createdBy, String assignedToUid, String assignedToName) {
        this.id = id;
        this.name = name;
        this.familyCode = familyCode;
        this.createdBy = createdBy;
        this.assignedToUid = assignedToUid;
        this.assignedToName = assignedToName;
        this.isChecked = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFamilyCode() { return familyCode; }
    public void setFamilyCode(String familyCode) { this.familyCode = familyCode; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getAssignedToUid() { return assignedToUid; }
    public void setAssignedToUid(String assignedToUid) { this.assignedToUid = assignedToUid; }

    public String getAssignedToName() { return assignedToName; }
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }
}