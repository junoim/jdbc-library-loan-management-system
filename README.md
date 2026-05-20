# End-to-End JDBC Library Loan Management System using Apache Derby

## Project Objective
This mini project implements a console-driven Library Loan Management System using Java JDBC and Apache Derby. It demonstrates explicit transaction management, rollback handling, savepoints, prepared statements, resource cleanup, and performance evaluation of different JDBC access patterns.

## Technologies Used
- Java 17
- JDBC
- Apache Derby Embedded Database
- Maven

## Project Structure
```text
src/main/java/com/miniproject/library/
├── ConnectionManager.java      # Derby connection, schema creation, seed data, shutdown
├── BusinessLogic.java          # Member/book CRUD and query operations
├── TransactionService.java     # Loan/return transactions, commit, rollback, savepoint
├── PerformanceEvaluator.java   # Benchmark tests and report generation
└── MainApp.java                # Console menu and workflow

docs/
├── analysis-document.md
└── sample-performance-report.md
```

## Database Schema
The application creates three normalized tables automatically:

1. `Members`
2. `Books`
3. `Loans`

Indexes are created on:
- `Books.ISBN`
- `Loans.MemberID`
- `Loans.ReturnDate`

## How to Run

### Option 1: Using Maven
```bash
mvn clean compile
mvn exec:java
```

### Option 2: Run from IDE
1. Open the folder in IntelliJ IDEA / Eclipse / NetBeans.
2. Let Maven download dependencies.
3. Run `MainApp.java`.

## Sample Menu
```text
========== Library Loan Management System ==========
1. Register Member
2. Add Book
3. Process Loan
4. Return Book
5. Show Active Loans by Member
6. Show Overdue Books
7. List Books
8. List Members
9. Demonstrate Rollback
10. Run Performance Benchmarks
0. Exit
```

## Important Features

### 1. Explicit Transaction Management
Auto-commit is disabled during loan and return operations:
```java
conn.setAutoCommit(false);
```
The transaction is committed only if all steps succeed. If any step fails, rollback is executed.

### 2. Loan Processing Transaction
The loan process includes:
1. Verify book availability
2. Verify member status
3. Update book availability
4. Insert loan record
5. Update member active loan count
6. Commit transaction

If any step fails, all changes are rolled back.

### 3. Savepoint Support
A savepoint is created before inserting the loan record. This demonstrates partial rollback support inside a transaction.

### 4. PreparedStatement Usage
All user-input based SQL operations use `PreparedStatement` to prevent SQL injection and improve safety.

### 5. Performance Evaluation
The benchmark module compares:
- Individual insert vs batch insert
- Statement vs PreparedStatement
- Commit per operation vs batched commit
- Full-table scan vs indexed lookup

The output shows:
- Operation type
- Record count
- Average execution time
- Standard deviation
- Throughput
- Observations

## Sample Test Cases

### Successful Loan
1. Choose option `7` to list books.
2. Choose option `8` to list members.
3. Choose option `3` and enter a valid BookID and MemberID.
4. Loan is processed and transaction is committed.

### Rollback Demo
Choose option `9`.
The program tries invalid IDs and rolls back the transaction.

### Duplicate Insert Test
Try adding a book with an existing ISBN. Derby will reject it because ISBN is unique.

## Notes
- The database is created automatically in the project folder as `lab10db`.
- Derby shutdown is handled when the program exits.
- Benchmark timings may differ based on laptop speed and current system load.
