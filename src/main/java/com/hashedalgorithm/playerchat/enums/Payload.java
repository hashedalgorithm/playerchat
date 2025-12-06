package com.hashedalgorithm.playerchat.enums;

/**
 * Enum representing the different keys used in client-server message payloads.
 *
 * <p>This enum defines standard string keys for structuring messages between clients and the server.
 * Using these constants ensures consistency and simplifies parsing and serialization of messages.
 *
 * <p>Values include:
 * <ul>
 *     <li>{@link #MESSAGE} - Represents the actual message content ("msg").</li>
 *     <li>{@link #REQUEST} - Represents the type of request ("req").</li>
 *     <li>{@link #TO} - Represents the recipient's instance ID ("to").</li>
 *     <li>{@link #FROM} - Represents the sender's instance ID ("from").</li>
 *     <li>{@link #STATUS} - Represents the status of a request or message ("stat").</li>
 *     <li>{@link #INSTANCE_ID} - Represents the unique ID of a client instance ("id").</li>
 * </ul>
 */
public enum Payload {
    MESSAGE("msg"),
    REQUEST("req"),
    TO("to"),
    FROM("from"),
    STATUS("stat"),
    INSTANCE_ID("id");

    private final String value;

    /**
     * Constructs a {@link Payload} enum with the associated string key.
     *
     * @param type The string key used in message payloads.
     */
    Payload(String type) {
        this.value = type;
    }

    /**
     * Returns the string key associated with this payload type.
     *
     * @return The string value corresponding to this enum constant.
     */
    public String getValue() {
        return this.value;
    }
}