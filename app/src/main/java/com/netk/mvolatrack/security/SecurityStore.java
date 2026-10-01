package com.netk.mvolatrack.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

public final class SecurityStore {
    public static final long IMMEDIATE = 0L;
    public static final long ONE_MINUTE = 60_000L;
    public static final long FIVE_MINUTES = 300_000L;
    private final SharedPreferences preferences;

    public SecurityStore(Context context) {
        preferences = context.getSharedPreferences("access_security", Context.MODE_PRIVATE);
    }
    public boolean hasPin() { return preferences.contains("pin_hash"); }
    public boolean isLockEnabled() { return preferences.getBoolean("lock_enabled", false); }
    public void setLockEnabled(boolean enabled) { preferences.edit().putBoolean("lock_enabled", enabled).apply(); }
    public boolean isBiometricEnabled() { return preferences.getBoolean("biometric_enabled", false); }
    public void setBiometricEnabled(boolean enabled) { preferences.edit().putBoolean("biometric_enabled", enabled).apply(); }
    public long getTimeout() { return preferences.getLong("lock_timeout_ms", IMMEDIATE); }
    public void setTimeout(long timeout) { preferences.edit().putLong("lock_timeout_ms", timeout).apply(); }
    public void savePin(char[] pin) {
        byte[] salt = PinHasher.newSalt();
        byte[] hash = PinHasher.hash(pin, salt, PinHasher.ITERATIONS);
        preferences.edit()
                .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("pin_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
                .putInt("pin_iterations", PinHasher.ITERATIONS).apply();
    }
    public boolean verifyPin(char[] pin) {
        if (pin.length == 0) {
            java.util.Arrays.fill(pin, '\0');
            return false;
        }
        String salt = preferences.getString("pin_salt", null);
        String hash = preferences.getString("pin_hash", null);
        if (salt == null || hash == null) {
            java.util.Arrays.fill(pin, '\0');
            return false;
        }
        return PinHasher.verify(pin, Base64.decode(salt, Base64.NO_WRAP),
                Base64.decode(hash, Base64.NO_WRAP),
                preferences.getInt("pin_iterations", PinHasher.ITERATIONS));
    }
}
