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


/**
 * Client represents a chat client that connects to a {@link com.hashedalgorithm.playerchat.server.Server}.
 * Each client runs in its own thread and can send/receive messages from other clients.
 *
 * <p>The Client performs a handshake with the server, maintains a connection,
 * and enforces message limits using {@link #MAX_MESSAGES}.
 * It also manages separate threads for listening to incoming messages and user input.
 *
 * <p>Example usage:
 * <pre>
 *     Client client = new Client("127.0.0.1", 12345);
 *     client.start();
 * </pre>
 *
 * <p>Thread safety: The client uses multiple threads (main thread, listener, inputListener)
 * to handle messages asynchronously. Access to shared fields like {@link #counter} and
 * {@link #receivedMessageCounter} should be carefully managed.
 *
 * @author Sanjay
 * @version 1.0
 * @since 2025-12-06
 */

public class Client extends Thread {
    /** Maximum number of messages a client can send or receive */
    public static final int MAX_MESSAGES = 10;

    /** Unique identifier for this client */
    public String instanceId;

    /** Socket connected to the server */
    private Socket clientSocket;

    /** Instance ID of the connected recipient client */
    private String recipientInstanceId;

    /** Output stream to the server */
    private PrintWriter out;

    /** Input stream from the server */
    private BufferedReader in;

    /** Counter for messages sent */
    private int counter = 1;

    /** Counter for messages received */
    private int receivedMessageCounter = 1;

    /** Scanner to read user input */
    private final Scanner scanner = new Scanner(System.in);

    /** Parser for serializing/deserializing messages */
    private final MessageParser parser = new MessageParser();

    /** Thread to listen for incoming messages */
    private Thread listener;

    /** Thread to listen for user input messages */
    private Thread inputListener;

    /**
     * Constructs a Client that connects to the specified server IP and port.
     * Initiates a handshake immediately after connecting.
     *
     * @param ip   The server IP address
     * @param port The server port
     */
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

    /**
     * Prompts the user to enter their chat name and sets {@link #instanceId}.
     */
    private void initializeClient() {
        System.out.print("[+] - Enter your chat name: ");
        this.instanceId = scanner.nextLine();
    }

    /**
     * Connects the client to the server at the specified IP and port.
     *
     * @param ip   Server IP address
     * @param port Server port
     */
    private void connectToServer(String ip, int port) {
        try{
            this.clientSocket = new Socket(ip, port);
            System.out.println("[+] - Connected to server successfully!");
        } catch (IOException e) {
            System.err.println("[!] - Could not connect to the server!");
            System.exit(-1);
        }
    }

    /**
     * Performs the handshake process with the server and waits for acknowledgment.
     */
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

    /**
     * Sends the client instance ID to the server for handshake.
     */
    private void handshakeSendInstanceIdToServer() {
        Map<String, String> result = new HashMap<>(Map.of(
                Payload.REQUEST.getValue(), PayloadValue.HANDSHAKE.getValue(),
                Payload.FROM.getValue(), this.instanceId
        ));

        this.writeOutputBuffer(this.parser.serialize(result));
    }

    /**
     * Receives and validates the server handshake acknowledgment.
     * Exits the program if handshake fails.
     */
    private void handshake() throws SocketException {
        System.out.println("[+] - Initiating handshake with server");

        this.handshakeSendInstanceIdToServer();
        this.clientSocket.setSoTimeout(1000 * 5);
        this.handshakeReceiveAckFromServer();

        System.out.println("[+] - Handshake completed successfully!");
        this.clientSocket.setSoTimeout(0);
    }


    /**
     * Sends a serialized message to the server.
     *
     * @param message The serialized message string
     */
    private void writeOutputBuffer(String message) {
        this.out.println(message);
        this.out.flush();
    }

    /**
     * Sends a chat request to another client.
     *
     * @param to Recipient client ID
     */
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

    /**
     * Handles incoming chat request confirmation from the server.
     *
     * @param to Sender client ID
     */
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

    /**
     * Closes the client connection and associated streams.
     */
    private void closeConnection() {
        try{
            this.out.close();
            this.in.close();
            this.clientSocket.close();
        } catch(IOException e) {
            System.err.printf("[!] - %s\n", e.getMessage());
        }
    }

    /**
     * Processes a raw message received from the server.
     *
     * <p>This method parses the raw message using the {@link MessageParser} and
     * determines if it is a valid message payload. It handles the following:
     * <ul>
     *     <li>If the payload contains all fields (request, from, to, message, status), it
     *         is considered invalid and an {@link IOException} is thrown.</li>
     *     <li>If the payload contains a message and a sender, it calls
     *         {@link #processMessage(String, String)} to display the message.</li>
     *     <li>Any other payload structure is considered invalid and triggers an exception.</li>
     * </ul>
     *
     * @param raw The raw message string received from the server.
     * @throws IOException If the payload is invalid or cannot be processed.
     */
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

    /**
     * Handles an incoming message request from another client.
     *
     * <p>This method reads from the server input stream and waits for a message request
     * or confirmation. It enforces a retry mechanism and a socket timeout to handle
     * network delays. Specifically:
     * <ul>
     *     <li>If the client is already connected to another recipient, the request is ignored.</li>
     *     <li>If a valid SUCCESS status is received, the {@link #recipientInstanceId} is set and the method returns.</li>
     *     <li>If a FAILED status is received, an {@link IOException} is thrown.</li>
     *     <li>If no status is present, the method automatically confirms the request
     *         by calling {@link #handleMessageRequestConfirmation(String)}.</li>
     * </ul>
     *
     * <p>Retries up to 4 times in case of socket timeouts.
     *
     * @throws IOException If the payload from the server is invalid or connection fails.
     */
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

    /**
     * Listens for incoming messages from the server and processes them.
     */
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

    /**
     * Listens for user input messages and sends them to the recipient.
     */
    private void listenForMessageInputs() {
        while (this.counter <= MAX_MESSAGES) {
            System.out.printf("[%s]: {%d} - ", this.instanceId, this.counter);
            this.sendMessage(scanner.nextLine());
        }
    }

    /**
     * Sends a message to the connected recipient.
     *
     * @param message The message content
     */
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

    /**
     * Processes a message received from the server.
     *
     * @param from Sender client ID
     * @param message Message content
     * @throws IOException if an error occurs during processing
     */
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

    /**
     * Main thread execution for the client. Displays menu, handles
     * input, starts listener threads for messages and user input.
     */
    public void run() {
        System.out.println("[+] - Choose from menu");
        System.out.println("\t1. Send Chat Request\n\t2. Listen for Chat Request\n\t3. Exit");
        int choice = 0;
        boolean exit = false;
        try{


            while(!exit){
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
