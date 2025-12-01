package com.hashedalgorithm.playerchat.server;

import com.hashedalgorithm.playerchat.utils.MessageParser;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


public class ClientInstance extends Thread {
    private static final Logger logger = Logger.getLogger(ClientInstance.class.getName());
    private final Socket clientSocket;
    private final int clientInstanceId;
    private final Server server;
    private final PrintWriter out;
    private final BufferedReader in;
    private String name;
    private int counter = 0;
    MessageParser parser = new MessageParser();

    public ClientInstance(int instanceId, Server server, Socket clientSocket) throws IOException {
        this.clientInstanceId = instanceId;
        this.clientSocket = clientSocket;
        this.server = server;

        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        this.handshake();
    }

    private void sendInstanceMetadetailsToClient() throws IOException {
        Map<String, String> result = new HashMap<>(Map.of(
                "id", String.valueOf(this.clientInstanceId)
        ));

        String serializedMessage = this.parser.serialize(result);
        this.out.println(serializedMessage);
    }

    private void receiveInstanceMetadetailsFromClient() throws IOException {
        while (true) {
            try {
                String raw = this.in.readLine();

                Map<String, String> parsed = parser.parseMessage(raw);

                String name = parsed.get("name");
                if(name == null) {
                    throw new IOException("[!] - Invalid request!");
                }

                this.name = name;
                return;
            } catch (SocketTimeoutException e) {
                System.out.println("[!] - Timeout while handshake! Retrying...");
            }
        }
    }

    private void handshake() {
        try{

            System.out.printf("[+] - Initiating handshake with client - %d\n", this.clientInstanceId);

            this.sendInstanceMetadetailsToClient();
            this.clientSocket.setSoTimeout(5000);
            this.receiveInstanceMetadetailsFromClient();
            this.clientSocket.setSoTimeout(1000 * 60 * 2);

            System.out.printf("[+] - Handshake with client[%d] - %s completed successfully!\n",  this.clientInstanceId, this.name);
            this.clientSocket.setSoTimeout(0);
        }
        catch (IOException e){
            System.out.println("[!] - Handshake failed! Exiting...");
            System.exit(-1);
        }
    }

    private String getClientName() {
        return this.name;
    }


    public void closeInputBuffer() throws IOException {
        this.in.close();
    }
    public void closeOutputBuffer() {
        this.out.close();
    }

    public void closeConnection() throws IOException {
        this.out.close();
        this.in.close();
        this.clientSocket.close();
        System.out.printf("[+] - Connection closed for client %s\n", this.name);
    }

    public void sendMessage(String message, int to) throws IOException {
        ClientInstance receiver = this.server.getClient(to);

        if(this.counter >= 10) {
            System.out.println("[+] - Maximum limit reached");
            this.closeOutputBuffer();
            return;
        }

        this.counter = counter + 1;

        Map<String, String> result = new HashMap<>(Map.of(
                "from", String.valueOf(this.clientInstanceId),
                "to", String.valueOf(to),
                "message", message
        ));

        String serializedMessage = this.parser.serialize(result);

        receiver.out.println(serializedMessage);
    }

    private void sendRequest(int to) {

    }

    private void processClientRawData(String raw) throws IOException {
        Map<String, String> parsed = parser.parseMessage(raw);

        String message = parsed.get("message");
        int request = Integer.parseInt(parsed.get("request"));
        int from = Integer.parseInt(parsed.get("from"));
        int to = Integer.parseInt(parsed.get("to"));

        if(request > 0) {
            this.processClientRequests(request);
            return;
        }

        if(message != null && to > 0) {
            this.processClientMessage(from, to, message);
        }

    }

    public void processClientRequests(int request) throws IOException {
        new Thread(() -> {
            while (true) {
                ClientInstance receiver = this.server.getClient(request);

                if(receiver == null) {
                    System.out.println("[!] - No such player exists!");
                    break;
                }

                Map<String, String> result = new HashMap<>(Map.of(
                        "from", String.valueOf(this.clientInstanceId),
                        "request", String.valueOf(request)

                ));
                receiver.out.println(parser.serialize(result));

            }
        }).start();
    }

    private void processClientMessage(int from, int to, String message) throws IOException {
        ClientInstance receiver = this.server.getClient(to);

        System.out.printf("[+] - Received message: from %s, to %s \n", from, receiver.getClientName());
        this.sendMessage(message, receiver.clientInstanceId);
    }


    public void run() {
        try {
            while (true) {

                String raw = this.in.readLine();
                if (raw == null) {
                    break;
                }

                this.processClientRawData(raw);
            }

        }
        catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }

}
