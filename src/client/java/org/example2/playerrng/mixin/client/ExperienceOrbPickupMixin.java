package org.example2.playerrng.mixin.client;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.component.ComponentType;
import net.minecraft.enchantment.EnchantmentEffectContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.example2.playerrng.client.PlayerRngDiffTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbPickupMixin {
    @Redirect(
            method = "repairPlayerGears",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/enchantment/EnchantmentHelper;chooseEquipmentWith(Lnet/minecraft/component/ComponentType;Lnet/minecraft/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Optional;"
            )
    )
    private Optional<EnchantmentEffectContext> playerrng$trackRepairSelection(
            ComponentType<?> componentType,
            LivingEntity entity,
            Predicate<ItemStack> predicate,
            ServerPlayerEntity player,
            int amount
    ) {
        Optional<EnchantmentEffectContext> result = EnchantmentHelper.chooseEquipmentWith(componentType, entity, predicate);
        if (result.isPresent() && !player.getWorld().isClient()) {
            PlayerRngDiffTracker.recordPredictedSteps("xp_orb_repair_selection", 1);
        }
        return result;
    }
}
