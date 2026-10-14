package com.closky.rpgmod.settlement;

import java.util.Random;

/**
 * Relation / war arithmetic (sections 17.3–17.6). Deliberately free of Minecraft types so the rules can be unit
 * tested in isolation.
 */
public final class RelationMath {
    public static final int MIN = -100;
    public static final int MAX = 100;
    public static final int WAR_THRESHOLD = -71;
    public static final int PEACE_THRESHOLD = -40;

    private RelationMath() {
    }

    public static int clamp(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }

    /** Moves a relation value towards zero by {@code rate} (never crossing zero). */
    public static int decay(int value, int rate) {
        if (rate <= 0 || value == 0) {
            return value;
        }
        if (value > 0) {
            return Math.max(0, value - rate);
        }
        return Math.min(0, value + rate);
    }

    /** Whether a war should be declared given the relation, the aggressor's strength and a random roll. */
    public static boolean shouldDeclareWar(int relation, int aggressorMilitary, double frequency, double roll) {
        return relation <= WAR_THRESHOLD && aggressorMilitary >= 30 && roll < frequency;
    }

    /** Whether a running war should end in peace. */
    public static boolean shouldMakePeace(int relation, int attackerMilitary, int defenderMilitary, int roundsFought) {
        if (relation > PEACE_THRESHOLD) {
            return true;
        }
        if (attackerMilitary <= 5 || defenderMilitary <= 5) {
            return true;
        }
        return roundsFought >= 12;
    }

    /**
     * Resolves one abstract battle round. Returns {attackerLoss, defenderLoss} as positive numbers.
     * Strength is compared with randomness; the stronger side loses less.
     */
    public static int[] resolveBattle(int attackerMilitary, int defenderMilitary, Random random) {
        double total = Math.max(1, attackerMilitary + defenderMilitary);
        double attackerShare = attackerMilitary / total;
        double luck = (random.nextDouble() - 0.5D) * 0.3D;
        double attackerWinChance = Math.max(0.05D, Math.min(0.95D, attackerShare + luck));
        boolean attackerWins = random.nextDouble() < attackerWinChance;
        int winnerLoss = 2 + random.nextInt(4);
        int loserLoss = 5 + random.nextInt(8);
        return attackerWins ? new int[]{winnerLoss, loserLoss} : new int[]{loserLoss, winnerLoss};
    }

    /** Relation change applied to both sides when an alliance exists and one of them is attacked. */
    public static int allyOutrage() {
        return -15;
    }

    /** Converts a player's reputation to a trade price multiplier (0.8 .. 1.3). */
    public static float priceMultiplier(int reputation) {
        int r = clamp(reputation);
        if (r >= 0) {
            return 1.0F - 0.2F * (r / 100.0F);
        }
        return 1.0F + 0.3F * (-r / 100.0F);
    }
}
