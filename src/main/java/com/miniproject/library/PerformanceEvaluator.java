package com.miniproject.library;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PerformanceEvaluator {
    private static final int RUNS = 3;

    public void runAllBenchmarks() {
        System.out.println("\nPerformance Benchmark Report");
        System.out.println("Warm-up started...");
        warmUp();

        List<Result> results = new ArrayList<>();
        results.add(compareIndividualInsert(1000));
        results.add(compareBatchInsert(1000));
        results.add(compareStatement(1000));
        results.add(comparePreparedStatement(1000));
        results.add(compareCommitEachOperation(100));
        results.add(compareSingleBatchCommit(100));
        results.add(indexedLookupTest(1000));
        results.add(fullTableScanTest(1000));

        printReport(results);
    }

    private void warmUp() {
        try (Connection conn = ConnectionManager.getConnection(); Statement st = conn.createStatement()) {
            for (int i = 0; i < 100; i++) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Books")) {
                    rs.next();
                }
            }
        } catch (SQLException e) {
            System.out.println("Warm-up warning: " + e.getMessage());
        }
    }

    private Result compareIndividualInsert(int count) {
        String sql = "INSERT INTO Books(Title, Author, ISBN, Available) VALUES(?, ?, ?, 1)";
        double[] times = new double[RUNS];
        for (int r = 0; r < RUNS; r++) {
            String tag = "IND" + System.currentTimeMillis() + "_" + r;
            long start = System.nanoTime();
            try (Connection conn = ConnectionManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < count; i++) {
                    ps.setString(1, "Individual Book " + i);
                    ps.setString(2, "Benchmark");
                    ps.setString(3, tag + "_" + i);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                System.out.println("Individual insert benchmark failed: " + e.getMessage());
            }
            times[r] = elapsedMs(start);
        }
        return new Result("Individual executeUpdate() insert", count, mean(times), std(times), "Simple but slower because each row is sent separately.");
    }

    private Result compareBatchInsert(int count) {
        String sql = "INSERT INTO Books(Title, Author, ISBN, Available) VALUES(?, ?, ?, 1)";
        double[] times = new double[RUNS];
        for (int r = 0; r < RUNS; r++) {
            String tag = "BAT" + System.currentTimeMillis() + "_" + r;
            long start = System.nanoTime();
            try (Connection conn = ConnectionManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < count; i++) {
                    ps.setString(1, "Batch Book " + i);
                    ps.setString(2, "Benchmark");
                    ps.setString(3, tag + "_" + i);
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) {
                System.out.println("Batch insert benchmark failed: " + e.getMessage());
            }
            times[r] = elapsedMs(start);
        }
        return new Result("Batch addBatch()+executeBatch() insert", count, mean(times), std(times), "Usually faster because many rows are executed together.");
    }

    private Result compareStatement(int count) {
        double[] times = new double[RUNS];
        for (int r = 0; r < RUNS; r++) {
            String tag = "ST" + System.currentTimeMillis() + "_" + r;
            long start = System.nanoTime();
            try (Connection conn = ConnectionManager.getConnection(); Statement st = conn.createStatement()) {
                for (int i = 0; i < count; i++) {
                    st.executeUpdate("INSERT INTO Books(Title, Author, ISBN, Available) VALUES('Statement Book " + i + "','Benchmark','" + tag + "_" + i + "',1)");
                }
            } catch (SQLException e) {
                System.out.println("Statement benchmark failed: " + e.getMessage());
            }
            times[r] = elapsedMs(start);
        }
        return new Result("Statement string concatenation", count, mean(times), std(times), "Less safe and repeated parsing can increase overhead.");
    }

    private Result comparePreparedStatement(int count) {
        String sql = "INSERT INTO Books(Title, Author, ISBN, Available) VALUES(?, ?, ?, 1)";
        double[] times = new double[RUNS];
        for (int r = 0; r < RUNS; r++) {
            String tag = "PS" + System.currentTimeMillis() + "_" + r;
            long start = System.nanoTime();
            try (Connection conn = ConnectionManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < count; i++) {
                    ps.setString(1, "Prepared Book " + i);
                    ps.setString(2, "Benchmark");
                    ps.setString(3, tag + "_" + i);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                System.out.println("PreparedStatement benchmark failed: " + e.getMessage());
            }
            times[r] = elapsedMs(start);
        }
        return new Result("PreparedStatement parameterized insert", count, mean(times), std(times), "Safer and can reuse compiled execution plan.");
    }

    private Result compareCommitEachOperation(int count) {
        String sql = "INSERT INTO Members(Name, Email) VALUES(?, ?)";
        double[] times = new double[RUNS];
        for (int r = 0; r < RUNS; r++) {
            long start = System.nanoTime();
            try (Connection conn = ConnectionManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                for (int i = 0; i < count; i++) {
                    ps.setString(1, "CommitEach " + i);
                    ps.setString(2, "commiteach" + System.nanoTime() + i + "@bench.com");
                    ps.executeUpdate();
                    conn.commit();
                }
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Commit-each benchmark failed: " + e.getMessage());
            }
            times[r] = elapsedMs(start);
        }
        return new Result("Transaction granularity: commit per operation", count, mean(times), std(times), "Safer per row but more disk/log overhead.");
    }

    private Result compareSingleBatchCommit(int count) {
        String sql = "INSERT INTO Members(Name, Email) VALUES(?, ?)";
        double[] times = new double[RUNS];
        for (int r = 0; r < RUNS; r++) {
            long start = System.nanoTime();
            try (Connection conn = ConnectionManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                for (int i = 0; i < count; i++) {
                    ps.setString(1, "BatchCommit " + i);
                    ps.setString(2, "batchcommit" + System.nanoTime() + i + "@bench.com");
                    ps.executeUpdate();
                }
                conn.commit();
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                System.out.println("Batch-commit benchmark failed: " + e.getMessage());
            }
            times[r] = elapsedMs(start);
        }
        return new Result("Transaction granularity: one commit for 100 ops", count, mean(times), std(times), "Faster because commit/log overhead is reduced.");
    }

    private Result indexedLookupTest(int count) {
        double[] times = new double[RUNS];
        for (int r = 0; r < RUNS; r++) {
            long start = System.nanoTime();
            try (Connection conn = ConnectionManager.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM Loans WHERE MemberID = ?")) {
                for (int i = 0; i < count; i++) {
                    ps.setInt(1, 1);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) { /* consume */ }
                    }
                }
            } catch (SQLException e) {
                System.out.println("Indexed lookup benchmark failed: " + e.getMessage());
            }
            times[r] = elapsedMs(start);
        }
        return new Result("Indexed lookup on Loans.MemberID", count, mean(times), std(times), "Uses index and is better for selective searches.");
    }

    private Result fullTableScanTest(int count) {
        double[] times = new double[RUNS];
        for (int r = 0; r < RUNS; r++) {
            long start = System.nanoTime();
            try (Connection conn = ConnectionManager.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM Loans")) {
                for (int i = 0; i < count; i++) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) { /* consume */ }
                    }
                }
            } catch (SQLException e) {
                System.out.println("Full table scan benchmark failed: " + e.getMessage());
            }
            times[r] = elapsedMs(start);
        }
        return new Result("Full-table scan on Loans", count, mean(times), std(times), "Scans all records and becomes slower as table grows.");
    }

    private double elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000.0;
    }

    private double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    private double std(double[] values) {
        double avg = mean(values);
        double sum = 0;
        for (double v : values) sum += Math.pow(v - avg, 2);
        return Math.sqrt(sum / values.length);
    }

    private void printReport(List<Result> results) {
        System.out.println("\n-------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-48s %-10s %-15s %-15s %-15s%n", "Operation", "Records", "Avg Time(ms)", "Std Dev", "Throughput");
        System.out.println("-------------------------------------------------------------------------------------------------------------");
        for (Result r : results) {
            double throughput = r.count / (r.avgMs / 1000.0);
            System.out.printf("%-48s %-10d %-15.2f %-15.2f %-15.2f%n", r.operation, r.count, r.avgMs, r.stdDev, throughput);
            System.out.println("Observation: " + r.note);
        }
        System.out.println("-------------------------------------------------------------------------------------------------------------");
    }

    private static class Result {
        String operation;
        int count;
        double avgMs;
        double stdDev;
        String note;

        Result(String operation, int count, double avgMs, double stdDev, String note) {
            this.operation = operation;
            this.count = count;
            this.avgMs = avgMs;
            this.stdDev = stdDev;
            this.note = note;
        }
    }
}
