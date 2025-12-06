package com.hashedalgorithm.playerchat.client;

/**
 * Entry point for the Player Chat Client application.
 *
 * <p>This class is responsible for starting the client application and initializing
 * a {@link Client} instance that connects to the chat server.
 *
 * <p>It prints a simple welcome message to the console and then starts the client
 * thread to handle user interaction, message sending, and receiving.
 */
public class App {

    /**
     * Main method to start the Player Chat Client application.
     *
     * <p>It creates a new {@link Client} instance configured to connect to the
     * server at the specified IP address and port, and then starts the client
     * in a separate thread.
     *
     * @param args Command-line arguments (not used in this application)
     */
    public static void main(String[] args) {

        System.out.println("Player Chat Client - by SanjayKumar Kumaravelan");
        System.out.println("-----------------------------------------------");
        System.out.println();

        // Initialize and start the client
        Client client = new Client( "127.0.0.1", 12345);
        client.start();

    }
}
