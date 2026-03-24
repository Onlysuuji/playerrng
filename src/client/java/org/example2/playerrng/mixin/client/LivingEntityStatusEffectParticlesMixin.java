package org.example2.playerrng.mixin.client;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.example2.playerrng.client.PlayerRngDiffTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityStatusEffectParticlesMixin {
    @Inject(
            method = "tickStatusEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I",
                    ordinal = 0
            )
    )
    private void playerrng$trackStatusEffectTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity && !self.getWorld().isClient()) {
            PlayerRngDiffTracker.recordPredictedSteps("status_effect_tick", 1);
        }
    }

    @Inject(
            method = "tickStatusEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Util;getRandom(Ljava/util/List;Lnet/minecraft/util/math/random/Random;)Ljava/lang/Object;"
            )
    )
    private void playerrng$trackStatusEffectParticleSpawn(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity && !self.getWorld().isClient()) {
            PlayerRngDiffTracker.recordPredictedSteps("status_effect_particle_spawn", 7);
        }
    }
}
