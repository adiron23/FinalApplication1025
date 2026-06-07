package com.example.finalapplication;

// מחלקת מודל המייצגת פריט ברשימת הקניות – משמשת לסריאליזציה/דה-סריאליזציה מ-Firestore
public class ShoppingItem {
    private String id;
    private String name;
    private String familyCode;
    private String createdBy;
    private String assignedToUid;
    private String assignedToName;
    private boolean isChecked;

    // קונסטרקטור ריק – נדרש על ידי Firebase כדי ליצור אובייקט מהמסמך
    public ShoppingItem() {}

    // קונסטרקטור מלא ליצירת פריט קנייה חדש
    public ShoppingItem(String id, String name, String familyCode, String createdBy, String assignedToUid, String assignedToName) {
        this.id             = id;
        this.name           = name;
        this.familyCode     = familyCode;
        this.createdBy      = createdBy;
        this.assignedToUid  = assignedToUid;
        this.assignedToName = assignedToName;
        this.isChecked      = false;
    }

    // מחזיר את מזהה הפריט
    public String getId() { return id; }
    // קובע את מזהה הפריט
    public void setId(String id) { this.id = id; }

    // מחזיר את שם הפריט
    public String getName() { return name; }
    // קובע את שם הפריט
    public void setName(String name) { this.name = name; }

    // מחזיר את קוד המשפחה המשויך לפריט
    public String getFamilyCode() { return familyCode; }
    // קובע את קוד המשפחה
    public void setFamilyCode(String familyCode) { this.familyCode = familyCode; }

    // מחזיר את שם מי שהוסיף את הפריט
    public String getCreatedBy() { return createdBy; }
    // קובע את שם מי שהוסיף את הפריט
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    // מחזיר את ה-UID של מי שהפריט מיועד אליו
    public String getAssignedToUid() { return assignedToUid; }
    // קובע את ה-UID של מי שהפריט מיועד אליו
    public void setAssignedToUid(String assignedToUid) { this.assignedToUid = assignedToUid; }

    // מחזיר את שם מי שהפריט מיועד אליו
    public String getAssignedToName() { return assignedToName; }
    // קובע את שם מי שהפריט מיועד אליו
    public void setAssignedToName(String assignedToName) { this.assignedToName = assignedToName; }

    // מחזיר האם הפריט סומן כנקנה
    public boolean isChecked() { return isChecked; }
    // קובע את מצב הסימון של הפריט
    public void setChecked(boolean checked) { isChecked = checked; }
}