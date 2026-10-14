package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Custom player attributes. Stats, skill nodes, forged materials and class sets all funnel into these so that
 * every bonus source stacks predictably through the vanilla attribute system.
 */
public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, RpgMod.MODID);

    public static final RegistryObject<Attribute> MAX_MANA = ATTRIBUTES.register("max_mana",
            () -> new RangedAttribute("attribute.rpgmod.max_mana", 20.0D, 0.0D, 10000.0D).setSyncable(true));
    public static final RegistryObject<Attribute> MANA_REGEN = ATTRIBUTES.register("mana_regen",
            () -> new RangedAttribute("attribute.rpgmod.mana_regen", 1.0D, 0.0D, 1000.0D).setSyncable(true));
    public static final RegistryObject<Attribute> CRIT_CHANCE = ATTRIBUTES.register("crit_chance",
            () -> new RangedAttribute("attribute.rpgmod.crit_chance", 0.05D, 0.0D, 1.0D).setSyncable(true));
    public static final RegistryObject<Attribute> ARMOR_PENETRATION = ATTRIBUTES.register("armor_penetration",
            () -> new RangedAttribute("attribute.rpgmod.armor_penetration", 0.0D, 0.0D, 1.0D).setSyncable(true));
    public static final RegistryObject<Attribute> MAGIC_POWER = ATTRIBUTES.register("magic_power",
            () -> new RangedAttribute("attribute.rpgmod.magic_power", 0.0D, -1.0D, 100.0D).setSyncable(true));
    public static final RegistryObject<Attribute> RANGED_DAMAGE = ATTRIBUTES.register("ranged_damage",
            () -> new RangedAttribute("attribute.rpgmod.ranged_damage", 0.0D, -1.0D, 100.0D).setSyncable(true));
    public static final RegistryObject<Attribute> HEALING_POWER = ATTRIBUTES.register("healing_power",
            () -> new RangedAttribute("attribute.rpgmod.healing_power", 0.0D, -1.0D, 100.0D).setSyncable(true));

    private ModAttributes() {
    }

    public static void addPlayerAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MAX_MANA.get());
        event.add(EntityType.PLAYER, MANA_REGEN.get());
        event.add(EntityType.PLAYER, CRIT_CHANCE.get());
        event.add(EntityType.PLAYER, ARMOR_PENETRATION.get());
        event.add(EntityType.PLAYER, MAGIC_POWER.get());
        event.add(EntityType.PLAYER, RANGED_DAMAGE.get());
        event.add(EntityType.PLAYER, HEALING_POWER.get());
    }
}
