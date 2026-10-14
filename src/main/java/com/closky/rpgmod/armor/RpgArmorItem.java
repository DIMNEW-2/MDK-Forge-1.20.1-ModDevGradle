package com.closky.rpgmod.armor;

import com.closky.rpgmod.loot.RpgItemData;
import com.closky.rpgmod.player.PlayerClass;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Class armor piece (section 7.4). Four pieces of the same class grant the class and its set bonus.
 */
public class RpgArmorItem extends ArmorItem {
    private final RpgArmorMaterial rpgMaterial;

    public RpgArmorItem(RpgArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
        this.rpgMaterial = material;
    }

    public PlayerClass getPlayerClass() {
        return rpgMaterial.getPlayerClass();
    }

    public int getBaseItemLevel() {
        return 8;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> base = super.getAttributeModifiers(slot, stack);
        if (slot != this.getEquipmentSlot()) {
            return base;
        }
        Multimap<Attribute, AttributeModifier> map = LinkedHashMultimap.create(base);
        RpgItemData.applyBonusAttributes(stack, slot, map);
        return map;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        RpgItemData.appendTooltip(stack, tooltip);
        tooltip.add(Component.translatable("tooltip.rpgmod.armor.set", getPlayerClass().getDisplayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  ").append(getPlayerClass().getBonusDescription().copy().withStyle(ChatFormatting.DARK_AQUA)));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
