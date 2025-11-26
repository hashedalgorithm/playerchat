package com.hashedalgorithm.playerchat.server;

import com.hashedalgorithm.playerchat.enums.Role;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;


public class ClientInstance extends Thread {
    private Socket clientSocket;
    private Socket serverSocket;
    private String name;
    private final PrintWriter out;
    private final BufferedReader in;

    public ClientInstance(Socket clientSocket, Socket serverSocket, Role role) throws IOException {
        this.clientSocket = clientSocket;
        this.serverSocket = serverSocket;

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

    private void sendMessage(String message) {
        this.out.println(String.format("[%s]: {%s}", this.name, message));
    }

    public void run() {
        try {
            String inputLine;
            this.clientSocket.setSoTimeout(0);
            do {
                inputLine = in.readLine();
                System.out.printf("[+] - Received message from %s\n", this.name);
                this.sendMessage(inputLine);
            }
            while (inputLine != null);
            in.close();

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

}
