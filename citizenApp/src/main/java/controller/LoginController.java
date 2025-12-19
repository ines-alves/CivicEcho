package controller;

import service.LoginService;

import java.util.Scanner;
import javax.net.ssl.SSLSocket;

public class LoginController {
    public String login(Scanner scanner, SSLSocket socket) {
        int attempts = 3;
        while (true) {
            System.out.println();
            System.out.println("┌─────────────────────────────────┐");
            System.out.println("│ Enter your Citizen ID or 'exit' │");
            System.out.println("└─────────────────────────────────┘");
            System.out.print("▸ ");
            String citizenID = scanner.nextLine().trim();
            
            if (citizenID.equalsIgnoreCase("exit")) {
                System.out.println("\nExiting login.");
                return null;
            }

            boolean authenticated = LoginService.sendLogin(socket, citizenID);
            
            if (authenticated) {
                System.out.println("\nLogin successful!");
                return citizenID;
            } else {
                attempts--;
                if (attempts == 0) {
                    System.out.println("\nMaximum login attempts reached.");
                    return null;
                }
                System.out.println("\nLogin failed, please try again.");
                System.out.println("For now the only Citizen IDs supported are \"citizen1\" and \"citizen2\".");
            }
        }
    }
}
