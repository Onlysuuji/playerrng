package org.example2.playerrng.mixin.client;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.example2.playerrng.client.PlayerRngDiffTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEquipMixin {
    @Inject(
            method = "onEquipStack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextLong()J")
    )
    private void playerrng$trackEquipStack(EquipmentSlot slot, ItemStack previousStack, ItemStack newStack, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayerEntity && !self.getWorld().isClient()) {
            PlayerRngDiffTracker.recordPredictedSteps("equip_item", 2);
        }
    }
}
