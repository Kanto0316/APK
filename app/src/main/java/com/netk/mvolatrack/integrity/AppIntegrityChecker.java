package com.netk.mvolatrack.integrity;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import com.netk.mvolatrack.BuildConfig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Fail-closed verification of the certificate used to sign the installed APK. */
public final class AppIntegrityChecker {
    public enum Result { AUTHENTIC, APP_INTEGRITY_FAILED }

    private AppIntegrityChecker() { }

    public static Result check(Context context) {
        Set<String> trusted = parseTrustedFingerprints(
                BuildConfig.TRUSTED_APP_CERTIFICATE_SHA256);
        if (trusted.isEmpty()) return Result.APP_INTEGRITY_FAILED;

        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info;
            Signature[] signatures;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info = manager.getPackageInfo(context.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES);
                SigningInfo signingInfo = info.signingInfo;
                if (signingInfo == null) return Result.APP_INTEGRITY_FAILED;
                // Multiple current signers must all be trusted. With certificate rotation,
                // accept any trusted certificate in the platform-verified signing history.
                boolean multipleSigners = signingInfo.hasMultipleSigners();
                signatures = multipleSigners ? signingInfo.getApkContentsSigners()
                        : signingInfo.getSigningCertificateHistory();
                return certificatesAreTrusted(signatures, trusted, multipleSigners)
                        ? Result.AUTHENTIC : Result.APP_INTEGRITY_FAILED;
            } else {
                @SuppressWarnings("deprecation")
                PackageInfo legacyInfo = manager.getPackageInfo(context.getPackageName(),
                        PackageManager.GET_SIGNATURES);
                info = legacyInfo;
                @SuppressWarnings("deprecation")
                Signature[] legacySignatures = info.signatures;
                signatures = legacySignatures;
            }
            return certificatesAreTrusted(signatures, trusted, true)
                    ? Result.AUTHENTIC : Result.APP_INTEGRITY_FAILED;
        } catch (PackageManager.NameNotFoundException | RuntimeException exception) {
            return Result.APP_INTEGRITY_FAILED;
        }
    }

    static boolean certificatesAreTrusted(Signature[] signatures, Set<String> trusted,
            boolean requireAll) {
        if (signatures == null || signatures.length == 0 || trusted.isEmpty()) return false;
        byte[][] certificates = new byte[signatures.length][];
        for (int index = 0; index < signatures.length; index++) {
            if (signatures[index] == null) return false;
            certificates[index] = signatures[index].toByteArray();
        }
        return certificateBytesAreTrusted(certificates, trusted, requireAll);
    }

    static boolean certificateBytesAreTrusted(byte[][] certificates, Set<String> trusted,
            boolean requireAll) {
        if (certificates == null || certificates.length == 0 || trusted.isEmpty()) return false;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            boolean matched = false;
            for (byte[] certificate : certificates) {
                boolean current = certificate != null
                        && trusted.contains(toHex(digest.digest(certificate)));
                if (requireAll && !current) return false;
                matched |= current;
                digest.reset();
            }
            return matched;
        } catch (NoSuchAlgorithmException impossible) {
            return false;
        }
    }

    static Set<String> parseTrustedFingerprints(String configured) {
        Set<String> fingerprints = new HashSet<>();
        if (configured == null) return fingerprints;
        for (String candidate : configured.split(",")) {
            String normalized = candidate.replace(":", "").trim().toUpperCase(Locale.ROOT);
            if (normalized.matches("[0-9A-F]{64}")) fingerprints.add(normalized);
        }
        return fingerprints;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format(Locale.ROOT, "%02X", value & 0xff));
        return result.toString();
    }
}
