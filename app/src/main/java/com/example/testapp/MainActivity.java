package com.example.testapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapp.sms.SmsReceiver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private TextView permissionText;
    private TextView emptyText;
    private SmsAdapter adapter;
    private final ExecutorService smsExecutor = Executors.newSingleThreadExecutor();
    private boolean refreshReceiverRegistered;

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SmsReceiver.ACTION_SMS_INBOX_CHANGED.equals(intent.getAction())) loadSms();
        }
    };

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
                    refreshPermissionState());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionText = findViewById(R.id.permissionText);
        emptyText = findViewById(R.id.emptyText);
        RecyclerView list = findViewById(R.id.transactionsList);
        adapter = new SmsAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        requestRequiredPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(this, refreshReceiver,
                new android.content.IntentFilter(SmsReceiver.ACTION_SMS_INBOX_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        refreshReceiverRegistered = true;
        refreshPermissionState();
    }

    @Override
    protected void onStop() {
        if (refreshReceiverRegistered) {
            unregisterReceiver(refreshReceiver);
            refreshReceiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        smsExecutor.shutdownNow();
        super.onDestroy();
    }

    private void requestRequiredPermissions() {
        List<String> missing = new ArrayList<>();
        if (!hasPermission(Manifest.permission.READ_SMS)) missing.add(Manifest.permission.READ_SMS);
        if (!hasPermission(Manifest.permission.RECEIVE_SMS)) missing.add(Manifest.permission.RECEIVE_SMS);
        if (!missing.isEmpty()) {
            permissionLauncher.launch(missing.toArray(new String[0]));
        } else {
            refreshPermissionState();
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshPermissionState() {
        boolean denied = !hasPermission(Manifest.permission.READ_SMS)
                || !hasPermission(Manifest.permission.RECEIVE_SMS);
        permissionText.setVisibility(denied ? View.VISIBLE : View.GONE);
        if (denied) {
            adapter.submitList(new ArrayList<>());
            emptyText.setVisibility(View.GONE);
        } else {
            loadSms();
        }
    }

    private void loadSms() {
        if (!hasPermission(Manifest.permission.READ_SMS)) return;
        smsExecutor.execute(() -> {
            List<SmsItem> messages = new ArrayList<>();
            String[] projection = {
                    Telephony.Sms.Inbox._ID,
                    Telephony.Sms.Inbox.ADDRESS,
                    Telephony.Sms.Inbox.DATE,
                    Telephony.Sms.Inbox.BODY
            };
            try (Cursor cursor = getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI,
                    projection, null, null, Telephony.Sms.Inbox.DATE + " DESC")) {
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox._ID);
                    int addressColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS);
                    int dateColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE);
                    int bodyColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY);
                    while (cursor.moveToNext()) {
                        messages.add(new SmsItem(cursor.getLong(idColumn),
                                cursor.getString(addressColumn), cursor.getLong(dateColumn),
                                cursor.getString(bodyColumn)));
                    }
                }
            } catch (SecurityException ignored) {
                runOnUiThread(() -> {
                    permissionText.setVisibility(View.VISIBLE);
                    adapter.submitList(new ArrayList<>());
                    emptyText.setVisibility(View.GONE);
                });
                return;
            }
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                adapter.submitList(messages);
                emptyText.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private static class SmsAdapter extends RecyclerView.Adapter<SmsViewHolder> {
        private List<SmsItem> items = new ArrayList<>();

        void submitList(List<SmsItem> messages) {
            items = new ArrayList<>(messages);
            notifyDataSetChanged();
        }

        @Override
        public SmsViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater(parent).inflate(R.layout.item_transaction, parent, false);
            return new SmsViewHolder(view);
        }

        private android.view.LayoutInflater getLayoutInflater(android.view.ViewGroup parent) {
            return android.view.LayoutInflater.from(parent.getContext());
        }

        @Override
        public void onBindViewHolder(SmsViewHolder holder, int position) {
            SmsItem item = items.get(position);
            holder.sender.setText(item.sender == null || item.sender.isEmpty()
                    ? "Expéditeur inconnu" : item.sender);
            holder.date.setText(new SimpleDateFormat("d MMM yyyy • HH:mm", Locale.FRENCH)
                    .format(item.receivedAt));
            holder.body.setText(item.body);
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static class SmsViewHolder extends RecyclerView.ViewHolder {
        final TextView sender;
        final TextView date;
        final TextView body;

        SmsViewHolder(View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.itemSender);
            date = itemView.findViewById(R.id.itemDate);
            body = itemView.findViewById(R.id.itemBody);
        }
    }

    private static class SmsItem {
        final long id;
        final String sender;
        final long receivedAt;
        final String body;

        SmsItem(long id, String sender, long receivedAt, String body) {
            this.id = id;
            this.sender = sender;
            this.receivedAt = receivedAt;
            this.body = body;
        }
    }
}
