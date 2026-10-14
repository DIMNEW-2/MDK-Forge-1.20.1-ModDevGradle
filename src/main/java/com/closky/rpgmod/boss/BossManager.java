package com.closky.rpgmod.boss;

import com.closky.rpgmod.RpgMod;
import com.closky.rpgmod.capability.MobRpgData;
import com.closky.rpgmod.capability.PlayerRpgData;
import com.closky.rpgmod.enemy.MobLevel;
import com.closky.rpgmod.player.CharacterLevel;
import com.closky.rpgmod.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Spawns bosses, keeps their boss bars up to date and handles death rewards. Boss bars are server-side objects
 * keyed by the entity UUID; they are rebuilt lazily after a server restart.
 */
public final class BossManager {
    public static final String NBT_BOSS_ID = "rpgmod:boss_id";
    private static final UUID KNOCKBACK_UUID = UUID.fromString("c1a7e2d0-3b4f-4e5a-9d6c-7e8f9a0b0010");
    private static final Map<UUID, ServerBossEvent> BOSS_BARS = new HashMap<>();

    private BossManager() {
    }

    @Nullable
    public static Mob spawn(ServerLevel level, BlockPos pos, BossDefinition definition) {
        Mob mob = definition.getEntityType().create(level);
        if (mob == null) {
            return null;
        }
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null, null);
        definition.equip(mob);
        mob.setCustomName(definition.getDisplayName().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
        mob.getPersistentData().putString(NBT_BOSS_ID, definition.getKey());
        AttributeInstance kb = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            kb.removeModifier(KNOCKBACK_UUID);
            kb.addPermanentModifier(new AttributeModifier(KNOCKBACK_UUID, "rpgmod boss knockback", 0.8D, AttributeModifier.Operation.ADDITION));
        }
        MobRpgData data = MobRpgData.get(mob);
        if (data != null) {
            data.setTitle(definition.getKey());
        }
        MobLevel.setLevel(mob, Math.max(definition.getLevel(), MobLevel.zoneLevel(level, pos)), true);
        level.addFreshEntity(mob);
        level.playSound(null, pos, ModSounds.BOSS_SPAWN.get(), SoundSource.HOSTILE, 1.5F, 0.8F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 40, 0.6D, 1.0D, 0.6D, 0.05D);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(Vec3.atCenterOf(pos)) < 48 * 48) {
                player.displayClientMessage(Component.translatable("message.rpgmod.boss_awakens", definition.getDisplayName()).withStyle(ChatFormatting.DARK_RED), false);
            }
        }
        return mob;
    }

    @Nullable
    public static BossDefinition definitionOf(LivingEntity entity) {
        String key = entity.getPersistentData().getString(NBT_BOSS_ID);
        return key.isEmpty() ? null : BossDefinition.byKey(key);
    }

    public static boolean isBoss(LivingEntity entity) {
        return !entity.getPersistentData().getString(NBT_BOSS_ID).isEmpty();
    }

    /** Called every few ticks for living bosses: keeps the boss bar synced with nearby players. */
    public static void tickBossBar(LivingEntity boss) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        BossDefinition definition = definitionOf(boss);
        if (definition == null) {
            return;
        }
        ServerBossEvent bar = BOSS_BARS.computeIfAbsent(boss.getUUID(), id -> {
            ServerBossEvent event = new ServerBossEvent(definition.getDisplayName(), definition.getBarColor(), BossEvent.BossBarOverlay.NOTCHED_10);
            event.setDarkenScreen(false);
            return event;
        });
        bar.setProgress(boss.getMaxHealth() <= 0 ? 0.0F : boss.getHealth() / boss.getMaxHealth());
        List<ServerPlayer> near = level.getPlayers(p -> p.distanceToSqr(boss) < 64 * 64 && p.isAlive());
        for (ServerPlayer player : bar.getPlayers().toArray(new ServerPlayer[0])) {
            if (!near.contains(player)) {
                bar.removePlayer(player);
            }
        }
        for (ServerPlayer player : near) {
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        }
    }

    public static void removeBossBar(LivingEntity boss) {
        ServerBossEvent bar = BOSS_BARS.remove(boss.getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    public static void clearAll() {
        for (ServerBossEvent bar : BOSS_BARS.values()) {
            bar.removeAllPlayers();
        }
        BOSS_BARS.clear();
    }

    /** Drops the boss loot table and rewards nearby players with character XP. */
    public static void onBossDeath(LivingEntity boss, Collection<ItemEntity> drops, @Nullable Player killer) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        BossDefinition definition = definitionOf(boss);
        if (definition == null) {
            return;
        }
        removeBossBar(boss);
        LootTable table = level.getServer().getLootData().getLootTable(definition.getLootTable());
        LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, boss)
                .withParameter(LootContextParams.ORIGIN, boss.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                .withOptionalParameter(LootContextParams.KILLER_ENTITY, killer)
                .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer);
        if (killer != null) {
            builder.withLuck(killer.getLuck());
        }
        LootParams params = builder.create(LootContextParamSets.ENTITY);
        int zone = Math.max(definition.getLevel(), MobLevel.zoneLevel(level, boss.blockPosition()));
        for (ItemStack stack : table.getRandomItems(params)) {
            com.closky.rpgmod.loot.LootItemHelper.finalizeLoot(stack, zone, level.getRandom());
            drops.add(new ItemEntity(level, boss.getX(), boss.getY() + 0.5D, boss.getZ(), stack));
        }
        int xp = CharacterLevel.xpForBoss(definition.getLevel());
        for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(boss) < 48 * 48)) {
            PlayerRpgData data = PlayerRpgData.get(player);
            if (data != null) {
                data.addXp(player, xp);
                data.sync(player);
            }
            player.displayClientMessage(Component.translatable("message.rpgmod.boss_defeated", definition.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
        }
        RpgMod.LOGGER.debug("Boss {} defeated at {}", definition.getKey(), boss.blockPosition());
    }
}
