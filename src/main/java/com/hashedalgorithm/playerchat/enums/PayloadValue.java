package com.hashedalgorithm.playerchat.enums;

public enum PayloadValue {
    MESSAGE("msg"),
    HANDSHAKE("handshake"),
    SUCCESS("success"),
    FAILED("failed");

    private final String value;

    PayloadValue(String type) {
        this.value = type;
    }

    public String getValue() {
        return this.value;
    }
}