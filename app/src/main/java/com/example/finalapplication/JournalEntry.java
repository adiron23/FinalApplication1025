package com.example.finalapplication;

import com.google.firebase.firestore.Exclude;

public class JournalEntry {

    private String entryId;
    private String text;
    private String authorUid;
    private String authorName;
    private String familyCode;
    private long   timestamp;

    // Required empty constructor for Firestore
    public JournalEntry() {}

    @Exclude
    public String getEntryId()              { return entryId; }
    public void   setEntryId(String id)     { this.entryId = id; }

    public String getText()                 { return text; }
    public void   setText(String text)      { this.text = text; }

    public String getAuthorUid()            { return authorUid; }
    public void   setAuthorUid(String uid)  { this.authorUid = uid; }

    public String getAuthorName()           { return authorName; }
    public void   setAuthorName(String n)   { this.authorName = n; }

    public String getFamilyCode()           { return familyCode; }
    public void   setFamilyCode(String fc)  { this.familyCode = fc; }

    public long   getTimestamp()            { return timestamp; }
    public void   setTimestamp(long ts)     { this.timestamp = ts; }
}
