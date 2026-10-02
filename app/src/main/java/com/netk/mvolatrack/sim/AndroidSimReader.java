package com.netk.mvolatrack.sim;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Reads carrier-controlled subscription data; deliberately never uses the editable display name. */
public final class AndroidSimReader {
    private final Context context;

    public AndroidSimReader(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public List<SimOperatorVerifier.SimIdentity> readActiveSims() {
        if (!hasPermission()) return null;
        SubscriptionManager manager = context.getSystemService(SubscriptionManager.class);
        TelephonyManager telephony = context.getSystemService(TelephonyManager.class);
        if (manager == null || telephony == null) return null;
        try {
            List<SubscriptionInfo> subscriptions = manager.getActiveSubscriptionInfoList();
            if (subscriptions == null) return Collections.emptyList();
            List<SimOperatorVerifier.SimIdentity> result = new ArrayList<>();
            for (SubscriptionInfo subscription : subscriptions) {
                int simState = telephony.getSimState(subscription.getSimSlotIndex());
                if (simState != TelephonyManager.SIM_STATE_READY) {
                    return null;
                }
                CharSequence carrier = subscription.getCarrierName();
                String networkId = networkId(subscription);
                result.add(new SimOperatorVerifier.SimIdentity(
                        carrier == null ? "" : carrier.toString(), networkId,
                        subscription.getSimSlotIndex()));
            }
            return result;
        } catch (RuntimeException unavailable) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static String networkId(SubscriptionInfo subscription) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String mcc = subscription.getMccString();
            String mnc = subscription.getMncString();
            return mcc == null || mnc == null ? "" : mcc + mnc;
        }
        int mcc = subscription.getMcc();
        int mnc = subscription.getMnc();
        return mcc <= 0 || mnc < 0 ? "" : String.format(Locale.ROOT, "%03d%02d", mcc, mnc);
    }
}
