package org.example2.playerrng.mixin.client;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.example2.playerrng.client.PlayerRngDiffTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntitySwimmingStartMixin {
    @Inject(method = "onSwimmingStart", at = @At("HEAD"))
    private void playerrng$trackSwimmingStart(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof ServerPlayerEntity) || self.getWorld().isClient()) {
            return;
        }

        int particleBursts = MathHelper.ceil(1.0F + self.getWidth() * 20.0F);
        int steps = 2 + particleBursts * 10;
        PlayerRngDiffTracker.recordPredictedSteps("water_enter_swimming_start", steps);
    }
}
