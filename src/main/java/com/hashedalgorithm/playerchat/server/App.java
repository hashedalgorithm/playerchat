package com.hashedalgorithm.playerchat.server;

import java.io.IOException;
import java.util.logging.Logger;

public class App {

    private static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        System.out.println("Player Chat Server - by SanjayKumar Kumaravelan");
        System.out.println("-----------------------------------------------");
        System.out.println();

        try {
            Server server = new Server(12345);
            server.start();
        } catch (IOException e) {
            logger.severe(e.getMessage());
        }
    }
}
