package com.netk.mvolatrack.history;

import com.netk.mvolatrack.database.SmsMessage;
import com.netk.mvolatrack.sms.MvolaMessageParser;
import com.netk.mvolatrack.verification.TransactionBalanceVerification;
import com.netk.mvolatrack.verification.TransactionBalanceVerifier;
import com.netk.mvolatrack.verification.TransactionDirection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Display projection of a transaction parsed from the application's persisted SMS source. */
public final class HistoryTransaction {
    public final String type;
    public final String clientNumber;
    public final long amount;
    public final Long bonus;
    public final Long fee;
    public final Long balance;
    public final String reference;
    public final long timestamp;
    /** Stable identity of the persisted SMS that produced this projection. */
    public final String sourceUniqueKey;
    public final TransactionBalanceVerification verification;

    private HistoryTransaction(String type, String clientNumber, long amount, Long bonus, Long fee,
                               Long balance, String reference, long timestamp, String sourceUniqueKey,
                               TransactionBalanceVerification verification) {
        this.type = type;
        this.clientNumber = clientNumber;
        this.amount = amount;
        this.bonus = bonus;
        this.fee = fee;
        this.balance = balance;
        this.reference = reference;
        this.timestamp = timestamp;
        this.sourceUniqueKey = sourceUniqueKey;
        this.verification = verification;
    }

    /**
     * Uses the same stored SMS and central parser as Messages and Statistics. No history data is
     * persisted separately. The business timestamp wins, with SMS reception time as fallback.
     */
    public static List<HistoryTransaction> fromMessages(List<SmsMessage> messages) {
        List<HistoryTransaction> result = new ArrayList<>();
        if (messages == null) return result;
        List<SmsMessage> chronological = new ArrayList<>(messages);
        chronological.sort(Comparator.comparingLong((SmsMessage item) -> item.receivedDate)
                .thenComparingLong(item -> item.id));
        SmsMessage previousMessage = null;
        for (SmsMessage message : chronological) {
            MvolaMessageParser.ParsedTransaction parsed =
                    MvolaMessageParser.parse(message.messageBody, message.receivedDate);
            if (parsed == null || parsed.clientNumber == null) {
                previousMessage = message;
                continue;
            }
            long timestamp = parsed.transactionAt > 0
                    ? parsed.transactionAt : message.receivedDate;
            MvolaMessageParser.ParsedTransaction previous = previousMessage == null ? null
                    : MvolaMessageParser.parse(previousMessage.messageBody,
                    previousMessage.receivedDate);
            TransactionBalanceVerification verification = verify(parsed, previous, timestamp);
            result.add(new HistoryTransaction(parsed.type, parsed.clientNumber, parsed.amount,
                    parsed.bonus, parsed.fee, parsed.balance, parsed.reference, timestamp,
                    message.uniqueKey,
                    verification));
            previousMessage = message;
        }
        result.sort(Comparator.comparingLong((HistoryTransaction item) -> item.timestamp)
                .reversed());
        return result;
    }

    /**
     * Computes the chronological verification once from the complete persisted SMS list and
     * indexes it by the SMS identity. Consumers can then filter/reorder rows without changing the
     * evidence or accidentally matching duplicate/missing transaction references.
     */
    public static Map<String, TransactionBalanceVerification> verificationsByMessageKey(
            List<SmsMessage> messages) {
        Map<String, TransactionBalanceVerification> result = new LinkedHashMap<>();
        for (HistoryTransaction transaction : fromMessages(messages)) {
            result.put(transaction.sourceUniqueKey, transaction.verification);
        }
        return result;
    }

    private static TransactionBalanceVerification verify(
            MvolaMessageParser.ParsedTransaction current,
            MvolaMessageParser.ParsedTransaction previous, long timestamp) {
        TransactionBalanceVerifier.Input input = new TransactionBalanceVerifier.Input();
        input.ancienSolde = previous == null ? null : previous.balance;
        input.montantTransaction = current.amount;
        input.frais = current.fee == null ? 0 : current.fee;
        input.bonusAttendu = current.bonus == null ? 0 : current.bonus;
        input.nouveauSoldeReel = current.balance;
        input.referenceTransaction = current.reference;
        input.dateTransaction = timestamp;
        input.typeTransaction = direction(current.type);
        // A direct pair of balance-bearing, distinctly referenced SMS is required. If not, the
        // arithmetic would risk attributing an intermediate operation to the current transaction.
        input.correspondanceAmbigue = previous == null || previous.balance == null
                || empty(current.reference) || empty(previous.reference)
                || current.reference.equals(previous.reference);
        return TransactionBalanceVerifier.verifyTransactionBalance(input);
    }

    private static TransactionDirection direction(String type) {
        return "Dépôt".equalsIgnoreCase(type) || "Crédit".equalsIgnoreCase(type)
                ? TransactionDirection.SORTANTE : TransactionDirection.ENTRANTE;
    }

    private static boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Returns at most {@code count} items without changing the already sorted source list. */
    public static List<HistoryTransaction> latest(List<HistoryTransaction> transactions,
                                                   int count) {
        if (transactions == null || transactions.isEmpty() || count <= 0) {
            return new ArrayList<>();
        }
        return new ArrayList<>(transactions.subList(0, Math.min(count, transactions.size())));
    }
}
