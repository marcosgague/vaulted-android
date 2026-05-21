package com.example.vaulted;

public class ChatConversation {
    public final String chatId;
    public final String userId;
    public final String name;
    public final String username;
    public final String avatarUrl;
    public final String lastMessage;
    public final boolean unread;
    public final int unreadCount;

    public ChatConversation(String userId, String name, String username,
                            String avatarUrl, String lastMessage) {
        this(null, userId, name, username, avatarUrl, lastMessage);
    }

    public ChatConversation(String chatId, String userId, String name, String username,
                            String avatarUrl, String lastMessage) {
        this(chatId, userId, name, username, avatarUrl, lastMessage, false);
    }

    public ChatConversation(String chatId, String userId, String name, String username,
                            String avatarUrl, String lastMessage, boolean unread) {
        this(chatId, userId, name, username, avatarUrl, lastMessage, unread, unread ? 1 : 0);
    }

    public ChatConversation(String chatId, String userId, String name, String username,
                            String avatarUrl, String lastMessage, boolean unread, int unreadCount) {
        this.chatId = chatId;
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.lastMessage = lastMessage;
        this.unread = unread;
        this.unreadCount = unreadCount;
    }
}
