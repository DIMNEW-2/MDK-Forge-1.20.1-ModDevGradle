package com.closky.rpgmod.util;

import com.closky.rpgmod.RpgMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    private ModTags() {
    }

    public static final class Items {
        public static final TagKey<Item> BOLTS = tag("bolts");
        public static final TagKey<Item> RPG_WEAPONS = tag("rpg_weapons");
        public static final TagKey<Item> FORGING_MATERIALS = tag("forging_materials");
        public static final TagKey<Item> STAFFS = tag("staffs");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, RpgMod.id(name));
        }
    }

    public static final class Blocks {
        public static final TagKey<Block> LOOT_CHESTS = TagKey.create(Registries.BLOCK, RpgMod.id("loot_chests"));
    }

    public static final class Biomes {
        public static final TagKey<Biome> HAS_SURFACE_DUNGEONS = TagKey.create(Registries.BIOME, RpgMod.id("has_structure/surface_dungeons"));
        public static final TagKey<Biome> HAS_UNDERGROUND_DUNGEONS = TagKey.create(Registries.BIOME, RpgMod.id("has_structure/underground_dungeons"));
    }
}
