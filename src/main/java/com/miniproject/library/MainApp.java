package com.miniproject.library;

import java.util.Scanner;

public class MainApp {
    public static void main(String[] args) {
        ConnectionManager.initializeDatabase();

        BusinessLogic business = new BusinessLogic();
        TransactionService transactionService = new TransactionService();
        PerformanceEvaluator evaluator = new PerformanceEvaluator();

        try (Scanner sc = new Scanner(System.in)) {
            int choice;
            do {
                printMenu();
                choice = readInt(sc, "Enter choice: ");
                switch (choice) {
                    case 1 -> {
                        System.out.print("Enter member name: ");
                        String name = sc.nextLine();
                        System.out.print("Enter email: ");
                        String email = sc.nextLine();
                        business.registerMember(name, email);
                    }
                    case 2 -> {
                        System.out.print("Enter book title: ");
                        String title = sc.nextLine();
                        System.out.print("Enter author: ");
                        String author = sc.nextLine();
                        System.out.print("Enter ISBN: ");
                        String isbn = sc.nextLine();
                        business.addBook(title, author, isbn);
                    }
                    case 3 -> {
                        int bookId = readInt(sc, "Enter BookID: ");
                        int memberId = readInt(sc, "Enter MemberID: ");
                        transactionService.processLoan(bookId, memberId);
                    }
                    case 4 -> {
                        int loanId = readInt(sc, "Enter LoanID: ");
                        transactionService.returnBook(loanId);
                    }
                    case 5 -> {
                        int memberId = readInt(sc, "Enter MemberID: ");
                        business.showActiveLoansByMember(memberId);
                    }
                    case 6 -> business.showOverdueBooks();
                    case 7 -> business.listBooks();
                    case 8 -> business.listMembers();
                    case 9 -> transactionService.demonstrateRollback();
                    case 10 -> evaluator.runAllBenchmarks();
                    case 0 -> System.out.println("Exiting application...");
                    default -> System.out.println("Invalid choice.");
                }
            } while (choice != 0);
        } finally {
            ConnectionManager.shutdownDatabase();
        }
    }

    private static void printMenu() {
        System.out.println("\n========== Library Loan Management System ==========");
        System.out.println("1. Register Member");
        System.out.println("2. Add Book");
        System.out.println("3. Process Loan");
        System.out.println("4. Return Book");
        System.out.println("5. Show Active Loans by Member");
        System.out.println("6. Show Overdue Books");
        System.out.println("7. List Books");
        System.out.println("8. List Members");
        System.out.println("9. Demonstrate Rollback");
        System.out.println("10. Run Performance Benchmarks");
        System.out.println("0. Exit");
    }

    private static int readInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(sc.nextLine().trim());
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}
