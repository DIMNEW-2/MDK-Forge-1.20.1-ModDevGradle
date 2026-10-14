package com.closky.rpgmod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Server/common and client configuration (section 10 of the design document).
 *
 * <p>Values are read through the static accessors so the rest of the code never touches the spec directly.</p>
 */
public final class RpgConfig {
    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        Pair<Common, ForgeConfigSpec> common = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = common.getLeft();
        COMMON_SPEC = common.getRight();
        Pair<Client, ForgeConfigSpec> client = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();
    }

    private RpgConfig() {
    }

    /** Looks up a boolean option of the common config by its key (used by {@code rpgmod:config} recipe conditions). */
    public static boolean getBoolean(String key) {
        if (!COMMON_SPEC.isLoaded()) {
            return true;
        }
        try {
            for (java.lang.reflect.Field field : Common.class.getFields()) {
                if (field.getName().equals(key) && ForgeConfigSpec.BooleanValue.class.isAssignableFrom(field.getType())) {
                    return ((ForgeConfigSpec.BooleanValue) field.get(COMMON)).get();
                }
            }
        } catch (IllegalAccessException ignored) {
        }
        return true;
    }

    public static final class Common {
        // --- progression
        public final ForgeConfigSpec.DoubleValue characterXpMultiplier;
        public final ForgeConfigSpec.IntValue xpPerLevel;
        public final ForgeConfigSpec.IntValue levelsPerStatPoint;
        public final ForgeConfigSpec.IntValue skillPointPerLevel;
        public final ForgeConfigSpec.BooleanValue enableSkillTree;
        public final ForgeConfigSpec.BooleanValue allowRespec;
        public final ForgeConfigSpec.BooleanValue giveStarterKit;
        // --- enemies
        public final ForgeConfigSpec.DoubleValue mobLevelScaling;
        public final ForgeConfigSpec.IntValue blocksPerMobLevel;
        public final ForgeConfigSpec.IntValue maxMobLevel;
        public final ForgeConfigSpec.DoubleValue damageReductionCap;
        // --- loot
        public final ForgeConfigSpec.BooleanValue lootOnlyFromChests;
        public final ForgeConfigSpec.BooleanValue enableCasinoAnimation;
        public final ForgeConfigSpec.IntValue casinoMinClaimDelayTicks;
        // --- magic
        public final ForgeConfigSpec.BooleanValue magicEnabled;
        public final ForgeConfigSpec.DoubleValue baseManaRegenPerSecond;
        // --- villages
        public final ForgeConfigSpec.BooleanValue enableSettlementSimulation;
        public final ForgeConfigSpec.IntValue settlementTickInterval;
        public final ForgeConfigSpec.BooleanValue enableVillageWars;
        public final ForgeConfigSpec.DoubleValue villageWarFrequency;
        public final ForgeConfigSpec.DoubleValue relationDecayRate;
        // --- world
        public final ForgeConfigSpec.IntValue worldGenMaxHeight;
        // --- weapons
        public final ForgeConfigSpec.BooleanValue bombsDestroyBlocks;
        public final ForgeConfigSpec.BooleanValue spearReturnEnabled;
        public final ForgeConfigSpec.BooleanValue spearChargeEnabled;
        public final ForgeConfigSpec.BooleanValue maceStunEnabled;
        public final ForgeConfigSpec.BooleanValue maceAoeEnabled;
        public final ForgeConfigSpec.BooleanValue swordDashEnabled;
        public final ForgeConfigSpec.BooleanValue bowTripleShotEnabled;
        public final ForgeConfigSpec.BooleanValue crossbowMultiShotEnabled;
        public final ForgeConfigSpec.BooleanValue customArrowsEnabled;
        public final ForgeConfigSpec.BooleanValue boltsEnabled;

        Common(ForgeConfigSpec.Builder b) {
            b.comment("Character progression").push("progression");
            characterXpMultiplier = b.comment("Multiplier applied to all character XP gains").defineInRange("characterXpMultiplier", 1.0D, 0.0D, 100.0D);
            xpPerLevel = b.comment("Character XP required for one character level (design: 5)").defineInRange("xpPerLevel", 5, 1, 1000);
            levelsPerStatPoint = b.comment("A stat point is awarded every N character levels").defineInRange("levelsPerStatPoint", 5, 1, 100);
            skillPointPerLevel = b.comment("Skill points awarded per character level").defineInRange("skillPointPerLevel", 1, 0, 10);
            enableSkillTree = b.define("enableSkillTree", true);
            allowRespec = b.comment("Allow resetting skill nodes (costs emeralds)").define("allowRespec", true);
            giveStarterKit = b.comment("Give a wooden RPG starter kit on first join (weapons/tools cannot be crafted)").define("giveStarterKit", true);
            b.pop();

            b.comment("Enemies").push("enemies");
            mobLevelScaling = b.comment("Global multiplier for mob levels").defineInRange("mobLevelScaling", 1.0D, 0.0D, 10.0D);
            blocksPerMobLevel = b.comment("Distance from world spawn (in blocks) per additional mob level").defineInRange("blocksPerMobLevel", 400, 16, 100000);
            maxMobLevel = b.defineInRange("maxMobLevel", 60, 1, 1000);
            damageReductionCap = b.comment("Maximum damage reduction applied when the enemy level is higher than the player's").defineInRange("damageReductionCap", 0.8D, 0.0D, 1.0D);
            b.pop();

            b.comment("Loot").push("loot");
            lootOnlyFromChests = b.comment("Remove vanilla weapon/tool recipes (loot only)").define("lootOnlyFromChests", true);
            enableCasinoAnimation = b.comment("Play the slot-machine animation when opening loot chests").define("enableCasinoAnimation", true);
            casinoMinClaimDelayTicks = b.comment("Server-side minimum delay between rolling and claiming a loot chest").defineInRange("casinoMinClaimDelayTicks", 40, 0, 1200);
            b.pop();

            b.comment("Magic").push("magic");
            magicEnabled = b.define("magicEnabled", true);
            baseManaRegenPerSecond = b.defineInRange("baseManaRegenPerSecond", 1.0D, 0.0D, 100.0D);
            b.pop();

            b.comment("Villages & politics").push("villages");
            enableSettlementSimulation = b.define("enableSettlementSimulation", true);
            settlementTickInterval = b.comment("Ticks between abstract settlement simulation steps").defineInRange("settlementTickInterval", 1200, 20, 72000);
            enableVillageWars = b.define("enableVillageWars", true);
            villageWarFrequency = b.comment("Chance per simulation step that a HOSTILE relation degrades into WAR").defineInRange("villageWarFrequency", 0.05D, 0.0D, 1.0D);
            relationDecayRate = b.comment("How fast relations drift back towards neutral per simulation step").defineInRange("relationDecayRate", 0.5D, 0.0D, 100.0D);
            b.pop();

            b.comment("World generation").push("world");
            worldGenMaxHeight = b.comment("Informational: build height configured by the bundled data pack (dimension_type)").defineInRange("worldGenMaxHeight", 640, 320, 2032);
            b.pop();

            b.comment("Weapons").push("weapons");
            bombsDestroyBlocks = b.define("bombsDestroyBlocks", false);
            spearReturnEnabled = b.define("spearReturnEnabled", true);
            spearChargeEnabled = b.define("spearChargeEnabled", true);
            maceStunEnabled = b.define("maceStunEnabled", true);
            maceAoeEnabled = b.define("maceAoeEnabled", true);
            swordDashEnabled = b.define("swordDashEnabled", true);
            bowTripleShotEnabled = b.define("bowTripleShotEnabled", true);
            crossbowMultiShotEnabled = b.define("crossbowMultiShotEnabled", true);
            customArrowsEnabled = b.define("customArrowsEnabled", true);
            boltsEnabled = b.define("boltsEnabled", true);
            b.pop();
        }
    }

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue showMobNameplates;
        public final ForgeConfigSpec.IntValue nameplateRange;
        public final ForgeConfigSpec.BooleanValue showRpgHud;
        public final ForgeConfigSpec.BooleanValue hideVanillaXpBar;
        public final ForgeConfigSpec.BooleanValue enableCasinoAnimation;

        Client(ForgeConfigSpec.Builder b) {
            b.push("hud");
            showMobNameplates = b.define("showMobNameplates", true);
            nameplateRange = b.defineInRange("nameplateRange", 24, 4, 128);
            showRpgHud = b.define("showRpgHud", true);
            hideVanillaXpBar = b.define("hideVanillaXpBar", true);
            enableCasinoAnimation = b.comment("Client-side toggle for the slot machine animation").define("enableCasinoAnimation", true);
            b.pop();
        }
    }
}
