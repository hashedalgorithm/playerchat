package com.hashedalgorithm.playerchat.client;

import com.hashedalgorithm.playerchat.enums.Role;

import java.util.Scanner;

public class App {

    public static void main(String[] args) {

        int counter = 0;

        Scanner scanner = new Scanner(System.in);

        System.out.println("Player Chat - by SanjayKumar Kumaravelan");
        System.out.println("----------------------------------------");
        System.out.println();
        System.out.println();

        System.out.print("[+] - Enter your chat name: ");
        String playerName = scanner.nextLine();
//        System.out.print("[+] - Enter ip: ");
//        String ip = scanner.nextLine();
//        System.out.print("[+] - Enter server port: ");
//        int port = Integer.parseInt(scanner.nextLine().trim());

        Client client = new Client(playerName, "127.0.0.1", 12345);

        do {
            if(client.serverCommunicator.clientRole == Role.INITIATOR){
                System.out.printf("[%s]: ", playerName);
                String message = scanner.nextLine();

                client.sendMessage(message);
                counter++;
            }
            if(client.serverCommunicator.clientRole == Role.RECEIVER){
                System.out.println(client.waitForReply());
            }

        } while (counter < 10);

        System.out.println("Message Limit Reached! Exiting!");
        scanner.close();
    }
}
