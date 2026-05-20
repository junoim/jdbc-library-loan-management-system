package com.miniproject.library;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class BusinessLogic {

    public void registerMember(String name, String email) {
        String sql = "INSERT INTO Members(Name, Email) VALUES(?, ?)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.executeUpdate();
            System.out.println("Member registered successfully.");
        } catch (SQLException e) {
            System.out.println("Could not register member: " + e.getMessage());
        }
    }

    public void addBook(String title, String author, String isbn) {
        String sql = "INSERT INTO Books(Title, Author, ISBN, Available) VALUES(?, ?, ?, 1)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setString(3, isbn);
            ps.executeUpdate();
            System.out.println("Book added successfully.");
        } catch (SQLException e) {
            System.out.println("Could not add book: " + e.getMessage());
        }
    }

    public void listBooks() {
        String sql = "SELECT BookID, Title, Author, ISBN, Available FROM Books ORDER BY BookID";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.println("\nBookID | Title | Author | ISBN | Available");
            System.out.println("-------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%d | %s | %s | %s | %s%n",
                        rs.getInt("BookID"), rs.getString("Title"), rs.getString("Author"),
                        rs.getString("ISBN"), rs.getInt("Available") == 1 ? "YES" : "NO");
            }
        } catch (SQLException e) {
            System.out.println("Could not list books: " + e.getMessage());
        }
    }

    public void listMembers() {
        String sql = "SELECT MemberID, Name, Email, ActiveLoans, Status FROM Members ORDER BY MemberID";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            System.out.println("\nMemberID | Name | Email | ActiveLoans | Status");
            System.out.println("-------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%d | %s | %s | %d | %s%n",
                        rs.getInt("MemberID"), rs.getString("Name"), rs.getString("Email"),
                        rs.getInt("ActiveLoans"), rs.getString("Status"));
            }
        } catch (SQLException e) {
            System.out.println("Could not list members: " + e.getMessage());
        }
    }

    public void showActiveLoansByMember(int memberId) {
        String sql = "SELECT l.LoanID, b.Title, l.LoanDate, l.DueDate " +
                "FROM Loans l JOIN Books b ON l.BookID = b.BookID " +
                "WHERE l.MemberID = ? AND l.ReturnDate IS NULL";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\nActive loans for member " + memberId);
                System.out.println("LoanID | Book | LoanDate | DueDate");
                System.out.println("-------------------------------------------------------------");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("%d | %s | %s | %s%n",
                            rs.getInt("LoanID"), rs.getString("Title"), rs.getDate("LoanDate"), rs.getDate("DueDate"));
                }
                if (!found) System.out.println("No active loans found.");
            }
        } catch (SQLException e) {
            System.out.println("Could not query loans: " + e.getMessage());
        }
    }

    public void showOverdueBooks() {
        String sql = "SELECT l.LoanID, m.Name, b.Title, l.DueDate " +
                "FROM Loans l JOIN Members m ON l.MemberID = m.MemberID " +
                "JOIN Books b ON l.BookID = b.BookID " +
                "WHERE l.ReturnDate IS NULL AND l.DueDate < ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\nOverdue Books");
                System.out.println("LoanID | Member | Book | DueDate");
                System.out.println("-------------------------------------------------------------");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("%d | %s | %s | %s%n",
                            rs.getInt("LoanID"), rs.getString("Name"), rs.getString("Title"), rs.getDate("DueDate"));
                }
                if (!found) System.out.println("No overdue books found.");
            }
        } catch (SQLException e) {
            System.out.println("Could not query overdue books: " + e.getMessage());
        }
    }

    public int countRows(String tableName) {
        String safeTable;
        switch (tableName.toUpperCase()) {
            case "MEMBERS" -> safeTable = "Members";
            case "BOOKS" -> safeTable = "Books";
            case "LOANS" -> safeTable = "Loans";
            default -> throw new IllegalArgumentException("Invalid table name");
        }
        try (Connection conn = ConnectionManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + safeTable)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            return -1;
        }
    }
}
