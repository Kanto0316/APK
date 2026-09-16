package com.example.testapp.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long date;
    public String expediteur;
    public String type;
    public double montant;
    public String devise;
    public Double solde;
    public Double bonus;
    public String messageOriginal;

    public Transaction(long date, String expediteur, String type, double montant, String devise,
                       Double solde, Double bonus, String messageOriginal) {
        this.date = date;
        this.expediteur = expediteur;
        this.type = type;
        this.montant = montant;
        this.devise = devise;
        this.solde = solde;
        this.bonus = bonus;
        this.messageOriginal = messageOriginal;
    }
}
