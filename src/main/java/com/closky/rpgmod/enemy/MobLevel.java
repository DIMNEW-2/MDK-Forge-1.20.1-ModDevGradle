package com.closky.rpgmod.enemy;

import com.closky.rpgmod.capability.MobRpgData;
import com.closky.rpgmod.config.RpgConfig;
import com.closky.rpgmod.network.ModNetwork;
import com.closky.rpgmod.network.packet.SyncMobLevelPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Mob level logic (section 8). Levels depend on the distance from the world spawn and the dimension; the level
 * scales health and damage through attribute modifiers and is shown on the nameplate.
 */
public final class MobLevel {
    public static final UUID HEALTH_UUID = UUID.fromString("c1a7e2d0-3b4f-4e5a-9d6c-7e8f9a0b0001");
    public static final UUID DAMAGE_UUID = UUID.fromString("c1a7e2d0-3b4f-4e5a-9d6c-7e8f9a0b0002");
    public static final UUID BOSS_HEALTH_UUID = UUID.fromString("c1a7e2d0-3b4f-4e5a-9d6c-7e8f9a0b0003");
    public static final UUID BOSS_DAMAGE_UUID = UUID.fromString("c1a7e2d0-3b4f-4e5a-9d6c-7e8f9a0b0004");

    private MobLevel() {
    }

    /** Base level of a location: 1 + distance / blocksPerMobLevel, with dimension offsets. */
    public static int zoneLevel(Level level, BlockPos pos) {
        int perLevel = Math.max(16, RpgConfig.COMMON.blocksPerMobLevel.get());
        BlockPos spawn = level.getSharedSpawnPos();
        double dx = pos.getX() - spawn.getX();
        double dz = pos.getZ() - spawn.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        int zone = 1 + (int) (distance / perLevel);
        if (level.dimension() == Level.NETHER) {
            zone += 8;
        } else if (level.dimension() == Level.END) {
            zone += 16;
        }
        // Deep underground is more dangerous (Tectonic-like deep caves)
        if (pos.getY() < 0) {
            zone += Math.min(6, (-pos.getY()) / 16);
        }
        return Mth.clamp(zone, 1, RpgConfig.COMMON.maxMobLevel.get());
    }

    /** Assigns a level to a freshly spawned mob (no-op if already initialised). */
    public static void initialize(Mob mob) {
        if (!(mob.level() instanceof ServerLevel server) || !RpgConfig.COMMON.mobLevelScaling.get()) {
            return;
        }
        MobRpgData data = MobRpgData.get(mob);
        if (data == null || data.isInitialized()) {
            return;
        }
        int zone = zoneLevel(server, mob.blockPosition());
        int variance = server.getRandom().nextInt(5) - 2;
        int level = Mth.clamp(zone + variance, 1, RpgConfig.COMMON.maxMobLevel.get());
        if (!(mob instanceof Enemy)) {
            level = Math.max(1, level / 2);
        }
        setLevel(mob, level, data.isBoss());
    }

    /** Sets the level, applies modifiers and syncs to tracking clients. */
    public static void setLevel(LivingEntity entity, int level, boolean boss) {
        MobRpgData data = MobRpgData.get(entity);
        if (data == null) {
            return;
        }
        data.setLevel(level);
        data.setBoss(boss);
        entity.getPersistentData().putInt(MobRpgData.NBT_LEVEL, level);
        entity.getPersistentData().putBoolean(MobRpgData.NBT_BOSS, boss);
        applyModifiers(entity, level, boss);
        sync(entity);
    }

    public static void applyModifiers(LivingEntity entity, int level, boolean boss) {
        float healthPct = entity.getMaxHealth() <= 0 ? 1.0F : entity.getHealth() / entity.getMaxHealth();
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance damage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        double perLevel = 0.08D * (level - 1);
        if (health != null) {
            health.removeModifier(HEALTH_UUID);
            health.removeModifier(BOSS_HEALTH_UUID);
            if (perLevel > 0) {
                health.addPermanentModifier(new AttributeModifier(HEALTH_UUID, "rpgmod level health", perLevel, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            if (boss) {
                health.addPermanentModifier(new AttributeModifier(BOSS_HEALTH_UUID, "rpgmod boss health", 4.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        if (damage != null) {
            damage.removeModifier(DAMAGE_UUID);
            damage.removeModifier(BOSS_DAMAGE_UUID);
            if (perLevel > 0) {
                damage.addPermanentModifier(new AttributeModifier(DAMAGE_UUID, "rpgmod level damage", perLevel * 0.6D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            if (boss) {
                damage.addPermanentModifier(new AttributeModifier(BOSS_DAMAGE_UUID, "rpgmod boss damage", 0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        entity.setHealth(Math.max(1.0F, entity.getMaxHealth() * healthPct));
    }

    public static int getLevel(LivingEntity entity) {
        MobRpgData data = MobRpgData.get(entity);
        if (data != null && data.isInitialized()) {
            return data.getLevel();
        }
        return entity.getPersistentData().getInt(MobRpgData.NBT_LEVEL);
    }

    public static boolean isBoss(LivingEntity entity) {
        MobRpgData data = MobRpgData.get(entity);
        return data != null ? data.isBoss() : entity.getPersistentData().getBoolean(MobRpgData.NBT_BOSS);
    }

    public static void sync(LivingEntity entity) {
        MobRpgData data = MobRpgData.get(entity);
        if (data == null || entity.level().isClientSide) {
            return;
        }
        ModNetwork.sendToTracking(entity, new SyncMobLevelPacket(entity.getId(), data.getLevel(), data.isBoss(), data.getTitle()));
    }

    public static void syncTo(LivingEntity entity, ServerPlayer player) {
        MobRpgData data = MobRpgData.get(entity);
        if (data == null || !data.isInitialized()) {
            return;
        }
        ModNetwork.sendToPlayer(player, new SyncMobLevelPacket(entity.getId(), data.getLevel(), data.isBoss(), data.getTitle()));
    }

    /**
     * Damage multiplier for a player attacking a mob {@code diff} levels above them (section 8.3):
     * -5% per level above 3, capped by config.
     */
    public static float playerDamageMultiplier(int playerLevel, int mobLevel) {
        int diff = mobLevel - playerLevel;
        if (diff <= 3) {
            return 1.0F;
        }
        double cap = RpgConfig.COMMON.damageReductionCap.get();
        return (float) Math.max(1.0D - cap, 1.0D - (diff - 3) * 0.05D);
    }
}
