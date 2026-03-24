package org.example2.playerrng.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import org.example2.playerrng.mixin.CheckedRandomAccessor;
import org.example2.playerrng.mixin.LocalRandomAccessor;
import org.jetbrains.annotations.Nullable;

public final class PlayerRngReader {
    private static final long MASK_48 = (1L << 48) - 1;

    private PlayerRngReader() {}

    public static @Nullable Long readCurrent48() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !client.isInSingleplayer()) {
            return null;
        }

        IntegratedServer server = client.getServer();
        if (server == null) {
            return null;
        }

        ServerPlayerEntity serverPlayer = server.getPlayerManager().getPlayer(client.player.getUuid());
        if (serverPlayer == null) {
            return null;
        }

        Random rng = serverPlayer.getRandom();

        if (rng instanceof LocalRandom local) {
            long seed = ((LocalRandomAccessor) local).playerrng$getSeed();
            return seed & MASK_48;
        }

        if (rng instanceof CheckedRandom checked) {
            long seed = ((CheckedRandomAccessor) checked).playerrng$getSeed().get();
            return seed & MASK_48;
        }

        return null;
    }

    public static String readCurrent48Hex() {
        Long seed = readCurrent48();
        return seed == null ? "N/A" : String.format("0x%012X", seed);
    }
}
