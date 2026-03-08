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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatComponent.class, priority = 1100)
public class ChatHudMixin {

    // Guards against infinite recursion when we re-submit the decrypted message.
    private static final ThreadLocal<Boolean> DECRYPTING = ThreadLocal.withInitial(() -> false);

    /**
     * Intercepts every message added to the chat HUD.
     * If a key is set and the message contains the [SC] prefix, cancels the
     * encrypted call and re-submits a new call with the decrypted Component.
     * This ensures Meteor Client (and any other mod) only ever sees the
     * decrypted text — they never receive the encrypted version to display.
     */
    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void silphcrypt_decryptIncoming(Component message, MessageSignature sig, GuiMessageTag tag, CallbackInfo ci) {
        if (DECRYPTING.get()) return;
        if (!KeyManager.hasKey()) return;

        String text = message.getString();
        int scIdx = text.indexOf(CryptoUtil.PREFIX);
        if (scIdx == -1) return;

        try {
            String prefix = text.substring(0, scIdx);
            String encryptedPart = text.substring(scIdx);

            String decrypted = CryptoUtil.decrypt(encryptedPart, KeyManager.getKey());
            if (decrypted == null) return;

            Component decryptedComponent = Component.literal(prefix)
                .append(
                    Component.literal(decrypted)
                        .withStyle(s -> s.withColor(0x55FFFF))  // cyan = decrypted
                )
                .append(
                    Component.literal(" [SC]")
                        .withStyle(s -> s.withColor(0x008888))  // dark cyan = indicator
                );

            // Cancel the encrypted call, then re-submit with the decrypted Component.
            // The recursion guard ensures our injection is skipped on the second call,
            // so all other mods (Meteor etc.) process the decrypted Component normally.
            ci.cancel();
            DECRYPTING.set(true);
            try {
                ((ChatComponent) (Object) this).addMessage(decryptedComponent, sig, tag);
            } finally {
                DECRYPTING.set(false);
            }

        } catch (Exception e) {
            SilphCrypt.LOGGER.debug("[SilphCrypt] Could not decrypt incoming message (wrong key or not for us)");
        }
    }
}
