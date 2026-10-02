package com.netk.mvolatrack.account;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.netk.mvolatrack.MainActivity;
import com.netk.mvolatrack.R;

public final class AccountActivity extends AppCompatActivity {
    public static final String EXTRA_REQUIRED = "cash_point_name_required";
    private boolean required;
    private EditText name;
    private EditText number;
    private TextView nameError;
    private TextView numberError;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        required = getIntent().getBooleanExtra(EXTRA_REQUIRED, false);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_account);
        applyInsets(findViewById(R.id.accountRoot));
        name = findViewById(R.id.cashPointName);
        number = findViewById(R.id.cashPointNumber);
        nameError = findViewById(R.id.cashPointNameError);
        numberError = findViewById(R.id.cashPointNumberError);
        // Do not truncate a legacy value merely by opening this screen. The restored filter
        // still requires it to be shortened before the account can next be saved.
        name.setFilters(new InputFilter[0]);
        name.setText(AccountStore.getName(this));
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(CashPointName.MAX_LENGTH)});
        name.setSelection(name.length());
        number.setText(CashPointNumber.format(AccountStore.getNumber(this)));
        number.setSelection(number.length());
        findViewById(R.id.accountBack).setVisibility(required ? View.INVISIBLE : View.VISIBLE);
        findViewById(R.id.accountBack).setOnClickListener(view -> finish());
        findViewById(R.id.saveCashPointName).setOnClickListener(view -> save());
        if (required) {
            name.requestFocus();
            name.post(() -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .showSoftInput(name, InputMethodManager.SHOW_IMPLICIT));
        }
    }

    private void save() {
        String normalized = CashPointName.normalize(name.getText().toString());
        nameError.setVisibility(View.GONE);
        numberError.setVisibility(View.GONE);
        if (!CashPointName.isValid(normalized)) {
            nameError.setText(normalized.isEmpty() ? "Le nom du cash point est obligatoire."
                    : "Le nom ne peut pas dépasser " + CashPointName.MAX_LENGTH + " caractères.");
            nameError.setVisibility(View.VISIBLE);
            return;
        }
        String normalizedNumber = CashPointNumber.normalize(number.getText().toString());
        if (normalizedNumber == null) {
            numberError.setText("Saisissez un numéro malgache valide (0XXXXXXXXX ou +261XXXXXXXXX).");
            numberError.setVisibility(View.VISIBLE);
            return;
        }
        if (!AccountStore.save(this, normalized, normalizedNumber)) {
            nameError.setText("Impossible d’enregistrer le compte. Réessayez.");
            nameError.setVisibility(View.VISIBLE);
            return;
        }
        if (required) {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        finish();
    }

    private static void applyInsets(View root) {
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(left + bars.left, top + bars.top, right + bars.right,
                    bottom + Math.max(bars.bottom, ime.bottom));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    @Override public void onBackPressed() {
        if (required) finishAffinity();
        else super.onBackPressed();
    }
}
