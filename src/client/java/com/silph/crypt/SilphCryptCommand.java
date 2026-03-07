package com.silph.crypt;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SilphCryptCommand {

    // Cyan for encryption-related feedback
    private static final int COLOR_ON  = 0x55FFFF;
    private static final int COLOR_OFF = 0xAAAAAA;
    private static final int COLOR_WARN = 0xFF5555;

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(
                literal("silphcrypt")
                    // /silphcrypt key <key>
                    .then(literal("key")
                        .then(argument("key", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String key = StringArgumentType.getString(ctx, "key");
                                KeyManager.setKey(key);
                                ctx.getSource().sendFeedback(
                                    Component.literal("[SilphCrypt] Encryption key set. Chat messages will now be encrypted.")
                                        .withStyle(s -> s.withColor(COLOR_ON))
                                );
                                return 1;
                            })
                        )
                    )
                    // /silphcrypt clear
                    .then(literal("clear")
                        .executes(ctx -> {
                            KeyManager.clearKey();
                            ctx.getSource().sendFeedback(
                                Component.literal("[SilphCrypt] Encryption key cleared. Messages will be sent as plain text.")
                                    .withStyle(s -> s.withColor(COLOR_OFF))
                            );
                            return 1;
                        })
                    )
                    // /silphcrypt status
                    .then(literal("status")
                        .executes(ctx -> {
                            String msg = KeyManager.hasKey()
                                ? "[SilphCrypt] Encryption is ACTIVE."
                                : "[SilphCrypt] No key set — encryption is OFF.";
                            ctx.getSource().sendFeedback(
                                Component.literal(msg)
                                    .withStyle(s -> s.withColor(KeyManager.hasKey() ? COLOR_ON : COLOR_OFF))
                            );
                            return 1;
                        })
                    )
            )
        );
    }
}
