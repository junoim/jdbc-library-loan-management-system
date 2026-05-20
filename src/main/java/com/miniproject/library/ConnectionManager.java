package com.miniproject.library;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionManager {
    private static final String DB_URL = "jdbc:derby:lab10db;create=true";
    private static final String SHUTDOWN_URL = "jdbc:derby:lab10db;shutdown=true";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            createTables(conn);
            createIndexes(conn);
            seedData(conn);
            verifyMetadata(conn);
        } catch (SQLException e) {
            System.out.println("Database initialization error: " + e.getMessage());
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            if (!tableExists(conn, "MEMBERS")) {
                st.executeUpdate("CREATE TABLE Members (" +
                        "MemberID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY, " +
                        "Name VARCHAR(100) NOT NULL, " +
                        "Email VARCHAR(120) UNIQUE, " +
                        "ActiveLoans INT DEFAULT 0 CHECK (ActiveLoans >= 0), " +
                        "Status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (Status IN ('ACTIVE','BLOCKED'))" +
                        ")");
            }

            if (!tableExists(conn, "BOOKS")) {
                st.executeUpdate("CREATE TABLE Books (" +
                        "BookID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY, " +
                        "Title VARCHAR(150) NOT NULL, " +
                        "Author VARCHAR(100), " +
                        "ISBN VARCHAR(30) UNIQUE NOT NULL, " +
                        "Available SMALLINT DEFAULT 1 CHECK (Available IN (0,1))" +
                        ")");
            }

            if (!tableExists(conn, "LOANS")) {
                st.executeUpdate("CREATE TABLE Loans (" +
                        "LoanID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY, " +
                        "MemberID INT NOT NULL, " +
                        "BookID INT NOT NULL, " +
                        "LoanDate DATE NOT NULL, " +
                        "DueDate DATE NOT NULL, " +
                        "ReturnDate DATE, " +
                        "FOREIGN KEY (MemberID) REFERENCES Members(MemberID), " +
                        "FOREIGN KEY (BookID) REFERENCES Books(BookID)" +
                        ")");
            }
        }
    }

    private static void createIndexes(Connection conn) {
        createIndexIfMissing(conn, "IDX_BOOKS_ISBN", "CREATE INDEX idx_books_isbn ON Books(ISBN)");
        createIndexIfMissing(conn, "IDX_LOANS_MEMBER", "CREATE INDEX idx_loans_member ON Loans(MemberID)");
        createIndexIfMissing(conn, "IDX_LOANS_RETURNDATE", "CREATE INDEX idx_loans_returnDate ON Loans(ReturnDate)");
    }

    private static void createIndexIfMissing(Connection conn, String indexName, String sql) {
        try (ResultSet rs = conn.getMetaData().getIndexInfo(null, null, null, false, false)) {
            while (rs.next()) {
                String existing = rs.getString("INDEX_NAME");
                if (existing != null && existing.equalsIgnoreCase(indexName)) {
                    return;
                }
            }
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
            }
        } catch (SQLException ignored) {
            // Index may already exist. Ignore to keep bootstrapping simple.
        }
    }

    private static void seedData(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Members")) {
            rs.next();
            if (rs.getInt(1) > 0) return;
        }

        BusinessLogic logic = new BusinessLogic();
        logic.registerMember("Shradha Mohanty", "shradha@example.com");
        logic.registerMember("Amit Kumar", "amit@example.com");
        logic.registerMember("Priya Das", "priya@example.com");

        logic.addBook("Database System Concepts", "Silberschatz", "ISBN-DB-001");
        logic.addBook("Java Complete Reference", "Herbert Schildt", "ISBN-JAVA-002");
        logic.addBook("Operating System Concepts", "Galvin", "ISBN-OS-003");
        logic.addBook("Computer Networks", "Tanenbaum", "ISBN-CN-004");
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName.toUpperCase(), null)) {
            return rs.next();
        }
    }

    private static void verifyMetadata(Connection conn) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("Connected to: " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
        } catch (SQLException e) {
            System.out.println("Metadata check failed: " + e.getMessage());
        }
    }

    public static void shutdownDatabase() {
        try {
            DriverManager.getConnection(SHUTDOWN_URL);
        } catch (SQLException e) {
            if ("08006".equals(e.getSQLState())) {
                System.out.println("Derby database shut down successfully.");
            } else {
                System.out.println("Derby shutdown message: " + e.getMessage());
            }
        }
    }
}
