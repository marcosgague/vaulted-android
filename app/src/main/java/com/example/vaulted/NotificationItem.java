package com.example.vaulted;

import com.google.firebase.Timestamp;

public class NotificationItem {
    public final String type;
    public final String title;
    public final String body;
    public final Timestamp createdAt;

    public NotificationItem(String type, String title, String body, Timestamp createdAt) {
        this.type = type;
        this.title = title;
        this.body = body;
        this.createdAt = createdAt;
    }
}
