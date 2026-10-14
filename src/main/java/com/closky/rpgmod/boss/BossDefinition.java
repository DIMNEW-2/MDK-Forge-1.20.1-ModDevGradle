package com.closky.rpgmod.boss;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The eight dungeon bosses (section 5.3). Bosses are vanilla mobs with boosted attributes, RPG equipment, a boss bar
 * and a dedicated loot table {@code rpgmod:bosses/<id>}.
 */
public enum BossDefinition {
    SKELETON_KING(() -> EntityType.SKELETON, 5, "catacombs_basic", BossEvent.BossBarColor.WHITE),
    BANDIT_CHIEF(() -> EntityType.VINDICATOR, 6, "bandit_camp", BossEvent.BossBarColor.RED),
    MINE_OVERSEER(() -> EntityType.HUSK, 7, "deep_mines", BossEvent.BossBarColor.YELLOW),
    MANOR_GHOST(() -> EntityType.ILLUSIONER, 8, "abandoned_manor", BossEvent.BossBarColor.BLUE),
    LABYRINTH_MINOTAUR(() -> EntityType.RAVAGER, 9, "labyrinth_basic", BossEvent.BossBarColor.PURPLE),
    FORTRESS_LORD(() -> EntityType.WITHER_SKELETON, 10, "underground_fortress", BossEvent.BossBarColor.PINK),
    FALLEN_KING(() -> EntityType.ZOMBIE, 12, "ruined_castle", BossEvent.BossBarColor.GREEN),
    ARCHMAGE(() -> EntityType.EVOKER, 15, "arcane_tower", BossEvent.BossBarColor.PURPLE);

    private final Supplier<EntityType<? extends Mob>> type;
    private final int level;
    private final String dungeon;
    private final BossEvent.BossBarColor barColor;
    private final String key = name().toLowerCase(Locale.ROOT);

    BossDefinition(Supplier<EntityType<? extends Mob>> type, int level, String dungeon, BossEvent.BossBarColor barColor) {
        this.type = type;
        this.level = level;
        this.dungeon = dungeon;
        this.barColor = barColor;
    }

    public EntityType<? extends Mob> getEntityType() {
        return type.get();
    }

    public int getLevel() {
        return level;
    }

    public String getDungeon() {
        return dungeon;
    }

    public BossEvent.BossBarColor getBarColor() {
        return barColor;
    }

    public String getKey() {
        return key;
    }

    public Component getDisplayName() {
        return Component.translatable("boss.rpgmod." + key);
    }

    public ResourceLocation getLootTable() {
        return RpgMod.id("bosses/" + key);
    }

    /** Equips the boss with class-appropriate RPG gear. */
    public void equip(Mob mob) {
        switch (this) {
            case SKELETON_KING -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.BOW_IRON.get()));
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
            }
            case BANDIT_CHIEF -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SWORD_IRON.get()));
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            }
            case MINE_OVERSEER -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MACE_IRON.get()));
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            }
            case MANOR_GHOST -> mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.STAFF_FROST.get()));
            case LABYRINTH_MINOTAUR -> {
                // ravagers cannot hold items; strength comes from attributes
            }
            case FORTRESS_LORD -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.MACE_DIAMOND.get()));
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            }
            case FALLEN_KING -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SWORD_DIAMOND.get()));
                mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
                mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            }
            case ARCHMAGE -> mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.STAFF_LIGHTNING.get()));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.0F);
        }
    }

    @Nullable
    public static BossDefinition byKey(String key) {
        for (BossDefinition b : values()) {
            if (b.key.equalsIgnoreCase(key)) {
                return b;
            }
        }
        return null;
    }
}
