package com.hashedalgorithm.playerchat.client;

import java.util.Scanner;

public class App {

    public static void main(String[] args) {

//        Scanner scanner = new Scanner(System.in);

        System.out.println("Player Chat Client - by SanjayKumar Kumaravelan");
        System.out.println("-----------------------------------------------");
        System.out.println();

//      System.out.print("[+] - Enter ip: ");
//      String ip = scanner.nextLine();
//      System.out.print("[+] - Enter server port: ");
//      int port = Integer.parseInt(scanner.nextLine().trim());

        Client client = new Client( "127.0.0.1", 12345);
        client.start();

//        try{
//            while(true){
//                System.out.printf("[%s]: ", name);
//                String message = scanner.nextLine();
//
//                client.sendMessage(message);
//            }
//        } catch (Exception e){
//            e.printStackTrace();
//        }

//        scanner.close();
    }
}
