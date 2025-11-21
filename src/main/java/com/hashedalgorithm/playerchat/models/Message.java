package com.hashedalgorithm.playerchat.models;

import java.util.UUID;

public class Message {
    long createdAt;
    String sentBy;
    String sentTo;
    String msg;
    String messageId;

    public Message(String sentBy, String sentTo, String msg) {
        this.messageId = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.sentBy = sentBy;
        this.sentTo = sentTo;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageId=" + messageId +
                ", createdAt=" + createdAt +
                ", sentBy=" + sentBy +
                ", sentTo=" + sentTo +
                ", msg='" + msg + '\'' +
                '}';
    }
}
