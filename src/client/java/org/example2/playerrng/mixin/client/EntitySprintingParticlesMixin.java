package org.example2.playerrng.mixin.client;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.example2.playerrng.client.PlayerRngDiffTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntitySprintingParticlesMixin {
    @Inject(
            method = "spawnSprintingParticles",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/random/Random;nextDouble()D",
                    ordinal = 0
            )
    )
    private void playerrng$trackSprintingParticles(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ServerPlayerEntity && !self.getWorld().isClient()) {
            PlayerRngDiffTracker.recordPredictedSteps("run_sprinting_particles", 4);
        }
    }
}
