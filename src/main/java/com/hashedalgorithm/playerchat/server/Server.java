package com.hashedalgorithm.playerchat.server;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private ServerSocket serverSocket;

    public Map<String, ClientInstance> clients = new HashMap<>();

    public Server(int port) throws IOException {
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (BindException be) {
            System.out.printf("[+] - Port %d is already in use.\n", port);
            System.exit(1);
        } catch (IOException e) {
            logger.severe(e.getMessage());
            System.exit(1);
        }
    }

    public ClientInstance getClient(String clientInstanceId) {
        return this.clients.getOrDefault(clientInstanceId, null);
    }

    public boolean isClientExists(String clientInstanceId) {
        return this.clients.containsKey(clientInstanceId);
    }

    public void closeClientInstance(String instanceId) throws IOException {
        ClientInstance client = this.clients.get(instanceId);

        if(client == null) {
            System.out.printf("[+] - No client with id %s found.\n", instanceId);
            return;
        }

        client.closeConnection();
        this.clients.remove(instanceId);
    }

    public void start() {
        new Thread(() -> {
            try {
                System.out.println("[+] - Listening for connections...");
                while (true) {

                    if(clients.size() >= 10) {
                        System.out.println("[+] - Maximum number of connections reached!.");
                        System.out.println("[+] - Stopped listening.");
                    }

                    Socket clientSocket = serverSocket.accept();

                    ClientInstance instance = new ClientInstance(this, clientSocket);
                    this.clients.put(instance.instanceId, instance);
                    instance.start();
                }
            } catch (IOException e) {
                logger.severe(e.getMessage());
            }

        }).start();
    }

}