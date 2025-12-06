package com.hashedalgorithm.playerchat.enums;

/**
 * Enum representing the status of a client or a client request in the chat system.
 *
 * <p>This enum provides a standard set of statuses to indicate whether a client request
 * or message operation succeeded, failed, or was blocked. Using these constants ensures
 * consistent handling of client statuses across the server and clients.
 *
 * <p>Values include:
 * <ul>
 *     <li>{@link #SUCCESS} - Indicates that the operation or request completed successfully ("success").</li>
 *     <li>{@link #FAILED} - Indicates that the operation or request failed ("failed").</li>
 *     <li>{@link #BLOCKED} - Indicates that the operation or request is blocked ("blocked").</li>
 * </ul>
 */
public enum ClientStatus {
    SUCCESS("success"),
    FAILED("failed"),
    BLOCKED("blocked");

    private final String status;

    /**
     * Constructs a {@link ClientStatus} enum with the associated string value.
     *
     * @param status The string value representing the status.
     */
    ClientStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the string value associated with this status.
     *
     * @return The string representation of the client status.
     */
    public String getValue() {
        return this.status;
    }

}
