package com.closky.rpgmod.player;

import com.closky.rpgmod.config.RpgConfig;

/**
 * Character level math (section 7.1). The design uses a flat curve: {@code xpPerLevel} (default 5) character XP
 * per level. Character XP is separate from vanilla experience.
 */
public final class CharacterLevel {
    public static final int MAX_LEVEL = 100;

    private CharacterLevel() {
    }

    /** XP needed to advance from {@code level} to {@code level + 1}. */
    public static int xpToNext(int level) {
        return Math.max(1, RpgConfig.COMMON.xpPerLevel.get());
    }

    /** Character XP granted for killing a mob of the given level. */
    public static int xpForKill(int mobLevel, int playerLevel) {
        int base = 1 + mobLevel / 3;
        if (mobLevel + 5 < playerLevel) {
            base = Math.max(0, base - 1);
        }
        return scale(base);
    }

    public static int xpForChest(int chestTier) {
        return scale(2 + chestTier * 2);
    }

    public static int xpForNewBiome() {
        return scale(3);
    }

    public static int xpForBoss(int bossLevel) {
        return scale(10 + bossLevel * 2);
    }

    private static int scale(int xp) {
        return (int) Math.round(xp * RpgConfig.COMMON.characterXpMultiplier.get());
    }

    /** Stat points awarded when reaching {@code newLevel}. */
    public static int statPointsForLevel(int newLevel) {
        int every = Math.max(1, RpgConfig.COMMON.levelsPerStatPoint.get());
        return newLevel % every == 0 ? 1 : 0;
    }

    public static int skillPointsForLevel(int newLevel) {
        return RpgConfig.COMMON.skillPointPerLevel.get();
    }
}
