package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, RpgMod.MODID);

    /** Blue spark used by mana regeneration, lapis weapons and lightning spells. */
    public static final RegistryObject<SimpleParticleType> MANA_SPARK = PARTICLES.register("mana_spark", () -> new SimpleParticleType(false));
    /** Golden sparkle used by the casino and legendary drops. */
    public static final RegistryObject<SimpleParticleType> CASINO_SPARKLE = PARTICLES.register("casino_sparkle", () -> new SimpleParticleType(false));
    /** Green leaf used by nature spells. */
    public static final RegistryObject<SimpleParticleType> NATURE_LEAF = PARTICLES.register("nature_leaf", () -> new SimpleParticleType(false));

    private ModParticles() {
    }
}
