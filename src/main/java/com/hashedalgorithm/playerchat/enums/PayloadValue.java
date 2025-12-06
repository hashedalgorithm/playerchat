package com.hashedalgorithm.playerchat.enums;


/**
 * Enum representing the different types of payload values used in client-server communication.
 *
 * <p>This enum standardizes the string representations for different message types and statuses
 * to ensure consistent message formatting and parsing across the application.
 *
 * <p>Values include:
 * <ul>
 *     <li>{@link #MESSAGE} - Represents a chat message payload ("msg").</li>
 *     <li>{@link #HANDSHAKE} - Represents a handshake request payload ("handshake").</li>
 *     <li>{@link #SUCCESS} - Represents a successful status response ("success").</li>
 *     <li>{@link #FAILED} - Represents a failed status response ("failed").</li>
 * </ul>
 */
public enum PayloadValue {
    MESSAGE("msg"),
    HANDSHAKE("handshake"),
    SUCCESS("success"),
    FAILED("failed");

    private final String value;

    /**
     * Constructs a {@link PayloadValue} enum with the associated string key.
     *
     * @param type The string key used in message payloads.
     */
    PayloadValue(String type) {
        this.value = type;
    }

    /**
     * Returns the string representation of this payload value.
     *
     * @return The string value corresponding to this enum constant.
     */
    public String getValue() {
        return this.value;
    }
}