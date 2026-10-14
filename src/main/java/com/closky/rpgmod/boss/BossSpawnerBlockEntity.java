package com.closky.rpgmod.boss;

import com.closky.rpgmod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BossSpawnerBlockEntity extends BlockEntity {
    public static final double TRIGGER_RANGE = 10.0D;

    private String bossId = BossDefinition.SKELETON_KING.getKey();
    private int cooldown;

    public BossSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOSS_SPAWNER.get(), pos, state);
    }

    public String getBossId() {
        return bossId;
    }

    public void setBossId(String bossId) {
        this.bossId = bossId;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BossSpawnerBlockEntity spawner) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (spawner.cooldown-- > 0) {
            return;
        }
        spawner.cooldown = 20;
        Player player = server.getNearestPlayer(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, TRIGGER_RANGE, false);
        if (player == null) {
            return;
        }
        BossDefinition definition = BossDefinition.byKey(spawner.bossId);
        if (definition == null) {
            definition = BossDefinition.SKELETON_KING;
        }
        BlockPos spawnPos = pos.above();
        if (!server.getBlockState(spawnPos).isAir()) {
            spawnPos = pos;
        }
        server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        BossManager.spawn(server, spawnPos, definition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Boss", bossId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Boss")) {
            bossId = tag.getString("Boss");
        }
    }
}
