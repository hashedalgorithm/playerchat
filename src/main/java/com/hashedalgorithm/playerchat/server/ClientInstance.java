package com.hashedalgorithm.playerchat.server;

import com.hashedalgorithm.playerchat.enums.Role;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;


public class ClientInstance extends Thread {
    private Socket clientSocket;
    private ServerSocket serverSocket;
    private String name;
    private final Role role;
    private final Server server;
    private final PrintWriter out;
    private final BufferedReader in;
    private int counter = 0;

    public ClientInstance(Server server, Socket clientSocket, Role role) throws IOException {
        this.clientSocket = clientSocket;
        this.server = server;
        this.role = role;

        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.clientSocket.setSoTimeout(5000);

        while (true) {
            try {
                this.name = in.readLine();
                System.out.printf("[+] - %s has been joined as %s.\n", this.name, role.getValue());

                this.clientSocket.setSoTimeout(1000 * 60);
                out.println(role.getValue());
                break;
            } catch (SocketTimeoutException e) {
                System.out.println("[+] - Waiting for client details!!!");
            }
        }
    }

    public void sendMessage(String message, Role to) throws IOException {
        if(this.counter >= 10) {
            System.out.println("[+] - Maximum limit reached");
            if(this.role == Role.RECEIVER) {
                this.server.receiver.clientSocket.close();
            }

            if(this.role == Role.INITIATOR) {
                this.server.initiator.clientSocket.close();
            }
            return;
        }

        if(to == Role.INITIATOR){
            this.server.receiver.out.println(String.format("[%s][%d]: {%s}", this.name, this.counter + 1, message));
            this.counter = counter + 1;
            return;

        }

        if(to == Role.RECEIVER){
            this.server.initiator.out.println(String.format("[%s][%d]: {%s}", this.name,this.counter + 1, message));
            this.counter = counter + 1;
            return;
        }


        throw new IllegalArgumentException("Invalid Role");

    }

    public void run() {
        try {
            String inputLine;
            this.clientSocket.setSoTimeout(0);
            do {
                inputLine = in.readLine();
                System.out.printf("[+] - Received message from %s\n", this.name);
                this.sendMessage(inputLine, this.role);
            }
            while (inputLine != null);
            in.close();

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

}
