package com.example.testapp.background;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Centralises the best-effort checks and manufacturer-specific navigation needed to let the
 * user remove background restrictions. Android deliberately exposes no API for changing, or
 * reliably reading, an OEM's auto-start switch.
 */
public final class BackgroundExecutionManager {
    private final Context context;

    public BackgroundExecutionManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Returns the restrictions Android makes observable. OEM auto-start state is not readable. */
    public boolean isBackgroundExecutionAllowed() {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        boolean backgroundRestricted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && activityManager != null && activityManager.isBackgroundRestricted();

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean batteryOptimized = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && powerManager != null
                && !powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        return !backgroundRestricted && !batteryOptimized;
    }

    /**
     * Opens the first settings Activity installed on this device. No setting is ever changed by
     * the app: the user remains in control of both auto-start and background execution.
     */
    public boolean openBackgroundSettings() {
        for (Intent candidate : settingsCandidates(Build.MANUFACTURER)) {
            if (candidate.resolveActivity(context.getPackageManager()) == null) continue;
            candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(candidate);
                return true;
            } catch (ActivityNotFoundException | SecurityException ignored) {
                // Vendor components vary between firmware releases; continue to Android settings.
            }
        }
        return false;
    }

    List<Intent> settingsCandidates(String manufacturer) {
        List<Intent> intents = new ArrayList<>();
        String brand = manufacturer == null ? "" : manufacturer.toLowerCase(Locale.ROOT);
        if (brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")) {
            addComponent(intents, "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity");
        } else if (brand.contains("huawei") || brand.contains("honor")) {
            addComponent(intents, "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");
            addComponent(intents, "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity");
        } else if (brand.contains("oppo") || brand.contains("realme")
                || brand.contains("oneplus")) {
            addComponent(intents, "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity");
            addComponent(intents, "com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity");
            addComponent(intents, "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity");
        } else if (brand.contains("vivo") || brand.contains("iqoo")) {
            addComponent(intents, "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity");
            addComponent(intents, "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity");
        } else if (brand.contains("samsung")) {
            addComponent(intents, "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity");
        }

        // Standard Android fallbacks. The first screen lets the user explicitly exempt the app;
        // application details remains available on devices that hide that screen.
        intents.add(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        intents.add(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + context.getPackageName())));
        intents.add(new Intent(Settings.ACTION_SETTINGS));
        return intents;
    }

    private static void addComponent(List<Intent> intents, String packageName, String className) {
        intents.add(new Intent().setComponent(new ComponentName(packageName, className)));
    }
}
