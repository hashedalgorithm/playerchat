package com.hashedalgorithm.playerchat.models;

public class Message {
    int messageId;
    long createdAt;
    int sentBy;
    long sentTo;
    String msg;

    public Message(int messageId, int sentBy, int sentTo, String msg) {
        this.messageId = messageId;
        this.createdAt = System.currentTimeMillis();
        this.sentBy = sentBy;
        this.sentTo = sentTo;
        this.msg = msg;
    }
}
