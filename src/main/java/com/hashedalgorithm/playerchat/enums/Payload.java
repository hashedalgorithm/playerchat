package com.hashedalgorithm.playerchat.enums;

public enum Payload {
    MESSAGE("msg"),
    REQUEST("req"),
    TO("to"),
    FROM("from"),
    STATUS("stat"),
    INSTANCE_ID("id");

    private final String value;
    Payload(String type) {
        this.value = type;
    }

    public String getValue() {
        return this.value;
    }
}