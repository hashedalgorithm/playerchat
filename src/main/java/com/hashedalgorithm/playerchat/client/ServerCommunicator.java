package com.hashedalgorithm.playerchat.client;

import com.hashedalgorithm.playerchat.enums.Role;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerCommunicator {

    private PrintWriter out;
    private BufferedReader in;
    public Role clientRole;


    public ServerCommunicator(String name, String ip, int port) throws IOException {
        try{
            Socket clientSocket = new Socket(ip, port);
            clientSocket.setSoTimeout(1000 * 60);

            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            System.out.println("[+] - Connected to server successfully!");
            // Sending first message client name - client identifier
            out.println(name);

            while (true) {
                try {
                    String rawRole = in.readLine();
                    System.out.println("[+] - Got acknowledgment from server");
                    if (rawRole.equalsIgnoreCase(Role.INITIATOR.getValue())) {
                        this.clientRole = Role.INITIATOR;
                        break;
                    }
                    else if (rawRole.equalsIgnoreCase(Role.RECEIVER.getValue())) {
                        this.clientRole = Role.RECEIVER;
                        break;
                    }
                    else throw new IOException("Invalid Role!");

                }
                catch (SocketTimeoutException e) {
                    System.out.println("[+] - Waiting for server ACK!");
                }
                catch (IOException e) {
                    System.out.println("[!] - Error in determining role!");
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        } catch (IOException e) {
            System.err.println("[!] - Could not connect to the server");
            e.printStackTrace();
            System.exit(-1);
        }


    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String waitForReply(){
        while (true) {
            try {
                String inputLine = in.readLine();
                if(inputLine == null) throw new IllegalArgumentException("Invalid input from the server!");

                return inputLine;

            } catch (SocketTimeoutException e) {
                System.out.println("[!] - Socket timeout! Waiting for reply...");
            } catch (IOException e) {
                System.out.println("[!] - Error in determining reply!");
                e.printStackTrace();
            }
        }

    }

}
