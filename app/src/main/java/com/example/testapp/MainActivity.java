package com.example.testapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testapp.database.SmsMessage;
import com.example.testapp.backup.SmsBackupManager;

import androidx.appcompat.app.AlertDialog;

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
    private TextView lastBackupText;
    private SmsBackupManager backupManager;
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
        lastBackupText = findViewById(R.id.lastBackupText);
        backupManager = new SmsBackupManager(this);
        findViewById(R.id.backupButton).setOnClickListener(view -> requestPassword(false));
        findViewById(R.id.restoreButton).setOnClickListener(view -> requestPassword(true));
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
        refreshBackupStatus(true);
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
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && !hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            missing.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (missing.isEmpty()) refreshPermissionState();
        else permissionLauncher.launch(missing.toArray(new String[0]));
    }

    private void refreshBackupStatus(boolean offerRestore) {
        backupManager.findBackup((date, error) -> runOnUiThread(() -> {
            if (error != null) {
                lastBackupText.setText("Sauvegarde indisponible");
                return;
            }
            if (date == null) {
                lastBackupText.setText("Dernière sauvegarde : aucune");
                return;
            }
            lastBackupText.setText("Dernière sauvegarde : "
                    + new SimpleDateFormat("d MMM yyyy • HH:mm", Locale.FRENCH).format(date));
            boolean alreadyOffered = getPreferences(MODE_PRIVATE).getBoolean("restore_offered", false);
            if (offerRestore && !alreadyOffered) {
                getPreferences(MODE_PRIVATE).edit().putBoolean("restore_offered", true).apply();
                new AlertDialog.Builder(this).setTitle("Sauvegarde trouvée")
                        .setMessage("Restaurer l’historique SMS sauvegardé sur cet appareil ?")
                        .setNegativeButton("Plus tard", null)
                        .setPositiveButton("Restaurer", (dialog, which) -> requestPassword(true)).show();
            }
        }));
    }

    private void requestPassword(boolean restore) {
        EditText input = new EditText(this);
        input.setHint("8 caractères minimum");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, 0, padding, 0);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(restore ? "Mot de passe de sauvegarde" : "Protéger la sauvegarde")
                .setMessage(restore
                        ? "Saisissez le mot de passe utilisé lors de la sauvegarde."
                        : "Ce mot de passe sera requis après une réinstallation. Il ne peut pas être récupéré.")
                .setView(input).setNegativeButton("Annuler", null)
                .setPositiveButton(restore ? "Restaurer" : "Sauvegarder", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            char[] password = input.getText().toString().toCharArray();
            if (password.length < 8) { input.setError("8 caractères minimum"); return; }
            dialog.dismiss();
            if (restore) restore(password); else backup(password);
            java.util.Arrays.fill(password, '\0');
        }));
        dialog.show();
    }

    private void backup(char[] password) {
        backupManager.backup(password, true, (date, error) -> runOnUiThread(() -> {
            Toast.makeText(this, error == null ? "Sauvegarde chiffrée créée"
                    : "Échec : " + error.getMessage(), Toast.LENGTH_LONG).show();
            if (error == null) refreshBackupStatus(false);
        }));
    }

    private void restore(char[] password) {
        backupManager.restore(password, (count, error) -> runOnUiThread(() -> {
            Toast.makeText(this, error == null ? count + " messages restaurés"
                    : "Restauration impossible : " + error.getMessage(), Toast.LENGTH_LONG).show();
            if (error == null) refreshBackupStatus(false);
        }));
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
