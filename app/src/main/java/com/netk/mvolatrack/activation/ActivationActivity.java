package com.netk.mvolatrack.activation;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.netk.mvolatrack.MainActivity;
import com.netk.mvolatrack.R;
import com.netk.mvolatrack.integrity.AppIntegrityChecker;
import com.netk.mvolatrack.integrity.IntegrityFailureUi;

public final class ActivationActivity extends AppCompatActivity {
    public static final String EXTRA_MANAGE_LICENSE = "manage_license";
    private String installationId;
    private EditText activationCode;
    private boolean showingClockRollback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureAuthentic()) return;
        if (ActivationStore.hasValidActivation(this)
                && !getIntent().getBooleanExtra(EXTRA_MANAGE_LICENSE, false)) {
            openApplication();
            return;
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_activation);
        ActivationVerifier.Verification current = ActivationStore.status(this);
        if (current.result == ActivationVerifier.Result.CLOCK_ROLLBACK) {
            showClockRollback();
        } else if (current.result == ActivationVerifier.Result.EXPIRED && current.license != null) {
            ((TextView) findViewById(R.id.activationStateTitle)).setText("Licence expirée");
            ((TextView) findViewById(R.id.activationStateMessage)).setText(
                    "Votre licence a expiré le :\n" + LicenseDisplay.expiry(current.license)
                            + "\n\nPour continuer à utiliser MVolaCash,\n"
                            + "une nouvelle activation est nécessaire.");
        } else if (getIntent().getBooleanExtra(EXTRA_MANAGE_LICENSE, false)) {
            ((TextView) findViewById(R.id.activationStateTitle)).setText("Gérer la licence");
            ((TextView) findViewById(R.id.activationStateMessage)).setText(
                    "Entrez une nouvelle activation pour remplacer la licence actuelle.");
        }
        installationId = InstallationIdentity.getOrCreate(this);
        TextView requestCode = findViewById(R.id.activationRequestCode);
        requestCode.setText(ActivationRequest.format(installationId));
        activationCode = findViewById(R.id.activationCode);
        findViewById(R.id.copyRequestCode).setOnClickListener(view -> copyRequest(requestCode.getText()));
        findViewById(R.id.sendRequestCodeBySms).setOnClickListener(
                view -> sendRequestBySms(requestCode.getText()));
        findViewById(R.id.pasteActivationCode).setOnClickListener(view -> pasteActivation());
        findViewById(R.id.activateButton).setOnClickListener(view -> activate());
        findViewById(R.id.openDateSettings).setOnClickListener(view -> openDateSettings());
        findViewById(R.id.retryLicenseValidation).setOnClickListener(view -> retryValidation());
    }

    private void showClockRollback() {
        showingClockRollback = true;
        ((TextView) findViewById(R.id.activationStateTitle)).setText("Date et heure incorrectes");
        ((TextView) findViewById(R.id.activationStateMessage)).setText(
                "Une modification de la date ou de l’heure\na été détectée.\n\n"
                        + "Rétablissez la date et l’heure correctes\n"
                        + "pour continuer à utiliser MVolaCash.");
        findViewById(R.id.activationFields).setVisibility(View.GONE);
        findViewById(R.id.clockRollbackActions).setVisibility(View.VISIBLE);
    }

    private void openDateSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_DATE_SETTINGS));
        } catch (ActivityNotFoundException exception) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (ActivityNotFoundException fallbackException) {
                Toast.makeText(this, "Impossible d’ouvrir les paramètres.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void retryValidation() {
        if (!ensureAuthentic()) return;
        ActivationVerifier.Verification status = ActivationStore.status(this);
        if (status.result == ActivationVerifier.Result.VALID) {
            openApplication();
        } else if (status.result == ActivationVerifier.Result.CLOCK_ROLLBACK) {
            showClockRollback();
        } else {
            recreate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (showingClockRollback) retryValidation();
    }

    private void copyRequest(CharSequence formattedCode) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Code de demande MVolaCash", formattedCode));
        Toast.makeText(this, "Code de demande copié", Toast.LENGTH_SHORT).show();
    }

    private void sendRequestBySms(CharSequence formattedCode) {
        String message = "Demande d'activation MVolaCash\nCode : " + formattedCode;
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:0341411058"));
        intent.putExtra("sms_body", message);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "Aucune application SMS disponible.", Toast.LENGTH_SHORT).show();
        }
    }

    private void pasteActivation() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (!clipboard.hasPrimaryClip() || clipboard.getPrimaryClip() == null
                || clipboard.getPrimaryClip().getItemCount() == 0) return;
        CharSequence text = clipboard.getPrimaryClip().getItemAt(0).coerceToText(this);
        if (text != null) activationCode.setText(text.toString().trim());
    }

    private void activate() {
        // APK authenticity always takes precedence over an otherwise valid MVACT1 proof.
        if (!ensureAuthentic()) return;
        String proof = activationCode.getText().toString().trim();
        ActivationVerifier.Result result = ActivationVerifier.verify(proof, installationId);
        if (result == ActivationVerifier.Result.WRONG_INSTALLATION) {
            Toast.makeText(this,
                    "Ce code d'activation ne correspond pas à cette installation.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (result == ActivationVerifier.Result.EXPIRED) {
            Toast.makeText(this, "Cette licence est déjà expirée.", Toast.LENGTH_LONG).show();
            return;
        }
        if (result != ActivationVerifier.Result.VALID || !ActivationStore.saveVerifiedProof(this, proof)) {
            Toast.makeText(this, "Code d'activation invalide.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Activation réussie", Toast.LENGTH_SHORT).show();
        openApplication();
    }

    private void openApplication() {
        if (!ensureAuthentic()) return;
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private boolean ensureAuthentic() {
        if (AppIntegrityChecker.check(this) == AppIntegrityChecker.Result.AUTHENTIC) return true;
        IntegrityFailureUi.showAndClose(this);
        return false;
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getBooleanExtra(EXTRA_MANAGE_LICENSE, false)) finish();
        else finishAffinity();
    }
}
