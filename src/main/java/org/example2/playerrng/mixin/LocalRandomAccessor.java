package org.example2.playerrng.mixin;

import net.minecraft.util.math.random.LocalRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalRandom.class)
public interface LocalRandomAccessor {
    @Accessor("seed")
    long playerrng$getSeed();
}
