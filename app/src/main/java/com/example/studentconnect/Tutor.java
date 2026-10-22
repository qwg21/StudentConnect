package com.example.studentconnect;

import java.util.List;

public class Tutor {
    public final String uid;
    public final String name;
    public final String bio;
    public final List<String> matchingCourses;
    public final double avgRating;
    public final long ratingCount;
    public final long studentsHelped;

    public Tutor(String uid, String name, String bio, List<String> matchingCourses,
                 double avgRating, long ratingCount, long studentsHelped) {
        this.uid = uid;
        this.name = name;
        this.bio = bio;
        this.matchingCourses = matchingCourses;
        this.avgRating = avgRating;
        this.ratingCount = ratingCount;
        this.studentsHelped = studentsHelped;
    }
}