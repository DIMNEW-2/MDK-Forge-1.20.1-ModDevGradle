package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Status effects. Note: mob effects are registered before custom attributes, so none of these may reference
 * {@link ModAttributes} in their constructors - effects that touch custom attributes are handled in code instead.
 */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RpgMod.MODID);

    /** Mace stun: cannot move or attack (attacks are cancelled in CombatEvents). */
    public static final RegistryObject<MobEffect> STUN = EFFECTS.register("stun",
            () -> new RpgMobEffect(MobEffectCategory.HARMFUL, 0xFFD700)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, "9a6e2a6b-2a1c-4d7d-9d23-5d9d8d5f1a01", -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_SPEED, "9a6e2a6b-2a1c-4d7d-9d23-5d9d8d5f1a02", -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));

    /** Spear root: cannot move but can still attack. */
    public static final RegistryObject<MobEffect> ROOT = EFFECTS.register("root",
            () -> new RpgMobEffect(MobEffectCategory.HARMFUL, 0x4F8A2F)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, "9a6e2a6b-2a1c-4d7d-9d23-5d9d8d5f1a03", -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));

    /** Nature magic / mana potions: +2 mana per second (applied by the mana regeneration code). */
    public static final RegistryObject<MobEffect> MANA_SURGE = EFFECTS.register("mana_surge",
            () -> new RpgMobEffect(MobEffectCategory.BENEFICIAL, 0x3F76E4));

    /** Bark skin (nature school): +6 armor. */
    public static final RegistryObject<MobEffect> BARK_SKIN = EFFECTS.register("bark_skin",
            () -> new RpgMobEffect(MobEffectCategory.BENEFICIAL, 0x6B4F2A)
                    .addAttributeModifier(Attributes.ARMOR, "9a6e2a6b-2a1c-4d7d-9d23-5d9d8d5f1a05", 6.0D, AttributeModifier.Operation.ADDITION));

    private ModEffects() {
    }

    /** {@link MobEffect} has a protected constructor; this subclass only exposes it. */
    public static class RpgMobEffect extends MobEffect {
        public RpgMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) {
            return false;
        }

        @Override
        public void applyEffectTick(LivingEntity entity, int amplifier) {
        }
    }
}
