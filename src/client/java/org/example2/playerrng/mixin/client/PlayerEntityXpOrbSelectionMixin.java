package org.example2.playerrng.mixin.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.example2.playerrng.client.PlayerRngDiffTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityXpOrbSelectionMixin {
    @Inject(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Util;getRandom(Ljava/util/List;Lnet/minecraft/util/math/random/Random;)Ljava/lang/Object;"
            )
    )
    private void playerrng$trackXpOrbSelection(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self instanceof ServerPlayerEntity && !self.getWorld().isClient()) {
            PlayerRngDiffTracker.recordPredictedSteps("xp_orb_selection", 1);
        }
    }
}
