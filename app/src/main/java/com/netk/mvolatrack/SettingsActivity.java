package com.netk.mvolatrack;

import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

/** Application settings displayed as a full screen instead of a dialog. */
public final class SettingsActivity extends AppCompatActivity {
    private File pendingDiagnosticReport;
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
