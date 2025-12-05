package com.hashedalgorithm.playerchat.enums;

public enum ClientStatus {
    SUCCESS("success"),
    FAILED("failed"),
    BLOCKED("blocked");

    private final String status;

    ClientStatus(String status) {
        this.status = status;
    }

    public String getValue() {
        return this.status;
    }

}
