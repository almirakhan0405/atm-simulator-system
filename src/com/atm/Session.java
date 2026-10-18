package com.atm;

public final class Session {
    private static volatile long accountId;
    private static volatile String cardNumber;
    private static volatile String customerName;

    private Session() { }

    public static synchronized void start(long id, String card, String name) {
        accountId = id;
        cardNumber = card;
        customerName = name;
    }

    public static synchronized boolean isActive() {
        return accountId > 0;
    }

    public static synchronized long getAccountId() {
        return accountId;
    }

    public static synchronized String getCardNumber() {
        return cardNumber;
    }

    public static synchronized String getCustomerName() {
        return customerName;
    }

    public static synchronized void clear() {
        accountId = 0;
        cardNumber = null;
        customerName = null;
    }
}
