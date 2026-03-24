package org.example2.playerrng.mixin.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.example2.playerrng.client.PlayerRngDiffTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityEnchantmentCostsMixin {
    @Inject(method = "applyEnchantmentCosts", at = @At("HEAD"))
    private void playerrng$trackEnchantmentCosts(ItemStack stack, int experienceLevels, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self instanceof ServerPlayerEntity && !self.getWorld().isClient()) {
            PlayerRngDiffTracker.recordPredictedSteps("enchant_item", 1);
        }
    }

    @Inject(method = "applyEnchantmentCosts", at = @At("TAIL"))
    private void playerrng$observeEnchantmentSeed(ItemStack stack, int experienceLevels, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self instanceof ServerPlayerEntity && !self.getWorld().isClient()) {
            PlayerRngDiffTracker.recordUnboundedNextIntObservation("enchantment_xp_seed", self.getEnchantmentTableSeed());
        }
    }
}
