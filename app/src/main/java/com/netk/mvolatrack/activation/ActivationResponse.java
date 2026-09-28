package com.netk.mvolatrack.activation;

import java.nio.charset.StandardCharsets;

/** Parser and canonical representation for the public MVACT1 response protocol. */
public final class ActivationResponse {
    public static final String PREFIX = "MVACT1";
    public static final String PRODUCT = "MVOLACASH";
    public static final int VERSION = 1;
    public static final String TYPE = "PERMANENT";

    private final String installationId;
    private final byte[] signature;
    private final byte[] canonicalPayload;

    private ActivationResponse(String installationId, byte[] signature, byte[] canonicalPayload) {
        this.installationId = installationId;
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
        if (!java.util.Arrays.equals(payload, canonical.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid UTF-8");
        }
        String[] fields = canonical.split("\\|", -1);
        if (fields.length != 4 || !PRODUCT.equals(fields[0])
                || !Integer.toString(VERSION).equals(fields[1]) || !TYPE.equals(fields[2])) {
            throw new IllegalArgumentException("Unsupported payload");
        }
        String installationId = ActivationRequest.parse(fields[3]);
        if (!installationId.equals(fields[3])) throw new IllegalArgumentException("Non-canonical id");
        String expected = canonical(installationId);
        if (!expected.equals(canonical)) throw new IllegalArgumentException("Non-canonical payload");
        return new ActivationResponse(installationId, signature, payload);
    }

    public static String canonical(String installationId) {
        return PRODUCT + "|" + VERSION + "|" + TYPE + "|" + ActivationRequest.parse(installationId);
    }

    public String getInstallationId() { return installationId; }
    public byte[] getSignature() { return signature.clone(); }
    public byte[] getCanonicalPayload() { return canonicalPayload.clone(); }
}
