package com.codeflow.api.models;

import java.time.LocalDateTime;

/** Client-side message model (no DB – sent/received via API). */
public class Message {
    private String id;
    private String senderUsername;
    private String receiverUsername;
    private String content;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    public Message() {}
    public Message(String receiverUsername, String content) {
        this.receiverUsername = receiverUsername;
        this.content = content;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String s) { this.senderUsername = s; }
    public String getReceiverUsername() { return receiverUsername; }
    public void setReceiverUsername(String r) { this.receiverUsername = r; }
    public String getContent() { return content; }
    public void setContent(String c) { this.content = c; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime t) { this.sentAt = t; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime t) { this.readAt = t; }
}
