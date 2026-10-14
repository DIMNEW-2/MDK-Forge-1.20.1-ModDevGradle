package com.closky.rpgmod.loot;

import com.closky.rpgmod.armor.RpgArmorItem;
import com.closky.rpgmod.weapon.IRpgWeapon;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Random;

/**
 * Finalises generated loot: assigns an item level around the zone level and rolls bonuses for RPG items that the
 * loot table did not already configure (via {@code rpgmod:set_rpg_level}).
 */
public final class LootItemHelper {
    private static final Item[] FILLERS = {Items.IRON_INGOT, Items.GOLD_INGOT, Items.EMERALD, Items.BREAD, Items.ARROW,
            Items.LAPIS_LAZULI, Items.REDSTONE, Items.BONE, Items.STRING, Items.COAL, Items.GOLDEN_APPLE, Items.DIAMOND};

    private LootItemHelper() {
    }

    public static void finalizeLoot(ItemStack stack, int zoneLevel, RandomSource random) {
        if (!RpgItemData.isRpgItem(stack)) {
            return;
        }
        if (!RpgItemData.hasExplicitLevel(stack)) {
            int base = stack.getItem() instanceof IRpgWeapon weapon ? weapon.getTier().getBaseItemLevel()
                    : stack.getItem() instanceof RpgArmorItem armor ? armor.getBaseItemLevel() : 1;
            int level = Math.max(base, zoneLevel + random.nextInt(5) - 2);
            RpgItemData.setItemLevel(stack, level);
        }
        if (RpgItemData.getBonuses(stack).isEmpty()) {
            int level = RpgItemData.getItemLevel(stack);
            int count = bonusCount(level, random);
            if (count > 0) {
                RpgItemData.rollBonuses(stack, new Random(random.nextLong()), count);
            }
        }
    }

    /** Number of random bonuses for an item of the given level (higher level = more and better bonuses). */
    public static int bonusCount(int level, RandomSource random) {
        float roll = random.nextFloat();
        if (level >= 15) {
            return roll < 0.5F ? 3 : 2;
        }
        if (level >= 8) {
            return roll < 0.3F ? 2 : 1;
        }
        if (level >= 4) {
            return roll < 0.5F ? 1 : 0;
        }
        return roll < 0.2F ? 1 : 0;
    }

    public static Item randomFiller(RandomSource random) {
        return FILLERS[random.nextInt(FILLERS.length)];
    }
}
