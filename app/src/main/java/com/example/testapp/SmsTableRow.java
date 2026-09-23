package com.example.testapp;

import com.example.testapp.database.SmsMessage;
import com.example.testapp.sms.ClientNumberNormalizer;
import com.example.testapp.sms.MvolaMessageParser;

import java.text.SimpleDateFormat;
import java.util.Locale;

/** Presentation-only row for an SMS already recognised as a business transaction. */
final class SmsTableRow {
    static final String MISSING_VALUE = "-";

    final int number;
    final String dateTime;
    final String type;
    final String numero;
    final String nom;
    final String montant;
    final String reference;
    final String bonus;
    final String frais;
    final String solde;

    private SmsTableRow(int number, String dateTime, String type, String numero, String nom,
                        String montant, String reference, String bonus, String frais, String solde) {
        this.number = number;
        this.dateTime = dateTime;
        this.type = type;
        this.numero = numero;
        this.nom = nom;
        this.montant = montant;
        this.reference = reference;
        this.bonus = bonus;
        this.frais = frais;
        this.solde = solde;
    }

    static SmsTableRow from(SmsDateFilter.DisplayMessage displayed) {
        SmsMessage message = displayed.message;
        MvolaMessageParser.ParsedTransaction transaction =
                MvolaMessageParser.parse(message.messageBody, message.receivedDate);
        if (transaction == null) {
            throw new IllegalArgumentException("A parsed transaction is required for display");
        }
        return new SmsTableRow(
                displayed.originalNumber,
                new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
                        .format(transaction.transactionAt),
                transaction.type,
                ClientNumberNormalizer.format(transaction.clientNumber),
                transaction.clientName,
                ariary(transaction.amount),
                transaction.reference,
                transaction.bonus == null ? null : ariary(transaction.bonus),
                transaction.fee == null ? null : ariary(transaction.fee),
                transaction.balance == null ? null : ariary(transaction.balance));
    }

    private static String ariary(long value) {
        return String.format(Locale.FRENCH, "%,d Ar", value).replace('\u00a0', ' ')
                .replace('\u202f', ' ');
    }

    static String display(String value) {
        return value == null || value.trim().isEmpty() ? MISSING_VALUE : value;
    }
}
