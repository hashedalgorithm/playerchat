package com.hashedalgorithm.playerchat.enums;

public enum Role {
    INITIATOR("initiator"),
    RECEIVER("receiver");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}