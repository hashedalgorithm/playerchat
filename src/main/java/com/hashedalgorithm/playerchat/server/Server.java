package com.hashedalgorithm.playerchat.server;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Server {
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private ServerSocket serverSocket;

    public Map<Integer, ClientInstance> clients = new HashMap<>();

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

    public ClientInstance getClient(int clientInstanceId) {
        return this.clients.getOrDefault(clientInstanceId, null);
    }

    private int generateId() {
        return this.clients.size() + 1;
    }

    public void closeClientInstance(int instanceId) throws IOException {
        ClientInstance client = this.clients.get(instanceId);

        if(client == null) {
            System.out.printf("[+] - No client with id %d found.\n", instanceId);
            return;
        }

        client.closeConnection();
        this.clients.remove(instanceId);
    }

    private void listen() {
        new Thread(() -> {
            try {
                System.out.println("[+] - Listening for connections...");
                while (true) {

                    if(clients.size() >= 10) {
                        System.out.println("[+] - Maximum number of connections reached!.");
                        System.out.println("[+] - Stopped listening.");
                    }

                    Socket clientSocket = serverSocket.accept();

                    int instanceId = generateId();
                    ClientInstance instance = new ClientInstance(instanceId,this, clientSocket);
                    this.clients.put(instanceId, instance);
                    instance.start();
                }
            } catch (IOException e) {
                logger.severe(e.getMessage());
            }

        }).start();
    }

    public void start() {
        this.listen();
    }

}

// req
// from
// to
// msg
// id
// name