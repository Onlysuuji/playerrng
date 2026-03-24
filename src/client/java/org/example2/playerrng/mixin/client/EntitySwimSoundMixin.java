package org.example2.playerrng.mixin.client;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.example2.playerrng.client.PlayerRngDiffTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntitySwimSoundMixin {
    @Inject(method = "playSwimSound(F)V", at = @At("HEAD"))
    private void playerrng$trackSwimSound(float volume, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof ServerPlayerEntity && !self.getWorld().isClient()) {
            PlayerRngDiffTracker.recordPredictedSteps("water_swim_sound", 2);
        }
    }
}
