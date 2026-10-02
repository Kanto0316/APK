package com.netk.mvolatrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.netk.mvolatrack.activation.ActivationActivity;
import com.netk.mvolatrack.activation.ActivationStore;

/** Initial cash point setup and later editing from the navigation drawer. */
public final class AccountActivity extends AppCompatActivity {
    private boolean firstSetup;
    private EditText nameInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ActivationStore.hasValidActivation(this)) {
            startActivity(new Intent(this, ActivationActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
            return;
        }
        firstSetup = !CashPointStore.isConfigured(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_account);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        applyHeaderInsets(findViewById(R.id.accountHeader));
        applyContentInsets(findViewById(R.id.accountScroll));
        findViewById(R.id.accountBack).setOnClickListener(view -> leave());
        ((TextView) findViewById(R.id.accountDescription)).setText(firstSetup
                ? "Pour commencer, donnez un nom à votre cash point. Il figurera sur les nouvelles factures."
                : "Modifiez le nom affiché sur les nouvelles factures. Les factures déjà enregistrées ne changent pas.");
        nameInput = findViewById(R.id.accountName);
        nameInput.setText(CashPointStore.getName(this));
        nameInput.setSelection(nameInput.length());
        findViewById(R.id.accountSave).setOnClickListener(view -> save());
    }

    private void save() {
        String name = CashPointStore.normalize(nameInput.getText().toString());
        if (name.isEmpty()) {
            nameInput.setError("Saisissez le nom du cash point");
            nameInput.requestFocus();
            return;
        }
        if (name.length() > CashPointStore.MAX_NAME_LENGTH) {
            nameInput.setError("24 caractères maximum");
            nameInput.requestFocus();
            return;
        }
        if (!CashPointStore.saveName(this, name)) {
            Toast.makeText(this, "Impossible d’enregistrer le nom du cash point.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (firstSetup) {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        } else {
            Toast.makeText(this, "Nom du cash point enregistré.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void leave() {
        if (firstSetup) finishAffinity();
        else finish();
    }

    @Override
    public void onBackPressed() {
        leave();
    }

    private static void applyHeaderInsets(View header) {
        final int initialHeight = header.getLayoutParams().height;
        final int initialLeft = header.getPaddingLeft();
        final int initialTop = header.getPaddingTop();
        final int initialRight = header.getPaddingRight();
        final int initialBottom = header.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(header, (view, insets) -> {
            Insets top = insets.getInsets(WindowInsetsCompat.Type.statusBars()
                    | WindowInsetsCompat.Type.displayCutout());
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = initialHeight + top.top;
            view.setLayoutParams(params);
            view.setPadding(initialLeft + top.left, initialTop + top.top,
                    initialRight + top.right, initialBottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    private static void applyContentInsets(ScrollView content) {
        final int initialBottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), initialBottom + Math.max(bars.bottom, keyboard.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
