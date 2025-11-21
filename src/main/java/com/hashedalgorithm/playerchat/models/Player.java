package com.hashedalgorithm.playerchat.models;

public class Player {

    int playerId;
    String playerName;
    int messageSent = 0;

    Player(int playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    void incrementMessageSent() {
        if (messageSent >= 10) {
            System.err.println("Maximum quanity of messages sent!");
            return;
        }

        this.messageSent += 1;
        return;
    }

}
