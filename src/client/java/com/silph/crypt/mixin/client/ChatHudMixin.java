package com.silph.crypt.mixin.client;

import com.silph.crypt.CryptoUtil;
import com.silph.crypt.KeyManager;
import com.silph.crypt.SilphCrypt;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class ChatHudMixin {

    /**
     * Intercepts every message added to the chat HUD.
     * If a key is set and the message contains the [SC] prefix, attempts decryption.
     * On success the rendered Component is replaced with the decrypted text.
     * On failure (wrong key, malformed data) the original ciphertext is left untouched.
     */
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true,
        index = 0
    )
    private Component silphcrypt_decryptIncoming(Component original) {
        if (!KeyManager.hasKey()) return original;

        String text = original.getString();
        int scIdx = text.indexOf(CryptoUtil.PREFIX);
        if (scIdx == -1) return original;

        try {
            String prefix = text.substring(0, scIdx);
            String encryptedPart = text.substring(scIdx);

            String decrypted = CryptoUtil.decrypt(encryptedPart, KeyManager.getKey());
            if (decrypted == null) return original;

            // Rebuild the component: preserve any name prefix in grey,
            // render decrypted content in cyan, append a small [SC] marker.
            return Component.literal(prefix)
                .append(
                    Component.literal(decrypted)
                        .withStyle(s -> s.withColor(0x55FFFF))  // cyan = decrypted
                )
                .append(
                    Component.literal(" [SC]")
                        .withStyle(s -> s.withColor(0x008888))  // dark cyan = indicator
                );

        } catch (Exception e) {
            // Wrong key or corrupted payload — show raw ciphertext as-is
            SilphCrypt.LOGGER.debug("[SilphCrypt] Could not decrypt incoming message (wrong key or not for us)");
        }

        return original;
    }
}
