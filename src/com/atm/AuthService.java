package com.atm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {
    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }

    public boolean login(String cardNumber, String pin) throws SQLException, AuthException {
        if (cardNumber == null || !cardNumber.matches("\\d{16}")) {
            throw new AuthException("Card number must be exactly 16 digits.");
        }
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new AuthException("PIN must be exactly 4 digits.");
        }

        String sql = "SELECT a.account_id, a.card_number, a.pin_hash, a.pin_salt, a.failed_attempts, a.status, c.full_name "
                + "FROM accounts a "
                + "JOIN customers c ON c.customer_id = a.customer_id "
                + "WHERE a.card_number = ?";

        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, cardNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new AuthException("Invalid card number or PIN.");
                }

                long accountId = rs.getLong("account_id");
                String card = rs.getString("card_number");
                String fullName = rs.getString("full_name");
                String status = rs.getString("status");
                int failedAttempts = rs.getInt("failed_attempts");
                String storedHash = rs.getString("pin_hash");
                String salt = rs.getString("pin_salt");

                if ("BLOCKED".equalsIgnoreCase(status)) {
                    throw new AuthException("Card is BLOCKED due to excessive failed attempts. Please contact customer service.");
                }
                if (!"ACTIVE".equalsIgnoreCase(status)) {
                    throw new AuthException("Account is not active.");
                }

                String computedHash;
                if (salt != null && !salt.trim().isEmpty()) {
                    computedHash = hashWithSalt(pin, salt.trim());
                } else {
                    computedHash = hash(pin);
                }

                boolean matches = MessageDigest.isEqual(
                        storedHash.trim().toLowerCase().getBytes(StandardCharsets.UTF_8),
                        computedHash.trim().toLowerCase().getBytes(StandardCharsets.UTF_8)
                );

                if (matches) {
                    if (failedAttempts > 0) {
                        try (PreparedStatement rst = cn.prepareStatement("UPDATE accounts SET failed_attempts = 0 WHERE account_id = ?")) {
                            rst.setLong(1, accountId);
                            rst.executeUpdate();
                        }
                    }
                    if (salt == null || salt.trim().isEmpty()) {
                        String newSalt = generateSalt();
                        String upgradedHash = hashWithSalt(pin, newSalt);
                        try (PreparedStatement upg = cn.prepareStatement("UPDATE accounts SET pin_hash = ?, pin_salt = ? WHERE account_id = ?")) {
                            upg.setString(1, upgradedHash);
                            upg.setString(2, newSalt);
                            upg.setLong(3, accountId);
                            upg.executeUpdate();
                        }
                    }
                    Session.start(accountId, card, fullName);
                    return true;
                } else {
                    int newAttempts = failedAttempts + 1;
                    if (newAttempts >= 3) {
                        try (PreparedStatement blk = cn.prepareStatement("UPDATE accounts SET failed_attempts = ?, status = 'BLOCKED' WHERE account_id = ?")) {
                            blk.setInt(1, newAttempts);
                            blk.setLong(2, accountId);
                            blk.executeUpdate();
                        }
                        throw new AuthException("Incorrect PIN. Card has been BLOCKED after 3 consecutive failed attempts.");
                    } else {
                        try (PreparedStatement inc = cn.prepareStatement("UPDATE accounts SET failed_attempts = ? WHERE account_id = ?")) {
                            inc.setInt(1, newAttempts);
                            inc.setLong(2, accountId);
                            inc.executeUpdate();
                        }
                        int remaining = 3 - newAttempts;
                        throw new AuthException("Incorrect PIN. " + remaining + " attempt(s) remaining before card is blocked.");
                    }
                }
            }
        }
    }

    public boolean changePin(String currentPin, String newPin) throws SQLException, AuthException {
        if (!Session.isActive()) {
            throw new AuthException("No active session.");
        }
        if (currentPin == null || !currentPin.matches("\\d{4}")) {
            throw new IllegalArgumentException("Current PIN must be exactly 4 digits.");
        }
        if (newPin == null || !newPin.matches("\\d{4}")) {
            throw new IllegalArgumentException("New PIN must be exactly 4 digits.");
        }
        if (currentPin.equals(newPin)) {
            throw new IllegalArgumentException("New PIN must be different from current PIN.");
        }

        String check = "SELECT pin_hash, pin_salt, status FROM accounts WHERE account_id = ? FOR UPDATE";
        String update = "UPDATE accounts SET pin_hash = ?, pin_salt = ?, failed_attempts = 0 WHERE account_id = ?";

        try (Connection cn = Database.getConnection()) {
            cn.setAutoCommit(false);
            try {
                String storedHash;
                String salt;
                try (PreparedStatement ps = cn.prepareStatement(check)) {
                    ps.setLong(1, Session.getAccountId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new AuthException("Account not found.");
                        }
                        if (!"ACTIVE".equalsIgnoreCase(rs.getString("status"))) {
                            throw new AuthException("Account is not active.");
                        }
                        storedHash = rs.getString("pin_hash");
                        salt = rs.getString("pin_salt");
                    }
                }

                String computedCurrentHash;
                if (salt != null && !salt.trim().isEmpty()) {
                    computedCurrentHash = hashWithSalt(currentPin, salt.trim());
                } else {
                    computedCurrentHash = hash(currentPin);
                }

                if (!MessageDigest.isEqual(
                        storedHash.trim().toLowerCase().getBytes(StandardCharsets.UTF_8),
                        computedCurrentHash.trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
                    throw new IllegalArgumentException("Current PIN is incorrect.");
                }

                String newSalt = generateSalt();
                String newHash = hashWithSalt(newPin, newSalt);

                try (PreparedStatement up = cn.prepareStatement(update)) {
                    up.setString(1, newHash);
                    up.setString(2, newSalt);
                    up.setLong(3, Session.getAccountId());
                    int updated = up.executeUpdate();
                    cn.commit();
                    return updated == 1;
                }
            } catch (Exception e) {
                try {
                    cn.rollback();
                } catch (SQLException ignored) {
                }
                if (e instanceof AuthException) throw (AuthException) e;
                if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
                if (e instanceof SQLException) throw (SQLException) e;
                throw new SQLException("Failed to change PIN", e);
            } finally {
                try {
                    cn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return toHex(salt);
    }

    public static String hashWithSalt(String pin, String salt) {
        return hash(salt + pin);
    }

    public static String hash(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
