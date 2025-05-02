package com.example.attendance;

import java.util.Date;

public class AttendanceRecord {
    private String id;
    private String userName;
    private Date timestamp;
    private String courseName;

    public AttendanceRecord(String id, String userName, Date timestamp, String courseName) {
        this.id = id;
        this.userName = userName;
        this.timestamp = timestamp;
        this.courseName = courseName;
    }

    public String getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getCourseName() {
        return courseName;
    }
}