package com.netk.mvolatrack.activation;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Strict parser for the V1 and Admin 1.1 (V2) MVACT1 payloads. */
public final class ActivationResponse {
    public static final String PREFIX = "MVACT1";
    public static final String PRODUCT = "MVOLACASH";

    public enum LicenseType { WEEK, MONTH, PERMANENT }

    private final int version;
    private final LicenseType type;
    private final String installationId;
    private final long issuedAt;
    private final long expiresAt;
    private final byte[] signature;
    private final byte[] canonicalPayload;

    private ActivationResponse(int version, LicenseType type, String installationId,
            long issuedAt, long expiresAt, byte[] signature, byte[] canonicalPayload) {
        this.version = version;
        this.type = type;
        this.installationId = installationId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.signature = signature;
        this.canonicalPayload = canonicalPayload;
    }

    public static ActivationResponse parse(String encoded) {
        if (encoded == null) throw new IllegalArgumentException("Missing activation");
        String[] sections = encoded.trim().split("\\.", -1);
        if (sections.length != 3 || !PREFIX.equals(sections[0])
                || sections[1].isEmpty() || sections[2].isEmpty()) {
            throw new IllegalArgumentException("Invalid envelope");
        }
        byte[] payload = Base64Url.decode(sections[1]);
        byte[] signature = Base64Url.decode(sections[2]);
        String canonical = new String(payload, StandardCharsets.UTF_8);
        if (!Arrays.equals(payload, canonical.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid UTF-8");
        }
        String[] fields = canonical.split("\\|", -1);
        if (fields.length < 2 || !PRODUCT.equals(fields[0])) {
            throw new IllegalArgumentException("Unsupported payload");
        }
        if ("1".equals(fields[1])) return parseV1(fields, canonical, signature, payload);
        if ("2".equals(fields[1])) return parseV2(fields, canonical, signature, payload);
        throw new IllegalArgumentException("Unsupported version");
    }

    private static ActivationResponse parseV1(String[] fields, String canonical,
            byte[] signature, byte[] payload) {
        if (fields.length != 4 || !"PERMANENT".equals(fields[2])) {
            throw new IllegalArgumentException("Unsupported V1 payload");
        }
        String id = canonicalId(fields[3]);
        if (!canonicalV1(id).equals(canonical)) throw new IllegalArgumentException("Non-canonical payload");
        return new ActivationResponse(1, LicenseType.PERMANENT, id, 0, 0, signature, payload);
    }

    private static ActivationResponse parseV2(String[] fields, String canonical,
            byte[] signature, byte[] payload) {
        // Admin 1.1 canonical form:
        // MVOLACASH|2|TYPE|INSTALLATION_ID|issuedAtEpochMillis|expiresAtEpochMillis
        if (fields.length != 6) throw new IllegalArgumentException("Unsupported V2 payload");
        LicenseType type;
        try { type = LicenseType.valueOf(fields[2]); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("Unsupported type"); }
        String id = canonicalId(fields[3]);
        long issuedAt = parseTimestamp(fields[4]);
        long expiresAt = parseTimestamp(fields[5]);
        if (issuedAt <= 0) throw new IllegalArgumentException("Invalid issuedAt");
        if (type == LicenseType.PERMANENT) {
            if (expiresAt != 0) throw new IllegalArgumentException("Permanent V2 must not expire");
        } else if (expiresAt <= issuedAt) {
            throw new IllegalArgumentException("Invalid expiry");
        }
        if (!canonicalV2(type, id, issuedAt, expiresAt).equals(canonical)) {
            throw new IllegalArgumentException("Non-canonical payload");
        }
        return new ActivationResponse(2, type, id, issuedAt, expiresAt, signature, payload);
    }

    private static String canonicalId(String value) {
        String id = ActivationRequest.parse(value);
        if (!id.equals(value)) throw new IllegalArgumentException("Non-canonical id");
        return id;
    }

    private static long parseTimestamp(String value) {
        if (value.isEmpty() || (value.length() > 1 && value.charAt(0) == '0')) {
            throw new IllegalArgumentException("Non-canonical timestamp");
        }
        try { return Long.parseLong(value); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("Invalid timestamp"); }
    }

    /** The legacy canonicalisation is intentionally unchanged. */
    public static String canonical(String installationId) { return canonicalV1(installationId); }
    public static String canonicalV1(String installationId) {
        return PRODUCT + "|1|PERMANENT|" + ActivationRequest.parse(installationId);
    }
    public static String canonicalV2(LicenseType type, String installationId,
            long issuedAt, long expiresAt) {
        return PRODUCT + "|2|" + type.name() + "|" + ActivationRequest.parse(installationId)
                + "|" + issuedAt + "|" + expiresAt;
    }

    public int getVersion() { return version; }
    public LicenseType getType() { return type; }
    public String getInstallationId() { return installationId; }
    public long getIssuedAt() { return issuedAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isTemporary() { return type != LicenseType.PERMANENT; }
    public byte[] getSignature() { return signature.clone(); }
    public byte[] getCanonicalPayload() { return canonicalPayload.clone(); }
}
