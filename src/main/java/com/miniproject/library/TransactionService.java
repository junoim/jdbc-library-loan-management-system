package com.miniproject.library;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.LocalDate;

public class TransactionService {

    public void processLoan(int bookId, int memberId) {
        String checkBook = "SELECT Available FROM Books WHERE BookID = ?";
        String checkMember = "SELECT Status FROM Members WHERE MemberID = ?";
        String updateBook = "UPDATE Books SET Available = 0 WHERE BookID = ?";
        String insertLoan = "INSERT INTO Loans(MemberID, BookID, LoanDate, DueDate) VALUES(?, ?, ?, ?)";
        String updateMember = "UPDATE Members SET ActiveLoans = ActiveLoans + 1 WHERE MemberID = ?";

        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            Savepoint beforeLoanInsert = null;

            try {
                if (!isBookAvailable(conn, checkBook, bookId)) {
                    throw new SQLException("Book is not available or does not exist.");
                }
                if (!isMemberActive(conn, checkMember, memberId)) {
                    throw new SQLException("Member does not exist or is blocked.");
                }

                try (PreparedStatement ps = conn.prepareStatement(updateBook)) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

                beforeLoanInsert = conn.setSavepoint("BeforeLoanInsert");

                try (PreparedStatement ps = conn.prepareStatement(insertLoan)) {
                    ps.setInt(1, memberId);
                    ps.setInt(2, bookId);
                    ps.setDate(3, Date.valueOf(LocalDate.now()));
                    ps.setDate(4, Date.valueOf(LocalDate.now().plusDays(14)));
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(updateMember)) {
                    ps.setInt(1, memberId);
                    int rows = ps.executeUpdate();
                    if (rows == 0) {
                        if (beforeLoanInsert != null) conn.rollback(beforeLoanInsert);
                        throw new SQLException("Member update failed. Partial rollback to savepoint done.");
                    }
                }

                conn.commit();
                System.out.println("Loan processed successfully. Transaction committed.");
            } catch (SQLException e) {
                conn.rollback();
                System.out.println("Loan failed. Transaction rolled back. Reason: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("Transaction service error: " + e.getMessage());
        }
    }

    public void returnBook(int loanId) {
        String findLoan = "SELECT BookID, MemberID FROM Loans WHERE LoanID = ? AND ReturnDate IS NULL";
        String updateLoan = "UPDATE Loans SET ReturnDate = ? WHERE LoanID = ?";
        String updateBook = "UPDATE Books SET Available = 1 WHERE BookID = ?";
        String updateMember = "UPDATE Members SET ActiveLoans = ActiveLoans - 1 WHERE MemberID = ? AND ActiveLoans > 0";

        try (Connection conn = ConnectionManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int bookId;
                int memberId;
                try (PreparedStatement ps = conn.prepareStatement(findLoan)) {
                    ps.setInt(1, loanId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Active loan not found.");
                        bookId = rs.getInt("BookID");
                        memberId = rs.getInt("MemberID");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(updateLoan)) {
                    ps.setDate(1, Date.valueOf(LocalDate.now()));
                    ps.setInt(2, loanId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(updateBook)) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(updateMember)) {
                    ps.setInt(1, memberId);
                    ps.executeUpdate();
                }

                conn.commit();
                System.out.println("Book returned successfully. Transaction committed.");
            } catch (SQLException e) {
                conn.rollback();
                System.out.println("Return failed. Transaction rolled back. Reason: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("Return transaction error: " + e.getMessage());
        }
    }

    public void demonstrateRollback() {
        System.out.println("\nRollback demo: trying to issue a loan with invalid member/book IDs.");
        processLoan(99999, 99999);
        System.out.println("After rollback, database remains consistent because no partial update is committed.");
    }

    private boolean isBookAvailable(Connection conn, String sql, int bookId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("Available") == 1;
            }
        }
    }

    private boolean isMemberActive(Connection conn, String sql, int memberId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && "ACTIVE".equalsIgnoreCase(rs.getString("Status"));
            }
        }
    }
}
