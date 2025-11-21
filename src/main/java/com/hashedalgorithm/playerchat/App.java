package com.hashedalgorithm.playerchat;

import java.util.Scanner;

import com.hashedalgorithm.playerchat.models.Player;
import com.hashedalgorithm.playerchat.services.MessagingService;

public class App {

    public static void main(String[] args) {

        System.out.println("Player Chat - by SanjayKumar Kumaravelan");
        System.out.println("----------------------------------------");
        System.out.println();
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        System.out.printf("[+] - Enter initiator player name: ");
        String initiatorPlayerName = scanner.nextLine();

        System.out.printf("[+] - Enter receiver player name: ");
        String receiverPlayerName = scanner.nextLine();

        Player initiator = new Player(initiatorPlayerName);
        Player receiver = new Player(receiverPlayerName);

        MessagingService service = new MessagingService(initiator, receiver);

        do {
            System.out.printf("[%s]: ", initiator.playerName);
            String msgI = scanner.nextLine();
            service.sendMessage(initiator.playerId, receiver.playerId, msgI);
            System.out.printf("[%s]: ", receiver.playerName);
            String msgR = scanner.nextLine();
            service.sendMessage(receiver.playerId, initiator.playerId, msgR);
        } while (!service.checkPlayersMessageLimit());

        System.out.println("Message Limit Reached! Exiting!");
        scanner.close();
    }
}
