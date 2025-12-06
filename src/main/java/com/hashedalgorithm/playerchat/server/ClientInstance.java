package com.hashedalgorithm.playerchat.server;

import com.hashedalgorithm.playerchat.enums.ClientStatus;
import com.hashedalgorithm.playerchat.enums.Payload;
import com.hashedalgorithm.playerchat.enums.PayloadValue;
import com.hashedalgorithm.playerchat.utils.MessageParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

/**
 * ClientInstance represents a connected client on the chat server.
 * Each ClientInstance runs in its own thread, handles communication
 * with its respective client socket, and manages handshake, message
 * sending, and receiving.
 *
 * <p>This class enforces a maximum number of messages per client using
 * {@link #MAX_MESSAGES}. Messages are forwarded to other clients via
 * the {@link Server} instance.
 *
 * <p>Example usage:
 * <pre>
 *     Socket socket = serverSocket.accept();
 *     ClientInstance client = new ClientInstance(server, socket);
 *     client.start();
 * </pre>
 *
 * <p>Thread safety: Each client runs in its own thread, but {@link Server#clients}
 * map access must be synchronized if accessed by multiple threads concurrently.
 *
 * @author Sanjay
 * @version 1.0
 * @since 2025-12-06
 */

public class ClientInstance extends Thread {

    /** Maximum messages allowed per client */
    private final int MAX_MESSAGES= 10;

    /** Unique identifier for this client instance */
    public String instanceId;

    /** Socket connected to the client */
    private final Socket clientSocket;

    /** Reference to the main Server instance */
    private final Server server;

    /** Output stream to the client */
    private final PrintWriter out;

    /** Input stream to the client */
    private final BufferedReader in;

    /** Counter to track number of messages sent */
    private int counter = 0;

    /** Parser for serializing and deserializing messages */
    private final MessageParser parser = new MessageParser();

    /**
     * Constructs a ClientInstance for a given server and client socket.
     * Initiates a handshake to authenticate and register the client.
     *
     * @param server       Reference to the server instance
     * @param clientSocket The connected client socket
     * @throws IOException if an I/O error occurs during handshake setup
     */
    public ClientInstance(Server server, Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.server = server;

        this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

        this.handshake();
    }

    /**
     * Sends a message to the client via the output buffer.
     *
     * @param message The message to send
     */
    private void writeOutputBuffer(String message){
        this.out.println(message);
        this.out.flush();
    }

    /**
     * Sends handshake confirmation to the client with status SUCCESS.
     *
     * @throws IOException if the client instance ID is null or handshake fails
     */
    private void sendConfirmationToClient() throws IOException {

        if(this.instanceId == null){
            this.rejectClientHandshakeRequest(null);
            throw new IOException("Invalid request from this anonymous client! Dropping request!");
        }

        Map<String, String> result = new HashMap<>(Map.of(
                Payload.REQUEST.getValue(), PayloadValue.HANDSHAKE.getValue(),
                Payload.INSTANCE_ID.getValue(), this.instanceId,
                Payload.STATUS.getValue(), ClientStatus.SUCCESS.getValue()
        ));

        this.writeOutputBuffer(this.parser.serialize(result));
    }

    /**
     * Rejects a client handshake request with status FAILED.
     *
     * @param from The client ID attempting handshake
     */
    private void rejectClientHandshakeRequest(String from) {
        Map<String, String> result = new HashMap<>(Map.of(
                Payload.REQUEST.getValue(), PayloadValue.HANDSHAKE.getValue(),
                Payload.INSTANCE_ID.getValue(), String.valueOf(from),
                Payload.STATUS.getValue(), String.valueOf(ClientStatus.FAILED.getValue() )
        ));

        this.writeOutputBuffer(this.parser.serialize(result));
    }

    /**
     * Receives and validates the client's instance ID during handshake.
     *
     * @throws IOException if the handshake fails, client already exists, or input is invalid
     */
    private void receiveInstanceIdFromClient() throws IOException {
        int retries = 3;
        while (retries != 0) {
            try {
                String raw = this.in.readLine();

                Map<String, String> parsed = parser.parseMessage(raw);
                String req = parsed.get(Payload.REQUEST.getValue());
                String from = parsed.get(Payload.FROM.getValue());

                if (req == null || !req.equals(PayloadValue.HANDSHAKE.getValue()) || from == null) {
                    this.rejectClientHandshakeRequest(from);
                    throw new IOException("Invalid request from this anonymous client! Dropping request!");
                }

                ClientInstance client = this.server.getClient(from);
                if (client != null) {
                    this.rejectClientHandshakeRequest(from);
                    throw new IOException(String.format("Client with this name -  %s already exists! Dropping request!", from));
                }

                this.instanceId = from;
                return;
            } catch (NullPointerException e) {
                System.err.println("[!] - Connection closed! Handshake failed!");
                return;
            }
            catch (SocketTimeoutException e) {
                retries--;
                System.out.println("[!] - Timeout in handshake! Retrying...");
            } catch (IOException e) {
                System.out.printf("[!] - %s\n", e.getMessage());
            }
        }
        System.out.println("[!] - Timeout in handshake! Aborting...");
    }

    /**
     * Performs the handshake process with the client, including receiving instance ID
     * and sending confirmation.
     */
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
            System.out.printf("[!] - %s\n", e.getMessage());
        }
    }

    /**
     * Closes the connection to the client and removes it from the server's client map.
     */
    private void closeConnection() {
        try{
            this.out.close();
            this.in.close();
            this.server.deleteClientInstance(this.instanceId);
        } catch (IOException e){
            System.out.printf("[!] - %s\n", e.getMessage());
        }
    }

    /**
     * Sends a message to another client via the server.
     *
     * @param message The message to send
     * @param to      The recipient client instance ID
     * @throws IOException if the message limit is reached
     */
    private void sendMessage(String message, String to) throws IOException {
        ClientInstance receiver = this.server.getClient(to);

        if(this.counter >= MAX_MESSAGES) {
            this.out.close();
            throw new IOException("Maximum limit reached");
        }

        this.counter += 1;

        Map<String, String> result = new HashMap<>(Map.of(
                Payload.FROM.getValue(), this.instanceId,
                Payload.MESSAGE.getValue(), String.format("{%d} - %s", this.counter, message)
        ));

        String serializedMessage = this.parser.serialize(result);
        receiver.out.println(serializedMessage);
    }


    /**
     * Parses and handles raw messages received from the client.
     *
     * @param raw The raw serialized message from the client
     * @throws IOException if the payload is invalid
     */
    private void handleClientRawData(String raw) throws IOException {
        Map<String, String> parsed = parser.parseMessage(raw);

        String message = parsed.get(Payload.MESSAGE.getValue());
        String request = parsed.get(Payload.REQUEST.getValue());
        String status = parsed.get(Payload.STATUS.getValue());
        String from = parsed.get(Payload.FROM.getValue());
        String to = parsed.get(Payload.TO.getValue());


        if(request != null && from != null && to != null && message != null && status != null) {
            throw new IOException(String.format("Invalid payload received from client %s!", from));
        }


        if(request != null && from != null && to != null) {
            if(status != null) {
                this.forwardMessageRequestConfirmation(from, to, status);
            } else {
                this.forwardMessageRequest(from, to);
            }
            return;
        }

        if(message != null && from != null && to != null) {
            this.processClientMessage(from, to, message);
            return;
        }

        throw new IOException(String.format("Invalid payload received from client %s!", from));
    }

    /**
     * Sends a confirmation of a message request to the recipient.
     *
     * @param from   The sender client ID
     * @param to     The recipient client ID
     * @param status The status of the request
     */
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

    /**
     * Forwards a message request from a sender to the recipient.
     *
     * @param from The sender client ID
     * @param to   The recipient client ID
     */
    private void forwardMessageRequest(String from, String to) {
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

    /**
     * Processes a client message and forwards it to the recipient.
     *
     * @param from    Sender client ID
     * @param to      Recipient client ID
     * @param message The message content
     * @throws IOException If sending fails
     */
    private void processClientMessage(String from, String to, String message) throws IOException {
        System.out.printf("[+] - Received message: from %s, to %s \n", from, to);
        this.sendMessage(message, to);
    }

    /**
     * The main thread execution. Continuously reads messages from the client
     * and handles them accordingly.
     */
    public void run() {
        try {
            while (true) {
                String raw = this.in.readLine();
                if (raw == null) {
                   throw new NullPointerException();
                }

                this.handleClientRawData(raw);
            }

        }
        catch (SocketTimeoutException e){
            // Do nothing and continue listening.
        }
        catch (NullPointerException npe) {
            System.out.printf("[!] - Connection closed with %s!\n", this.instanceId);
            this.closeConnection();
        }
        catch (IOException e) {
            System.out.printf("[!] - %s\n", e.getMessage());
        }
    }

}
