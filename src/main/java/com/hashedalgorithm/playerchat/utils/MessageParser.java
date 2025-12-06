package com.hashedalgorithm.playerchat.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility class for parsing and serializing messages between clients and the server.
 *
 * <p>This class provides methods to:
 * <ul>
 *     <li>Parse a raw message string received from a socket into a structured {@link Map}.</li>
 *     <li>Serialize a {@link Map} of message key-value pairs into a string suitable for
 *         sending over a socket.</li>
 * </ul>
 *
 * <p>The expected format for messages is a '|' delimited string of key-value pairs, where
 * each key and value are separated by a ':'.
 *
 * <p>Example message string:
 * <pre>
 *     FROM:client1|TO:client2|REQUEST:MESSAGE|MESSAGE:Hello
 * </pre>
 *
 * <p>Example parsed map:
 * <pre>
 *     {
 *         "FROM" = "client1",
 *         "TO" = "client2",
 *         "REQUEST" = "MSG" | "HANDSHAKE",
 *         "MESSAGE" = "Hello"
 *     }
 * </pre>
 */

public class MessageParser {

    /**
     * Parses a message string into a {@link Map} of key-value pairs.
     *
     * <p>The input string should have key-value pairs separated by '|', and keys and values
     * separated by ':'.
     *
     * @param message The raw message string to parse.
     * @return A {@link Map} containing the parsed key-value pairs.
     */
    public Map<String, String> parseMessage(String message) {
        Map<String, String> result = new HashMap<>();

        // Split the string based on the '|' delimiter
        String[] parts = message.split("\\|");

        for (String part : parts) {
            // Split each part into key and value using ':'
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                result.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return result;
    }

    /**
     * Serializes a {@link Map} of key-value pairs into a message string suitable for transmission.
     *
     * <p>The output string will have key-value pairs separated by '|', and keys and values
     * separated by ':'.
     *
     * @param message The {@link Map} containing key-value pairs to serialize.
     * @return A string representation of the map in the message format.
     */
    public String serialize(Map<String, String> message) {
        StringBuilder result = new StringBuilder();

        Iterator<Map.Entry<String, String>> iterator = message.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            result.append(entry.getKey()).append(":").append(entry.getValue());

            if (iterator.hasNext()) {
                result.append("|");
            }
        }
        return result.toString();
    }
}

