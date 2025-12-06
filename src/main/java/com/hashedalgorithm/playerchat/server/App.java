package com.hashedalgorithm.playerchat.server;

/**
 * Entry point for the Player Chat Server application.
 *
 * <p>This class is responsible for starting the chat server and initializing
 * a {@link Server} instance that listens for incoming client connections
 * on the specified port.
 *
 * <p>The server handles multiple clients by creating a new thread for each
 * connected client. Any exceptions during startup are caught and logged.
 */
public class App {
    /**
     * Main method to start the Player Chat Server application.
     *
     * <p>It prints a welcome message to the console, initializes a {@link Server}
     * instance on port 12345, and starts the server in a new thread. In case of
     * any exception during server startup, it logs the error message and
     * indicates that the server will attempt to restart.
     *
     * @param args Command-line arguments (not used in this application)
     */
    public static void main(String[] args) {
        System.out.println("Player Chat Server - by SanjayKumar Kumaravelan");
        System.out.println("-----------------------------------------------");
        System.out.println();

        try {
            Server server = new Server(12345);
            server.start();
        }
        catch (Exception e) {
            System.out.printf("[!] - %s\n", e.getMessage());
            System.out.println("Restarting server...");
        }
    }
}
