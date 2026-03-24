package org.example2.playerrng.mixin.client;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.example2.playerrng.client.PlayerRngDiffTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDrownParticlesMixin {
    @Inject(
            method = "baseTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;setAir(I)V",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private void playerrng$trackDrownParticles(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity && !self.getWorld().isClient()) {
            PlayerRngDiffTracker.recordPredictedSteps("drown_bubbles", 96);
        }
    }
}
