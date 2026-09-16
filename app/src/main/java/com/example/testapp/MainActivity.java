package com.example.testapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapp.database.Transaction;
import com.example.testapp.repository.TransactionRepository;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView permissionText;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
                    updatePermissionWarning());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView balanceText = findViewById(R.id.balanceText);
        TextView bonusText = findViewById(R.id.bonusText);
        permissionText = findViewById(R.id.permissionText);
        RecyclerView list = findViewById(R.id.transactionsList);
        TransactionAdapter adapter = new TransactionAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        TransactionRepository repository = new TransactionRepository(getApplicationContext());
        repository.getRecentTransactions().observe(this, adapter::submitList);
        repository.getLatestBalance().observe(this, transaction ->
                balanceText.setText("Solde actuel : " + formatValue(transaction, true)));
        repository.getLatestBonus().observe(this, transaction ->
                bonusText.setText("Bonus : " + formatValue(transaction, false)));

        requestRequiredPermissions();
    }

    private void requestRequiredPermissions() {
        List<String> missing = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECEIVE_SMS);
        }
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!missing.isEmpty()) {
            permissionLauncher.launch(missing.toArray(new String[0]));
        }
        updatePermissionWarning();
    }

    private void updatePermissionWarning() {
        boolean denied = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED;
        permissionText.setVisibility(denied ? View.VISIBLE : View.GONE);
    }

    private String formatValue(Transaction transaction, boolean balance) {
        if (transaction == null) return "—";
        Double value = balance ? transaction.solde : transaction.bonus;
        if (value == null) return "—";
        return SmsAmountFormatter.format(value) + " " + transaction.devise;
    }

    private static class TransactionAdapter extends RecyclerView.Adapter<TransactionViewHolder> {
        private List<Transaction> items = new ArrayList<>();

        void submitList(List<Transaction> transactions) {
            items = transactions == null ? new ArrayList<>() : new ArrayList<>(transactions);
            notifyDataSetChanged();
        }

        @Override
        public TransactionViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater(parent).inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }

        private android.view.LayoutInflater getLayoutInflater(android.view.ViewGroup parent) {
            return android.view.LayoutInflater.from(parent.getContext());
        }

        @Override
        public void onBindViewHolder(TransactionViewHolder holder, int position) {
            Transaction item = items.get(position);
            holder.typeAmount.setText(item.type + "  •  "
                    + SmsAmountFormatter.format(item.montant) + " " + item.devise);
            String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                    Locale.getDefault()).format(item.date);
            holder.senderDate.setText(item.expediteur + "  •  " + date);
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static class TransactionViewHolder extends RecyclerView.ViewHolder {
        final TextView typeAmount;
        final TextView senderDate;

        TransactionViewHolder(View itemView) {
            super(itemView);
            typeAmount = itemView.findViewById(R.id.itemTypeAmount);
            senderDate = itemView.findViewById(R.id.itemSenderDate);
        }
    }

    private static class SmsAmountFormatter {
        static String format(double value) {
            if (value == Math.rint(value)) return String.format(Locale.getDefault(), "%,.0f", value);
            return String.format(Locale.getDefault(), "%,.2f", value);
        }
    }
}
