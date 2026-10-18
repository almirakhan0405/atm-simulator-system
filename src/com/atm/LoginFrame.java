package com.atm;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.Arrays;

public class LoginFrame extends JFrame {
    private final JTextField cardField = new JTextField();
    private final JPasswordField pinField = new JPasswordField();
    private final AuthService auth = new AuthService();

    public LoginFrame() {
        setTitle("ATM Simulator System - Login");
        setSize(450, 320);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JPanel outer = new JPanel(new BorderLayout(12, 12));
        outer.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("ATM SIMULATOR", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        outer.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 14));
        form.add(new JLabel("Card Number:"));
        form.add(cardField);
        form.add(new JLabel("4-digit PIN:"));
        form.add(pinField);

        JButton login = new JButton("Login");
        JButton clear = new JButton("Clear");
        form.add(login);
        form.add(clear);
        outer.add(form, BorderLayout.CENTER);

        JLabel hint = new JLabel("Demo Card: 1234123412341234 | PIN: 1234", SwingConstants.CENTER);
        hint.setForeground(Color.DARK_GRAY);
        outer.add(hint, BorderLayout.SOUTH);

        setContentPane(outer);

        login.addActionListener(this::doLogin);
        clear.addActionListener(e -> {
            cardField.setText("");
            pinField.setText("");
        });
        getRootPane().setDefaultButton(login);
    }

    private void doLogin(ActionEvent e) {
        String card = cardField.getText().trim();
        char[] pinChars = pinField.getPassword();
        String pin = new String(pinChars);
        Arrays.fill(pinChars, '\0');

        if (!card.matches("\\d{16}") || !pin.matches("\\d{4}")) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a valid 16-digit card number and a 4-digit PIN.",
                    "Input Validation",
                    JOptionPane.WARNING_MESSAGE);
            pinField.setText("");
            return;
        }

        try {
            if (auth.login(card, pin)) {
                new DashboardFrame().setVisible(true);
                dispose();
            }
        } catch (AuthService.AuthException ae) {
            JOptionPane.showMessageDialog(this,
                    ae.getMessage(),
                    "Authentication Failed",
                    JOptionPane.ERROR_MESSAGE);
            pinField.setText("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not connect to database. Please check config/atm.properties and MySQL service.",
                    "Database Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "An unexpected error occurred. Please try again.",
                    "System Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
