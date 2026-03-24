package org.example2.playerrng.mixin.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.example2.playerrng.client.PlayerRngDiffTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.screen.AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin {
    @Inject(
            method = "method_24922(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextFloat()F")
    )
    private static void playerrng$trackAnvilUse(PlayerEntity player, World world, BlockPos pos, CallbackInfo ci) {
        if (player instanceof ServerPlayerEntity && !world.isClient()) {
            PlayerRngDiffTracker.recordPredictedSteps("anvil_use", 1);
        }
    }
}
