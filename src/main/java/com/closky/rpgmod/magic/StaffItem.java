package com.closky.rpgmod.magic;

import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.loot.RpgItemData;
import com.closky.rpgmod.loot.WeaponClass;
import com.closky.rpgmod.registry.ModAttributes;
import com.closky.rpgmod.registry.ModSpells;
import com.closky.rpgmod.weapon.IRpgWeapon;
import com.closky.rpgmod.weapon.WeaponAttributes;
import com.closky.rpgmod.weapon.WeaponTier;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Magic staff (section 6.2). Right click casts the selected spell; sneak + right click cycles through the known
 * spells of the staff's school. The selection is stored in the stack NBT so every staff remembers its own spell.
 */
public class StaffItem extends Item implements IRpgWeapon {
    public static final String NBT_SPELL = "rpgmod:spell";

    private final SpellSchool school;
    private final WeaponTier tier;
    private final float magicPowerBonus;
    @Nullable
    private Multimap<Attribute, AttributeModifier> defaultModifiers;

    public StaffItem(SpellSchool school, WeaponTier tier, float magicPowerBonus, Properties properties) {
        super(properties);
        this.school = school;
        this.tier = tier;
        this.magicPowerBonus = magicPowerBonus;
    }

    public SpellSchool getSchool() {
        return school;
    }

    @Override
    public WeaponClass getWeaponClass() {
        return WeaponClass.STAFF;
    }

    @Override
    public WeaponTier getTier() {
        return tier;
    }

    public float getMagicPowerBonus() {
        return magicPowerBonus;
    }

    // ------------------------------------------------------------------ spell selection

    @Nullable
    public static Spell getSelectedSpell(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_SPELL)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(NBT_SPELL));
            if (id != null) {
                Spell spell = ModSpells.REGISTRY.get().getValue(id);
                if (spell != null) {
                    return spell;
                }
            }
        }
        return null;
    }

    public static void setSelectedSpell(ItemStack stack, @Nullable Spell spell) {
        if (spell == null || spell.getId() == null) {
            if (stack.getTag() != null) {
                stack.getTag().remove(NBT_SPELL);
            }
        } else {
            stack.getOrCreateTag().putString(NBT_SPELL, spell.getId().toString());
        }
    }

    /** Spells of this staff's school available to the player (basic + learned), sorted by required level. */
    public List<Spell> getAvailableSpells(Player player) {
        PlayerRpgData data = PlayerRpgData.get(player);
        List<Spell> list = new ArrayList<>();
        for (Spell spell : ModSpells.REGISTRY.get().getValues()) {
            if (spell.getSchool() != school) {
                continue;
            }
            if (spell.isBasic() || (data != null && SpellCaster.knows(data, spell))) {
                list.add(spell);
            }
        }
        list.sort((a, b) -> Integer.compare(a.getRequiredLevel(), b.getRequiredLevel()));
        return list;
    }

    public Spell getOrSelectSpell(ItemStack stack, Player player) {
        Spell selected = getSelectedSpell(stack);
        List<Spell> available = getAvailableSpells(player);
        if (selected == null || selected.getSchool() != school || !available.contains(selected)) {
            selected = available.isEmpty() ? null : available.get(0);
            setSelectedSpell(stack, selected);
        }
        return selected;
    }

    public void cycleSpell(ItemStack stack, Player player) {
        List<Spell> available = getAvailableSpells(player);
        if (available.isEmpty()) {
            return;
        }
        Spell current = getSelectedSpell(stack);
        int index = current == null ? -1 : available.indexOf(current);
        Spell next = available.get((index + 1) % available.size());
        setSelectedSpell(stack, next);
        player.displayClientMessage(Component.translatable("message.rpgmod.spell_selected", next.getDisplayName()), true);
    }

    // ------------------------------------------------------------------ use

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                cycleSpell(stack, player);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        Spell spell = getOrSelectSpell(stack, player);
        if (spell == null) {
            player.displayClientMessage(Component.translatable("message.rpgmod.no_spells").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (SpellCaster.tryCast((ServerPlayer) player, spell, stack, false)) {
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            player.swing(hand, true);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 20;
    }

    // ------------------------------------------------------------------ attributes / tooltip

    private Multimap<Attribute, AttributeModifier> defaultModifiers() {
        if (defaultModifiers == null) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 2.0D, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.6D, AttributeModifier.Operation.ADDITION));
            builder.put(ModAttributes.MAGIC_POWER.get(),
                    new AttributeModifier(WeaponAttributes.STAFF_MAGIC_UUID, "Staff magic power", magicPowerBonus, AttributeModifier.Operation.ADDITION));
            defaultModifiers = builder.build();
        }
        return defaultModifiers;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return WeaponAttributes.withRpgBonuses(stack, slot, slot == EquipmentSlot.MAINHAND ? defaultModifiers() : ImmutableMultimap.of(), false);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return WeaponAttributes.scaledMaxDamage(stack, super.getMaxDamage(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        RpgItemData.appendTooltip(stack, tooltip);
        tooltip.add(Component.translatable("tooltip.rpgmod.staff.school", school.getDisplayName()).withStyle(ChatFormatting.GRAY));
        Spell selected = getSelectedSpell(stack);
        if (selected != null) {
            tooltip.add(Component.translatable("tooltip.rpgmod.staff.spell", selected.getDisplayName()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.rpgmod.staff.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
