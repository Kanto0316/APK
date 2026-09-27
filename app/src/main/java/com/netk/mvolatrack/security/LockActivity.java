package com.netk.mvolatrack.security;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public final class LockActivity extends AppCompatActivity {
    private SecurityStore store;
    private EditText pin;
    private TextView error;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new SecurityStore(this);
        if (!store.isLockEnabled()) { finish(); return; }
        setContentView(com.netk.mvolatrack.R.layout.activity_lock);
        pin = findViewById(com.netk.mvolatrack.R.id.lockPin);
        error = findViewById(com.netk.mvolatrack.R.id.lockError);
        findViewById(com.netk.mvolatrack.R.id.unlockButton).setOnClickListener(v -> verifyPin());
        pin.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { error.setText(""); }
            public void afterTextChanged(Editable e) {}
        });
        View biometric = findViewById(com.netk.mvolatrack.R.id.biometricButton);
        boolean canUse = store.isBiometricEnabled() && BiometricAuth.isAvailable(this);
        biometric.setVisibility(canUse ? View.VISIBLE : View.GONE);
        biometric.setOnClickListener(v -> BiometricAuth.authenticate(this, this::unlock));
    }
    private void verifyPin() {
        char[] candidate = pin.getText().toString().toCharArray();
        pin.getText().clear();
        if (store.verifyPin(candidate)) unlock(); else error.setText("Code PIN incorrect");
    }
    private void unlock() { AppLockManager.markUnlocked(); finish(); overridePendingTransition(0, 0); }
    @Override public void onBackPressed() { moveTaskToBack(true); }
}
