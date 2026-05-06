package com.example.finalapplication;

import com.google.firebase.firestore.Exclude;

public class CalendarEvent {

    private String eventId;
    private String title;
    private String notes;
    private String date;        // "yyyy-MM-dd"
    private String familyCode;
    private String authorUid;
    private String authorName;
    private long   timestamp;

    /** Required no-arg constructor for Firestore deserialization. */
    public CalendarEvent() {}

    @Exclude
    public String getEventId()              { return eventId; }
    public void   setEventId(String id)     { this.eventId = id; }

    public String getTitle()                { return title; }
    public void   setTitle(String t)        { this.title = t; }

    public String getNotes()                { return notes; }
    public void   setNotes(String n)        { this.notes = n; }

    public String getDate()                 { return date; }
    public void   setDate(String d)         { this.date = d; }

    public String getFamilyCode()           { return familyCode; }
    public void   setFamilyCode(String fc)  { this.familyCode = fc; }

    public String getAuthorUid()            { return authorUid; }
    public void   setAuthorUid(String uid)  { this.authorUid = uid; }

    public String getAuthorName()           { return authorName; }
    public void   setAuthorName(String n)   { this.authorName = n; }

    public long   getTimestamp()            { return timestamp; }
    public void   setTimestamp(long ts)     { this.timestamp = ts; }

    private String time; // "HH:mm", optional
    public String getTime()               { return time; }
    public void   setTime(String t)       { this.time = t; }
}
