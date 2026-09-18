package com.example.testapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapp.database.SmsMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView permissionText;
    private TextView emptyText;
    private ProgressBar loadingIndicator;
    private SmsAdapter adapter;
    private SmsViewModel viewModel;
    private List<SmsMessage> messages = new ArrayList<>();
    private boolean roomLoaded;
    private boolean importRunning;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result ->
                    refreshPermissionState());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionText = findViewById(R.id.permissionText);
        emptyText = findViewById(R.id.emptyText);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        RecyclerView list = findViewById(R.id.transactionsList);
        adapter = new SmsAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(SmsViewModel.class);
        viewModel.getMessages().observe(this, storedMessages -> {
            messages = storedMessages == null ? new ArrayList<>() : storedMessages;
            roomLoaded = true;
            renderState();
        });
        requestRequiredPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionState();
    }

    private void requestRequiredPermissions() {
        List<String> missing = new ArrayList<>();
        if (!hasPermission(Manifest.permission.READ_SMS)) missing.add(Manifest.permission.READ_SMS);
        if (!hasPermission(Manifest.permission.RECEIVE_SMS)) missing.add(Manifest.permission.RECEIVE_SMS);
        if (missing.isEmpty()) refreshPermissionState();
        else permissionLauncher.launch(missing.toArray(new String[0]));
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasSmsPermissions() {
        return hasPermission(Manifest.permission.READ_SMS)
                && hasPermission(Manifest.permission.RECEIVE_SMS);
    }

    private void refreshPermissionState() {
        if (hasSmsPermissions() && !importRunning) {
            importRunning = true;
            renderState();
            viewModel.importInboxOnce(
                    () -> runOnUiThread(() -> { importRunning = false; renderState(); }),
                    () -> runOnUiThread(() -> { importRunning = false; renderState(); }));
        } else {
            renderState();
        }
    }

    private void renderState() {
        boolean denied = !hasSmsPermissions();
        boolean loading = !denied && (!roomLoaded || importRunning);
        permissionText.setVisibility(denied ? View.VISIBLE : View.GONE);
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        adapter.submitList(denied ? new ArrayList<>() : messages);
        emptyText.setVisibility(!denied && !loading && messages.isEmpty()
                ? View.VISIBLE : View.GONE);
    }

    private static class SmsAdapter extends RecyclerView.Adapter<SmsViewHolder> {
        private List<SmsMessage> items = new ArrayList<>();

        void submitList(List<SmsMessage> messages) {
            items = new ArrayList<>(messages);
            notifyDataSetChanged();
        }

        @Override
        public SmsViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new SmsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SmsViewHolder holder, int position) {
            SmsMessage item = items.get(position);
            holder.sender.setText(item.sender);
            holder.date.setText(new SimpleDateFormat("d MMM yyyy • HH:mm", Locale.FRENCH)
                    .format(item.receivedDate));
            holder.body.setText(item.messageBody);
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
}
