package org.example2.playerrng.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.text.Text;
import org.example2.playerrng.mixin.client.MinecraftClientInvoker;
import org.lwjgl.glfw.GLFW;

public class PlayerrngClient implements ClientModInitializer {
    private static final String RESET_KEY_TRANSLATION = "key.playerrng.reset_tracker";
    private static final String SPRINT_BURST_KEY_TRANSLATION = "key.playerrng.sprint_toggle_burst";
    private static final String RIGHT_CLICK_BURST_KEY_TRANSLATION = "key.playerrng.right_click_burst";
    private static final String KEY_CATEGORY_TRANSLATION = "category.playerrng.controls";
    private static final int SPRINT_TOGGLE_COUNT = 4;
    private static final int RIGHT_CLICK_BURST_COUNT = 10;
    private static KeyBinding resetKey;
    private static KeyBinding sprintBurstKey;
    private static KeyBinding rightClickBurstKey;

    @Override
    public void onInitializeClient() {
        PlayerRngDiffTracker.init();
        PlayerRngHudOverlay.init();
        PlayerRngFileLogger.log("client initialized");

        resetKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                RESET_KEY_TRANSLATION,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                KEY_CATEGORY_TRANSLATION
        ));

        sprintBurstKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                SPRINT_BURST_KEY_TRANSLATION,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                KEY_CATEGORY_TRANSLATION
        ));

        rightClickBurstKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                RIGHT_CLICK_BURST_KEY_TRANSLATION,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                KEY_CATEGORY_TRANSLATION
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (resetKey.wasPressed()) {
                PlayerRngDiffTracker.reset("manual_reset");
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("[playerrng] tracker reset"), true);
                }
            }

            while (sprintBurstKey.wasPressed()) {
                triggerSprintToggleBurst(client);
            }

            while (rightClickBurstKey.wasPressed()) {
                triggerRightClickBurst(client);
            }
        });
    }

    private static void triggerSprintToggleBurst(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        boolean sprinting = client.player.isSprinting();
        PlayerRngDiffTracker.markCause("sprint_toggle_burst_x4");
        PlayerRngFileLogger.log("action sprint_toggle_burst count=" + SPRINT_TOGGLE_COUNT);

        for (int i = 0; i < SPRINT_TOGGLE_COUNT; i++) {
            sprinting = !sprinting;
            ClientCommandC2SPacket.Mode mode = sprinting
                    ? ClientCommandC2SPacket.Mode.START_SPRINTING
                    : ClientCommandC2SPacket.Mode.STOP_SPRINTING;
            client.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(client.player, mode));
        }

        client.player.sendMessage(Text.literal("[playerrng] sprint toggle burst x4"), true);
    }

    private static void triggerRightClickBurst(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.currentScreen != null) {
            return;
        }

        MinecraftClientInvoker invoker = (MinecraftClientInvoker) client;
        PlayerRngFileLogger.log("action right_click_burst count=" + RIGHT_CLICK_BURST_COUNT);
        for (int i = 0; i < RIGHT_CLICK_BURST_COUNT; i++) {
            invoker.playerrng$invokeDoItemUse();
        }

        client.player.sendMessage(Text.literal("[playerrng] right click burst x10"), true);
    }
}
