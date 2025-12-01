package com.hashedalgorithm.playerchat.client;

import com.hashedalgorithm.playerchat.utils.MessageParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Client {
    private String name;
    private int to;
    public int assignedInstanceId;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private int counter = 0;
    private Scanner scanner;
    private final MessageParser parser = new MessageParser();


    public Client(Scanner scanner, String name, String ip, int port) {
        try{
            this.name = name;
            this.scanner = scanner;
            this.connectToServer(ip, port);


            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            this.handshake(name);
        } catch (IOException e) {
            System.err.println("[!] - Could not connect to the server");
            System.exit(-1);
        }
    }

    private void connectToServer(String ip, int port) throws IOException {
        try{
            this.clientSocket = new Socket(ip, port);
            System.out.println("[+] - Connected to server successfully!");
        } catch (SocketTimeoutException e){
            System.out.println("[!] - Could not connect to the server!");
        }
    }

    private void handshake(String name) throws SocketException {
        System.out.println("[+] - Initiating handshake with server");
        this.clientSocket.setSoTimeout(1000 * 60);

        while (true) {
            try {
                this.assignedInstanceId = Integer.parseInt(this.in.readLine());
                System.out.printf("[+] - Instance ID: %s. Share this to other user to start chatting with them.\n", this.assignedInstanceId);
                break;
            }
            catch (SocketTimeoutException e) {
                System.out.println("[+] - Timeout waiting for server!");
            }
            catch (IOException e) {
                System.out.println("[!] - Handshake failed! Exiting...");
                System.exit(-1);
            }
        }

        this.out.println(name);



        System.out.println("[+] - Handshake completed successfully!");
    }

    public void establishConnection(String ip, int port) throws IOException {
        System.out.println("[+] - 1. Send Chat Request\n 2. Listen for Chat Request\n 3. Exit");
        int request = Integer.parseInt(this.scanner.nextLine());
        int instanceId;
        switch (request) {
            case 1: {
                System.out.print("[+] - Enter player Id: ");
                instanceId = Integer.parseInt(scanner.nextLine());


                break;
            }
            case 2: break;
            case 3: break;
            default: System.out.println("[!] - Invalid request! Try again!");
        }

    }

    private void closeConnection() throws IOException {
        this.out.close();
        this.in.close();
        this.clientSocket.close();
    }

    public void listenForIncomingMessages(){
        new Thread(() -> {
            while (true) {
                String resp = this.waitForIncomingMessages();
                System.out.print("\r\033[2K");
                System.out.println(resp);
                System.out.printf("[%s]: ", this.name);
            }
        }).start();
    }

    public void sendMessage(String message) throws IOException {
        if(this.counter >= 10){
            System.out.println("[+] - Max limit reached!");
            this.closeConnection();
            throw new IOException("[+] - Max limit reached!");
        }

        Map<String, String> result = new HashMap<>(Map.of(
                "from", String.valueOf(this.assignedInstanceId),
                "to", String.valueOf(this.to),
                "message", message
        ));

        String serializedMessage = this.parser.serialize(result);

        this.out.println(serializedMessage);
        this.counter += 1;
    }
    private void processConnectionRequest(){

    }
    private void processMessage(String message){

    }
    private void processRawData(String raw){
        Map<String, String> parsed = parser.parseMessage(raw);
        String message = parsed.get("message");
        String from = parsed.get("from");

        return String.format("[%s]: %s", from, message);
    }

    public String waitForIncomingMessages() {
        try {
            String raw = in.readLine();
            if (raw == null) {
                throw new IllegalStateException("[!] - Could not read from server!");
            }


            Map<String, String> parsed = parser.parseMessage(raw);
            String message = parsed.get("message");
            String from = parsed.get("from");

            return String.format("[%s]: %s", from, message);

        } catch (SocketTimeoutException e) {
            System.out.println("[!] - Socket timeout! Waiting for reply...");
            return null;
        } catch (IOException e) {
            System.out.println("[!] - Error in determining reply!");
            return null;
        }

    }

}
