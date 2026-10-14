package com.closky.rpgmod.magic;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/** Applies a set of status effects to the caster (buffs). */
public class SelfSpell extends Spell {
    public record EffectEntry(Supplier<MobEffect> effect, int duration, int amplifier) {
    }

    private final List<EffectEntry> effects;

    public SelfSpell(SpellSchool school, Properties properties, List<EffectEntry> effects) {
        super(school, properties);
        this.effects = effects;
    }

    @Override
    public boolean cast(ServerLevel level, Player caster, ItemStack staff, float power) {
        int durationBonus = (int) ((power - 1.0F) * 100.0F);
        for (EffectEntry entry : effects) {
            caster.addEffect(new MobEffectInstance(entry.effect().get(), entry.duration() + Math.max(0, durationBonus), entry.amplifier()));
        }
        level.sendParticles(ParticleTypes.ENCHANT, caster.getX(), caster.getY() + 1.0D, caster.getZ(), 30, 0.5D, 0.8D, 0.5D, 0.5D);
        return true;
    }
}
