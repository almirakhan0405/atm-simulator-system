package com.atm;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class DashboardFrame extends JFrame {
    private final AtmService atm = new AtmService();
    private final AuthService auth = new AuthService();
    private final JLabel balanceLabel = new JLabel("Balance: loading...");

    public DashboardFrame() {
        if (!Session.isActive()) {
            SwingUtilities.invokeLater(() -> {
                new LoginFrame().setVisible(true);
                dispose();
            });
            return;
        }

        setTitle("ATM Simulator System - Dashboard");
        setSize(660, 460);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel welcome = new JLabel("Welcome, " + Session.getCustomerName());
        welcome.setFont(new Font("SansSerif", Font.BOLD, 20));
        root.add(welcome, BorderLayout.NORTH);

        balanceLabel.setHorizontalAlignment(SwingConstants.CENTER);
        balanceLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        balanceLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        root.add(balanceLabel, BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new GridLayout(2, 3, 14, 14));
        JButton b1 = new JButton("Balance Inquiry");
        JButton b2 = new JButton("Cash Withdrawal");
        JButton b3 = new JButton("Cash Deposit");
        JButton b4 = new JButton("Mini Statement");
        JButton b5 = new JButton("Change PIN");
        JButton b6 = new JButton("Logout");

        buttons.add(b1);
        buttons.add(b2);
        buttons.add(b3);
        buttons.add(b4);
        buttons.add(b5);
        buttons.add(b6);
        root.add(buttons, BorderLayout.CENTER);

        setContentPane(root);

        b1.addActionListener(e -> refreshBalance(true));
        b2.addActionListener(e -> amountDialog(false));
        b3.addActionListener(e -> amountDialog(true));
        b4.addActionListener(e -> statement());
        b5.addActionListener(e -> changePin());
        b6.addActionListener(e -> {
            Session.clear();
            new LoginFrame().setVisible(true);
            dispose();
        });

        refreshBalance(false);
    }

    private void refreshBalance(boolean show) {
        try {
            BigDecimal b = atm.getBalance();
            balanceLabel.setText("Current balance: ₹" + b.toPlainString());
            if (show) {
                JOptionPane.showMessageDialog(this,
                        "Available balance: ₹" + b.toPlainString(),
                        "Balance Inquiry",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void amountDialog(boolean deposit) {
        String actionName = deposit ? "Deposit" : "Withdrawal";
        String prompt = deposit ? "Enter deposit amount (₹):" : "Enter withdrawal amount (₹):";
        String text = JOptionPane.showInputDialog(this, prompt, actionName, JOptionPane.QUESTION_MESSAGE);
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(text.trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be strictly greater than zero.");
            }
            if (amount.scale() > 2) {
                throw new IllegalArgumentException("Amount cannot have more than two decimal places.");
            }

            BigDecimal b = deposit ? atm.deposit(amount) : atm.withdraw(amount);
            balanceLabel.setText("Current balance: ₹" + b.toPlainString());
            JOptionPane.showMessageDialog(this,
                    actionName + " successful.\nNew balance: ₹" + b.toPlainString(),
                    "Transaction Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this,
                    "Invalid amount format. Please enter a valid number.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void statement() {
        try {
            List<AtmService.TransactionRow> rows = atm.recentTransactions(10);
            String[] cols = {"Date / Time", "Type", "Amount (₹)", "Balance After (₹)", "Reference Code"};
            DefaultTableModel model = new DefaultTableModel(cols, 0) {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
            for (AtmService.TransactionRow r : rows) {
                model.addRow(new Object[]{
                        r.displayTime(),
                        r.type,
                        r.amount.toPlainString(),
                        r.balance.toPlainString(),
                        r.reference
                });
            }
            JTable table = new JTable(model);
            table.setAutoCreateRowSorter(true);
            JScrollPane pane = new JScrollPane(table);
            pane.setPreferredSize(new Dimension(720, 260));
            JOptionPane.showMessageDialog(this, pane, "Mini Statement (Last 10 Transactions)", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void changePin() {
        JPanel p = new JPanel(new GridLayout(3, 2, 8, 8));
        JPasswordField oldPin = new JPasswordField();
        JPasswordField newPin = new JPasswordField();
        JPasswordField confirm = new JPasswordField();

        p.add(new JLabel("Current 4-digit PIN:"));
        p.add(oldPin);
        p.add(new JLabel("New 4-digit PIN:"));
        p.add(newPin);
        p.add(new JLabel("Confirm New PIN:"));
        p.add(confirm);

        int result = JOptionPane.showConfirmDialog(this, p, "Change PIN", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        char[] oChars = oldPin.getPassword();
        char[] nChars = newPin.getPassword();
        char[] cChars = confirm.getPassword();

        String o = new String(oChars);
        String n = new String(nChars);
        String c = new String(cChars);

        Arrays.fill(oChars, '\0');
        Arrays.fill(nChars, '\0');
        Arrays.fill(cChars, '\0');

        if (!o.matches("\\d{4}") || !n.matches("\\d{4}")) {
            JOptionPane.showMessageDialog(this,
                    "Both current and new PINs must be exactly four digits.",
                    "Input Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!n.equals(c)) {
            JOptionPane.showMessageDialog(this,
                    "The new PIN and confirmation PIN do not match.",
                    "Input Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (o.equals(n)) {
            JOptionPane.showMessageDialog(this,
                    "The new PIN must be different from the current PIN.",
                    "Input Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (auth.changePin(o, n)) {
                JOptionPane.showMessageDialog(this,
                        "PIN changed successfully. Please remember your new PIN.",
                        "PIN Updated",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        String msg = (ex instanceof IllegalArgumentException || ex instanceof AuthService.AuthException)
                ? ex.getMessage()
                : "Operation failed. Please check the database connection.";
        JOptionPane.showMessageDialog(this, msg, "Operation Notice", JOptionPane.ERROR_MESSAGE);
    }
}
