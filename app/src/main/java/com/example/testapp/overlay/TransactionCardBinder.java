package com.example.testapp.overlay;

import android.view.View;
import android.widget.TextView;

import com.example.testapp.R;
import com.example.testapp.sms.MvolaMessageParser;

final class TransactionCardBinder {
    private TransactionCardBinder() {}

    static void bind(View view, MvolaMessageParser.ParsedTransaction transaction,
                     View.OnClickListener closeListener) {
        text(view, R.id.transactionType, TransactionDisplayFormatter.text(transaction.type));
        text(view, R.id.transactionNumber,
                TransactionDisplayFormatter.phone(transaction.clientNumber));
        text(view, R.id.transactionName,
                TransactionDisplayFormatter.text(transaction.clientName));
        text(view, R.id.transactionAmount,
                TransactionDisplayFormatter.amount(transaction.type, transaction.amount));
        text(view, R.id.transactionBalance,
                TransactionDisplayFormatter.balance(transaction.balance));
        view.findViewById(R.id.transactionOk).setOnClickListener(closeListener);
    }

    private static void text(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value);
    }
}
