package org.example2.playerrng.mixin.client;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UseAction;
import org.example2.playerrng.client.PlayerRngDiffTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityConsumptionMixin {
    @Inject(method = "spawnConsumptionEffects", at = @At("HEAD"))
    private void playerrng$trackConsumptionEffects(ItemStack stack, int particleCount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity) || self.getWorld().isClient()) {
            return;
        }
        if (particleCount <= 0 || stack.isEmpty() || stack.getUseAction() != UseAction.EAT) {
            return;
        }

        int steps = particleCount * 3 + 3;
        PlayerRngDiffTracker.recordPredictedSteps("consume_eat", steps);
    }

    @Inject(method = "applyFoodEffects", at = @At("HEAD"))
    private void playerrng$trackFoodEffects(FoodComponent foodComponent, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity) || self.getWorld().isClient()) {
            return;
        }

        int steps = foodComponent.effects().size();
        if (steps <= 0) {
            return;
        }

        PlayerRngDiffTracker.recordPredictedSteps("consume_food_effects", steps);
    }
}
