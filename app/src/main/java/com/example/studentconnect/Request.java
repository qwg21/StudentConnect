package com.example.studentconnect;

public class Request {
    public final String requestId;
    public final String studentId;
    public final String studentName;
    public final String tutorId;
    public final String tutorName;
    public final String course;
    public final String status;
    public final long createdAt;

    public Request(String requestId, String studentId, String studentName,
                   String tutorId, String tutorName, String course,
                   String status, long createdAt) {
        this.requestId = requestId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.tutorId = tutorId;
        this.tutorName = tutorName;
        this.course = course;
        this.status = status;
        this.createdAt = createdAt;
    }
}