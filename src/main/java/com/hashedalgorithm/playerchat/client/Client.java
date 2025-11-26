package com.hashedalgorithm.playerchat.client;

import java.io.IOException;

public class Client {

    ServerCommunicator serverCommunicator;

    public Client(String name, String ip, int port) {
        try {
            this.serverCommunicator = new ServerCommunicator(name, ip, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String destination) {
        this.serverCommunicator.sendMessage(destination);
    }

    public String waitForReply() {
        return serverCommunicator.waitForReply();
    }

}
