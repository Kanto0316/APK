package com.netk.mvolatrack.security;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.netk.mvolatrack.R;
import java.util.Arrays;

public final class SecurityActivity extends AppCompatActivity {
    private SecurityStore store;
    private SwitchCompat lockSwitch;
    private SwitchCompat biometricSwitch;
    private TextView changePin;
    private TextView autoLock;
    private boolean rendering;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        }
        setContentView(R.layout.activity_security);
        applyHeaderInsets(findViewById(R.id.securityHeader));
        store = new SecurityStore(this);
        lockSwitch = findViewById(R.id.appLockSwitch);
        biometricSwitch = findViewById(R.id.biometricSwitch);
        changePin = findViewById(R.id.changePin);
        autoLock = findViewById(R.id.autoLock);
        findViewById(R.id.securityBack).setOnClickListener(v -> finish());
        lockSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (rendering) return;
            if (checked) enableLock(); else authenticate(this::disableLock);
        });
        biometricSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (rendering) return;
            if (checked && !BiometricAuth.isAvailable(this)) {
                Toast.makeText(this, "Aucune biométrie utilisable n’est enregistrée.", Toast.LENGTH_LONG).show();
                render();
            } else store.setBiometricEnabled(checked);
        });
        changePin.setOnClickListener(v -> authenticate(this::createPin));
        autoLock.setOnClickListener(v -> chooseTimeout());
        render();
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

    private void enableLock() {
        if (store.hasPin()) {
            store.setLockEnabled(true); AppLockManager.markUnlocked(); render();
        } else {
            render();
            createPin();
        }
    }
    private void createPin() {
        PinDialogs.request(this, "Créer votre code PIN", "Saisissez 4 à 6 chiffres.", first ->
                PinDialogs.request(this, "Confirmer votre code PIN", "Saisissez à nouveau votre code.", second -> {
                    if (!Arrays.equals(first, second)) {
                        Arrays.fill(first, '\0'); Arrays.fill(second, '\0');
                        Toast.makeText(this, "Les codes PIN ne correspondent pas.", Toast.LENGTH_LONG).show();
                        render(); return;
                    }
                    Arrays.fill(second, '\0');
                    store.savePin(first);
                    store.setLockEnabled(true);
                    AppLockManager.markUnlocked();
                    render();
                }));
    }
    private void authenticate(Runnable success) {
        String[] choices = store.isBiometricEnabled() && BiometricAuth.isAvailable(this)
                ? new String[]{"Code PIN", "Biométrie"} : new String[]{"Code PIN"};
        new AlertDialog.Builder(this).setTitle("Confirmer votre identité")
                .setItems(choices, (dialog, which) -> {
                    if (which == 1) BiometricAuth.authenticate(this, success);
                    else PinDialogs.request(this, "PIN actuel", "Saisissez votre code PIN.", pin -> {
                        if (store.verifyPin(pin)) success.run();
                        else Toast.makeText(this, "Code PIN incorrect", Toast.LENGTH_SHORT).show();
                        render();
                    });
                }).setNegativeButton("Annuler", (d, w) -> render()).show();
    }
    private void disableLock() {
        store.setLockEnabled(false);
        store.setBiometricEnabled(false);
        AppLockManager.markUnlocked();
        render();
    }
    private void chooseTimeout() {
        String[] labels = {"Immédiatement", "Après 1 minute", "Après 5 minutes"};
        long[] values = {SecurityStore.IMMEDIATE, SecurityStore.ONE_MINUTE, SecurityStore.FIVE_MINUTES};
        int selected = store.getTimeout() == values[1] ? 1 : store.getTimeout() == values[2] ? 2 : 0;
        new AlertDialog.Builder(this).setTitle("Verrouillage automatique")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    store.setTimeout(values[which]); dialog.dismiss(); render();
                }).setNegativeButton("Annuler", null).show();
    }
    private void render() {
        rendering = true;
        boolean enabled = store.isLockEnabled();
        lockSwitch.setChecked(enabled);
        boolean available = BiometricAuth.isAvailable(this);
        biometricSwitch.setVisibility(enabled ? View.VISIBLE : View.GONE);
        biometricSwitch.setEnabled(available);
        biometricSwitch.setChecked(enabled && available && store.isBiometricEnabled());
        changePin.setVisibility(enabled ? View.VISIBLE : View.GONE);
        autoLock.setEnabled(enabled);
        long timeout = store.getTimeout();
        String value = timeout == SecurityStore.ONE_MINUTE ? "Après 1 minute"
                : timeout == SecurityStore.FIVE_MINUTES ? "Après 5 minutes" : "Immédiatement";
        autoLock.setText(value + "                                      ›");
        rendering = false;
    }
}
