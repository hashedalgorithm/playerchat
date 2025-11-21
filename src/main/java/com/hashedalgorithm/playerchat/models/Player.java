package com.hashedalgorithm.playerchat.models;

import java.util.UUID;

public class Player {

    public int messageSent = 0;
    public String playerId;
    public String playerName;

    public Player(String playerName) {
        this.playerId = UUID.randomUUID().toString();
        this.playerName = playerName;
    }

    public void incrementMessageSent() {
        if (messageSent >= 10) {
            System.err.println("Maximum quanity of messages sent!");
            return;
        }

        this.messageSent += 1;
        return;
    }

    @Override
    public String toString() {
        return String.format("Player{playerId=%d, playerName='%s', messageSent=%d}", playerId, playerName, messageSent);
    }

}
