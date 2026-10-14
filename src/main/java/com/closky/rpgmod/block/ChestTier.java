package com.closky.rpgmod.block;

import com.closky.rpgmod.RpgMod;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Loot chest tiers (section 13). Determines the default loot table, roll count and casino visuals. */
public enum ChestTier implements StringRepresentable {
    COMMON(1, 2, 0.02F, ChatFormatting.WHITE),
    RARE(2, 3, 0.05F, ChatFormatting.AQUA),
    BOSS(3, 4, 0.12F, ChatFormatting.GOLD);

    private final int level;
    private final int rolls;
    private final float jackpotChance;
    private final ChatFormatting color;
    private final String key = name().toLowerCase(Locale.ROOT);

    ChestTier(int level, int rolls, float jackpotChance, ChatFormatting color) {
        this.level = level;
        this.rolls = rolls;
        this.jackpotChance = jackpotChance;
        this.color = color;
    }

    public int getLevel() {
        return level;
    }

    public int getRolls() {
        return rolls;
    }

    public float getJackpotChance() {
        return jackpotChance;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public ResourceLocation getDefaultLootTable() {
        return RpgMod.id("chests/default_" + key);
    }

    @Override
    public String getSerializedName() {
        return key;
    }

    public static ChestTier byOrdinal(int ordinal) {
        ChestTier[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
