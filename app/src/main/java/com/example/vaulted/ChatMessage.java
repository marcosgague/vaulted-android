package com.example.vaulted;

import com.google.firebase.Timestamp;

public class ChatMessage {
    public final String from;
    public final String text;
    public final Timestamp createdAt;

    public ChatMessage(String from, String text, Timestamp createdAt) {
        this.from = from;
        this.text = text;
        this.createdAt = createdAt;
    }
}
