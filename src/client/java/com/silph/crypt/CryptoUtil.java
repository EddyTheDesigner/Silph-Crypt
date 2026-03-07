package com.silph.crypt;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class CryptoUtil {

    public static final String PREFIX = "[SC]";
    private static final int IV_LENGTH = 16;

    /**
     * Encrypts plaintext with AES-256-CBC.
     * Format: PREFIX + Base64(IV[16] + ciphertext)
     */
    public static String encrypt(String plaintext, String keyString) throws Exception {
        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(keyString.getBytes(StandardCharsets.UTF_8));

        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[IV_LENGTH + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
        System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

        return PREFIX + Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypts a message produced by {@link #encrypt}.
     * Returns null if the message doesn't start with PREFIX or decryption fails.
     */
    public static String decrypt(String cipherMessage, String keyString) throws Exception {
        if (!cipherMessage.startsWith(PREFIX)) return null;

        byte[] data = Base64.getDecoder().decode(cipherMessage.substring(PREFIX.length()));
        if (data.length < IV_LENGTH + 16) return null;

        byte[] iv = Arrays.copyOfRange(data, 0, IV_LENGTH);
        byte[] encrypted = Arrays.copyOfRange(data, IV_LENGTH, data.length);

        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(keyString.getBytes(StandardCharsets.UTF_8));

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }
}
