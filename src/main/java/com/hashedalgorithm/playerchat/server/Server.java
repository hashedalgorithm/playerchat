package com.hashedalgorithm.playerchat.server;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Server represents a simple chat server that listens for incoming client connections,
 * manages connected clients, and maintains a mapping of client instance IDs to their
 * respective ClientInstance threads.
 *
 * <p>The server accepts new connections until a maximum number of clients (10) is reached.
 * Each client connection is handled in its own thread using the ClientInstance class.
 *
 * <p>Example usage:
 * <pre>
 *     Server server = new Server(12345);
 *     server.start();
 * </pre>
 *
 * <p>Thread safety: The {@link #clients} map is not synchronized. If multiple threads
 * access it concurrently, consider wrapping it with {@link java.util.Collections#synchronizedMap(Map)}.
 *
 * @author Sanjay
 * @version 1.0
 * @since 2025-12-06
 */

public class Server extends Thread {

    /** The server socket used to listen for incoming client connections */
    private ServerSocket serverSocket;
    /** Mapping of client instance IDs to ClientInstance objects */
    public Map<String, ClientInstance> clients = new HashMap<>();

    /**
     * Creates a new server instance listening on the specified port.
     *
     * @param port The port number on which the server will listen.
     * @throws RuntimeException if the server socket cannot be created.
     */
    public Server(int port) {
        try {
            System.out.printf("[+] - Starting Server on port: %s\n", port);
            this.serverSocket = new ServerSocket(port);
        } catch (BindException be) {
            System.out.printf("[+] - Port %d is already in use.\n", port);
            System.exit(1);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Retrieves a connected client instance by its instance ID.
     *
     * @param clientInstanceId The unique ID of the client.
     * @return The ClientInstance if found; otherwise, null.
     */
    public ClientInstance getClient(String clientInstanceId) {
        return this.clients.getOrDefault(clientInstanceId, null);
    }

    /**
     * Deletes a client instance from the server's clients map.
     *
     * <p>Note: This does not close the client's socket. Closing the client connection
     * should be handled separately via the ClientInstance object.
     *
     * @param instanceId The unique ID of the client to remove.
     * @throws IOException if an I/O error occurs while deleting the client.
     */
    public void deleteClientInstance(String instanceId) throws IOException {
        ClientInstance client = this.clients.get(instanceId);

        if(client == null) {
            System.out.printf("[+] - No client with id %s found.\n", instanceId);
            return;
        }

        System.out.printf("[+] - Deleting client id %s\n", instanceId);
        this.clients.remove(instanceId);
    }

    /**
     * Starts the server thread, continuously listening for new client connections.
     *
     * <p>If the maximum number of allowed clients (10) is reached, the server
     * will temporarily stop accepting new connections.
     */
    public void run() {
        try {
            System.out.println("[+] - Server Started. Listening for connections.");

            while (true) {

                if(clients.size() >= 10) {
                    System.out.println("[+] - Maximum number of connections reached!.");
                    System.out.println("[+] - Stopped listening.");
                    continue;
                }

                Socket clientSocket = serverSocket.accept();

                ClientInstance instance = new ClientInstance(this, clientSocket);
                this.clients.put(instance.instanceId, instance);
                instance.start();
            }

        } catch (IOException e) {
            System.out.printf("[!] - %s\n", e.getMessage());
        }

    }

}