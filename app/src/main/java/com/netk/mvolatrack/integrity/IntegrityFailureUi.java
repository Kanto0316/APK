package com.netk.mvolatrack.integrity;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

/** Single non-technical, terminal UI for an APK authenticity failure. */
public final class IntegrityFailureUi {
    private IntegrityFailureUi() { }

    public static void showAndClose(Activity activity) {
        if (activity.isFinishing()) return;
        new AlertDialog.Builder(activity)
                .setTitle("Application non authentique")
                .setMessage("Cette version de MVolaCash n'a pas pu être authentifiée.\n"
                        + "Installez une version officielle de l'application.")
                .setCancelable(false)
                .setPositiveButton("FERMER", (dialog, which) -> activity.finishAffinity())
                .show();
    }
}
