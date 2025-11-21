package com.hashedalgorithm.playerchat.services;

import java.util.ArrayList;

import com.hashedalgorithm.playerchat.models.Message;
import com.hashedalgorithm.playerchat.models.Player;

public class MessagingService {

    ArrayList<Message> messages = new ArrayList<Message>();
    Player initiator;
    Player receiver;

    public MessagingService(Player initiator, Player receiver) {
        this.initiator = initiator;
        this.receiver = receiver;
    }

    public void sendMessage(String to, String from, String message) {
        Message newMessage = new Message(from, to, message);
        this.messages.add(newMessage);

        return;
    }

    public boolean checkPlayersMessageLimit() {
        return messages.size() >= 20;
    }

}
