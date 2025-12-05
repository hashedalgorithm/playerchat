package com.hashedalgorithm.playerchat.server;

import com.hashedalgorithm.playerchat.enums.ClientStatus;
import com.hashedalgorithm.playerchat.enums.PayloadValue;
import com.hashedalgorithm.playerchat.utils.MessageParser;
import com.hashedalgorithm.playerchat.enums.Payload;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;


public class ClientInstance extends Thread {
    private static final Logger logger = Logger.getLogger(ClientInstance.class.getName());
    private final Socket clientSocket;
    public String instanceId;
    private final Server server;
    private final PrintWriter out;
    private final BufferedReader in;
    private int counter = 0;
    MessageParser parser = new MessageParser();

    public ClientInstance(Server server, Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.server = server;

        this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

        this.handshake();
    }

    private void writeOutputBuffer(String message){
        this.out.println(message);
        this.out.flush();
    }

    private void sendConfirmationToClient() throws IOException {

        if(this.instanceId == null){
            this.rejectClientHandshakeRequest("anonymous");
            throw new IOException("[!] - Invalid request from this anonymous client! Dropping request!");
        }

        Map<String, String> result = new HashMap<>(Map.of(
                Payload.REQUEST.getValue(), PayloadValue.HANDSHAKE.getValue(),
                Payload.INSTANCE_ID.getValue(), this.instanceId,
                Payload.STATUS.getValue(), ClientStatus.SUCCESS.getValue()
        ));

        this.writeOutputBuffer(this.parser.serialize(result));
    }
    private void rejectClientHandshakeRequest(String from) throws IOException {
        Map<String, String> result = new HashMap<>(Map.of(
                Payload.REQUEST.getValue(), PayloadValue.HANDSHAKE.getValue(),
                Payload.INSTANCE_ID.getValue(), from,
                Payload.STATUS.getValue(), String.valueOf(ClientStatus.FAILED.getValue() )
        ));

        this.writeOutputBuffer(this.parser.serialize(result));
    }

    private void receiveInstanceIdFromClient() throws IOException {
        while (true) {
            try {
                String raw = this.in.readLine();

                Map<String, String> parsed = parser.parseMessage(raw);
                String req = parsed.get(Payload.REQUEST.getValue());
                String from = parsed.get(Payload.FROM.getValue());

                if(req == null || !req.equals(PayloadValue.HANDSHAKE.getValue())) {
                    this.rejectClientHandshakeRequest(Objects.requireNonNullElse(from, "anonymous"));
                    throw new IOException("[!] - Invalid request from this anonymous client! Dropping request!");
                }

                if(from == null){
                    this.rejectClientHandshakeRequest("anonymous");
                    throw new IOException("[!] - Invalid request from this anonymous client! Dropping request!");
                }

                boolean isClientExists =  this.server.isClientExists(from);
                if(isClientExists) {
                    this.rejectClientHandshakeRequest(from);
                    throw new IOException("[!] - Client with this name already exists! Dropping request!");
                }

                this.instanceId = from;
                return;
            } catch (SocketTimeoutException e) {
                System.out.println("[!] - Timeout in handshake! Retrying...");
            }
        }
    }

    private void handshake() {
        try{

            System.out.println("[+] - Initiating handshake with new client");

            this.receiveInstanceIdFromClient();
            this.clientSocket.setSoTimeout(1000 * 5);
            this.sendConfirmationToClient();

            System.out.printf("[+] - Handshake with client - %s completed successfully!\n",  this.instanceId);
            this.clientSocket.setSoTimeout(0);
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
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
        System.out.printf("[+] - Connection closed for client %s\n", this.instanceId);
    }

    public void sendMessage(String message, String to) {
        ClientInstance receiver = this.server.getClient(to);

        if(this.counter >= 10) {
            System.out.println("[+] - Maximum limit reached");
            this.closeOutputBuffer();
            return;
        }

        this.counter += 1;

        Map<String, String> result = new HashMap<>(Map.of(
                Payload.FROM.getValue(), this.instanceId,
                Payload.MESSAGE.getValue(), message
        ));

        String serializedMessage = this.parser.serialize(result);
        receiver.out.println(serializedMessage);
    }

    private void handleClientRawData(String raw) throws IOException {
        Map<String, String> parsed = parser.parseMessage(raw);

        String message = parsed.get(Payload.MESSAGE.getValue());
        String request = parsed.get(Payload.REQUEST.getValue());
        String status = parsed.get(Payload.STATUS.getValue());
        String from = parsed.get(Payload.FROM.getValue());
        String to = parsed.get(Payload.TO.getValue());


        if(request != null && from != null && to != null && message != null && status != null) {
            throw new IOException(String.format("[!] - Invalid payload received from client %s!", from));
        }


        if(request != null && from != null && to != null) {
            if(status != null) {
                this.forwardMessageRequestConfirmation(from, to, status);
                return;
            } else {
                this.forwardMessageRequest(from, to);
                return;
            }
        }

        if(message != null && from != null && to != null) {
            this.processClientMessage(from, to, message);
            return;
        }

        throw new IOException(String.format("[!] - Invalid payload received from client %s!", from));
    }

    private void forwardMessageRequestConfirmation(String from, String to, String status){


        System.out.printf("[+] - Message request confirmation from %s to %s is %s\n", from, to, status);

        ClientInstance receiver = this.server.getClient(to);
        Map<String, String> result = new HashMap<>(Map.of(
                Payload.REQUEST.getValue(), PayloadValue.MESSAGE.getValue(),
                Payload.STATUS.getValue(), status,
                Payload.FROM.getValue(), from
        ));

        if(receiver != null) {
            receiver.out.println(this.parser.serialize(result));
        } else {
            this.writeOutputBuffer(this.parser.serialize(result));
        }
    }

    public void forwardMessageRequest(String from, String to) {
        System.out.printf("[+] - Message request from %s to %s\n", from, to);

        ClientInstance receiver = this.server.getClient(to);

        if(receiver == null) {
            System.out.printf("[!] - %s No such player exists!\n", to);
            this.forwardMessageRequestConfirmation(to, from, ClientStatus.FAILED.getValue());
            return;
        }

        Map<String, String> result = new HashMap<>(Map.of(
                Payload.FROM.getValue(), this.instanceId,
                Payload.REQUEST.getValue(), PayloadValue.MESSAGE.getValue()
        ));

        String serializedRequest = this.parser.serialize(result);
        receiver.out.println(serializedRequest);
    }

    private void processClientMessage(String from, String to, String message) throws IOException {
        System.out.printf("[+] - Received message: from %s, to %s \n", from, to);
        this.sendMessage(message, to);
    }


    public void run() {
        try {
            while (true) {

                String raw = this.in.readLine();
                if (raw == null) {
                    break;
                }
                System.out.println(raw);
                this.handleClientRawData(raw);
            }

        }
        catch (SocketTimeoutException e){
            System.out.println("[!] - Listening...");
        }
        catch (IOException e) {
            System.out.printf("[!] - %s\n", e.getMessage());
        }
    }

}
