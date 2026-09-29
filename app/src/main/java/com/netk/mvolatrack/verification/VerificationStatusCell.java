package com.netk.mvolatrack.verification;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.netk.mvolatrack.R;

/** Binds the compound, accessible status cell used in both transaction tables. */
public final class VerificationStatusCell {
    private VerificationStatusCell() {}

    public static void bind(View cell, TransactionBalanceVerification verification) {
        AppCompatImageView icon = cell.findViewById(R.id.statusIcon);
        TextView text = cell.findViewById(R.id.statusText);
        VerificationStatusPresentation presentation = VerificationStatusPresentation.from(verification);
        int color = ContextCompat.getColor(cell.getContext(), presentation.color);

        // Always reset every recycled property, including the absent-result state.
        cell.setVisibility(View.VISIBLE);
        icon.setVisibility(View.VISIBLE);
        icon.setImageResource(presentation.icon);
        ImageViewCompat.setImageTintList(icon, android.content.res.ColorStateList.valueOf(color));
        icon.setContentDescription(null);
        text.setVisibility(View.VISIBLE);
        text.setText(presentation.label);
        text.setTextColor(color);
        cell.setContentDescription(cell.getContext().getString(
                R.string.sms_status_accessibility, text.getText()));
        cell.setOnClickListener(view -> BalanceVerificationDialog.show(
                view.getContext(), verification));
        cell.setClickable(true);
        cell.setFocusable(true);
    }
}
