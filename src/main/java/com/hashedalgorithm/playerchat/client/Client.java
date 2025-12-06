package com.hashedalgorithm.playerchat.client;

import com.hashedalgorithm.playerchat.enums.ClientStatus;
import com.hashedalgorithm.playerchat.enums.Payload;
import com.hashedalgorithm.playerchat.enums.PayloadValue;
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

public class Client extends Thread {
    public String instanceId;
    private Socket clientSocket;
    private String recipientInstanceId;
    private PrintWriter out;
    private BufferedReader in;
    private int counter = 0;
    private final Scanner scanner = new Scanner(System.in);
    private final MessageParser parser = new MessageParser();
    private Thread listener;

    public Client(String ip, int port) {
        try{
            this.initializeClient();
            this.connectToServer(ip, port);

            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            this.handshake();

        } catch (IOException e) {
            System.err.println("[!] - Could not connect to the server");
            System.exit(-1);
        }
    }

    private void initializeClient() {
        System.out.print("[+] - Enter your chat name: ");
        this.instanceId = scanner.nextLine();
    }

    private void connectToServer(String ip, int port) throws IOException {
        try{
            this.clientSocket = new Socket(ip, port);
            System.out.println("[+] - Connected to server successfully!");
        } catch (SocketTimeoutException e){
            System.out.println("[!] - Could not connect to the server!");
        }
    }

    private void handshakeReceiveAckFromServer() {
        while (true) {
            try {
                String raw = this.in.readLine();
                if(raw == null) {
                    throw new IOException("Raw message is empty!");
                }
                Map<String, String> parsed = parser.parseMessage(raw);

                String req = parsed.get(Payload.REQUEST.getValue());
                String id = parsed.get(Payload.INSTANCE_ID.getValue());
                String status = parsed.get(Payload.STATUS.getValue());

                if(req == null || id == null || status == null) {
                    throw new IOException("Invalid server response!");
                }

                if(!id.equals(this.instanceId) ) {
                    throw new IOException("[!] - Instance Id Mismatch!");
                }

                if(status.equals(ClientStatus.SUCCESS.getValue())) {
                    System.out.printf("[+] - Instance ID: %s. Share this to other user to start chatting with them.\n", this.instanceId);
                    return;
                }

                if(status.equals(ClientStatus.FAILED.getValue())) {
                    throw new IOException();
                }

            }
            catch (SocketTimeoutException e) {
                System.out.println("[+] - Waiting for server!");
            }
            catch (IOException e) {
                System.out.println("[!] - Handshake failed! Exiting...");
                System.exit(-1);
            }
        }
    }

    private void handshakeSendInstanceIdToServer() {
        Map<String, String> result = new HashMap<>(Map.of(
                Payload.REQUEST.getValue(), PayloadValue.HANDSHAKE.getValue(),
                Payload.FROM.getValue(), this.instanceId
        ));

        String serializedRequest = this.parser.serialize(result);
        this.out.println(serializedRequest);
        this.out.flush();
    }

    private void handshake() throws SocketException {
        System.out.println("[+] - Initiating handshake with server");

        this.handshakeSendInstanceIdToServer();
        this.clientSocket.setSoTimeout(1000 * 5);
        this.handshakeReceiveAckFromServer();

        System.out.println("[+] - Handshake completed successfully!");
        this.clientSocket.setSoTimeout(0);
    }

    private void writeOutputBuffer(String message) {
        this.out.println(message);
        this.out.flush();
    }

    private void sendMessageRequest(String to) {
        if(this.recipientInstanceId != null){
            System.out.printf("[+] - Already connected with a client %s.Dropping Chat Request Acknowledgement!\n", this.instanceId);
            return;
        }

        Map<String, String> result = new HashMap<>(Map.of(
                Payload.FROM.getValue(), this.instanceId,
                Payload.TO.getValue(), to,
                Payload.REQUEST.getValue(), PayloadValue.MESSAGE.getValue()
        ));

        this.writeOutputBuffer(this.parser.serialize(result));
    }

    private void handleMessageRequestConfirmation(String to, ClientStatus clientStatus) {
        System.out.printf("[+] - Accepting message request from %s \n", to);
        Map<String, String> result = new HashMap<>(Map.of(
                Payload.FROM.getValue(), this.instanceId,
                Payload.TO.getValue(), to,
                Payload.REQUEST.getValue(), PayloadValue.MESSAGE.getValue(),
                Payload.STATUS.getValue(), clientStatus.getValue()
        ));

        this.writeOutputBuffer(this.parser.serialize(result));
    }


    private void closeConnection() throws IOException {
        this.out.close();
        this.in.close();
        this.clientSocket.close();
    }

    private void processServerRawData(String raw) throws IOException {
        Map<String, String> parsed = parser.parseMessage(raw);

        String request = parsed.get(Payload.REQUEST.getValue());
        String from = parsed.get(Payload.FROM.getValue());
        String to = parsed.get(Payload.TO.getValue());
        String status = parsed.get(Payload.STATUS.getValue());
        String message = parsed.get(Payload.MESSAGE.getValue());

        if(null != request && from != null && to != null && message != null && status != null) {
            throw new IOException(String.format("[!] - Invalid payload received from server %s!", from));
        }

        if(message != null && from != null) {
            this.processMessage(from, message);
            return;
        }

        throw new IOException(String.format("[!] - Invalid payload received from client %s!", from));
    }

    public void handleMessageRequest() throws IOException {
        this.clientSocket.setSoTimeout(1000 * 60);

        if(this.recipientInstanceId != null){
            System.out.printf("[+] - Already connected with a client %s. Dropping Message Request!\n", this.recipientInstanceId);
            return;
        }
        try{
            while(true) {
                String raw = this.in.readLine();

                if(raw == null) {
                    System.out.println("[!] - Received empty response from server!");
                    return;
                }

                Map<String, String> parsed = parser.parseMessage(raw);

                String request = parsed.get(Payload.REQUEST.getValue());
                String from = parsed.get(Payload.FROM.getValue());
                String status = parsed.get(Payload.STATUS.getValue());

                if(request == null || from == null) {
                    throw new IOException("[!] - Invalid payload received from server!");
                }

                if(status != null) {
                    if(status.equals(PayloadValue.SUCCESS.getValue())) {
                        System.out.printf("[!] - Connected with %s!\n", from);
                        this.recipientInstanceId = from;
                        return;
                    }

                    if(status.equals(PayloadValue.FAILED.getValue())) {
                        System.out.printf("[!] - Could not connect with %s!\n", from);
                        this.recipientInstanceId = null;
                        return;
                    }

                    throw new IOException(String.format("[!] - Invalid payload received from server! %s!", from));
                }else{
                    this.handleMessageRequestConfirmation(from, ClientStatus.SUCCESS);
                    this.recipientInstanceId = from;
                    return;
                }
            }
        } catch (SocketTimeoutException e){
            System.out.println("[!] - Waiting for server!");
        }
    }


    public void listenForIncomingMessages(){
        while (true) {
            if(this.recipientInstanceId == null){
                continue;
            }

            try {
                String raw = in.readLine();
                if (raw == null) {
                    throw new IllegalStateException("[!] - Could not read from server!");
                }

                this.processServerRawData(raw);
            } catch (SocketTimeoutException e) {
                System.out.println("[!] - Socket timeout! Waiting for reply...");
            } catch (IOException e) {

            }
        }
    }

    public void sendMessage(String message) throws IOException {
        if(this.recipientInstanceId == null){
            System.out.println("[+] - No Recipient is connected! Try again after starting a session\n");
            return;
        }

        if(this.counter >= 10){
            System.out.println("[+] - Max limit reached!");
            this.closeConnection();
            throw new IOException("[+] - Max limit reached!");
        }

        Map<String, String> result = new HashMap<>(Map.of(
                Payload.FROM.getValue(), this.instanceId,
                Payload.TO.getValue(), this.recipientInstanceId,
                Payload.MESSAGE.getValue(), message
        ));

        String serializedMessage = this.parser.serialize(result);

        this.out.println(serializedMessage);
        this.counter += 1;
    }

    private void processMessage(String from, String message){
        System.out.print("\r\033[2K");
        System.out.printf("[%s]: %s\n", from, message);
        System.out.printf("[%s]: ", this.instanceId);
    }

    public void run() {
        System.out.println("[+] - Choose from menu");
        System.out.println("1. Send Chat Request\n2. Listen for Chat Request\n3. Exit");
        System.out.print("[+] Your choice: ");
        int choice = Integer.parseInt(this.scanner.nextLine());


        try{
            while(true) {
                if(this.recipientInstanceId == null){
                    switch (choice) {
                        case 1: {
                            System.out.print("[+] - Enter player Id: ");
                            String playerId = scanner.nextLine();
                            this.sendMessageRequest(playerId);
                            System.out.println("[+] - Chat request sent successfully!");
                            System.out.println("[+] - Waiting for the recipient to accept request");

                            this.handleMessageRequest();
                            continue;
                        }
                        case 2: {
                            System.out.println("[+] - Waiting for the recipient to accept request");
                            this.handleMessageRequest();
                            break;
                        }
                        case 3: {
                            System.out.println("[+] - Exiting...");
                            System.exit(-1);
                            break;
                        }
                        default: System.out.println("[!] - Invalid request! Try again!");
                    }
                } else {
                    if(this.listener == null) {
                        this.listener = new Thread(this::listenForIncomingMessages);
                        this.listener.start();
                    }

                    System.out.printf("[%s]: ", this.instanceId);
                    this.sendMessage(scanner.nextLine());
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }

    }


}
