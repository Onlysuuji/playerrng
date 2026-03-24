package org.example2.playerrng.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PlayerrngClient implements ClientModInitializer {
    private static KeyBinding resetKey;

    @Override
    public void onInitializeClient() {
        PlayerRngDiffTracker.init();
        PlayerRngHudOverlay.init();

        resetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.playerrng.reset_tracker",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                "category.playerrng"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (resetKey.wasPressed()) {
                PlayerRngDiffTracker.reset();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[playerrng] tracker reset"), true);
                }
            }
        });
    }
}