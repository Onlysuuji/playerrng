package org.example2.playerrng.mixin.client;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.server.network.ServerPlayerEntity;
import org.example2.playerrng.client.PlayerRngDiffTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {
    @Inject(
            method = "shoot(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/projectile/ProjectileEntity;IFFFLnet/minecraft/entity/LivingEntity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/CrossbowItem;getSoundPitch(Lnet/minecraft/util/math/random/Random;I)F"
            )
    )
    private void playerrng$trackCrossbowUse(
            LivingEntity shooter,
            ProjectileEntity projectile,
            int projectileIndex,
            float speed,
            float divergence,
            float simulated,
            LivingEntity target,
            CallbackInfo ci
    ) {
        if (projectileIndex == 0) {
            return;
        }
        if (shooter instanceof ServerPlayerEntity serverPlayer && !serverPlayer.getWorld().isClient()) {
            PlayerRngDiffTracker.recordPredictedSteps("crossbow_use", 1);
        }
    }
}
