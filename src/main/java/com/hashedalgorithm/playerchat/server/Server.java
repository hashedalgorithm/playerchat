package com.hashedalgorithm.playerchat.server;

import com.hashedalgorithm.playerchat.enums.Role;

import java.io.*;
import java.net.*;

public class Server {
    private ServerSocket serverSocket;

    public ClientInstance initiator;
    public ClientInstance receiver;

    public Server(int port) throws IOException {
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (BindException be) {
            System.out.printf("[+] - Port %d is already in use.\n", port);
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void start() {
        new Thread(() -> {
            try {
                System.out.println("[+] - Listening for connections...");
                while (true) {
                    if(this.initiator != null && this.receiver != null) {
                        System.out.println("[+] - Initiator and Receiver are connected.");
                        break;
                    }

                    Socket clientSocket = serverSocket.accept();

                    if(this.initiator == null && this.receiver == null) {
                        this.initiator = new ClientInstance(this,clientSocket, Role.INITIATOR);
                        this.initiator.start();
                        continue;
                    }

                    if(this.receiver == null) {
                        this.receiver = new ClientInstance(this, clientSocket, Role.RECEIVER);
                        this.receiver.start();
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

}
