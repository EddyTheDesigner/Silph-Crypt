package com.silph.crypt;

/**
 * Holds the player's current encryption key in memory.
 * The key is never persisted to disk — users must set it each session.
 */
public class KeyManager {

    private static String currentKey = null;

    public static void setKey(String key) {
        currentKey = key;
    }

    public static void clearKey() {
        currentKey = null;
    }

    public static String getKey() {
        return currentKey;
    }

    public static boolean hasKey() {
        return currentKey != null && !currentKey.isEmpty();
    }
}
