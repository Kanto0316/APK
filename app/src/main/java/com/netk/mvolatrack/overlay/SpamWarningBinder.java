package com.netk.mvolatrack.overlay;

import android.view.View;
import android.widget.TextView;

import com.netk.mvolatrack.R;

final class SpamWarningBinder {
    private SpamWarningBinder() {}

    static void bind(View view, String sender, View.OnClickListener listener) {
        String actualSender = sender == null || sender.trim().isEmpty()
                ? "Expéditeur inconnu" : sender;
        ((TextView) view.findViewById(R.id.spamWarningMessage)).setText(
                "Expéditeur non reconnu :\n" + actualSender
                        + "\n\nCe message n'a pas été enregistré comme transaction MVola.");
        view.findViewById(R.id.spamWarningOk).setOnClickListener(listener);
    }
}
