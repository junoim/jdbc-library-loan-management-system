# Analysis Document

## 1. Transaction Boundaries and Data Integrity

The most important part of this project is the use of explicit transaction management. In JDBC, auto-commit is normally enabled by default, which means every SQL statement is committed immediately after execution. This can be unsafe for multi-step business operations.

In this Library Loan Management System, processing a loan is not a single database action. It requires multiple steps:

1. Checking whether the book is available
2. Checking whether the member is active
3. Updating the book availability status
4. Inserting a new loan record
5. Updating the member's active loan count

These steps must behave like one single unit. If the book status is updated but the loan record is not inserted due to an error, the database becomes inconsistent. To avoid this problem, the program disables auto-commit using:

```java
conn.setAutoCommit(false);
```

After this, changes are not permanently saved until `conn.commit()` is called. If any exception occurs, `conn.rollback()` is executed. This restores the database to its earlier consistent state.

This follows the ACID properties:

- **Atomicity:** Either all steps of loan processing happen or none happen.
- **Consistency:** Constraints such as foreign keys and unique ISBN values are preserved.
- **Isolation:** Each transaction is handled independently from other operations.
- **Durability:** Once committed, changes are stored by Derby.

## 2. Savepoint Usage

The project also demonstrates savepoints. A savepoint allows partial rollback inside a transaction. In the loan process, a savepoint is created before the loan insertion step:

```java
Savepoint beforeLoanInsert = conn.setSavepoint("BeforeLoanInsert");
```

If a later step fails, the program can roll back to this savepoint. Although the final catch block performs a full rollback for safety, adding a savepoint demonstrates how partial recovery can be handled in more complex applications.

## 3. Why PreparedStatement is Used

The program uses `PreparedStatement` for almost all database operations. This is better than using plain `Statement` with string concatenation because:

1. It prevents SQL injection.
2. It separates SQL logic from user input.
3. It allows the database to reuse compiled query plans.
4. It makes the code cleaner and more reliable.

For example, instead of writing:

```java
"SELECT * FROM Books WHERE BookID = " + bookId
```

The program uses:

```java
SELECT * FROM Books WHERE BookID = ?
```

and passes the value using setter methods.

## 4. Performance Evaluation Findings

The performance evaluator compares different JDBC strategies.

### Individual Insert vs Batch Insert
Individual inserts call `executeUpdate()` repeatedly. Batch inserts use `addBatch()` and `executeBatch()`. Batch execution is usually faster because multiple operations are sent and handled together, reducing repeated JDBC overhead.

### Statement vs PreparedStatement
`Statement` requires SQL strings to be created dynamically. This can be unsafe and may involve repeated parsing. `PreparedStatement` is safer and can be more efficient when the same SQL structure is executed multiple times.

### Commit Per Operation vs Batched Commit
Committing after every operation is safer in some cases, but it creates more logging and disk overhead. Committing once after several operations is faster because Derby performs fewer commit operations.

### Full Table Scan vs Indexed Lookup
A full-table scan reads many rows. An indexed lookup searches using an index, such as `Loans.MemberID`, and is generally faster for selective queries. This shows why indexes are important on frequently searched columns.

## 5. Safety vs Speed Trade-off

The project shows that safe database programming and high performance must be balanced. Transactions and prepared statements improve correctness and security. Batch operations and grouped commits improve speed. However, very large transactions may use more memory and may hold locks for a longer time.

For a real library system, correctness is more important than raw speed. A wrongly issued loan or incorrect book status would be a serious data integrity problem. Therefore, transaction safety is necessary even if it adds slight overhead.

## 6. Conclusion

This project demonstrates a complete JDBC application with Apache Derby. It includes normalized database tables, constraints, indexes, transaction management, rollback handling, savepoints, CLI-based operations, and performance benchmarking. The results show that batch processing, prepared statements, indexed lookups, and grouped commits usually perform better than simple one-by-one operations.
