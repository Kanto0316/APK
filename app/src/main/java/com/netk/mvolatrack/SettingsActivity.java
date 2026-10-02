package com.netk.mvolatrack;

import android.content.ActivityNotFoundException;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.telephony.SubscriptionManager;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.netk.mvolatrack.sim.AndroidSimReader;
import com.netk.mvolatrack.sim.OperatorConfiguration;
import com.netk.mvolatrack.sim.OperatorConfigurationStore;
import com.netk.mvolatrack.sim.SimOperatorVerifier;

/** Application settings displayed as a full screen instead of a dialog. */
public final class SettingsActivity extends AppCompatActivity {
    private File pendingDiagnosticReport;
    private AutoCompleteTextView expectedOperator;
    private EditText acceptedOperatorNames;
    private TextView operatorStatus;
    private Button permissionButton;
    private OperatorConfigurationStore operatorStore;
    private AndroidSimReader simReader;
    private List<SimOperatorVerifier.SimIdentity> visibleSims = Collections.emptyList();
    private String selectedNetworkId = "";
    private SubscriptionManager subscriptionManager;
    private final SubscriptionManager.OnSubscriptionsChangedListener subscriptionsListener =
            new SubscriptionManager.OnSubscriptionsChangedListener() {
                @Override public void onSubscriptionsChanged() { refreshOperatorVerification(); }
            };

    private final ActivityResultLauncher<String> phonePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted ->
                    refreshOperatorVerification());

    private final ActivityResultLauncher<String> diagnosticSaveLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/plain"),
                    destination -> {
                        if (destination != null) {
                            saveDiagnosticReport(destination);
                        } else {
                            pendingDiagnosticReport = null;
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        }
        setContentView(R.layout.activity_settings);
        applyHeaderInsets(findViewById(R.id.settingsHeader));
        findViewById(R.id.settingsBack).setOnClickListener(view -> finish());
        findViewById(R.id.exportDiagnosticReport).setOnClickListener(view ->
                chooseDiagnosticDestination());
        bindOperatorVerification();
    }

    private void bindOperatorVerification() {
        operatorStore = new OperatorConfigurationStore(this);
        simReader = new AndroidSimReader(this);
        subscriptionManager = getSystemService(SubscriptionManager.class);
        expectedOperator = findViewById(R.id.expectedOperator);
        acceptedOperatorNames = findViewById(R.id.acceptedOperatorNames);
        operatorStatus = findViewById(R.id.operatorVerificationStatus);
        permissionButton = findViewById(R.id.requestPhonePermission);

        OperatorConfiguration saved = operatorStore.load();
        expectedOperator.setText(saved.expectedName);
        acceptedOperatorNames.setText(String.join(", ", saved.acceptedNames));
        selectedNetworkId = saved.networkId;
        expectedOperator.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < visibleSims.size()) {
                selectedNetworkId = visibleSims.get(position).networkId;
            }
        });
        expectedOperator.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                selectedNetworkId = "";
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        findViewById(R.id.saveOperatorConfiguration).setOnClickListener(view -> {
            String name = expectedOperator.getText() == null ? "" : expectedOperator.getText().toString();
            operatorStore.save(name, parseAcceptedNames(), selectedNetworkId);
            refreshOperatorVerification();
            Toast.makeText(this, "Configuration de l’opérateur enregistrée.", Toast.LENGTH_SHORT).show();
        });
        permissionButton.setOnClickListener(view ->
                phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE));
        refreshOperatorVerification();
    }

    private List<String> parseAcceptedNames() {
        String value = acceptedOperatorNames.getText() == null
                ? "" : acceptedOperatorNames.getText().toString();
        if (value.trim().isEmpty()) return Collections.emptyList();
        return Arrays.asList(value.split(","));
    }

    private void refreshOperatorVerification() {
        if (simReader == null) return;
        List<SimOperatorVerifier.SimIdentity> sims = simReader.readActiveSims();
        visibleSims = sims == null ? Collections.emptyList() : sims;
        List<String> carrierNames = new ArrayList<>();
        for (SimOperatorVerifier.SimIdentity sim : visibleSims) {
            carrierNames.add(sim.carrierName);
        }
        expectedOperator.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, carrierNames));

        OperatorConfiguration configuration = operatorStore.load();
        SimOperatorVerifier.Result result = SimOperatorVerifier.verify(
                configuration, sims, simReader.hasPermission());
        switch (result.state) {
            case DETECTED:
                operatorStatus.setText(configuration.expectedName + " détecté — SIM "
                        + (result.slotIndex + 1));
                break;
            case NOT_DETECTED:
                operatorStatus.setText(configuration.expectedName + " non détecté");
                break;
            case IMPOSSIBLE:
                operatorStatus.setText("Vérification impossible");
                break;
            default:
                operatorStatus.setText("Opérateur non configuré");
        }
        permissionButton.setVisibility(simReader.hasPermission() ? View.GONE : View.VISIBLE);
    }

    @Override protected void onStart() {
        super.onStart();
        refreshOperatorVerification();
        if (subscriptionManager != null && simReader.hasPermission()) {
            try { subscriptionManager.addOnSubscriptionsChangedListener(subscriptionsListener); }
            catch (SecurityException ignored) { refreshOperatorVerification(); }
        }
    }

    @Override protected void onStop() {
        if (subscriptionManager != null) {
            try { subscriptionManager.removeOnSubscriptionsChangedListener(subscriptionsListener); }
            catch (RuntimeException ignored) { /* Listener was not registered. */ }
        }
        super.onStop();
    }

    private static void applyHeaderInsets(View header) {
        final int initialHeight = header.getLayoutParams().height;
        final int initialLeft = header.getPaddingLeft();
        final int initialTop = header.getPaddingTop();
        final int initialRight = header.getPaddingRight();
        final int initialBottom = header.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(header, (view, windowInsets) -> {
            Insets topInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                            | WindowInsetsCompat.Type.displayCutout());
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = initialHeight + topInsets.top;
            view.setLayoutParams(layoutParams);
            view.setPadding(initialLeft + topInsets.left, initialTop + topInsets.top,
                    initialRight + topInsets.right, initialBottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    private void chooseDiagnosticDestination() {
        File report = CrashLogger.getReportFile(this);
        if (report == null || !report.isFile()) {
            Toast.makeText(this, "Aucun rapport diagnostic disponible.", Toast.LENGTH_LONG).show();
            return;
        }
        pendingDiagnosticReport = report;
        try {
            diagnosticSaveLauncher.launch(CrashLogger.FILE_NAME);
        } catch (ActivityNotFoundException error) {
            pendingDiagnosticReport = null;
            CrashLogger.recordException(this, error);
            Toast.makeText(this,
                    "Impossible d’ouvrir le sélecteur d’emplacement.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveDiagnosticReport(Uri destination) {
        File report = pendingDiagnosticReport;
        pendingDiagnosticReport = null;
        if (report == null || !report.isFile()) {
            Toast.makeText(this,
                    "Impossible d’enregistrer le rapport diagnostic : le rapport est introuvable.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        try (FileInputStream input = new FileInputStream(report);
             OutputStream output = getContentResolver().openOutputStream(destination, "wt")) {
            if (output == null) throw new IOException("Destination inaccessible");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            output.flush();
            Toast.makeText(this, "Rapport diagnostic enregistré.", Toast.LENGTH_LONG).show();
        } catch (IOException | SecurityException error) {
            CrashLogger.recordException(this, error);
            Toast.makeText(this,
                    "Impossible d’enregistrer le rapport diagnostic.", Toast.LENGTH_LONG).show();
        }
    }
}
