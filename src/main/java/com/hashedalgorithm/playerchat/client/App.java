package com.hashedalgorithm.playerchat.client;

public class App {

    public static void main(String[] args) {

        System.out.println("Player Chat Client - by SanjayKumar Kumaravelan");
        System.out.println("-----------------------------------------------");
        System.out.println();

        Client client = new Client( "127.0.0.1", 12345);
        client.start();

    }
}
