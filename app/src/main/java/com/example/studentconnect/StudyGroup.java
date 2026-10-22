package com.example.studentconnect;

import java.util.ArrayList;
import java.util.List;

public class StudyGroup {
    private String id;
    private String name;
    private String courseCode;
    private String description;
    private String creatorId;
    private String creatorName;
    private List<String> memberIds;
    private long memberCount;
    private long maxMembers;
    private String mode; // "Online" or "In person"
    private String location; // venue or meeting link
    private String schedule;
    private long createdAt;
    private String lastMessage;
    private long lastMessageAt;

    // Required empty constructor for Firestore deserialization
    public StudyGroup() {
        this.memberIds = new ArrayList<>();
        this.maxMembers = 8;
        this.mode = "Online";
    }

    public StudyGroup(String id, String name, String courseCode, String description,
                      String creatorId, String creatorName, List<String> memberIds,
                      long memberCount, long maxMembers, String mode, String location,
                      String schedule, long createdAt, String lastMessage, long lastMessageAt) {
        this.id = id;
        this.name = name;
        this.courseCode = courseCode;
        this.description = description;
        this.creatorId = creatorId;
        this.creatorName = creatorName;
        this.memberIds = memberIds != null ? memberIds : new ArrayList<>();
        this.memberCount = memberCount;
        this.maxMembers = maxMembers;
        this.mode = mode;
        this.location = location;
        this.schedule = schedule;
        this.createdAt = createdAt;
        this.lastMessage = lastMessage;
        this.lastMessageAt = lastMessageAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(long memberCount) {
        this.memberCount = memberCount;
    }

    public long getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(long maxMembers) {
        this.maxMembers = maxMembers;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(long lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    // Helpers
    public boolean isMember(String uid) {
        return uid != null && memberIds != null && memberIds.contains(uid);
    }

    public boolean isFull() {
        return memberCount >= maxMembers;
    }

    public boolean isCreator(String uid) {
        return uid != null && uid.equals(creatorId);
    }
}