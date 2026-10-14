package com.closky.rpgmod.magic;

import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.registry.ModSounds;
import com.closky.rpgmod.registry.ModSpells;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Spell scroll (section 6.5). Holds a spell id in NBT. Right click: if the player does not know the spell yet it is
 * learned (the scroll is consumed); otherwise the spell is cast once for free and the scroll is consumed.
 * Scrolls without a spell id default to the basic spell of their school.
 */
public class SpellScrollItem extends Item {
    public static final String NBT_SPELL = "rpgmod:spell";

    private final SpellSchool school;

    public SpellScrollItem(SpellSchool school, Properties properties) {
        super(properties);
        this.school = school;
    }

    public SpellSchool getSchool() {
        return school;
    }

    public static ItemStack withSpell(ItemStack stack, Spell spell) {
        if (spell.getId() != null) {
            stack.getOrCreateTag().putString(NBT_SPELL, spell.getId().toString());
        }
        return stack;
    }

    @Nullable
    public Spell getSpell(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_SPELL)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(NBT_SPELL));
            Spell spell = id == null ? null : ModSpells.REGISTRY.get().getValue(id);
            if (spell != null) {
                return spell;
            }
        }
        return ModSpells.basicSpell(school);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        Spell spell = getSpell(stack);
        PlayerRpgData data = PlayerRpgData.get(player);
        if (spell == null || data == null || spell.getId() == null) {
            return InteractionResultHolder.fail(stack);
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        if (!spell.isBasic() && !data.knowsSpell(spell.getId())) {
            if (data.getLevel() < spell.getRequiredLevel()) {
                player.displayClientMessage(Component.translatable("message.rpgmod.spell_level", spell.getRequiredLevel()).withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            data.learnSpell(spell.getId());
            data.sync(serverPlayer);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SPELL_LEARN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.rpgmod.spell_learned", spell.getDisplayName()).withStyle(ChatFormatting.GREEN), false);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.consume(stack);
        }
        if (SpellCaster.tryCast(serverPlayer, spell, stack, true)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Spell spell = getSpell(stack);
        if (spell != null) {
            return Component.translatable(this.getDescriptionId(stack) + ".named", spell.getDisplayName());
        }
        return super.getName(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Spell spell = getSpell(stack);
        if (spell != null) {
            spell.appendTooltip(tooltip);
        }
        tooltip.add(Component.translatable("tooltip.rpgmod.scroll.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
