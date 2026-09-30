package com.netk.mvolatrack;

import com.netk.mvolatrack.history.HistoryTransaction;
import com.netk.mvolatrack.verification.VerificationStatus;

import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/** Immutable aggregation of the application's real transaction history for the dashboard. */
final class DashboardSummary {
    final int transactionCount;
    final int todayTransactionCount;
    final int clientCount;
    final int newClientCount;
    final int activeClientCount;
    final String mostActiveClient;
    final int mostActiveClientTransactions;
    final long balance;
    final long lastUpdate;
    final long creditTotal;
    final long withdrawalTotal;
    final long depositTotal;
    final long bonusTotal;
    final int verifiedBonusCount;
    final int unverifiableBonusCount;
    final int missingBonusCount;
    final int superiorBonusCount;
    final int bonusAttentionCount;
    final List<HistoryTransaction> latestTransactions;

    private DashboardSummary(int transactionCount, int todayTransactionCount, int clientCount,
            int newClientCount, int activeClientCount, String mostActiveClient,
            int mostActiveClientTransactions, long balance, long lastUpdate, long creditTotal,
            long withdrawalTotal, long depositTotal, long bonusTotal, int verifiedBonusCount,
            int unverifiableBonusCount, int missingBonusCount, int superiorBonusCount,
            int bonusAttentionCount, List<HistoryTransaction> latestTransactions) {
        this.transactionCount = transactionCount;
        this.todayTransactionCount = todayTransactionCount;
        this.clientCount = clientCount;
        this.newClientCount = newClientCount;
        this.activeClientCount = activeClientCount;
        this.mostActiveClient = mostActiveClient;
        this.mostActiveClientTransactions = mostActiveClientTransactions;
        this.balance = balance;
        this.lastUpdate = lastUpdate;
        this.creditTotal = creditTotal;
        this.withdrawalTotal = withdrawalTotal;
        this.depositTotal = depositTotal;
        this.bonusTotal = bonusTotal;
        this.verifiedBonusCount = verifiedBonusCount;
        this.unverifiableBonusCount = unverifiableBonusCount;
        this.missingBonusCount = missingBonusCount;
        this.superiorBonusCount = superiorBonusCount;
        this.bonusAttentionCount = bonusAttentionCount;
        this.latestTransactions = latestTransactions;
    }

    static DashboardSummary from(List<HistoryTransaction> source, long now, TimeZone zone) {
        List<HistoryTransaction> transactions = source == null ? Collections.emptyList() : source;
        Calendar day = Calendar.getInstance(zone);
        day.setTimeInMillis(now);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        long todayStart = day.getTimeInMillis();
        day.add(Calendar.DAY_OF_MONTH, 1);
        long tomorrowStart = day.getTimeInMillis();

        Set<String> clients = new LinkedHashSet<>();
        Set<String> clientsBeforeToday = new LinkedHashSet<>();
        Set<String> clientsToday = new LinkedHashSet<>();
        Map<String, Integer> activity = new LinkedHashMap<>();
        long balance = 0, lastUpdate = 0, credit = 0, withdrawal = 0, deposit = 0, bonus = 0;
        int today = 0, verified = 0, unverifiable = 0, missing = 0, superior = 0, attention = 0;
        boolean balanceFound = false;
        for (HistoryTransaction transaction : transactions) {
            if (transaction == null) continue;
            String client = transaction.clientNumber == null ? "—" : transaction.clientNumber;
            clients.add(client);
            activity.put(client, activity.getOrDefault(client, 0) + 1);
            boolean isToday = transaction.timestamp >= todayStart
                    && transaction.timestamp < tomorrowStart;
            if (isToday) {
                today++;
                clientsToday.add(client);
            } else if (transaction.timestamp < todayStart) {
                clientsBeforeToday.add(client);
            }
            if (!balanceFound && transaction.balance != null) {
                balance = transaction.balance;
                lastUpdate = transaction.timestamp;
                balanceFound = true;
            }
            if ("Crédit".equalsIgnoreCase(transaction.type)) credit += transaction.amount;
            else if ("Retrait".equalsIgnoreCase(transaction.type)) withdrawal += transaction.amount;
            else if ("Dépôt".equalsIgnoreCase(transaction.type)) deposit += transaction.amount;
            if (transaction.bonus != null) bonus += transaction.bonus;
            VerificationStatus status = transaction.verification == null
                    ? VerificationStatus.NON_VERIFIABLE : transaction.verification.statut;
            if (status == VerificationStatus.BONUS_VERIFIE) verified++;
            else {
                attention++;
                if (status == VerificationStatus.NON_VERIFIABLE) unverifiable++;
                if (status == VerificationStatus.BONUS_NON_CREDITE) missing++;
                if (status == VerificationStatus.BONUS_SUPERIEUR) superior++;
            }
        }
        Set<String> newClients = new LinkedHashSet<>(clientsToday);
        newClients.removeAll(clientsBeforeToday);
        String mostActive = "—";
        int mostActiveCount = 0;
        for (Map.Entry<String, Integer> entry : activity.entrySet()) {
            if (entry.getValue() > mostActiveCount) {
                mostActive = entry.getKey();
                mostActiveCount = entry.getValue();
            }
        }
        return new DashboardSummary(transactions.size(), today, clients.size(), newClients.size(),
                clientsToday.size(), mostActive, mostActiveCount, balance, lastUpdate, credit,
                withdrawal, deposit, bonus, verified, unverifiable, missing, superior, attention,
                HistoryTransaction.latest(transactions, 5));
    }
}
