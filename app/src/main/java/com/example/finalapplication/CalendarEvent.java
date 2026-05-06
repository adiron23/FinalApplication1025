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
    public void   setTitle(String title)     { this.title = title; }

    public String getNotes()                { return notes; }
    public void   setNotes(String notes)    { this.notes = notes; }

    public String getDate()                 { return date; }
    public void   setDate(String date)      { this.date = date; }

    public String getFamilyCode()           { return familyCode; }
    public void   setFamilyCode(String fc)  { this.familyCode = fc; }

    public String getAuthorUid()            { return authorUid; }
    public void   setAuthorUid(String uid)  { this.authorUid = uid; }

    public String getAuthorName()           { return authorName; }
    public void   setAuthorName(String name) { this.authorName = name; }

    public long   getTimestamp()            { return timestamp; }
    public void   setTimestamp(long ts)     { this.timestamp = ts; }

    private String time; // "HH:mm", optional
    public String getTime()               { return time; }
    public void   setTime(String time)    { this.time = time; }
}
