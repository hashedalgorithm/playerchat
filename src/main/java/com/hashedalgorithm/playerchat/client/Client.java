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
    public static final int MAX_MESSAGES = 3;
    public String instanceId;
    private Socket clientSocket;
    private String recipientInstanceId;
    private PrintWriter out;
    private BufferedReader in;
    private int counter = 1;
    private final Scanner scanner = new Scanner(System.in);
    private final MessageParser parser = new MessageParser();
    private Thread listener;
    private Thread inputListener;
    private int receivedMessageCounter = 1;

    public Client(String ip, int port) {
        try{

            this.initializeClient();
            this.connectToServer(ip, port);

            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            this.handshake();
        } catch(IOException e){
            System.err.printf("[!] - %s\n", e.getMessage());
        }

    }

    private void initializeClient() {
        System.out.print("[+] - Enter your chat name: ");
        this.instanceId = scanner.nextLine();
    }

    private void connectToServer(String ip, int port) {
        try{
            this.clientSocket = new Socket(ip, port);
            System.out.println("[+] - Connected to server successfully!");
        } catch (IOException e) {
            System.err.println("[!] - Could not connect to the server!");
            System.exit(-1);
        }
    }

    private void handshakeReceiveAckFromServer() {
        int retries = 4;
        while (retries > 0) {
            try {
                String raw = this.in.readLine();
                if(raw == null) throw new NullPointerException();

                Map<String, String> parsed = parser.parseMessage(raw);

                String req = parsed.get(Payload.REQUEST.getValue());
                String id = parsed.get(Payload.INSTANCE_ID.getValue());
                String status = parsed.get(Payload.STATUS.getValue());

                if(req == null || id == null || status == null) throw new IOException("Invalid server response!");

                if(!id.equals(this.instanceId) ) throw new IOException("Instance Id Mismatch!");


                if(status.equals(ClientStatus.SUCCESS.getValue())) {
                    System.out.printf("[+] - Instance ID: %s. Share this to other user to start chatting with them.\n", this.instanceId);
                    return;
                }

                if(status.equals(ClientStatus.FAILED.getValue())) throw new IOException("Client/Server rejected connection!");

                throw new IOException("Invalid server response!");
            }
            catch (NullPointerException e) {
                System.err.println("[!] - Connection closed with server! Handshake failed!");
                System.exit(-1);
            }
            catch (SocketTimeoutException e) {
                retries--;
                System.out.println("[+] - Waiting for server!");
            }
            catch (IOException e) {
                System.err.printf("[!] - %s\n!", e.getMessage());
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

        this.writeOutputBuffer(this.parser.serialize(result));
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

    private void handleMessageRequestConfirmation(String to) {
        System.out.printf("[+] - Accepting message request from %s \n", to);
        Map<String, String> result = new HashMap<>(Map.of(
                Payload.FROM.getValue(), this.instanceId,
                Payload.TO.getValue(), to,
                Payload.REQUEST.getValue(), PayloadValue.MESSAGE.getValue(),
                Payload.STATUS.getValue(), ClientStatus.SUCCESS.getValue()
        ));

        this.writeOutputBuffer(this.parser.serialize(result));
    }


    private void closeConnection() {
        try{
            this.out.close();
            this.in.close();
            this.clientSocket.close();
        } catch(IOException e) {
            System.err.printf("[!] - %s\n", e.getMessage());
        }
    }

    private void processServerRawData(String raw) throws IOException {
        Map<String, String> parsed = parser.parseMessage(raw);

        String request = parsed.get(Payload.REQUEST.getValue());
        String from = parsed.get(Payload.FROM.getValue());
        String to = parsed.get(Payload.TO.getValue());
        String status = parsed.get(Payload.STATUS.getValue());
        String message = parsed.get(Payload.MESSAGE.getValue());

        if(null != request && from != null && to != null && message != null && status != null) {
            throw new IOException(String.format("Invalid payload received from server %s!", from));
        }

        if(message != null && from != null) {
            this.processMessage(from, message);
            return;
        }

        throw new IOException(String.format("Invalid payload received from client %s!", from));
    }

    private void handleMessageRequest() throws IOException {
        int retries =  4;
        this.clientSocket.setSoTimeout(1000 * 60);

        if(this.recipientInstanceId != null){
            System.out.printf("[+] - Already connected with a client %s. Dropping Message Request!\n", this.recipientInstanceId);
            return;
        }
        while(retries > 0) {
            try{
                String raw = this.in.readLine();

                if(raw == null) throw new NullPointerException();

                Map<String, String> parsed = parser.parseMessage(raw);

                String request = parsed.get(Payload.REQUEST.getValue());
                String from = parsed.get(Payload.FROM.getValue());
                String status = parsed.get(Payload.STATUS.getValue());

                if(request == null || from == null) throw new IOException("Invalid payload received from server!");

                if(status != null) {
                    if(status.equals(PayloadValue.SUCCESS.getValue())) {
                        System.out.printf("[+] - Connected with %s!\n", from);
                        this.recipientInstanceId = from;
                        return;
                    } else if(status.equals(PayloadValue.FAILED.getValue())) {
                        this.recipientInstanceId = null;
                        throw new IOException(String.format("Could not connect with %s!\n", from));
                    } else throw new IOException(String.format("Invalid payload received from server! %s!", from));
                } else {
                    this.handleMessageRequestConfirmation(from);
                    this.recipientInstanceId = from;
                    return;
                }
            } catch (NullPointerException e) {
                System.err.println("[!] - Connection closed with server! Message request failed!");
                return;
            }
            catch (SocketTimeoutException e){
                retries--;
                System.out.println("[!] - Waiting for server!");
            } catch (IOException e) {
                System.out.printf("[!] - %s\n", e.getMessage());
            }
        }
    }


    private void listenForIncomingMessages() {
        while (this.receivedMessageCounter <= MAX_MESSAGES) {
            if(this.recipientInstanceId == null){
                System.out.println("[!] - No recipient is connected!");
                break;
            }

            try {
                String raw = in.readLine();
                if (raw == null) throw new NullPointerException();

                this.processServerRawData(raw);
            } catch (NullPointerException e) {
                System.err.println("[!] - Connection closed with server!");
                return;
            }
            catch (SocketTimeoutException e) {
            //  Continue listening..
            } catch (IOException e) {
                System.out.printf("[!] - %s\n", e.getMessage());
            }
        }
    }

    private void listenForMessageInputs() {
        while (this.counter <= MAX_MESSAGES) {
            System.out.printf("[%s]: {%d} - ", this.instanceId, this.counter);
            this.sendMessage(scanner.nextLine());
        }
    }

    private void sendMessage(String message) {
        if(this.recipientInstanceId == null){
            System.out.println("[+] - No Recipient is connected! Try again after starting a session\n");
            return;
        }

        if(this.counter > MAX_MESSAGES){
            System.out.println("[+] - Max limit reached! Now you can only receive messages!");
            return;
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

    private void processMessage(String from, String message) throws IOException {
        // Moves the cursor to first position
        System.out.print("\r\033[2K");
        // Replaces the whole line
        System.out.printf("[%s]: %s\n", from, message);
        if(this.counter <= MAX_MESSAGES){
            System.out.printf("[%s]: {%d} - ", this.instanceId, this.counter);
        }
        this.receivedMessageCounter += 1;
    }

    public void run() {
        System.out.println("[+] - Choose from menu");
        System.out.println("1. Send Chat Request\n2. Listen for Chat Request\n3. Exit");
        int choice = 0;
        boolean exit = false;
        try{


            while(exit == false){
                try{
                    System.out.print("[+] Your choice: ");
                    choice = Integer.parseInt(this.scanner.nextLine());
                } catch (NumberFormatException e){
                    continue;
                }
                switch (choice) {
                    case 1: {
                        System.out.print("[+] - Enter player Id: ");
                        String playerId = scanner.nextLine();
                        this.sendMessageRequest(playerId);
                        System.out.printf("[+] - Chat request to %s sent successfully!\n", playerId);
                        System.out.println("[+] - Waiting for the recipient to accept request");

                        this.handleMessageRequest();
                        exit = true;
                        break;
                    }
                    case 2: {
                        System.out.println("[+] - Waiting for the recipient to accept request");
                        this.handleMessageRequest();
                        exit = true;
                        break;
                    }
                    case 3: {
                        System.out.println("[+] - Exiting...");
                        this.closeConnection();
                        System.exit(-1);
                        break;
                    }
                    default: System.out.println("[!] - Invalid request! Try again!");
                }
            }

            if(this.counter > MAX_MESSAGES && this.receivedMessageCounter > MAX_MESSAGES) {
                this.closeConnection();
                System.out.println("[+] - Exiting...!");
                System.exit(-1);
            }

            if(this.listener == null) {
                this.listener = new Thread(this::listenForIncomingMessages);
                this.listener.start();
            }

            if(this.inputListener == null){
                this.inputListener = new Thread(this::listenForMessageInputs);
                this.inputListener.start();
            }

        } catch (IOException e){
            System.err.printf("[!] - %s\n", e.getMessage());
            this.scanner.close();
        }

    }


}
