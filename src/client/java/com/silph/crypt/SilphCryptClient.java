package com.silph.crypt;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class SilphCryptClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SilphCryptCommand.register();
        registerOutgoingEncryption();
        SilphCrypt.LOGGER.info("[SilphCrypt] Client initialised. Use /silphcrypt key <key> to start encrypting.");
    }

    private static void registerOutgoingEncryption() {
        ClientSendMessageEvents.MODIFY_CHAT.register(message -> {
            if (!KeyManager.hasKey()) return message;

            try {
                String encrypted = CryptoUtil.encrypt(message, KeyManager.getKey());

                // Minecraft enforces a 256-character chat limit.
                // Warn the player if the encrypted form would be truncated.
                if (encrypted.length() > 256) {
                    Minecraft.getInstance().gui.getChat().addMessage(
                        Component.literal("[SilphCrypt] Message too long to encrypt — sending as plain text.")
                            .withStyle(ChatFormatting.RED)
                    );
                    return message;
                }

                return encrypted;
            } catch (Exception e) {
                SilphCrypt.LOGGER.error("[SilphCrypt] Failed to encrypt outgoing message", e);
                return message;
            }
        });
    }
}
