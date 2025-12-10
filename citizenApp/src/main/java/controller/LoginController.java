package controller;

import service.LoginService;

import java.util.Scanner;
import javax.net.ssl.SSLSocket;

public class LoginController {
    public boolean login(Scanner scanner, SSLSocket socket) {
        while (true) {
            System.out.println();
            System.out.println("┌─────────────────────────────────┐");
            System.out.println("│ Enter your Citizen ID or 'exit' │");
            System.out.println("└─────────────────────────────────┘");
            System.out.print("▸ ");
            String citizenID = scanner.nextLine().trim();
            
            if (citizenID.equalsIgnoreCase("exit")) {
                System.out.println("\nExiting login.");
                return false;
            }

            if (!citizenID.matches("\\d{6}")) {
                System.out.println("\nCitizen ID must be a six digit integer");
                continue;
            }

            boolean result = LoginService.sendLogin(socket, citizenID);
            
            if (result) {
                System.out.println("\nLogin successful!");
                return true;
            } else {
                System.out.println("\nLogin failed, please try again.");
            }
        }
    }
}
