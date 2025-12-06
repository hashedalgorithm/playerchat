package com.hashedalgorithm.playerchat.server;

public class App {
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
