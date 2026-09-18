package com.example.testapp.backup;

import com.example.testapp.database.SmsMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Versioned, password-encrypted backup format. No SMS content is stored in clear text. */
public final class BackupCodec {
    private static final byte[] MAGIC = "SMSTBKP1".getBytes(StandardCharsets.US_ASCII);
    private static final int FORMAT_VERSION = 1;
    private static final int ROOM_SCHEMA_VERSION = 2;
    private static final int ITERATIONS = 120_000;
    private static final int MAX_MESSAGES = 1_000_000;
    private static final int MAX_STRING_BYTES = 10 * 1024 * 1024;

    private BackupCodec() {}

    public static byte[] encode(List<SmsMessage> messages, char[] password)
            throws GeneralSecurityException, IOException {
        requirePassword(password);
        ByteArrayOutputStream payloadBytes = new ByteArrayOutputStream();
        try (DataOutputStream payload = new DataOutputStream(payloadBytes)) {
            payload.writeInt(ROOM_SCHEMA_VERSION);
            payload.writeInt(messages.size());
            for (SmsMessage message : messages) {
                writeString(payload, message.sender);
                writeString(payload, message.messageBody);
                payload.writeLong(message.receivedDate);
                payload.writeBoolean(message.readStatus);
                writeString(payload, message.uniqueKey);
            }
        }

        byte[] salt = randomBytes(16);
        byte[] iv = randomBytes(12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(128, iv));
        cipher.updateAAD(MAGIC);
        byte[] encrypted = cipher.doFinal(payloadBytes.toByteArray());

        ByteArrayOutputStream resultBytes = new ByteArrayOutputStream();
        try (DataOutputStream result = new DataOutputStream(resultBytes)) {
            result.write(MAGIC);
            result.writeInt(FORMAT_VERSION);
            result.write(salt);
            result.write(iv);
            result.writeInt(encrypted.length);
            result.write(encrypted);
        }
        return resultBytes.toByteArray();
    }

    public static List<SmsMessage> decode(byte[] backup, char[] password)
            throws GeneralSecurityException, IOException, BackupException {
        requirePassword(password);
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(backup))) {
            byte[] magic = new byte[MAGIC.length];
            input.readFully(magic);
            if (!java.util.Arrays.equals(magic, MAGIC)) throw new BackupException("Format de sauvegarde inconnu");
            if (input.readInt() != FORMAT_VERSION) throw new BackupException("Version de sauvegarde non prise en charge");
            byte[] salt = new byte[16]; input.readFully(salt);
            byte[] iv = new byte[12]; input.readFully(iv);
            int encryptedLength = input.readInt();
            if (encryptedLength < 16 || encryptedLength > backup.length) throw new BackupException("Sauvegarde tronquée");
            byte[] encrypted = new byte[encryptedLength]; input.readFully(encrypted);
            if (input.read() != -1) throw new BackupException("Données inattendues dans la sauvegarde");

            byte[] clear;
            try {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), new GCMParameterSpec(128, iv));
                cipher.updateAAD(MAGIC);
                clear = cipher.doFinal(encrypted);
            } catch (AEADBadTagException invalid) {
                throw new BackupException("Mot de passe incorrect ou sauvegarde corrompue", invalid);
            }
            return readPayload(clear);
        } catch (java.io.EOFException truncated) {
            throw new BackupException("Sauvegarde tronquée", truncated);
        }
    }

    private static List<SmsMessage> readPayload(byte[] clear) throws IOException, BackupException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(clear))) {
            int schemaVersion = input.readInt();
            if (schemaVersion < 1 || schemaVersion > ROOM_SCHEMA_VERSION) {
                throw new BackupException("Version de base de données non prise en charge : " + schemaVersion);
            }
            int count = input.readInt();
            if (count < 0 || count > MAX_MESSAGES) throw new BackupException("Nombre de messages invalide");
            List<SmsMessage> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String sender = readString(input);
                String body = readString(input);
                long date = input.readLong();
                boolean read = input.readBoolean();
                String uniqueKey = schemaVersion >= 2 ? readString(input) : SmsMessage.fingerprint(sender, date, body);
                result.add(new SmsMessage(sender, body, date, read, uniqueKey));
            }
            if (input.read() != -1) throw new BackupException("Contenu de sauvegarde invalide");
            return result;
        }
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException, BackupException {
        int length = input.readInt();
        if (length < 0 || length > MAX_STRING_BYTES) throw new BackupException("Champ de sauvegarde invalide");
        byte[] bytes = new byte[length]; input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static SecretKeySpec deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
        try {
            return new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded(), "AES");
        } finally { spec.clearPassword(); }
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size]; new SecureRandom().nextBytes(bytes); return bytes;
    }

    private static void requirePassword(char[] password) {
        if (password == null || password.length < 8) throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères");
    }

    public static final class BackupException extends Exception {
        BackupException(String message) { super(message); }
        BackupException(String message, Throwable cause) { super(message, cause); }
    }
}
