package com.netk.mvolatrack.security;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;

final class PinDialogs {
    interface PinResult { void accept(char[] pin); }
    static void request(Context context, String title, String message, PinResult result) {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        input.setHint("4 à 6 chiffres");
        int padding = (int) (24 * context.getResources().getDisplayMetrics().density);
        input.setPadding(padding, input.getPaddingTop(), padding, input.getPaddingBottom());
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle(title).setMessage(message)
                .setView(input).setNegativeButton("Annuler", null)
                .setPositiveButton("Continuer", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String value = input.getText().toString();
                    if (!value.matches("\\d{4,6}")) {
                        input.setError("Le code PIN doit contenir 4 à 6 chiffres.");
                        return;
                    }
                    input.getText().clear();
                    dialog.dismiss();
                    result.accept(value.toCharArray());
                }));
        dialog.getWindow();
        dialog.setOnDismissListener(ignored -> input.getText().clear());
        dialog.show();
        input.requestFocus();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
    private PinDialogs() {}
}
