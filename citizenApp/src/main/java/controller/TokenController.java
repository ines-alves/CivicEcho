package controller;

import service.TokenService;

import java.util.Scanner;
import javax.net.ssl.SSLSocket;

public class TokenController {
    public boolean requestTokens(Scanner scanner, String citizenID) {
        while (true) {
            System.out.println();
            System.out.println("┌─────────────────────────────────┐");
            System.out.println("│  It seems that you ran out of   │");
            System.out.println("│     tokens or they expired!     │");
            System.out.println("├─────────────────────────────────┤");
            System.out.println("│ 1 ▸ Request Tokens              │");
            System.out.println("│ 0 ▸ Go Back                     │");
            System.out.println("└─────────────────────────────────┘");
            System.out.print("Option: ");
            String option = scanner.nextLine().trim();

            switch (option) {
                case "1":
                    boolean result = TokenService.sendTokenRequest(citizenID);
                    return result;
                case "0":
                    System.out.println("\nGoing back to main menu.");
                    return false;
                default:
                    System.out.println("\nInvalid option.");
            }
        }
    }
}
