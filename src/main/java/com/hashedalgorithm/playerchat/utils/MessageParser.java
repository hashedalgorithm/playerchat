package com.hashedalgorithm.playerchat.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MessageParser {

    public Map<String, String> parseMessage(String message) {
        Map<String, String> result = new HashMap<>();

        // Split the string based on the '|' delimiter
        String[] parts = message.split("\\|");

        System.out.println(message);
        System.out.println(Arrays.toString(parts));
        for (String part : parts) {
            // Split each part into key and value using ':'
            String[] keyValue = part.split(":", 2);
            if (keyValue.length == 2) {
                result.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }

        System.out.println(result.toString());
        return result;
    }

    public String serialize(Map<String, String> message) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : message.entrySet()) {
            result.append(entry.getKey()).append(":").append(entry.getValue()).append("|");
        }

        return result.toString();
    }
}

