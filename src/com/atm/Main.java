package com.atm;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // The default Swing look and feel remains usable.
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { new LoginFrame().setVisible(true); }
        });
    }
}
