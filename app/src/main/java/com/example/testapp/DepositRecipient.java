package com.example.testapp;

/** A local-only recipient candidate from Android Contacts and/or deposit history. */
final class DepositRecipient {
    final String number;
    final String name;
    final long lastUsed;
    final boolean contact;

    DepositRecipient(String number, String name, long lastUsed, boolean contact) {
        this.number = number;
        this.name = name == null ? "" : name.trim();
        this.lastUsed = lastUsed;
        this.contact = contact;
    }

    boolean hasName() {
        return !name.isEmpty();
    }
}
