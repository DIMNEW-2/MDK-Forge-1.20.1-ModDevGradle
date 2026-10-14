package com.closky.rpgmod.magic;

import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.player.PlayerClass;
import com.closky.rpgmod.registry.ModAttributes;
import com.closky.rpgmod.registry.ModSounds;
import com.closky.rpgmod.skill.SkillEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side spell casting pipeline: checks (knowledge, level, cooldown, mana), power computation, execution,
 * cost application and sync. Used by staffs, scrolls and the quick-cast packet.
 */
public final class SpellCaster {
    private SpellCaster() {
    }

    public static boolean knows(PlayerRpgData data, Spell spell) {
        ResourceLocation id = spell.getId();
        return spell.isBasic() || (id != null && data.knowsSpell(id));
    }

    /** Spell power multiplier: 1 + magic power attribute + skill bonuses (+ mage set handled via attribute). */
    public static float getPower(Player player, PlayerRpgData data, Spell spell) {
        float power = 1.0F;
        AttributeInstance magic = player.getAttribute(ModAttributes.MAGIC_POWER.get());
        if (magic != null) {
            power += (float) magic.getValue();
        }
        power += data.sumSkillValue(SkillEffect.Type.SPELL_POWER);
        power += 0.05F * Math.max(0, data.getLevel() - 1) / 4.0F; // +1.25% per level
        return power;
    }

    public static float getManaCost(PlayerRpgData data, Spell spell) {
        float reduction = Math.min(0.75F, data.sumSkillValue(SkillEffect.Type.MANA_COST));
        if (data.getActiveClass() == PlayerClass.MAGE) {
            reduction += 0.10F;
        }
        return spell.getManaCost() * Math.max(0.1F, 1.0F - reduction);
    }

    public static int getCooldown(PlayerRpgData data, Spell spell) {
        float reduction = Math.min(0.6F, data.sumSkillValue(SkillEffect.Type.COOLDOWN));
        return Math.max(5, Math.round(spell.getCooldown() * (1.0F - reduction)));
    }

    /**
     * @param free when true (scroll usage) no mana / cooldown / knowledge is required
     */
    public static boolean tryCast(ServerPlayer player, Spell spell, ItemStack source, boolean free) {
        if (!RpgConfig.COMMON.magicEnabled.get()) {
            player.displayClientMessage(Component.translatable("message.rpgmod.magic_disabled").withStyle(ChatFormatting.RED), true);
            return false;
        }
        PlayerRpgData data = PlayerRpgData.get(player);
        if (data == null) {
            return false;
        }
        ResourceLocation id = spell.getId();
        if (!free) {
            if (!knows(data, spell)) {
                player.displayClientMessage(Component.translatable("message.rpgmod.spell_unknown", spell.getDisplayName()).withStyle(ChatFormatting.RED), true);
                fail(player);
                return false;
            }
            if (data.getLevel() < spell.getRequiredLevel()) {
                player.displayClientMessage(Component.translatable("message.rpgmod.spell_level", spell.getRequiredLevel()).withStyle(ChatFormatting.RED), true);
                fail(player);
                return false;
            }
            if (id != null && data.getSpellCooldown(id) > 0) {
                player.displayClientMessage(Component.translatable("message.rpgmod.spell_cooldown", data.getSpellCooldown(id) / 20.0F).withStyle(ChatFormatting.RED), true);
                return false;
            }
            float cost = getManaCost(data, spell);
            if (data.getMana() < cost && !player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.rpgmod.not_enough_mana").withStyle(ChatFormatting.RED), true);
                fail(player);
                return false;
            }
        }
        float power = getPower(player, data, spell);
        boolean cast = spell.cast((ServerLevel) player.level(), player, source, power);
        if (!cast) {
            return false;
        }
        if (!free) {
            if (!player.getAbilities().instabuild) {
                data.consumeMana(getManaCost(data, spell));
            }
            if (id != null) {
                data.setSpellCooldown(id, getCooldown(data, spell));
            }
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                spell.getSound() != null ? spell.getSound() : ModSounds.SPELL_CAST.get(), SoundSource.PLAYERS, 0.9F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(source.getItem()));
        data.sync(player);
        return true;
    }

    private static void fail(ServerPlayer player) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SPELL_FAIL.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
    }
}
