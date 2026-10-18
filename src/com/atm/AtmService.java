package com.atm;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AtmService {
    public static final BigDecimal MAX_DEPOSIT = new BigDecimal("1000000.00");
    public static final BigDecimal MAX_WITHDRAWAL = new BigDecimal("50000.00");

    public static class TransactionRow {
        public final Timestamp time;
        public final String type;
        public final BigDecimal amount;
        public final BigDecimal balance;
        public final String reference;

        public TransactionRow(Timestamp t, String ty, BigDecimal a, BigDecimal b, String r) {
            this.time = t;
            this.type = ty;
            this.amount = a;
            this.balance = b;
            this.reference = r;
        }

        public String displayTime() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
        }
    }

    public BigDecimal getBalance() throws SQLException {
        if (!Session.isActive()) {
            throw new SecurityException("Unauthorized: No active session.");
        }
        String sql = "SELECT balance FROM accounts WHERE account_id = ? AND status = 'ACTIVE'";
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, Session.getAccountId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Account unavailable or inactive.");
                }
                return rs.getBigDecimal(1);
            }
        }
    }

    public BigDecimal deposit(BigDecimal amount) throws SQLException {
        if (amount == null) {
            throw new IllegalArgumentException("Deposit amount cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be strictly greater than zero.");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Deposit amount cannot have more than 2 decimal places.");
        }
        if (amount.compareTo(MAX_DEPOSIT) > 0) {
            throw new IllegalArgumentException("Deposit amount exceeds per-transaction limit of ₹" + MAX_DEPOSIT.toPlainString() + ".");
        }
        return changeBalance(amount, "DEPOSIT");
    }

    public BigDecimal withdraw(BigDecimal amount) throws SQLException {
        if (amount == null) {
            throw new IllegalArgumentException("Withdrawal amount cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be strictly greater than zero.");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Withdrawal amount cannot have more than 2 decimal places.");
        }
        if (amount.compareTo(MAX_WITHDRAWAL) > 0) {
            throw new IllegalArgumentException("Withdrawal amount exceeds per-transaction limit of ₹" + MAX_WITHDRAWAL.toPlainString() + ".");
        }
        return changeBalance(amount.negate(), "WITHDRAWAL");
    }

    private BigDecimal changeBalance(BigDecimal delta, String type) throws SQLException {
        if (!Session.isActive()) {
            throw new SecurityException("Unauthorized: No active session.");
        }
        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Transaction amount must be non-zero.");
        }

        try (Connection cn = Database.getConnection()) {
            cn.setAutoCommit(false);
            try {
                BigDecimal current;
                String lockSql = "SELECT balance, status FROM accounts WHERE account_id = ? FOR UPDATE";
                try (PreparedStatement lock = cn.prepareStatement(lockSql)) {
                    lock.setLong(1, Session.getAccountId());
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Account unavailable.");
                        }
                        if (!"ACTIVE".equalsIgnoreCase(rs.getString("status"))) {
                            throw new SQLException("Account is not active.");
                        }
                        current = rs.getBigDecimal("balance");
                    }
                }

                BigDecimal next = current.add(delta);
                if (next.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("Insufficient funds. Available balance: ₹" + current.toPlainString());
                }

                String updateSql = "UPDATE accounts SET balance = ? WHERE account_id = ?";
                try (PreparedStatement up = cn.prepareStatement(updateSql)) {
                    up.setBigDecimal(1, next);
                    up.setLong(2, Session.getAccountId());
                    up.executeUpdate();
                }

                String insertSql = "INSERT INTO transactions(account_id, transaction_type, amount, balance_after, reference_code) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ins = cn.prepareStatement(insertSql)) {
                    ins.setLong(1, Session.getAccountId());
                    ins.setString(2, type);
                    ins.setBigDecimal(3, delta.abs());
                    ins.setBigDecimal(4, next);
                    // Use full 32-character reference code to eliminate collision risk
                    ins.setString(5, UUID.randomUUID().toString().replace("-", "").toUpperCase());
                    ins.executeUpdate();
                }

                cn.commit();
                return next;
            } catch (Exception e) {
                try {
                    cn.rollback();
                } catch (SQLException ignored) {
                }
                if (e instanceof SQLException) throw (SQLException) e;
                if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
                throw new SQLException("Transaction processing failed", e);
            } finally {
                try {
                    cn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public List<TransactionRow> recentTransactions(int limit) throws SQLException {
        if (!Session.isActive()) {
            throw new SecurityException("Unauthorized: No active session.");
        }
        if (limit <= 0 || limit > 100) {
            limit = 10;
        }

        List<TransactionRow> rows = new ArrayList<TransactionRow>();
        String sql = "SELECT transaction_time, transaction_type, amount, balance_after, reference_code "
                + "FROM transactions WHERE account_id = ? ORDER BY transaction_time DESC, transaction_id DESC LIMIT ?";

        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, Session.getAccountId());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TransactionRow(
                            rs.getTimestamp(1),
                            rs.getString(2),
                            rs.getBigDecimal(3),
                            rs.getBigDecimal(4),
                            rs.getString(5)
                    ));
                }
            }
        }
        return rows;
    }
}
