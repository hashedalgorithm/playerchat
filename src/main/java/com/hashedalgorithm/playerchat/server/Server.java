package com.hashedalgorithm.playerchat.server;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Server extends Thread {
    private ServerSocket serverSocket;
    public Map<String, ClientInstance> clients = new HashMap<>();

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

    public ClientInstance getClient(String clientInstanceId) {
        return this.clients.getOrDefault(clientInstanceId, null);
    }

    public void deleteClientInstance(String instanceId) throws IOException {
        ClientInstance client = this.clients.get(instanceId);

        if(client == null) {
            System.out.printf("[+] - No client with id %s found.\n", instanceId);
            return;
        }

        System.out.printf("[+] - Deleting client id %s\n", instanceId);
        this.clients.remove(instanceId);
    }

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