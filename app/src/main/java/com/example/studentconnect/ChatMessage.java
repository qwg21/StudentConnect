package com.example.studentconnect;

public class ChatMessage {
    public final String senderId;
    public final String senderName;
    public final String text;
    public final long timestamp;

    public ChatMessage(String senderId, String senderName, String text, long timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
    }
}