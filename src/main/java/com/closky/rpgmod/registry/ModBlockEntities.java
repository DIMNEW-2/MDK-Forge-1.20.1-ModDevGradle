package com.closky.rpgmod.registry;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.block.LootChestBlockEntity;
import com.closky.rpgmod.boss.BossSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RpgMod.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<LootChestBlockEntity>> LOOT_CHEST = BLOCK_ENTITIES.register("loot_chest",
            () -> BlockEntityType.Builder.of(LootChestBlockEntity::new,
                    ModBlocks.LOOT_CHEST.get(), ModBlocks.LOOT_CHEST_RARE.get(), ModBlocks.LOOT_CHEST_BOSS.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<BossSpawnerBlockEntity>> BOSS_SPAWNER = BLOCK_ENTITIES.register("boss_spawner",
            () -> BlockEntityType.Builder.of(BossSpawnerBlockEntity::new, ModBlocks.BOSS_SPAWNER.get()).build(null));

    private ModBlockEntities() {
    }
}
