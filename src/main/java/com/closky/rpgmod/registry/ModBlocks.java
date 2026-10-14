package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.block.ChestTier;
import com.closky.rpgmod.block.LootChestBlock;
import com.closky.rpgmod.boss.BossSpawnerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RpgMod.MODID);

    public static final RegistryObject<Block> LOOT_CHEST = BLOCKS.register("loot_chest",
            () -> new LootChestBlock(ChestTier.COMMON, chestProperties(MapColor.WOOD, 2.5F)));
    public static final RegistryObject<Block> LOOT_CHEST_RARE = BLOCKS.register("loot_chest_rare",
            () -> new LootChestBlock(ChestTier.RARE, chestProperties(MapColor.LAPIS, 3.5F)));
    public static final RegistryObject<Block> LOOT_CHEST_BOSS = BLOCKS.register("loot_chest_boss",
            () -> new LootChestBlock(ChestTier.BOSS, chestProperties(MapColor.GOLD, 5.0F)));
    public static final RegistryObject<Block> BOSS_SPAWNER = BLOCKS.register("boss_spawner",
            () -> new BossSpawnerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3600000.0F).sound(SoundType.LODESTONE).lightLevel(state -> 7).noLootTable()));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties chestProperties(MapColor color, float strength) {
        return BlockBehaviour.Properties.of().mapColor(color).strength(strength, 1200.0F).sound(SoundType.WOOD).noOcclusion()
                .lightLevel(state -> state.getValue(LootChestBlock.OPEN) ? 10 : 4);
    }
}
